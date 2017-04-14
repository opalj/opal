/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package explorer

import java.util.prefs.Preferences
import java.io.FileInputStream
import java.io.File

import org.w3c.dom.Element
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventListener
import org.w3c.dom.events.EventTarget

import javafx.concurrent.Worker.State
//import javafx.event.EventHandler
//import javafx.beans.value.ChangeListener
//import javafx.beans.value.ObservableValue
import scalafx.application.JFXApp
import scalafx.application.JFXApp.PrimaryStage
//import scalafx.geometry.Insets
import scalafx.Includes._
import scalafx.scene.Scene
import scalafx.stage.DirectoryChooser
import scalafx.stage.FileChooser
import scalafx.stage.FileChooser.ExtensionFilter
// /import scalafx.scene.control.Label
import scalafx.scene.control.MenuBar
import scalafx.scene.control.Menu
import scalafx.scene.control.MenuItem
import scalafx.scene.layout.BorderPane
import scalafx.scene.web.WebView
//import scalafx.scene.web.WebEvent
import scalafx.scene.web.WebEngine
import javafx.scene.input.KeyCombination

import org.opalj.da.ClassFileReader.ClassFile

/**
 *
 * @author Michael Eichberg
 */
object Explorer extends JFXApp {

    // We store the last opened file to reopen it.
    val preferences = Preferences.userNodeForPackage(this.getClass)
    final val LastClassFileKey = "last-class-file"

    var webEngine: WebEngine = null

    stage = new PrimaryStage {
        scene = new Scene {
            title = "OPAL Bytecode Explorer"
            root = new BorderPane {
                top = new MenuBar {
                    useSystemMenuBar = true
                    minWidth = 100
                    menus = Seq(new Menu("File") { items = createFileMenuItems() })
                }
                center = createWebViewForBytecode()
            }
        }
    }

    def createFileMenuItems(): List[MenuItem] = {
        List(
            new MenuItem("Open Folder...") {
                //accelerator = KeyCombination.keyCombination("Ctrl +O")
                onAction = handle {
                    val folderChooser = new DirectoryChooser {
                        title = "Open Project Folder"
                    }
                    val selectedFile = folderChooser.showDialog(stage)
                    if (selectedFile != null) {
                        //    stage.display(selectedFile)
                    }
                }
            },
            new MenuItem("Open Class File...") {
                accelerator = KeyCombination.keyCombination("Ctrl +O")
                onAction = handle {
                    val fileChooser = new FileChooser {
                        title = "Open Class File"
                        extensionFilters ++= Seq(new ExtensionFilter("Class Files", "*.class"))
                    }
                    val selectedFile = fileChooser.showOpenDialog(stage)
                    if (selectedFile != null) {
                        preferences.put(LastClassFileKey, selectedFile.getAbsoluteFile.toString)
                        preferences.flush()
                        webEngine.loadContent(loadClassFile(selectedFile).toXHTML().toString)
                    }
                }
            }
        )
    }

    def loadClassFile(file: File): ClassFile = {
        val classFileInputStreamCreator = () ⇒ new FileInputStream(file)
        ClassFile(classFileInputStreamCreator).head
    }

    def createWebViewForBytecode(): WebView = {
        // create initial/restore previous content
        var file: File = null
        val (lastClassFileAsXHTML: String, classFile: Option[ClassFile]) = {
            try {
                file = new File(preferences.get(LastClassFileKey, null))
                if (file == null || !file.exists || !file.isFile) {
                    file = null
                    ("<b>Load new class file.</b>", None)
                } else {
                    val classFile = loadClassFile(file)
                    (classFile.toXHTML().toString, Some(classFile))
                }
            } catch {
                case t: Throwable ⇒
                    (
                        s"<b>Failed loading previous class file: $file</b><br>Load new class file.",
                        None
                    )

            }
        }

        val typeClicklistener = new EventListener() {
            def handleEvent(event: Event): Unit = { println("cool...") }
        }

        val methodClicklistener = new EventListener() {
            def handleEvent(event: Event): Unit = { println("cool method...") }
        }

        val browser = new WebView { contextMenuEnabled = false }
        webEngine = browser.getEngine

        webEngine.getLoadWorker().stateProperty onChange { (_, _, newState: State) ⇒
            if (newState == State.SUCCEEDED) {
                if (file != null) stage.setTitle(file.toString)

                def attachClickListeners(element: Element): Unit = {
                    val childNodes = element.getChildNodes();
                    var i = 0
                    while (i < childNodes.getLength()) {
                        val item = childNodes.item(i);
                        item match {
                            case e: (Element with EventTarget) ⇒
                                {
                                    val attribute = e.getAttribute("class")
                                    if (attribute != null && attribute.contains("type")) {
                                        println(e.getFirstChild.getNodeValue)
                                        e.addEventListener("click", typeClicklistener, false)
                                    }
                                }

                                // TODO ... select the right target!
                                {
                                    val attribute = e.getAttribute("class")
                                    if (attribute != null && attribute.contains("method")) {
                                        val name = e.getFirstChild.getNodeValue
                                        println(name)
                                        classFile.get.methods.filter(m ⇒ m.name(classFile.get.constant_pool) == name)
                                        // ...
                                        e.addEventListener("click", methodClicklistener, false)
                                    }
                                }

                                attachClickListeners(e)

                            case _ ⇒ // nothing to do
                        }
                        i += 1
                    }
                }
                val document = webEngine.getDocument().getDocumentElement()
                attachClickListeners(document)
            }
        }

        webEngine.loadContent(lastClassFileAsXHTML)

        browser
    }
}
