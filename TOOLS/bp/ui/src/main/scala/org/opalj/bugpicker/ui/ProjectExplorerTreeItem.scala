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

import scala.collection.mutable.Map

import org.opalj.br.ClassFile
import org.opalj.br.ClassMember
import org.opalj.br.Field
import org.opalj.br.Method

import javafx.collections.ObservableList
import javafx.scene.control.TreeItem

import scalafx.collections.ObservableBuffer
import scalafx.scene.image.Image
import scalafx.scene.image.ImageView

/**
 * Extends TreeItem[ProjectExplorerData] to utilize lazy loading.
 *
 * @author David Becker
 */
class ProjectExplorerTreeItem(
        p:                  ProjectExplorerData,
        iv:                 ImageView,
        childrenNodes:      ObservableBuffer[ProjectExplorerTreeItem]                 = ObservableBuffer.empty,
        classFileStructure: Map[ProjectExplorerTreeItem, ObservableBuffer[ClassFile]] = Map.empty
)
    extends TreeItem[ProjectExplorerData](p, iv) {

    // We cache whether the ProjectExplorerData is a leaf or not. A ProjectExplorerData
    // is a leaf if it is a field or method. We cache this as isLeafValue is called often
    private[this] var isLeafValue: Boolean = true
    // We do the children and leaf testing only once, and then set these
    // booleans to false so that we do not check again during this run.
    private[this] var isFirstTimeChildren: Boolean = childrenNodes.isEmpty
    private[this] var isFirstTimeLeaf: Boolean = true

    if (!childrenNodes.isEmpty) {
        super.getChildren.setAll(childrenNodes)
    }

    override def getChildren: ObservableList[TreeItem[ProjectExplorerData]] = {
        if (isFirstTimeChildren) {
            isFirstTimeChildren = false;
            // First children call, so we actually go off and
            // determine the children of the ProjectExplorerData
            // contained in this TreeItem.
            super.getChildren.setAll(buildChildren(this))
        }
        return super.getChildren
    }

    override def isLeaf: Boolean = {
        if (isFirstTimeLeaf) {
            isFirstTimeLeaf = false;
            val data: ProjectExplorerData = getValue
            isLeafValue = data match {
                case f: ProjectExplorerFieldData  ⇒ true
                case m: ProjectExplorerMethodData ⇒ true
                case _                            ⇒ false
            }
        }
        isLeafValue
    }

    private def buildChildren(parent: ProjectExplorerTreeItem): ObservableBuffer[ProjectExplorerTreeItem] = {
        val data: ProjectExplorerData = parent.getValue
        data match {
            case p: ProjectExplorerPackageData ⇒ sortProjectExplorerTreeItems(
                addClassFiles(classFileStructure(parent))
            )
            case c: ProjectExplorerClassData ⇒ sortProjectExplorerTreeItems(
                addClassMembers(c.classFile, c.classFile.fields, determineFieldIcon) ++
                    addClassMembers(c.classFile, c.classFile.methods, determineMethodIcon)
            )
            case _ ⇒ ObservableBuffer.empty
        }
    }

    private def addClassFiles(
        classFiles: ObservableBuffer[ClassFile]
    ): ObservableBuffer[ProjectExplorerTreeItem] = {
        for (classFile ← classFiles) yield {
            val objectType = classFile.thisType
            new ProjectExplorerTreeItem(
                ProjectExplorerClassData(
                    objectType.simpleName,
                    classFile
                ),
                new ImageView {
                    image = determineClassIcon(classFile)
                }
            )
        }
    }

    private def addClassMembers[T <: ClassMember](
        classFile:     ClassFile,
        classMembers:  Seq[T],
        determineIcon: T ⇒ Image
    ): ObservableBuffer[ProjectExplorerTreeItem] = {
        for (classMember ← ObservableBuffer.apply(classMembers)) yield {
            new ProjectExplorerTreeItem(
                classMember match {
                    case m: Method ⇒ {
                        ProjectExplorerMethodData(
                            m.name,
                            classFile,
                            m,
                            m.isStatic,
                            m.isAbstract
                        )
                    }
                    case f: Field ⇒ {
                        ProjectExplorerFieldData(
                            f.name,
                            classFile,
                            f,
                            f.isStatic
                        )
                    }
                },
                new ImageView {
                    image = determineIcon(classMember)
                }
            )
        }
    }

    private def determineClassIcon(classFile: ClassFile): Image = {
        val imagePath = new StringBuilder("/org/opalj/bugpicker/ui/explorer/")
        if (classFile.isClassDeclaration) {
            imagePath.append("class")
            if (classFile.isAbstract)
                imagePath.append("_abstract")
            else if (classFile.isFinal)
                imagePath.append("_final")
        } else if (classFile.isAnnotationDeclaration)
            imagePath.append("annotation")
        else if (classFile.isEnumDeclaration)
            imagePath.append("enum")
        else if (classFile.isInterfaceDeclaration)
            imagePath.append("interface")

        imagePath.append(".gif")
        ProjectExplorer.getImage(imagePath.toString)
    }

    private def determineMethodIcon(method: Method): Image = {
        val imagePath = new StringBuilder("/org/opalj/bugpicker/ui/explorer/method")
        if (method.isPublic)
            imagePath.append("_public")
        else if (method.isProtected)
            imagePath.append("_protected")
        else if (method.isPrivate)
            imagePath.append("_private")
        else if (method.isPackagePrivate)
            imagePath.append("_default")

        if (method.isAbstract)
            imagePath.append("_abstract")
        else if (method.isConstructor)
            imagePath.append("_constructor")
        else {
            if (method.isStatic)
                imagePath.append("_static")
            if (method.isFinal)
                imagePath.append("_final")
        }

        imagePath.append(".gif")
        ProjectExplorer.getImage(imagePath.toString)
    }

    private def determineFieldIcon(field: Field): Image = {
        val imagePath = new StringBuilder("/org/opalj/bugpicker/ui/explorer/field")
        if (field.isPublic)
            imagePath.append("_public")
        else if (field.isProtected)
            imagePath.append("_protected")
        else if (field.isPrivate)
            imagePath.append("_private")
        else if (field.isPackagePrivate)
            imagePath.append("_default")

        if (field.isStatic)
            imagePath.append("_static")
        if (field.isFinal)
            imagePath.append("_final")

        imagePath.append(".gif")
        ProjectExplorer.getImage(imagePath.toString)
    }

    /**
     * Sorts a list of ProjectExplorerTreeItems with the
     * following ordering:
     *
     * Static before Non-Static
     * Fields before Methods
     * Alphabetically if everything else is equal
     */
    private def sortProjectExplorerTreeItems(
        items: ObservableBuffer[ProjectExplorerTreeItem]
    ): ObservableBuffer[ProjectExplorerTreeItem] = {
        items.sort((a, b) ⇒ {
            val value1 = a.getValue
            val value2 = b.getValue
            value1 match {
                case f1: ProjectExplorerFieldData ⇒ value2 match {
                    case f2: ProjectExplorerFieldData ⇒
                        if (f1.isStatic == f2.isStatic)
                            f1.name.value < f2.name.value
                        else
                            f1.isStatic.value
                    case m2: ProjectExplorerMethodData ⇒
                        if (f1.isStatic == m2.isStatic)
                            true
                        else
                            f1.isStatic.value
                }
                case m1: ProjectExplorerMethodData ⇒ value2 match {
                    case m2: ProjectExplorerMethodData ⇒
                        if (m1.isStatic == m2.isStatic)
                            m1.name.value < m2.name.value
                        else
                            m1.isStatic.value
                    case f2: ProjectExplorerFieldData ⇒
                        if (m1.isStatic == f2.isStatic)
                            false
                        else
                            m1.isStatic.value
                }
                case other1: ProjectExplorerData ⇒ value2 match {
                    case other2: ProjectExplorerData ⇒
                        other1.name.value < other2.name.value
                }
            }
        })
        items
    }
}