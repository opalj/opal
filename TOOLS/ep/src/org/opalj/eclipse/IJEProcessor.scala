/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
package org.opalj.eclipse

import java.io.File
import org.eclipse.core.commands.ExecutionEvent
import org.eclipse.core.runtime.IPath
import org.eclipse.core.runtime.Path
import org.eclipse.jdt.core.ICompilationUnit
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jface.dialogs.MessageDialog
import org.eclipse.swt.widgets.Shell
import org.eclipse.ui.IWorkbenchPage
import org.eclipse.ui.handlers.HandlerUtil
import org.opalj.da.ClassFileReader
import org.opalj.da.ClassFileReader.AllClassFiles
import org.opalj.da.ClassFileReader.ClassFile
import org.opalj.da.ClassFileReader.ClassFiles
import org.opalj.da.Constant_Pool_Entry
import org.opalj.eclipse.views.DisassemblerView
import org.eclipse.core.runtime.Status

/**
 * Finds the bytecode to different types of IJavaElements and displays it in DisassemblerViews.
 *
 * @author Lukas Becker
 * @author Simon Bohlender
 * @author Simon Guendling
 * @author Felix Zoeller
 */
class IJEProcessor {
    def process(event: ExecutionEvent, ije: IJavaElement): Unit = {
        val shell = HandlerUtil.getActiveShell(event)
        val page = HandlerUtil.getActiveWorkbenchWindow(event).getActivePage
        ije.getElementName.split('.')(1) match {
            case "class" ⇒
                processClassIJE(page, ije)
            case "jar" ⇒
                processJarIJE(shell, page, ije)
            case "java" ⇒
                processSourceIJE(shell, page, ije, "java")
            case "scala" ⇒
                processSourceIJE(shell, page, ije, "scala")
            case _ ⇒ // TODO: Error
        }
    }

    def processClassFile(activePage: IWorkbenchPage, classFile: ClassFile): Unit = {
        val viewID: String = DisassemblerView.Id
        val file = getClass.getResourceAsStream("style.css")
        val css = scala.io.Source.fromInputStream(file).mkString
        val html: String = classFile.toXHTML(css).toString
        activePage.showView(viewID, classFile.toString(), IWorkbenchPage.VIEW_ACTIVATE) match {
            case dv: DisassemblerView ⇒
                val cpe: Constant_Pool_Entry = classFile.constant_pool(classFile.this_class)
                dv.setPartName(cpe.toString(classFile.constant_pool).split('.').last)
                dv.setText(html)
            case _ ⇒
        }
    }

    def processClassIJE(activePage: IWorkbenchPage, ije: IJavaElement): Unit = {
        val jarPath: String = ije.getPath.toOSString
        val packageName: String = ije.getJavaProject match {
            case icu: ICompilationUnit ⇒
                icu.getPackageDeclarations.head.getElementName.replace(".", "/")+"/"
            case _ ⇒
                ije.getParent.toString.split(" ")(0).replace(".", "/")+"/"
        }
        val classFileName: String = packageName + ije.getElementName
        val classFile: ClassFile = ClassFile(jarPath, classFileName).head
        processClassFile(activePage, classFile)
    }

    def processJarIJE(shell: Shell, page: IWorkbenchPage, ije: IJavaElement): Unit = {
        val jarPath: String = ije.getPath.toOSString
        val classFiles = ClassFiles(new java.io.File(jarPath)).map(_._1)
        classFiles.length match {
            case n if n <= 0 ⇒
                MessageDialog.openError(shell, "Error", s"No classfiles found in $jarPath")
            case n if 1 to 5 contains n ⇒
                classFiles.foreach { processClassFile(page, _) }
            case n ⇒
                val msg: String = s"This will open $n new tabs. Continue?"
                val confirm: Boolean = MessageDialog.openConfirm(shell, "Warning", msg)
                if (confirm) {
                    classFiles.foreach { processClassFile(page, _) }
                }
        }
    }

    def getClassPaths(project: IJavaProject, pathInPackage: String): List[IPath] = {
        val defaultPath = project.getOutputLocation.removeFirstSegments(1)
        val projectPaths = project.getRawClasspath.toList.map { _.getOutputLocation }
        val validProjectPaths = projectPaths.filter { _ != null }
        val relativeProjectPaths = validProjectPaths.map { _.removeFirstSegments(1) }.distinct
        val workspacePath = project.getProject.getLocation
        val binaryPaths = if (relativeProjectPaths.length == 0) {
            List(workspacePath.append(defaultPath))
        } else {
            relativeProjectPaths.map { e ⇒ workspacePath.append(e) }
        }
        binaryPaths.map { path ⇒ path.append(pathInPackage) }
    }

    def processSourceIJE(
        shell: Shell,
        page: IWorkbenchPage,
        ije: IJavaElement,
        ext: String): Unit = {
        ije match {
            case icu: ICompilationUnit ⇒
                val pathInPackage = icu.getPackageDeclarations.length match {
                    case n if n <= 0 ⇒
                        ""
                    case _ ⇒
                        icu.getPackageDeclarations.head.getElementName.replace('.', '/')
                }
                val absolutePaths: List[IPath] = getClassPaths(icu.getJavaProject, pathInPackage)
                val classFileName: String = icu.getElementName.replaceAll(s"\\.$ext", "")

                def hasClass(e: IPath): Boolean =
                    e.toFile.exists && e.toFile.list.contains(classFileName+".class")
                val validPaths: List[IPath] = absolutePaths.filter(hasClass)

                def toRelatedFiles(e: IPath): List[File] =
                    e.toFile.listFiles.filter(isRelated).toList
                def isRelated(e: File): Boolean =
                    e.getAbsolutePath.matches(""".*(/|\\)"""+classFileName+"""(\$.*)?\.class""")
                val relatedFiles: List[File] = validPaths.map(toRelatedFiles).flatten

                val classFiles: List[ClassFile] = AllClassFiles(relatedFiles).map(_._1).toList
                classFiles.length match {
                    case n if n <= 0 ⇒
                        val msg = s"No related classfiles found for ${ije.getElementName}"
                        MessageDialog.openError(shell, "Error", msg)
                    case n if 1 to 5 contains n ⇒
                        classFiles.foreach { processClassFile(page, _) }
                    case n ⇒
                        val msg: String = s"This will open $n new tabs. Continue?"
                        val confirm: Boolean = MessageDialog.openConfirm(shell, "Warning", msg)
                        if (confirm) {
                            classFiles.foreach { processClassFile(page, _) }
                        }
                }
            case _ ⇒
        }
    }
}