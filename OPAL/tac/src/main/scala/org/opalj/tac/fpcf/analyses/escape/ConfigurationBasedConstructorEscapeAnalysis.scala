/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package escape

import org.opalj.br.ClassType
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.util.elidedAssert

import pureconfig.*

/**
 * In the configuration system it is possible to define escape information for the this-local in the
 * constructors of a specific class. This analysis sets the [[org.opalj.br.analyses.VirtualFormalParameter]] of the
 * this-local to the defined value.
 *
 * @author Florian Kuebler
 */
trait ConfigurationBasedConstructorEscapeAnalysis extends AbstractEscapeAnalysis {

    override type AnalysisContext <: AbstractEscapeAnalysisContext

    private case class PredefinedResult(classType: String, escapeOfThis: String) derives ConfigReader

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
            // 1. Get the ClassLoader
            val classLoader = this.getClass.getClassLoader

            // 2. Load the companion object or module class
            // Note: Scala modules (object) compiled to Java have a '$' suffix
            val moduleClass = classLoader.loadClass(r.escapeOfThis + "$")

            // 3. Access the singleton instance via the MODULE$ field
            val moduleField = moduleClass.getField("MODULE$")
            val property = moduleField.get(null).asInstanceOf[EscapeProperty]

            (ClassType(r.classType), property)
        }.toMap
    }

    abstract override protected def handleThisLocalOfConstructor(
        call: NonVirtualMethodCall[V]
    )(implicit context: AnalysisContext, state: AnalysisState): Unit = {
        elidedAssert(call.name == "<init>")
        elidedAssert(state.usesDefSite(call.receiver))
        elidedAssert(call.declaringClass.isClassType)

        val propertyOption = predefinedConstructors.get(call.declaringClass.asClassType)

        // the object constructor will not escape the this-local
        if (propertyOption.nonEmpty) {
            state.meetMostRestrictive(propertyOption.get)
        } else {
            super.handleThisLocalOfConstructor(call)
        }
    }
}
