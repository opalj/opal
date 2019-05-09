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

import org.opalj.br.{ClassFile, Field, Method}

import scalafx.beans.property.BooleanProperty
import scalafx.beans.property.StringProperty

/**
 * Basic container for project explorer data.
 *
 * @author David Becker
 */
class ProjectExplorerData(
        val name: StringProperty
) {

    override def toString: String = {
        name.value
    }
}

object ProjectExplorerData {

    def apply(
        name: String
    ): ProjectExplorerData = {
        new ProjectExplorerData(
            new StringProperty(this, "name", name)
        )
    }
}

/**
 * Container for project explorer package data.
 *
 * @author David Becker
 */
class ProjectExplorerPackageData(
        name: StringProperty
) extends ProjectExplorerData(name) {
}

object ProjectExplorerPackageData {

    def apply(
        name: String
    ): ProjectExplorerPackageData = {
        new ProjectExplorerPackageData(
            new StringProperty(this, "name", name)
        )
    }
}

/**
 * Container for project explorer class data.
 *
 * @author David Becker
 */
class ProjectExplorerClassData(
        name:          StringProperty,
        val classFile: ClassFile
) extends ProjectExplorerData(name) {
}

object ProjectExplorerClassData {

    def apply(
        name:      String,
        classFile: ClassFile
    ): ProjectExplorerClassData = {
        new ProjectExplorerClassData(
            new StringProperty(this, "name", name),
            classFile
        )
    }
}

/**
 * Container for project explorer method data.
 *
 * @author David Becker
 */
class ProjectExplorerMethodData(
        name:           StringProperty,
        val classFile:  ClassFile,
        val method:     Method,
        val isStatic:   BooleanProperty,
        val isAbstract: BooleanProperty
) extends ProjectExplorerData(name) {
}

object ProjectExplorerMethodData {

    def apply(
        name:       String,
        classFile:  ClassFile,
        method:     Method,
        isStatic:   Boolean,
        isAbstract: Boolean
    ): ProjectExplorerMethodData = {
        new ProjectExplorerMethodData(
            new StringProperty(this, "name", name),
            classFile,
            method,
            new BooleanProperty(this, "isStatic", isStatic),
            new BooleanProperty(this, "isAbstract", isAbstract)
        )
    }
}

/**
 * Container for project explorer field data.
 *
 * @author David Becker
 */
class ProjectExplorerFieldData(
        name:          StringProperty,
        val classFile: ClassFile,
        val field:     Field,
        val isStatic:  BooleanProperty
) extends ProjectExplorerData(name) {
}

object ProjectExplorerFieldData {

    def apply(
        name:      String,
        classFile: ClassFile,
        field:     Field,
        isStatic:  Boolean
    ): ProjectExplorerFieldData = {
        new ProjectExplorerFieldData(
            new StringProperty(this, "name", name),
            classFile,
            field,
            new BooleanProperty(this, "isStatic", isStatic)
        )
    }
}