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
package ai
package dataflow

import de.tud.cs.st.bat.resolved._

import de.tud.cs.st.bat.resolved.ai._

trait DataFlowConstraint {

    /**
     * The ''value'' that we want to track is identified by the program counter of the 
     * instruction that created the value. 
     * 
     * In case of a parameter that is passed to a method, the `PC` is 
     * `-(index of the parameter)`. 
     */
    def sources: Traversable[(Method, PC)]

    def sinks : Traversable[(Method, PC)]
    
}

object Demo {
    // Goal:
    //      Express data-flow constraints; i.e., that some information flows or not-flows
    //      from a well-identified source to a well-identified sink.
    //
    // Usage Scenario:
    //      We want to avoid that information is stored in the database/processed by the 
    //      backend without being sanitized.
    //
    // Basic idea:
    //
    // 1. Select sources
    //      1.1. Sources are parameters passed to methods (e.g., doPost(session : Session)
    //              (This covers the main method as well as typical callback methods.)
    //      1.2. Values returned by methods (e.g., System.in.read) (here, we identify the call site)
    //      (1.3. ???)
    //
    // 2. Select sinks
    //      2.1. A sink is either a field (in which the value is stored)
    //      2.2. a method (parameter) which is passed the value
    //      (2.3. ???) 
    //
    // 3. Filtering (Terminating) data-flows
    //      3.1. If a specific operation was performed, e.g.,
    //      3.2. If a comparison (e.g., against null, > 0 , ...)
    //      3.3. An instanceOf/a checkcast
    //      3.4  A mathematical operation (e.g. +.-,...)
    //      3.X. [OPTIMIZATION] If the value was passed to a specific method (e.g., check(x : X) - throws Exception if the check fails)
    //      3.Y. [OPTIMIZATION] If the value was returned by a well-identified method (e.g., String sanitized = s.replace(...,...))
    //
    // 4. Extending data-flows (Side Channels)
    //      OPEN:   What would be the general strategy if a value influences another value?
    //              [SIDE CHANNELS?] What happens if the value is stored in a field of an object and that object is used?
    //              [SIDE CHANNELS?] What happens if the value is used during the computation, but does not (directly) influence the output. 
    //                  (e.g., if(x == 0) 1; else 2;
}
