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

import scala.annotation.tailrec

import org.opalj.collection.immutable.{Chain ⇒ List}
import org.opalj.collection.immutable.{Naught ⇒ Nil}

/**
 * Common utility functionality.
 *
 * @author Michael Eichberg
 */
package object util {

    /**
     * Removes the first occurrence of the specified program counter from the given list
     * unless the given `test` has failed. ''If the test fails, the '''original'''
     * list is returned.''
     * The given test is executed before the test is made whether we have to remove
     * the element from the list.
     * If the original list is returned it is
     * possible to check whether the list is modified or not using
     * a reference comparison (`eq`).
     */
    @inline final def removeFirstUnless(
        worklist: List[Int /*PC*/ ], pc: Int
    )(
        test: Int ⇒ Boolean
    ): List[Int /*PC*/ ] = {
        var newWorklist: List[Int /*PC*/ ] = Nil
        var remainingWorklist = worklist
        while (remainingWorklist.nonEmpty) {
            val thePC = remainingWorklist.head
            if (test(thePC))
                return worklist
            if (thePC == pc)
                return newWorklist.reverse :&:: remainingWorklist.tail

            newWorklist :&:= thePC
            remainingWorklist = remainingWorklist.tail
        }
        worklist
    }

    /**
     * Tests if the given `pc` is found in the (optional) prefix of the `worklist`
     * where the end of the prefix is identified by `prefixEnd`.
     *
     * If the worklist is empty, false is returned. If the given `pc` is equal to
     * `prefixEnd` `true` will be returned.
     */
    @inline @tailrec final def containsInPrefix(
        worklist:  List[Int /*PC*/ ],
        pc:        Int,
        prefixEnd: Int
    ): Boolean = {
        if (worklist.isEmpty)
            false
        else {
            val head = worklist.head
            if (head == pc)
                true
            else if (head == prefixEnd)
                false
            else
                containsInPrefix(worklist.tail, pc, prefixEnd)
        }
    }

    // IMPROVE[very low relevance for now] Add an unsafe insertBefore to chain that actually mutates the list.
    /**
     * Inserts the given `pc` before `prefixEnd` in the list. If the list does not contain
     * `prefixEnd`, `pc` is appended to the list.
     */
    @inline final def insertBefore(
        worklist:  List[Int /*PC*/ ],
        pc:        Int,
        prefixEnd: Int
    ): List[Int] = {

        @tailrec def prepend(
            headWorklist: List[Int /*PC*/ ],
            tailWorklist: List[Int /*PC*/ ]
        ): List[Int /*PC*/ ] = {
            if (headWorklist.isEmpty)
                tailWorklist
            else
                prepend(headWorklist.tail, headWorklist.head :&: tailWorklist)
        }

        @tailrec def add(
            headWorklist: List[Int /*PC*/ ],
            tailWorklist: List[Int /*PC*/ ]
        ): List[Int /*PC*/ ] = {
            if (tailWorklist.isEmpty)
                (pc :&: headWorklist).reverse
            else {
                val nextPC = tailWorklist.head
                if (nextPC == prefixEnd)
                    prepend(headWorklist, pc :&: tailWorklist)
                else
                    add(nextPC :&: headWorklist, tailWorklist.tail)
            }
        }

        add(Nil, worklist)
    }

    /**
     * Inserts the given `pc` before `prefixEnd` in the list unless `pc` is already
     * contained in the list. If the list does not contain
     * `prefixEnd`, `pc` is appended to the list. If the list already contains `pc`
     * the original list is returned!
     */
    @inline final def insertBeforeIfNew(
        worklist:  List[Int /*PC*/ ],
        pc:        Int,
        prefixEnd: Int
    ): List[Int /*PC*/ ] = {

        @tailrec def prepend(
            headWorklist: List[Int /*PC*/ ],
            tailWorklist: List[Int /*PC*/ ]
        ): List[Int /*PC*/ ] = {
            if (headWorklist.isEmpty)
                tailWorklist
            else
                prepend(headWorklist.tail, headWorklist.head :&: tailWorklist)
        }

        @tailrec def add(
            headWorklist: List[Int /*PC*/ ],
            tailWorklist: List[Int /*PC*/ ]
        ): List[Int /*PC*/ ] = {
            if (tailWorklist.isEmpty)
                (pc :&: headWorklist).reverse
            else {
                val nextPC = tailWorklist.head
                if (nextPC == pc)
                    return worklist; // unchanged
                else if (nextPC == prefixEnd)
                    prepend(headWorklist, pc :&: tailWorklist)
                else
                    add(nextPC :&: headWorklist, tailWorklist.tail)
            }

        }

        add(Nil, worklist)
    }

    /**
     * Removes the first occurrence of the specified pc from the list.
     * If the pc is not found, the original list is returned. I.e., it is
     * possible to check whether the list is modified or not using
     * a reference comparison (`eq`).
     */
    @inline final def removeFirst(worklist: List[Int /*PC*/ ], pc: Int): List[Int /*PC*/ ] = {
        var newWorklist: List[Int /*PC*/ ] = List.empty
        var remainingWorklist = worklist
        while (remainingWorklist.nonEmpty) {
            val thePC = remainingWorklist.head
            if (thePC == pc) {
                return newWorklist.reverse :&:: remainingWorklist.tail
            } else {
                newWorklist = thePC :&: newWorklist
            }
            remainingWorklist = remainingWorklist.tail
        }
        worklist
    }
}
