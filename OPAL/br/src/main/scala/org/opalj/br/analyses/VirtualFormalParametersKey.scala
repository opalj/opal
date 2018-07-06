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

import scala.collection.mutable.OpenHashMap
import org.opalj.collection.immutable.ConstArray

/**
 * The set of all explicit and implicit virtual formal method parameters in a project.
 * The set also contains virtual formal parameters of library methods.
 *
 * @param data Contains for each virtual method the array with the virtual formal parameters. The
 *             value with index 0 contains the implicit `this` reference, the array elements with
 *             the indexes [1..method.descriptor.parametersCount] contain the respective
 *             [[VirtualFormalParameter]] objects for the corresponding method parameters.
 * @author Florian Kuebler
 */
class VirtualFormalParameters private[analyses] (
        val data: scala.collection.Map[DeclaredMethod, ConstArray[VirtualFormalParameter]]
) {
    /**
     * Returns the virtual formal parameters array for the given method. If the method is not known,
     * `null` is returned. If the method is known a non-null (but potentially empty)
     * [[org.opalj.collection.immutable.ConstArray]] is returned.
     */
    def apply(m: DeclaredMethod): ConstArray[VirtualFormalParameter] = data.getOrElse(m, null)

    def virtualFormalParameters: Iterable[VirtualFormalParameter] = {
        // todo Why should it ever be null?
        data.values.flatten.filter(_ ne null)
    }
}

/**
 * The ''key'' object to get information about all virtual formal parameters.
 *
 * @note    See [[org.opalj.br.analyses.VirtualFormalParameter]] and [[VirtualFormalParameters]]
 *          for further details.
 * @example To get the index use the [[org.opalj.br.analyses.Project]]'s `get` method and pass in
 *          `this` object.
 * @author  Florian Kuebler
 */
object VirtualFormalParametersKey extends ProjectInformationKey[VirtualFormalParameters, Nothing] {

    /**
     * The key uses the `VirtualForwardingMethodsKey`.
     */
    override protected def requirements: ProjectInformationKeys = {
        List(DeclaredMethodsKey)
    }

    /**
     * Collects all virtual formal parameters.
     *
     * @note This analysis is internally parallelized. I.e., it is advantageous to run this
     *       analysis in isolation.
     */
    override protected def compute(p: SomeProject): VirtualFormalParameters = {

        val sites = new OpenHashMap[DeclaredMethod, ConstArray[VirtualFormalParameter]]

        for {
            dm ← p.get(DeclaredMethodsKey).declaredMethods
            if (dm.hasSingleDefinedMethod)
        } {
            val md = dm.descriptor
            val parametersCount = md.parametersCount
            val formalParameters = new Array[VirtualFormalParameter](parametersCount + 1)
            if (dm.hasSingleDefinedMethod && !dm.definedMethod.isStatic)
                formalParameters(0) = new VirtualFormalParameter(dm, -1)

            var p = 1
            while (p <= parametersCount) {
                formalParameters(p) = new VirtualFormalParameter(dm, -p - 1)
                p += 1
            }
            sites += (dm → ConstArray(formalParameters))
        }

        new VirtualFormalParameters(sites)
    }

    //
    // HELPER FUNCTION TO MAKE IT EASILY POSSIBLE TO ADD VIRTUAL FORMAL PARAMETERS TO A
    // PROPERTYSTORE AND TO ENSURE THAT VIRTUAL FORMAL PARAMETERS AND THE PROPERTYSTORE CONTAIN THE SAME
    // OBJECTS!
    //
    final val entityDerivationFunction: (SomeProject) ⇒ (Traversable[AnyRef], VirtualFormalParameters) = {
        (p: SomeProject) ⇒
            {
                // this will collect the formal parameters of the project if not yet collected...
                val formalParameters = p.get(VirtualFormalParametersKey)
                (formalParameters.virtualFormalParameters, formalParameters)
            }
    }
}
