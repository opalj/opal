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

/**
 * Information about a method's explicit and implicit parameters.
 *
 * @author Michael Eichberg
 */
sealed abstract class Parameters[V <: Var[V]] {

    /**
     * The instance method's implicit `this` parameter.
     *
     * @return If the underlying methods is static `None` is returned, else `Some[V]`.
      */
    def thisParameter : Option[V]

    /**
     * @return The (non-null) array with the information about the method parameters.
     *         The array must not be mutated. The first parameter is ''always'' stored at
     *         location 1 (also in case of static methods) to enable a unified access to a
     *         method's parameters whether the method is static or not.
     */
    // TODO Define and check an @const annotation.
    def parameters : Array[V] // CONST

    override def toString: String = {
        val thisTxt = thisParameter.map(self => s"\n\tthis: $self").getOrElse("")
        val parametersTxt = parameters.iterator.zipWithIndex.map { e ⇒val (p, i) = e; s"$i: $p"}
        parametersTxt.mkString(s"Parameters($thisTxt\n\t", ",\n\t", "\n)")
    }
}

object Parameters{
    private[tac] final val noParameters = new Array[Nothing](0)

    /**
     * Creates a new Parameters instance which encapsulates the information about a method's
     * parameters.
     *
     * @param thisParameter The (optional) this reference.
     * @param parameters The array with the parameters. Can be `null` if the method has no
     *                   parameters.
     * @tparam V
     * @return
     */
    def apply[V <: Var[V]] (thisParameter : Option[V] ,                parameters : Array[V]): Parameters[V] = {
        thisParameter match {
            case thisParameter @ Some(_) =>
                if(parameters eq null || parameters.isEmpty)
                    new InstanceMethodNoParameters(thisParameter)
                else
                    new InstanceMethodParameters[V](thisParameter,parameters)
            case None =>
                if(parameters eq null || parameters.isEmpty)
                    StaticMethodNoParameters.asInstanceOf[Parameters[V]]
                else
                   new  StaticMethodParameters(parameters)
        }
    }
             }

// THE FOLLOWING CLASSES ENABLE THE MEMORY EFFICIENT STORING OF THE PARAMETERS !!!

private[tac] object StaticMethodNoParameters extends Parameters[Nothing] {
    def thisParameter : None.type = None
    def parameters : Array[Nothing] = Parameters.noParameters
}

private[tac] class StaticMethodParameters[V <: Var[V]](
                                                      val parameters: Array[V]
                                                      ) extends Parameters[V] {
    def thisParameter : Option[V] =  None
}

private[tac] class InstanceMethodNoParameters[V <: Var[V]](
                                                          val thisParameter : Some[V]
                                                          ) extends Parameters[V] {

    def parameters : Array[V] = Parameters.noParameters.asInstanceOf[Array[V]]


}

private[tac] class InstanceMethodParameters[V <: Var[V]](
                                                            val thisParameter : Some[V],
                                                            val parameters: Array[V]
                                                        ) extends Parameters[V]


