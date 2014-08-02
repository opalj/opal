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
package bi

/**
 * Convenience methods related to access flags.
 *
 * @author Michael Eichberg
 */
object AccessFlags {

    /**
     * Returns the names of the access flags set in a respective vector.
     */
    def toStrings(accessFlags: Int, ctx: AccessFlagsContext): Iterator[String] = {
        AccessFlagsIterator(accessFlags, ctx) map { accessFlag ⇒
            accessFlag.javaName.getOrElse("["+accessFlag.toString+"]")
        }
    }

    def toString(accessFlags: Int, ctx: AccessFlagsContext): String = {
        toStrings(accessFlags, ctx).mkString(" ")
    }

    def classFlagsToJava(accessFlags: Int): String = {
        val ctx = AccessFlagsContexts.CLASS_FLAGS
        // ACC_PUBLIC,ACC_FINAL,ACC_SUPER,ACC_INTERFACE,ACC_ABSTRACT,
        // ACC_SYNTHETIC,ACC_ANNOTATION,ACC_ENUM
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

        if (!ACC_SUPER.unapply(accessFlags))
            flags ::= "/*super bit NOT set*/"

        if (ACC_ENUM.unapply(accessFlags))
            flags ::= "enum"
        else if (!ACC_INTERFACE.unapply(accessFlags))
            flags ::= "class"

        flags.reverse.mkString(" ")
    }

}
