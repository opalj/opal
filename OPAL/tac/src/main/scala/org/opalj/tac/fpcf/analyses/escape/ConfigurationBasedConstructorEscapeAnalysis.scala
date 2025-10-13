/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package escape

import org.opalj.br.ClassType
import org.opalj.br.fpcf.properties.EscapeProperty

import pureconfig._
import pureconfig.generic.derivation.default._

/**
 * In the configuration system it is possible to define escape information for the this local in the
 * constructors of a specific class. This analysis sets the [[org.opalj.br.analyses.VirtualFormalParameter]] of the this local
 * to the defined value.
 *
 * @author Florian Kuebler
 */
trait ConfigurationBasedConstructorEscapeAnalysis extends AbstractEscapeAnalysis {

    override type AnalysisContext <: AbstractEscapeAnalysisContext

    private case class PredefinedResult(class_type: String, escape_of_this: String) derives ConfigReader

    private val ConfigKey = {
        "org.opalj.fpcf.analyses.ConfigurationBasedConstructorEscapeAnalysis.constructors"
    }

    /**
     * Statically loads the configuration and gets the escape property objects via reflection.
     *
     * @note The reflective code assumes that every [[EscapeProperty]] is an object and not a class.
     */
    private val predefinedConstructors: Map[ClassType, EscapeProperty] = {
        ConfigSource.fromConfig(project.config).at(ConfigKey).loadOrThrow[Seq[PredefinedResult]].map { r =>
            import scala.reflect.runtime.*
            val rootMirror = universe.runtimeMirror(getClass.getClassLoader)
            val module = rootMirror.staticModule(r.escape_of_this)
            val property = rootMirror.reflectModule(module).instance.asInstanceOf[EscapeProperty]
            (ClassType(r.class_type), property)
        }.toMap
    }

    abstract override protected def handleThisLocalOfConstructor(
        call: NonVirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        assert(call.name == "<init>")
        assert(state.usesDefSite(call.receiver))
        assert(call.declaringClass.isClassType)

        val propertyOption = predefinedConstructors.get(call.declaringClass.asClassType)

        // the object constructor will not escape the this local
        if (propertyOption.nonEmpty) {
            state.meetMostRestrictive(propertyOption.get)
        } else {
            super.handleThisLocalOfConstructor(call)
        }
    }
}
