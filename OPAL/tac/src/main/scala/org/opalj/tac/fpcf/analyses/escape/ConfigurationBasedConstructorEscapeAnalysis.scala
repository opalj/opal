/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package escape

import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

import org.opalj.br.ObjectType
import org.opalj.br.fpcf.properties.EscapeProperty

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
        project.config.as[Seq[PredefinedResult]](ConfigKey).map { r =>
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
        assert(state.usesDefSite(call.receiver))
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

