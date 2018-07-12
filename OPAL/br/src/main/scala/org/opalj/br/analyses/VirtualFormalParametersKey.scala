/* BSD 2-Clause License - see OPAL/LICENSE for details. */
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
