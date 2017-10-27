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
package tac

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
) extends (Int ⇒ P) {

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

    override def toString: String = {
        val parametersWithIndex = parameters.iterator.zipWithIndex
        val parametersTxt = parametersWithIndex.filter(_._1 ne null).map { e ⇒ val (p, i) = e; s"$i: $p" }
        parametersTxt.mkString(s"Parameters(\n\t", ",\n\t", "\n)")
    }
}
