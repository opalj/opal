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

/**
 * Implementation of a library for parsing Java bytecode and creating arbitrary
 * representations.
 *
 * OPAL's primary representation of Java byte code
 * is the [[org.opalj.br]] representation which is defined in the
 * respective package.
 *
 * == This Package ==
 * Common constants and type definitions used across OPAL.
 *
 * @author Michael Eichberg
 */
package object bi {

    type AccessFlagsContext = AccessFlagsContexts.Value

    type AttributeParent = AttributesParent.Value

    type ConstantPoolTag = ConstantPoolTags.Value

    /**
     * Every Java class file start with "0xCAFEBABE".
     */
    final val CLASS_FILE_MAGIC = 0xCAFEBABE

    def jdkVersion(majorVersion: Int): String = {
        // 52 == 8; ... 50 == 6
        if (majorVersion >= 49) {
            "Java "+(majorVersion - 44)
        } else if (majorVersion > 45) {
            "Java 2 Platform version 1."+(majorVersion - 44)
        } else {
            "JDK 1.1 (JDK 1.0.2)"
        }
    }

}
