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
package org.opalj.ai.analyses

import java.util.concurrent.ConcurrentLinkedQueue

import org.opalj.ai.{FormalParameter, ValueOrigin}
import org.opalj.br.Method
import org.opalj.br.analyses.{ProjectInformationKey, SomeProject}
import org.opalj.concurrent.defaultIsInterrupted
import org.opalj.log.OPALLogger

import scala.collection.JavaConverters._

/**
  * The set of all formal parameters in a project. The set also contains formal parameters of
  * methods in libraries, if the bodies of the libraries are loaded.
  *
  * @author Florian Kuebler
  */
class FormalParameters private[analyses](val data: Map[Method, Map[Int, FormalParameter]]) {

    def apply(m: Method): Map[Int, FormalParameter] = data.getOrElse(m, Map.empty)

    def formalParameters: Iterable[FormalParameter] = data.values.flatMap(_.values)
}

/**
  * The ''key'' object to get information about all formal parameters.
  *
  * @note See [[org.opalj.ai.FormalParameter]] for further details.
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
        implicit val logContext = p.logContext
        val sites = new ConcurrentLinkedQueue[(Method, Map[ValueOrigin, FormalParameter])]

        val errors = p.parForeachMethodWithBody(defaultIsInterrupted) { methodInfo ⇒
            val m = methodInfo.method
            val md = m.descriptor
            var fps = for {
                i ← 0 until md.parametersCount
                origin = org.opalj.ai.parameterIndexToValueOrigin(m.isStatic, md, i)
            } yield {
                origin → new FormalParameter(m, origin)
            }

            if (m.isNotStatic) fps = fps :+ -1 -> new FormalParameter(m, -1)

            if (fps.nonEmpty) sites.add((m, fps.toMap))
        }

        errors foreach { e ⇒
            OPALLogger.error("formal parameters", "collecting all formal parameters failed", e)
        }

        new FormalParameters(Map.empty ++ sites.asScala)
    }

    //
    // HELPER FUNCTION TO MAKE IT EASILY POSSIBLE TO ADD FORMAL PARAMETERS TO A PROPERTYSTORE
    // AND TO ENSURE THAT FORMAL PARAMETERS AND THE PROPERTYSTORE CONTAIN THE SAME OBJECTS!
    //

    final val entityDerivationFunction: (SomeProject) ⇒ (Traversable[AnyRef], FormalParameters) =
        (p: SomeProject) ⇒ {
            // this will collect the formal parameters of the project if not yet collected...
            val formalParameters = p.get(FormalParametersKey)
            (formalParameters.data.values.flatMap(_.values), formalParameters)
        }
}
