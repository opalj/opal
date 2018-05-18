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
package bi

/**
 * Convenience methods related to access flags.
 */
object AccessFlags {

    final val ACC_PUBLIC_STATIC /*:Int*/ = ACC_PUBLIC.mask | ACC_STATIC.mask

    final val ACC_NATIVE_VARARGS /*:Int*/ = ACC_NATIVE.mask | ACC_VARARGS.mask

    final val ACC_SYNTHETIC_PUBLIC_SUPER = ACC_SYNTHETIC.mask | ACC_PUBLIC.mask | ACC_SUPER.mask

    final val ACC_SYNTHETIC_STATIC_PUBLIC = ACC_SYNTHETIC.mask | ACC_STATIC.mask | ACC_PUBLIC.mask

    final val ACC_SYNTHETIC_STATIC_PRIVATE = ACC_SYNTHETIC.mask | ACC_STATIC.mask | ACC_PRIVATE.mask

    /**
     * Returns the names of the access flags set in a respective vector.
     */
    def toStrings(accessFlags: Int, ctx: AccessFlagsContext): Iterator[String] = {
        AccessFlagsIterator(accessFlags, ctx) map { accessFlag ⇒
            accessFlag.javaName.getOrElse("/*"+accessFlag.toString+"*/")
        }
    }

    def toString(accessFlags: Int, ctx: AccessFlagsContext): String = {
        toStrings(accessFlags, ctx).mkString(" ")
    }

    def classFlagsToJava(accessFlags: Int): String = {
        // ACC_PUBLIC,ACC_FINAL,ACC_SUPER,ACC_INTERFACE,ACC_ABSTRACT,
        // ACC_SYNTHETIC,ACC_ANNOTATION,ACC_ENUM,ACC_MODULE

        if (ACC_MODULE.unapply(accessFlags))
            return "module";

        var flags = List.empty[String]

        if (ACC_SYNTHETIC.unapply(accessFlags)) {
            flags ::= "/*synthetic*/"
        }

        if (ACC_PUBLIC.unapply(accessFlags)) {
            flags ::= "public"
        }

        if (ACC_FINAL.unapply(accessFlags)) {
            flags ::= "final"
        }

        if (ACC_ABSTRACT.unapply(accessFlags) && !ACC_INTERFACE.unapply(accessFlags))
            flags ::= "abstract"

        if (ACC_INTERFACE.unapply(accessFlags)) {
            if (!ACC_ABSTRACT.unapply(accessFlags))
                flags ::= "/*NOT abstract (specification violation)*/"

            if (ACC_ANNOTATION.unapply(accessFlags))
                flags ::= "@interface"
            else
                flags ::= "interface"
        }

        if (!ACC_SUPER.unapply(accessFlags) && !ACC_MODULE.unapply(accessFlags))
            flags ::= "/*super bit NOT set*/"

        if (ACC_ENUM.unapply(accessFlags))
            flags ::= "enum"
        else if (!ACC_INTERFACE.unapply(accessFlags))
            flags ::= "class"

        flags.reverse.mkString(" ")
    }

}
