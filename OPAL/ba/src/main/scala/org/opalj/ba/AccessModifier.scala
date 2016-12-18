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

import org.opalj.bi.ACC_INTERFACE
import org.opalj.bi.ACC_ANNOTATION
import org.opalj.br.ClassFile
import org.opalj.br.ObjectType

/**
 * Represents the access flags of a class, method or field declaration.
 *
 * All standard access flags are predefined in this package [[org.opalj.ba]].
 *
 * @author Malte Limmeroth
 * @author Michael Eichberg
 */
final class AccessModifier private[ba] (val accessFlags: Int) extends AnyVal {

    /**
     * Returns a new [[AccessModifier]] with both [[AccessModifier]]s `accessFlag`s set.
     */
    def +(that: AccessModifier): AccessModifier = {
        new AccessModifier(this.accessFlags | that.accessFlags)
    }

    /**
     * Creates a new [[ClassDeclarationBuilder]] with the given name and previously defined
     * AccessModifiers. The minorVersion is initialized as
     * [[ClassFileBuilder.defaultMinorVersion]] and the majorVersion as
     * [[ClassFileBuilder.defaultMajorVersion]].
     *
     * @param fqn The fully qualified class name in JVM notation, e.g. "MyClass" for a
     *            class in the default package or "my/package/MyClass" for a class in "my.package".
     */
    def CLASS(fqn: String): ClassDeclarationBuilder = {
        var accessFlags = this.accessFlags

        val superclassType: Option[ObjectType] =
            if (ACC_INTERFACE.isSet(accessFlags))
                Some(ObjectType.Object)
            else
                None

        if (ACC_ANNOTATION.isSet(accessFlags))
            accessFlags |= ACC_INTERFACE.mask

        ClassDeclarationBuilder(
            ClassFile(
                minorVersion = ClassFileBuilder.DefaultMinorVersion,
                majorVersion = ClassFileBuilder.DefaultMajorVersion,
                accessFlags = accessFlags,
                thisType = ObjectType(fqn),
                superclassType = superclassType,
                interfaceTypes = IndexedSeq.empty,
                fields = IndexedSeq.empty,
                methods = IndexedSeq.empty,
                attributes = IndexedSeq.empty
            )
        )
    }
}
