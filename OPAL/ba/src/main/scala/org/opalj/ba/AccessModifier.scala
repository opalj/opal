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
package ba

import org.opalj.bi.ACC_INTERFACE
import org.opalj.bi.ACC_ANNOTATION
import org.opalj.br.ObjectType

/**
 * Represents the access flags of a class, method or field declaration.
 *
 * All standard access flags are predefined in this package [[org.opalj.ba]].
 *
 * @author Malte Limmeroth
 * @author Michael Eichberg
 */
final class AccessModifier(private[ba] val accessFlags: Int) extends AnyVal {

    final def PUBLIC: AccessModifier = new AccessModifier(this.accessFlags | ba.PUBLIC.accessFlags)
    final def FINAL: AccessModifier = new AccessModifier(this.accessFlags | ba.FINAL.accessFlags)
    final def SUPER: AccessModifier = new AccessModifier(this.accessFlags | ba.SUPER.accessFlags)
    final def INTERFACE: AccessModifier = new AccessModifier(this.accessFlags | ba.INTERFACE.accessFlags)
    final def ABSTRACT: AccessModifier = new AccessModifier(this.accessFlags | ba.ABSTRACT.accessFlags)
    final def SYNTHETIC: AccessModifier = new AccessModifier(this.accessFlags | ba.SYNTHETIC.accessFlags)
    final def ANNOTATION: AccessModifier = new AccessModifier(this.accessFlags | ba.ANNOTATION.accessFlags)
    final def ENUM: AccessModifier = new AccessModifier(this.accessFlags | ba.ENUM.accessFlags)
    final def PRIVATE: AccessModifier = new AccessModifier(this.accessFlags | ba.PRIVATE.accessFlags)
    final def PROTECTED: AccessModifier = new AccessModifier(this.accessFlags | ba.PROTECTED.accessFlags)
    final def STATIC: AccessModifier = new AccessModifier(this.accessFlags | ba.STATIC.accessFlags)
    final def SYNCHRONIZED: AccessModifier = new AccessModifier(this.accessFlags | ba.SYNCHRONIZED.accessFlags)
    final def BRIDGE: AccessModifier = new AccessModifier(this.accessFlags | ba.BRIDGE.accessFlags)
    final def VARARGS: AccessModifier = new AccessModifier(this.accessFlags | ba.VARARGS.accessFlags)
    final def NATIVE: AccessModifier = new AccessModifier(this.accessFlags | ba.NATIVE.accessFlags)
    final def STRICT: AccessModifier = new AccessModifier(this.accessFlags | ba.STRICT.accessFlags)
    final def VOLATILE: AccessModifier = new AccessModifier(this.accessFlags | ba.VOLATILE.accessFlags)
    final def TRANSIENT: AccessModifier = new AccessModifier(this.accessFlags | ba.TRANSIENT.accessFlags)

    /**
     * Returns a new [[AccessModifier]] with both [[AccessModifier]]s `accessFlag`s set.
     */
    def +(that: AccessModifier): AccessModifier = {
        new AccessModifier(this.accessFlags | that.accessFlags)
    }

    /**
     * Creates a new [[ClassFileBuilder]] with the given name and previously defined
     * AccessModifiers. The `minorVersion` is initialized using
     * [[ClassFileBuilder.DefaultMinorVersion]] and the `majorVersion` using
     * [[ClassFileBuilder.DefaultMajorVersion]].
     *
     * @param fqn The fully qualified class name in JVM notation, e.g. "MyClass" for a
     *            class in the default package or "my/package/MyClass" for a class in "my.package".
     */
    def CLASS(fqn: String): ClassFileBuilder = {
        var accessFlags = this.accessFlags

        val superclassType: Option[ObjectType] =
            if (ACC_INTERFACE.isSet(accessFlags))
                Some(ObjectType.Object)
            else
                None

        if (ACC_ANNOTATION.isSet(accessFlags))
            accessFlags |= ACC_INTERFACE.mask

        new ClassFileBuilder(
            accessFlags = accessFlags,
            thisType = ObjectType(fqn),
            superclassType = superclassType
        )
    }

    /**
     * Creates a new [[MethodBuilder]] with the previously defined [[AccessModifier]]s.
     *
     * @param name The method name
     * @param parameters The method parameters in JVM notation, e.g. "()" for no parameters,
     *                   "(IB)" for one integer and one boolean argument or
     *                   "(Ljava/lang/String;)" for one String argument.
     * @param returnType The returnType of this method in JVM notation, e.g. "I" for integer.
     */
    def apply(
        name:       String,
        parameters: String,
        returnType: String
    ): MethodBuilder = {
        new MethodBuilder(
            accessFlags = accessFlags,
            name = name,
            descriptor = br.MethodDescriptor(parameters + returnType)
        )
    }

    /**
     * Creates a new FieldBuilder with the previously defined AccessModifiers.
     *
     * @param name the fields name
     * @param fieldType the fieldType in JVM notation, e.g. "I" for integer
     */
    def apply(name: String, fieldType: String): FieldBuilder = {
        new FieldBuilder(
            accessFlags = accessFlags,
            name = name,
            fieldType = br.FieldType(fieldType)
        )
    }

    /**
     * Adds this [[AccessModifier]]s AccessFlag to the AccessFlags of the given
     * [[ClassFileMemberBuilder]].
     */
    def +(cfmBuilder: ClassFileMemberBuilder): cfmBuilder.type = {
        cfmBuilder.addAccessFlags(accessFlags)
    }
}
