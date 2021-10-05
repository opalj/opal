/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses
package cg

import net.ceedubs.ficus.Ficus._

import org.opalj.log.OPALLogger

/**
 * The ''key'' object to get a function that determines whether a type is directly
 * extensible or not.
 *
 * @see [[ClassExtensibility]] for further information.
 * @author Michael Reif
 */
object ClassExtensibilityKey extends ProjectInformationKey[ClassExtensibility, Nothing] {

    final val ConfigKeyPrefix = "org.opalj.br.analyses.cg.ClassExtensibilityKey."

    final val DefaultClassExtensibilityAnalysis = {
        "org.opalj.br.analyses.cg.DefaultClassExtensibility"
    }

    /**
     * The [[ClassExtensibilityKey]] has the [[ClosedPackagesKey]] as prerequisite.
     */
    override def requirements(project: SomeProject): ProjectInformationKeys = Seq(ClosedPackagesKey)

    /**
     * Computes the direct type extensibility information for the given project.
     */
    override def compute(project: SomeProject): ClassExtensibility = {
        val configKey = ConfigKeyPrefix+"analysis"
        try {
            val configuredAnalysis = project.config.as[Option[String]](configKey)
            val analysisClassName = configuredAnalysis.getOrElse(DefaultClassExtensibilityAnalysis)
            val constructor = Class.forName(analysisClassName).getConstructors.head
            constructor.newInstance(project).asInstanceOf[ClassExtensibility]
        } catch {
            case t: Throwable =>
                val m = "cannot compute the extensibility of classes; extensibility will be unknown"
                OPALLogger.error("project configuration", m, t)(project.logContext)
                new ClassExtensibility {
                    def isClassExtensible(t: ObjectType): Answer = Unknown
                }
        }
    }

}
