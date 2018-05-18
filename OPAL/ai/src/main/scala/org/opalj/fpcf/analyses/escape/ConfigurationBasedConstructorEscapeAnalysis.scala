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
package fpcf
package analyses
package escape

import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

import org.opalj.br.ObjectType
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.tac.NonVirtualMethodCall

/**
 * In the configuration system it is possible to define escape information for the this local in the
 * constructors of a specific class. This analysis sets the [[org.opalj.br.analyses.VirtualFormalParameter]] of the this local
 * to the defined value.
 *
 * @author Florian Kuebler
 */
trait ConfigurationBasedConstructorEscapeAnalysis extends AbstractEscapeAnalysis {

    override type AnalysisContext <: AbstractEscapeAnalysisContext

    private[this] case class PredefinedResult(object_type: String, escape_of_this: String)

    private[this] val ConfigKey = {
        "org.opalj.fpcf.analyses.ConfigurationBasedConstructorEscapeAnalysis.constructors"
    }

    /**
     * Statically loads the configuration and gets the escape property objects via reflection.
     *
     * @note The reflective code assumes that every [[EscapeProperty]] is an object and not a class.
     */
    private[this] val predefinedConstructors: Map[ObjectType, EscapeProperty] = {
        project.config.as[Seq[PredefinedResult]](ConfigKey).map { r ⇒
            import scala.reflect.runtime._
            val rootMirror = universe.runtimeMirror(getClass.getClassLoader)
            val module = rootMirror.staticModule(r.escape_of_this)
            val property = rootMirror.reflectModule(module).instance.asInstanceOf[EscapeProperty]
            (ObjectType(r.object_type), property)
        }.toMap
    }

    protected[this] abstract override def handleThisLocalOfConstructor(
        call: NonVirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        assert(call.name == "<init>")
        assert(context.usesDefSite(call.receiver))
        assert(call.declaringClass.isObjectType)

        val propertyOption = predefinedConstructors.get(call.declaringClass.asObjectType)

        // the object constructor will not escape the this local
        if (propertyOption.nonEmpty) {
            state.meetMostRestrictive(propertyOption.get)
        } else {
            super.handleThisLocalOfConstructor(call)
        }
    }
}

