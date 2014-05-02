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
package project

import domain._
import bat.resolved.analyses._
import scala.collection.Set
import scala.collection.Map

/**
 * Common interface of all domains that collect the edges of a call graph
 * that are associated with a specific method.
 *
 * Each domain instance is associated with one specific method and is intended to
 * be used only once to perform an abstract interpretation (implementations of this
 * domain will have internal state.)
 *
 * @author Michael Eichberg
 */
trait CallGraphDomain extends Domain {

    // THE CONTEXT - SET DURING THE CREATION OF THE DOMAIN

    /* abstract */ val project: SomeProject

    /* abstract */ val theClassFile: ClassFile

    /* abstract */ val theMethod: Method

    // METHODS TO GET THE RESULTS AFTER THE DOMAIN WAS USED FOR THE ABSTRACT
    // INTERPRETATION OF THIS METHOD.
    /**
     * Returns the list of all methods that are called by `theMethod`.
     *
     * ==Requirement==
     * The list of methods that are called by a specific instruction must not
     * contain any duplicates.
     *
     * @note This method should only be called after the abstract interpretation
     *      of `theMethod` has completed.
     */
    def allCallEdges: (Method, Map[PC, Set[Method]])

    /**
     * Returns the list of all unresolved method calls of `theMethod`. A call
     * cannot be resolved if, e.g., the target class file is not available or
     * if the type of the receiver is an interface type and no appropriate implementations
     * are found.
     *
     * @note This method should only be called after the abstract interpretation
     *      of `thisMethod`.
     */
    def allUnresolvedMethodCalls: List[UnresolvedMethodCall]

}


