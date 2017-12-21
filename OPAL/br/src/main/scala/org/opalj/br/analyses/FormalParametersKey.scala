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
package br
package analyses

import java.util.concurrent.ConcurrentLinkedQueue

import scala.collection.JavaConverters._

import org.opalj.collection.immutable.ConstArray

/**
 * The set of all explicit and implicit formal method parameters in a project.
 * The set also contains formal parameters of library methods.
 *
 * @param data Contains for each method the array with the formal parameters. The value with
 *             index 0 contains the implicit `this` reference in case of an instance method,
 *             the array elements with the indexes [1..method.parametersCount] contain the
 *             respective [[FormalParameter]] objects for the corresponding method parameters.
 *
 * @author Florian Kuebler
 * @author Michael Eichberg
 */
class FormalParameters private[analyses] (val data: Map[Method, ConstArray[FormalParameter]]) {

    /**
     * Returns the formal parameters array for the given method. If the method is not known,
     * `null` is returned. If the method is known a non-null (but potentially empty)
     * [[org.opalj.collection.immutable.ConstArray]] is returned.
     */
    def apply(m: Method): ConstArray[FormalParameter] = data.getOrElse(m, null)

    def formalParameters: Iterable[FormalParameter] = data.values.flatten.filter(_ ne null)
}

/**
 * The ''key'' object to get information about all formal parameters.
 *
 * @note See [[org.opalj.br.FormalParameter]] and [[FormalParameters]] for further details.
 * @example To get the index use the [[org.opalj.br.analyses.Project]]'s `get` method and pass in
 *          `this` object.
 * @author Florian Kuebler
 */
object FormalParametersKey extends ProjectInformationKey[FormalParameters, Nothing] {

    /**
     * The analysis has no special prerequisites.
     *
     * @return `Nil`.
     */
    override protected def requirements: Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    /**
     * Collects all formal parameters.
     *
     * @note This analysis is internally parallelized. I.e., it is advantageous to run this
     *       analysis in isolation.
     */
    override protected def compute(p: SomeProject): FormalParameters = {

        val sites = new ConcurrentLinkedQueue[(Method, ConstArray[FormalParameter])]

        p.parForeachMethod() { m ⇒
            val md = m.descriptor
            val parametersCount = md.parametersCount
            if (m.isStatic && parametersCount == 0) {
                sites.add(m → ConstArray.empty)
            } else {
                val formalParameters = new Array[FormalParameter](parametersCount + 1)
                if (!m.isStatic) formalParameters(0) = new FormalParameter(m, -1)
                var p = 1
                while (p <= parametersCount) {
                    formalParameters(p) = new FormalParameter(m, -p - 1)
                    p += 1
                }
                sites.add(m → ConstArray(formalParameters))
            }
        }

        new FormalParameters(sites.asScala.toMap)
    }

    //
    // HELPER FUNCTION TO MAKE IT EASILY POSSIBLE TO ADD FORMAL PARAMETERS TO A PROPERTYSTORE
    // AND TO ENSURE THAT FORMAL PARAMETERS AND THE PROPERTYSTORE CONTAIN THE SAME OBJECTS!
    //

    final val entityDerivationFunction: (SomeProject) ⇒ (Traversable[AnyRef], FormalParameters) = {
        (p: SomeProject) ⇒
            {
                // this will collect the formal parameters of the project if not yet collected...
                val formalParameters = p.get(FormalParametersKey)
                (formalParameters.formalParameters, formalParameters)
            }
    }
}
