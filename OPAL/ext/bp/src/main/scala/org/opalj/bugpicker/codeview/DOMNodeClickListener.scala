/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package codeview

import java.io.File
import java.io.FilenameFilter
import java.net.URL
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.bugpicker.dialogs.DialogStage
import org.opalj.da.ClassFile
import org.w3c.dom.Node
import org.w3c.dom.events.EventListener
import scalafx.Includes._
import scalafx.scene.web.WebView

class DOMNodeClickListener(
        project: Project[URL],
        sources: Seq[File],
        node: Node,
        bytecodeWebview: WebView,
        sourceWebview: WebView,
        focus: WebView ⇒ Unit) extends EventListener {

    private val nodeAttributes = node.getAttributes

    private def getAttribute(name: String): Option[String] =
        if (nodeAttributes.getNamedItem(name) != null)
            Some(nodeAttributes.getNamedItem(name).getTextContent)
        else
            None

    def findSourceFile(
        theType: ObjectType,
        lineOption: Option[String]): Option[SourceFileWrapper] = {

        val classFile = project.classFile(theType)
        if (!classFile.isDefined) return None

        val cf = classFile.get

        val sourceFileName = cf.sourceFile.getOrElse(theType.simpleName)
        val sourcePackagePath = theType.packageName

        val sourceFile: Option[File] =
            if (cf.sourceFile.isDefined) {
                sources.toStream.map(dir ⇒ new File(dir, sourcePackagePath+"/"+cf.sourceFile.get)).find(_.exists())
            } else {
                val name = theType.simpleName
                val packageDir = sources.toStream.map(dir ⇒ new File(dir, sourcePackagePath)).find(_.exists())
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

    def decompileClassFile(project: Project[URL], theType: ObjectType): Option[ClassFile] = {
        project.source(theType).map { url ⇒
            val inStream = url.openStream
            val cf = org.opalj.da.ClassFileReader.ClassFile(() ⇒ inStream)
            inStream.close
            cf.head
        }
    }

    override def handleEvent(event: org.w3c.dom.events.Event) {
        val className = getAttribute("data-class").get
        val sourceType = ObjectType(className)
        val methodOption = getAttribute("data-method")
        val pcOption = getAttribute("data-pc")
        val lineOption = getAttribute("data-line")
        val (showBytecode, showSource) = getAttribute("data-show") match {
            case Some("bytecode")   ⇒ (true, false)
            case Some("sourcecode") ⇒ (false, true)
            case _                  ⇒ (true, sources.nonEmpty)
        }

        var noSourceFound = false

        val sourceFile = findSourceFile(sourceType, lineOption)
        if (!sourceFile.isDefined && showSource) {
            noSourceFound = true
            val msg = s"Could not find source code for type $className.\nShowing bytecode instead."
            DialogStage.showMessage("Info", msg, sourceWebview.scene().window())
            sourceWebview.engine.loadContent("")
        } else if (sourceFile.isDefined) {
            sourceWebview.engine.loadContent(sourceFile.get.toXHTML.toString)
            new JumpToProblemListener(webview = sourceWebview, methodOption = methodOption, pcOption = None, lineOption = lineOption)
            focus(sourceWebview)
        }

        val classFile = decompileClassFile(project, sourceType)
        if (classFile.isDefined)
            bytecodeWebview.engine.loadContent(classFile.get.toXHTML.toString)
        else
            bytecodeWebview.engine.loadContent(Messages.NO_BYTECODE_FOUND)
        new JumpToProblemListener(webview = bytecodeWebview, methodOption = methodOption, pcOption = pcOption, lineOption = None)
        if (noSourceFound || showBytecode && !showSource) focus(bytecodeWebview)
    }
}
