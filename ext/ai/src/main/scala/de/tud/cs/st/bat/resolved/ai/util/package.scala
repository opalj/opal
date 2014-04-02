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
package ai

import de.tud.cs.st.util.Answer

import scala.collection.Set

/**
 * Common utility functionality.
 *
 * @author Michael Eichberg
 */
package object util {

    /**
     * Removes the first occurrence of the specified program counter from the given list 
     * if the given `test` has not yet failed. If the test fails, the '''original''' 
     * list is returned.
     * The given test is executed before the test is made whether we have to remove
     * the element from the list.
     * If the original list is returned it is
     * possible to check whether the list is modified or not using
     * a reference comparison (`eq`).
     */
    @inline def removeFirstWhile(worklist: List[PC], pc: PC)(test: PC ⇒ Boolean): List[PC] = {
        var newWorklist: List[PC] = List.empty
        var removedPC: Boolean = false
        var remainingWorklist = worklist
        while (remainingWorklist.nonEmpty) {
            val thePC = remainingWorklist.head
            if (!test(thePC))
                return worklist
            if (thePC == pc) {
                return newWorklist.reverse ::: remainingWorklist.tail
            } else {
                newWorklist = thePC :: newWorklist
            }
            remainingWorklist = remainingWorklist.tail
        }
        worklist
    }

    /**
     * Removes the first occurrence of the specified pc from the list.
     * If the pc is not found, the original list is returned. I.e., it is
     * possible to check whether the list is modified or not using
     * a reference comparison (`eq`).
     */
    @inline def removeFirst(worklist: List[PC], pc: PC): List[PC] = {
        var newWorklist: List[PC] = List.empty
        var removedPC: Boolean = false
        var remainingWorklist = worklist
        while (remainingWorklist.nonEmpty) {
            val thePC = remainingWorklist.head
            if (thePC == pc) {
                return newWorklist.reverse ::: remainingWorklist.tail
            } else {
                newWorklist = thePC :: newWorklist
            }
            remainingWorklist = remainingWorklist.tail
        }
        worklist
    }
}
