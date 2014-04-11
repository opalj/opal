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

import de.tud.cs.st.collection.immutable.UIDSet

/**
 * Implementation of an abstract interpretation framework – called BATAI in the following.
 *
 * Please note, that BATAI just refers to the classes and traits defined in this package
 * (`ai`). The classes and traits defined in the sub-packages (in particular in `domain`)
 * are not considered to be part of the core of BATAI.
 *
 * @note This framework assumes that the analyzed bytecode is valid; i.e., the JVM's
 *      bytecode verifier would be able to verify the code. Furthermore, load-time errors
 *      (e.g., `LinkageErrors`) are – by default – completely ignored to facilitate the
 *      analysis of parts of a project. In general, if the presented bytecode is not valid,
 *      the result is undefined (i.e., BATAI may report meaningless results, crash or run
 *      indefinitely).
 *
 * @see [[de.tud.cs.st.bat.resolved.ai.AI]] - Implements the abstract interpreter that
 *      process a methods code and uses a analysis-specific domain to perform the
 *      abstract computations.
 * @see [[de.tud.cs.st.bat.resolved.ai.Domain]] - The core interface between the abstract
 *      interpretation framework and the abstract domain that is responsible for
 *      performing the abstract computations.
 *
 * @author Michael Eichberg
 */
package object ai {

    import language.existentials

    /**
     * Type alias that abstracts over all domains. `Domain` object are parameterized
     * over the source for which the domain object was created. For many analyses the
     * source information associated with a domain is not relevant.
     *
     * @note This type alias serves comprehension purposes only.
     */
    type SomeDomain = Domain[_]

    /**
     * Type alias that can be used if the AI can process all kinds of domains.
     *
     * @note This type alias serves comprehension purposes only.
     */
    type SomeAI[D <: SomeDomain] = AI[_ >: D]

    /**
     * Type alias that is used to identify a set of program counters.
     *
     * @note This type alias serves comprehension purposes only.
     */
    type PCs = collection.UShortSet

    /**
     * An upper type bound represents the available type information about a reference value.
     * It is always "just" an upper bound for a concrete type; i.e., we know that
     * the runtime type has to be a subtype of the type identified by the upper bound.
     * Furthermore, an upper bound can identify multiple '''independent''' types. E.g.,
     * a type bound for array objects could be: `java.io.Serializable` and
     * `java.lang.Cloneable`. Here, independent means that no two types of the bound
     * are in a subtype relationship. Hence, an upper bound is always a special set where
     * the values are not equal and are not in an inheritance relation. However, 
     * identifying independent types is the responsibility of the class hierarchy.
     *
     * In general, an upper bound identifies a single class type and a set of independent
     * interface types that are known to be implemented by the current object. '''Even if
     * the type contains a class type''' it may just be a super class of the concrete type
     * and, hence, just represent an abstraction.
     *
     * @note How type bounds related to reference types are handled and whether the domain
     *      makes it possible to distinguish between precise types and type bounds is at
     *      the sole discretion of the domain.
     */
    type UpperTypeBound = UIDSet[ReferenceType]
}
