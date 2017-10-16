/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
package bugpicker
package ui

import scala.language.existentials

import java.io.File
import java.io.FilenameFilter
import java.net.URL

import scala.collection.mutable
import org.opalj.ai.BaseAI
import org.opalj.ai.Domain
import org.opalj.ai.common.DomainRegistry
import org.opalj.ai.common.XHTML
import org.opalj.br._
import org.opalj.br.analyses.Project
import org.opalj.bugpicker.ui.codeview.JumpToProblemListener
import org.opalj.bugpicker.ui.codeview.SourceFileWrapper
import org.opalj.bugpicker.ui.dialogs.ChooseDomainDialog
import org.opalj.bugpicker.ui.dialogs.DialogStage
import org.opalj.bugpicker.ui.dialogs.WebViewStage

import scalafx.Includes._
import scalafx.collections.ObservableBuffer
import scalafx.event.ActionEvent
import scalafx.scene.control.ContextMenu
import scalafx.scene.control.MenuItem
import scalafx.scene.control.TreeView
import scalafx.scene.image.Image
import scalafx.scene.image.ImageView
import scalafx.scene.layout.Priority
import scalafx.scene.web.WebView
import scalafx.stage.Stage
import org.opalj.ai.domain.RecordDefUse
import org.opalj.tac.TACAI
import org.opalj.tac.ToTxt
import org.opalj.br.analyses.PropertyStoreKey
import org.opalj.fpcf.PropertyKey

/**
 * Given a TreeView and a Project[URL] creates a visual representation of the project structure
 *
 * @author David Becker
 */
class ProjectExplorer(
        tv:    TreeView[ProjectExplorerData],
        stage: Stage
) {

    private[this] val projectPackageStructure: mutable.Map[String, ProjectExplorerTreeItem] = mutable.Map.empty
    private[this] val libraryPackageStructure: mutable.Map[String, ProjectExplorerTreeItem] = mutable.Map.empty
    private[this] val projectClassFileStructure: mutable.Map[ProjectExplorerTreeItem, ObservableBuffer[ClassFile]] = mutable.Map.empty
    private[this] val libraryClassFileStructure: mutable.Map[ProjectExplorerTreeItem, ObservableBuffer[ClassFile]] = mutable.Map.empty
    private[this] var project: Project[URL] = null
    private[this] var sources: Seq[File] = Seq.empty

    tv.cellFactory = (v: TreeView[ProjectExplorerData]) ⇒ new javafx.scene.control.TreeCell[ProjectExplorerData] {
        override def updateItem(item: ProjectExplorerData, empty: Boolean) = {
            super.updateItem(item, empty)
            if (!empty) {
                item match {
                    case c: ProjectExplorerClassData ⇒ {
                        val classFile: ClassFile = c.classFile
                        val sc = new MenuItem("Show Source code") {
                            onAction = showSourceCode(classFile, None)
                        }
                        val bc = new MenuItem("Show Bytecode") {
                            onAction = showBytecode(classFile, None)
                        }
                        val tac = new MenuItem("Show 3-Address Code") {
                            onAction = showTAC(classFile, None)
                        }
                        val prop = new MenuItem("Show Properties") {
                            onAction = showProperties(classFile, None)
                        }
                        setContextMenu(new ContextMenu(sc, bc, tac, prop))
                    }
                    case m: ProjectExplorerMethodData ⇒ {
                        val method: Method = m.method
                        val classFile: ClassFile = m.classFile
                        val sc = new MenuItem("Show Source code") {
                            onAction = showSourceCode(classFile, Some(method))
                        }
                        val bc = new MenuItem("Show Bytecode") {
                            onAction = showBytecode(classFile, Some(method))
                        }
                        val tac = new MenuItem("Show 3-Address Code") {
                            onAction = showTAC(classFile, Some(method))
                        }
                        val prop = new MenuItem("Show Properties") {
                            onAction = showProperties(classFile, Some(method))
                        }
                        setContextMenu(if (!m.isAbstract.apply)
                            new ContextMenu(
                            sc,
                            bc,
                            tac,
                            prop,
                            new MenuItem("Run abstract interpretation") {
                                onAction = { e: ActionEvent ⇒
                                    val dia = new ChooseDomainDialog
                                    val chosenDomain = dia.show(stage)
                                    if (chosenDomain.isDefined) {
                                        runAbstractInterpretation(
                                            chosenDomain.get,
                                            classFile,
                                            method
                                        )
                                    }
                                }
                            }
                        )
                        else
                            new ContextMenu(sc, bc))
                    }
                    case f: ProjectExplorerFieldData ⇒ {
                        val field = f.field
                        val classFile = f.classFile
                        val prop = new MenuItem("Show Properties") {
                            onAction = showProperties(classFile, Some(field))
                        }

                        setContextMenu(
                            new ContextMenu(prop)
                        )
                    }
                    case _ ⇒
                }
                setText(item.name.apply)
                setGraphic(getTreeItem.getGraphic)
            } else {
                setContextMenu(null)
                setText(null)
                setGraphic(null)
            }
        }
    }

    def buildProjectExplorer(
        project:     Project[URL],
        sources:     Seq[File],
        projectName: String
    ): Unit = {
        this.project = project
        this.sources = sources
        val root = new ProjectExplorerTreeItem(
            ProjectExplorerData(projectName),
            new ImageView {
                image = ProjectExplorer.getImage("/org/opalj/bugpicker/ui/explorer/folder.gif")
            },
            ObservableBuffer(
                new ProjectExplorerTreeItem(
                    ProjectExplorerData("Project Files"),
                    new ImageView {
                        image = ProjectExplorer.getImage("/org/opalj/bugpicker/ui/explorer/folder.gif")
                    },
                    createPackageStructure(
                        project.projectPackages.toSeq.sorted,
                        projectPackageStructure,
                        projectClassFileStructure
                    )
                )
            )
        )
        root.expanded = true
        if (project.libraryClassFilesCount > 0) {
            root.children ++ Seq(new ProjectExplorerTreeItem(
                ProjectExplorerData("Library Files"),
                new ImageView {
                    image = ProjectExplorer.getImage("/org/opalj/bugpicker/ui/explorer/folder.gif")
                },
                createPackageStructure(
                    project.libraryPackages.toBuffer.sorted,
                    libraryPackageStructure,
                    libraryClassFileStructure
                )
            ))
            initializeClassFileStructure(
                project.allLibraryClassFiles,
                libraryPackageStructure,
                libraryClassFileStructure
            )
        }
        initializeClassFileStructure(
            project.allProjectClassFiles,
            projectPackageStructure,
            projectClassFileStructure
        )

        tv.root = root
    }

    def reset(projectName: String = ""): Unit = {
        projectPackageStructure.clear()
        libraryPackageStructure.clear()
        projectClassFileStructure.clear()
        libraryClassFileStructure.clear()
        tv.root = new ProjectExplorerTreeItem(
            ProjectExplorerData(projectName),
            new ImageView {
                image = ProjectExplorer.getImage("/org/opalj/bugpicker/ui/explorer/folder.gif")
            }
        )
    }

    private def createPackageStructure(
        packages:           Seq[String],
        packageStructure:   scala.collection.mutable.Map[String, ProjectExplorerTreeItem],
        classFileStructure: scala.collection.mutable.Map[ProjectExplorerTreeItem, ObservableBuffer[ClassFile]]
    ): ObservableBuffer[ProjectExplorerTreeItem] = {
        for (p ← ObservableBuffer.apply(packages)) yield {
            val node = new ProjectExplorerTreeItem(
                if (p == "")
                    ProjectExplorerPackageData(
                    "(default package)"
                )
                else
                    ProjectExplorerPackageData(
                        p.replace('/', '.')
                    ),
                new ImageView {
                    image = ProjectExplorer.getImage("/org/opalj/bugpicker/ui/explorer/package.gif")
                },
                ObservableBuffer.empty,
                classFileStructure
            )
            packageStructure += (node.value.apply.name.apply → node)
            node
        }
    }

    private def initializeClassFileStructure(
        classFiles:         Iterable[ClassFile],
        packageStructure:   scala.collection.mutable.Map[String, ProjectExplorerTreeItem],
        classFileStructure: scala.collection.mutable.Map[ProjectExplorerTreeItem, ObservableBuffer[ClassFile]]
    ): Unit = {
        for (classFile ← classFiles) {
            val objectType = classFile.thisType
            // get the ProjectExplorerTreeItem representing the Package of the ClassFile
            val packageName = if (objectType.packageName == "") "(default package)"
            else objectType.packageName.replace('/', '.')
            val packageNode = packageStructure(packageName)
            // add a link from the ProjectExplorerTreeItem to all ClassFiles belonging to it
            classFileStructure += (packageNode → (classFileStructure.getOrElse(packageNode, ObservableBuffer.empty) :+ classFile))
        }
    }

    private def runAbstractInterpretation(
        chosenDomain: String,
        classFile:    ClassFile,
        method:       Method
    ): Unit = {
        // WebView for AI result, independent of domain
        val aiDefaultView: WebView = new WebView {
            contextMenuEnabled = false
            vgrow = Priority.Always
            hgrow = Priority.Always
            engine.loadContent(Messages.ABSTRACT_INTERPRETATION_RUNNING)
        }
        val domainName: String = chosenDomain.split(']')(0).drop(1)
        WebViewStage.showWebView(
            s"Result of Abstract Interpretation[$domainName]: "+classFile.fqn + s"{ $method }",
            aiDefaultView, 125d, 50d
        )
        val domain: Domain = DomainRegistry.newDomain(
            chosenDomain,
            project,
            method
        )
        val aiResult = BaseAI(method, domain)
        val aiResultXHTML =
            XHTML.dump(
                classFile, method, "Result of Abstract Interpretation", aiResult
            ).toString
        aiDefaultView.engine.loadContent(aiResultXHTML.toString())
        // open additional WebViews depending on domain
        if (domain.isInstanceOf[RecordDefUse]) {
            val defUse = domain.asInstanceOf[RecordDefUse]
            val aiDefUseView: WebView = new WebView {
                contextMenuEnabled = false
                vgrow = Priority.Always
                hgrow = Priority.Always
                engine.loadContent(defUse.dumpDefUseInfo().toString())
            }
            WebViewStage.showWebView(
                "DefUse Info: "+classFile.fqn + s"{ $method }",
                aiDefUseView, 175d, 100d
            )
        }
    }

    private def showSourceCode(
        cf:     ClassFile,
        method: Option[Method]
    ): ActionEvent ⇒ Unit = { e: ActionEvent ⇒
        val sourceView: WebView = new WebView {
            contextMenuEnabled = false
            vgrow = Priority.Always
            hgrow = Priority.Always
        }
        val methodId: Option[String] =
            method.map { method ⇒ method.name + method.descriptor.toJVMDescriptor }

        val firstLineOfMethod: Option[String] =
            method.flatMap(_.body.flatMap(_.firstLineNumber.map { ln ⇒
                (if (ln > 2) (ln - 2) else 0).toString
            }))
        val sourceFile = findSourceFile(cf, firstLineOfMethod)
        if (!sourceFile.isDefined) {
            val fqn = cf.fqn
            val msg = s"Could not find source code for type $fqn."
            DialogStage.showMessage("Info", msg, stage)
        } else {
            sourceView.engine.loadContent(sourceFile.get.toXHTML.toString)

            new JumpToProblemListener(
                webview = sourceView,
                methodOption = methodId,
                pcOption = None,
                lineOption = firstLineOfMethod
            )

            val methodString = if (methodId.isDefined) ("{ "+methodId.get+" }") else ""
            WebViewStage.showWebView(
                "SourceCode for "+cf.fqn + methodString,
                sourceView, 125d, 50d
            )
        }
    }

    private def showBytecode(
        cf:     ClassFile,
        method: Option[Method]
    ): ActionEvent ⇒ Unit = { e: ActionEvent ⇒
        val byteView: WebView = new WebView {
            contextMenuEnabled = false
            vgrow = Priority.Always
            hgrow = Priority.Always
        }
        val methodId: Option[String] =
            method.map { method ⇒ method.name + method.descriptor.toJVMDescriptor }
        val classFile = decompileClassFile(project, cf.thisType)

        val content = classFile.map(_.toXHTML(project.source(cf)).toString).getOrElse(Messages.NO_BYTECODE_FOUND)
        byteView.engine.loadContent(content)

        new JumpToProblemListener(
            webview = byteView,
            methodOption = methodId,
            pcOption = None, lineOption = None
        )

        val title = "Byte Code for "+method.map(_.toJava).getOrElse(cf.thisType.toJava)
        WebViewStage.showWebView(title, byteView, 175d, 100d)
    }

    private def showTAC(
        cf:     ClassFile,
        method: Option[Method]
    ): ActionEvent ⇒ Unit = { e: ActionEvent ⇒
        val tacView: WebView = new WebView {
            contextMenuEnabled = false
            vgrow = Priority.Always
            hgrow = Priority.Always
        }

        if (method.isDefined && method.get.body.isEmpty) {
            tacView.engine.loadContent(Messages.METHOD_WITHOUT_BODY)
        } else {
            val methods = method.map(Seq(_)).getOrElse(cf.methods.filter(_.body.isDefined))
            tacView.engine.loadContent(
                s"class <b>${cf.thisType.toJava}</b> {<br/>"+
                    methods.
                    map { m ⇒
                        val method = scala.xml.Text(m.toJava)
                        s"<i>${method.toString}</i><pre>{\n${
                            val tac = TACAI(project, m)()
                            scala.xml.Text(ToTxt(tac).mkString("\n")).toString
                        }\n}</pre>"
                    }.
                    mkString("<br/>")+
                    "\n}"
            )
        }
        val title = "3-Address Code for "+method.map(_.toJava).getOrElse(cf.thisType.toJava)
        WebViewStage.showWebView(title, tacView, 175d, 100d)
    }

    private def showProperties(
        cf:          ClassFile,
        classMember: Option[ClassMember]
    ): ActionEvent ⇒ Unit = { e: ActionEvent ⇒
        val propView: WebView = new WebView {
            contextMenuEnabled = false
            vgrow = Priority.Always
            hgrow = Priority.Always
        }

        val propertyStore = project.get(PropertyStoreKey)
        val properties = propertyStore.properties(classMember.getOrElse(cf))

        if (properties.isEmpty) {
            propView.engine.loadContent(Messages.NO_PROPERTIES_FOUND)
        } else {
            val header = classMember match {
                case Some(method: Method) ⇒
                    val methodAsJava = scala.xml.Text(method.toJava)
                    s"class <b>${cf.thisType.toJava}</b> <i>${methodAsJava}</i>{<br/>"
                case Some(field: Field) ⇒
                    val fieldAsJava = scala.xml.Text(field.toJava)
                    s"class <b>${cf.thisType.toJava}</b> <i>${fieldAsJava}</i>{<br/>"
                case _ /* In case of a classFile*/ ⇒ s"class <b>${cf.thisType.toJava}</b>{<br/>"
            }

            val pageContent = new StringBuilder(header).append("<ul>")
            val lineSep = System.getProperty("line.separator")

            properties.foreach { p ⇒
                val property = scala.xml.Text(p.toString())
                pageContent.append(s" <li> <b> ${PropertyKey.name(p.key.id)}:</b> ${property.toString()} </li>")
                pageContent.append(lineSep)
            }

            pageContent.append(s"</ul>$lineSep}")
            propView.engine.loadContent(pageContent.toString)
        }

        val title = "Computed properties for "+classMember.collectFirst {
            case m: Method ⇒ m.toJava
            case f: Field  ⇒ f.toJava
        }.getOrElse(cf.thisType.toJava)

        WebViewStage.showWebView(title, propView, 175d, 100d)
    }

    private def findSourceFile(
        classFile:  ClassFile,
        lineOption: Option[String]
    ): Option[SourceFileWrapper] = {

        val theType: ObjectType = classFile.thisType
        val sourcePackagePath = theType.packageName

        val sourceFile: Option[File] =
            if (classFile.sourceFile.isDefined) {
                sources.toStream.map(dir ⇒
                    new File(dir, sourcePackagePath+"/"+classFile.sourceFile.get)).find(_.exists())
            } else {
                val name = theType.simpleName
                val packageDir =
                    sources.toStream.map(dir ⇒
                        new File(dir, sourcePackagePath)).find(_.exists())
                packageDir.map(_.listFiles(new FilenameFilter {
                    override def accept(file: File, filename: String): Boolean =
                        filename.matches("^"+name+"\\.\\w+$")
                })(0))
            }

        if (sourceFile.isDefined && sourceFile.get.exists) {
            val wrapper = new SourceFileWrapper(sourceFile.get, lineOption.getOrElse(""))
            Some(wrapper)
        } else {
            None
        }
    }

    private def decompileClassFile(
        project: Project[URL],
        theType: ObjectType
    ): Option[org.opalj.da.ClassFile] = {
        project.source(theType).map { url ⇒
            val inStream = url.openStream
            val cf = org.opalj.da.ClassFileReader.ClassFile(() ⇒ inStream)
            inStream.close
            cf.head
        }
    }
}

object ProjectExplorer {

    private[this] val imageCache: mutable.Map[String, Image] = mutable.Map(
        "fallback" → new Image("/org/opalj/bugpicker/ui/explorer/error.gif")
    )

    def getImage(path: String): Image = {
        imageCache.getOrElse(path, {
            try {
                val img = new Image(path, true)
                imageCache += (path → img)
                img
            } catch {
                case e: Exception ⇒ imageCache("fallback")
            }
        })
    }
}
