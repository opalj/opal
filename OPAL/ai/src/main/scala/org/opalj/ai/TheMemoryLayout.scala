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
package ai

import org.opalj.collection.immutable.{Chain ⇒ List}

/**
 * Mixin this trait if a domain needs access to the operands ([[Domain#OperandsArray]])
 * and/or locals ([[Domain#LocalsArray]]).
 *
 * ==Usage==
 * It is sufficient to mixin this trait in a [[Domain]] that needs to get access to the
 * memory structures. The abstract interpreter will then perform the initialization.
 *
 * This information is set immediately before the abstract interpretation is
 * started/continued.
 *
 * @author Michael Eichberg
 */
trait TheMemoryLayout { domain: ValuesDomain ⇒

    private[this] var theOperandsArray: OperandsArray = null

    private[this] var theLocalsArray: LocalsArray = null

    private[this] var theMemoryLayoutBeforeSubroutineCall: List[(Int /*PC*/ , this.OperandsArray, this.LocalsArray)] = null

    private[this] var theSubroutinesOperandsArray: OperandsArray = null
    private[this] var theSubroutinesLocalsArray: LocalsArray = null

    private[ai] def setMemoryLayout(
        theOperandsArray:                    this.OperandsArray,
        theLocalsArray:                      this.LocalsArray,
        theMemoryLayoutBeforeSubroutineCall: List[(Int /*PC*/ , this.OperandsArray, this.LocalsArray)],
        theSubroutinesOperandsArray:         this.OperandsArray,
        theSubroutinesLocalsArray:           this.LocalsArray
    ): Unit = {
        this.theOperandsArray = theOperandsArray
        this.theLocalsArray = theLocalsArray
        this.theMemoryLayoutBeforeSubroutineCall = theMemoryLayoutBeforeSubroutineCall
        this.theSubroutinesOperandsArray = theSubroutinesOperandsArray
        this.theSubroutinesLocalsArray = theSubroutinesLocalsArray
    }

    def operandsArray: OperandsArray = theOperandsArray

    def localsArray: LocalsArray = theLocalsArray

    def memoryLayoutBeforeSubroutineCall: List[(Int /*PC*/ , this.OperandsArray, this.LocalsArray)] = {
        theMemoryLayoutBeforeSubroutineCall
    }

    def subroutinesOperandsArray: OperandsArray = theSubroutinesOperandsArray

    def subroutinesLocalsArray: LocalsArray = theSubroutinesLocalsArray

}
