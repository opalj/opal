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
package ba

import org.opalj.br.ClassFile
import org.opalj.br.ObjectType

/**
 * Enhancing wrapper for combining AccessFlags.
 *
 * @author Malte Limmeroth
 * @author Michael Eichberg
 */
final class AccessModifier(val accessFlag: Int) extends AnyVal {

    /**
     * Returns a new [[AccessModifier]] with both [[AccessModifier]]s `accessFlag`s set.
     */
    def +(that: AccessModifier): AccessModifier = {
        new AccessModifier(this.accessFlag | that.accessFlag)
    }

    /**
     * Creates a new [[ClassDeclarationBuilder]] with the given name and previously defined
     * AccessModifiers. The minorVersion is initialized as
     * [[ClassFileBuilder.defaultMinorVersion]] and the majorVersion as
     * [[ClassFileBuilder.defaultMajorVersion]].
     *
     * @param fqn The class name in JVM notation as a fully qualified name, e.g. "MyClass" for a
     *            class in the default package or "my/package/MyClass" for a class in "my.package".
     */
    def CLASS(fqn: String): ClassDeclarationBuilder = {
        var accessFlags = this.accessFlags

        val superclassType: Option[ObjectType] =
            if (ACC_INTERFACE.isSet(accessFlags))
                Some(ObjectType.Object)
            else
                None
        ClassDeclarationBuilder(
            ClassFile(
                minorVersion = ClassFileBuilder.defaultMinorVersion,
                majorVersion = ClassFileBuilder.defaultMajorVersion,
                accessFlags = accessFlag,
                thisType = ObjectType(fqn),
                superclassType = None,
                interfaceTypes = IndexedSeq.empty,
                fields = IndexedSeq.empty,
                methods = IndexedSeq.empty,
                attributes = IndexedSeq.empty
            )
        )
    }
}
