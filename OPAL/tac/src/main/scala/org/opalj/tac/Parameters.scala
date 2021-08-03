/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import java.util.{Arrays => JArrays}

import org.opalj.ai.ValueOrigin

/**
 * Information about a method's explicit and implicit parameters.
 *
 * @param parameters The (non-null) array with the information about the explicit method parameters.
 *         The array must not be mutated. The first explicit parameter is ''always'' stored at
 *         location 1 (also in case of static methods) to enable a unified access to a
 *         method's parameters whether the method is static or not.
 * @author Michael Eichberg
 */
class Parameters[P <: AnyRef](
        val parameters: Array[P] // EVENTUALLY CONST
) extends (Int => P) {

    /**
     * Returns the parameter with the specified index; the first (declared) parameter has the
     * index 1. The (implicit) this parameter has the index 0, if it exists.
     */
    def apply(index: Int): P = this.parameters(index)

    /**
     * Returns the parameter with the respective value origin.
     *
     * @param vo The origin of the associated parameter. The origin is used in the 3-address code
     *           to identify parameters. The origin `-1` always identifies the `this` parameter in
     *           case of an instance method and is unused otherwise. The origins
     *           [-2..(-2-parametersCount)] correspond to the explicitly specified method
     *           parameters.
     *
     * @return The parameter with the respective value origin.
     */
    def parameter(vo: ValueOrigin): P = parameters(-vo - 1)

    /**
     * The instance method's implicit `this` parameter.
     *
     * @return The variable capturing information about the `this` parameter;
     *         if the underlying methods is static an `UnsupportedOperationException` is thrown.
     */
    def thisParameter: P = {
        val p = parameters(0)
        if (p eq null) throw new UnsupportedOperationException()
        p
    }

    override def equals(other: Any): Boolean = {
        other match {
            case that: Parameters[_] =>
                JArrays.equals(
                    this.parameters.asInstanceOf[Array[AnyRef]],
                    that.parameters.asInstanceOf[Array[AnyRef]]
                )
            case _ => false
        }
    }

    override def hashCode(): ValueOrigin = {
        17 * JArrays.hashCode(parameters.asInstanceOf[Array[AnyRef]])
    }

    override def toString: String = {
        val parametersWithIndex = parameters.iterator.zipWithIndex
        val parametersTxt = parametersWithIndex.filter(_._1 ne null).map { e => val (p, i) = e; s"$i: $p" }
        parametersTxt.mkString(s"Parameters(\n\t", ",\n\t", "\n)")
    }
}
