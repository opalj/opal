/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

/**
 * Explicitly models a formal parameter of a [[DeclaredMethod]] to make it possible to store it in the
 * property store and to compute properties for it.
 *
 * The first parameter explicitly defined by the method will have the origin `-2`, the second one
 * will have the origin `-3` and so on.
 * That is, the origin of an explicitly declared parameter is always `-(parameter_index + 2)`.
 * The origin of the `this` parameter is `-1`.
 *
 * @note The computational type category of the parameters is ignored to ease the mapping.
 *
 * @note This encoding is also used by the default three address code representation
 *       generated using a local data-flow analysis (see [[org.opalj.tac.TACAI]]).
 *
 *       '''In case of the bytecode based data-flow analysis the origin used by the analysis
 *       reflects the position of the parameter value on the tac; see
 *       [[org.opalj.ai.parameterIndexToValueOrigin]].'''
 *
 *
 * @param method The virtual method which contains the formal parameter.
 * @param origin The origin associated with the parameter. See the general description for
 *               further details.
 *
 * @author Florian Kuebler
 */
final class VirtualFormalParameter(val method: DeclaredMethod, val origin: Int) {

    /**
     * @return The index of the parameter or -1 if this formal parameter reflects the
     *         implicit `this` value.
     */
    def parameterIndex: Int = -origin - 2

    override def equals(other: Any): Boolean = {
        other match {
            case that: VirtualFormalParameter => (this.method == that.method) && this.origin == that.origin
            case _                            => false
        }
    }

    override def hashCode(): Int = method.hashCode() * 111 + origin

    override def toString: String = {
        s"VirtualFormalParameter(${method.toJava},origin=$origin)"
    }
}

object VirtualFormalParameter {

    def apply(method: DeclaredMethod, origin: Int): VirtualFormalParameter = {
        new VirtualFormalParameter(method, origin)
    }

    // TODO Using RefIntPair to avoid (un)boxing.
    def unapply(fp: VirtualFormalParameter): Some[(DeclaredMethod, Int)] = {
        Some((fp.method, fp.origin))
    }

}
