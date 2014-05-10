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
package de.tud.cs.st
package bat
package resolved
package analyses

import java.net.URL
import java.io.File

/**
 * Each report takes a severity parameter, which can be one of these pre-defined objects:
 *  - [[Severity.Error]]
 *  - [[Severity.Warning]]
 *  - [[Severity.Info]]
 *
 * @author Daniel Klauer
 */
abstract class Severity {
    /**
     * Returns a string using text and ANSI color codes suitable for console output to
     * allow humans to quickly identify the corresponding severity.
     * 
     * @param suffix An additional suffix string that will be appended to the severity
     * text and will be colored in the same way. This is useful, for example, to append
     * ": " to achieve a formatting such as "<severity>: " where this whole string uses
     * the severity's color. This matches the colored formatting done by clang.
     * 
     * @return Human-readable string identifying this severity level, ready to be printed
     * to the console.
     */
    def toAnsiColoredString(suffix: String): String
}

/**
 * Basically an enumeration of different severity objects. 
 * 
 * @author Daniel Klauer
 */
object Severity {
    /**
     * Should be used when reporting an issue that definitely is a bug.
     */
    case object Error extends Severity {
        def toAnsiColoredString(suffix: String): String =
            Console.BOLD + Console.RED+"error"+suffix+Console.RESET
    }

    /**
     * Should be used when reporting an issue that could potentially be a bug under
     * certain circumstances.
     */
    case object Warning extends Severity {
        def toAnsiColoredString(suffix: String): String =
            Console.BOLD + Console.YELLOW+"warning"+suffix+Console.RESET
    }

    /**
     * Should be used when reporting non-serious information. This refers to issues that
     * neither are bugs nor could lead to bugs, but may still be worth fixing (for example
     * unused fields).
     */
    case object Info extends Severity {
        def toAnsiColoredString(suffix: String): String =
            Console.BOLD + Console.BLUE+"info"+suffix+Console.RESET
    }
}
