/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf

import com.typesafe.config.Config
import net.ceedubs.ficus.Ficus._

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.PropertyStoreContext
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject

/**
 * The ''key'' object to get the project's [[org.opalj.fpcf.PropertyStore]].
 *
 * @note   It is possible to set the project's `debug` flag using the project's
 *         `org.opalj.br.analyses.PropertyStore.debug` config key.
 *
 * @author Michael Eichberg
 */
object PropertyStoreKey
    extends ProjectInformationKey[PropertyStore, (List[PropertyStoreContext[AnyRef]]) => PropertyStore] {

    final val configKey = "org.opalj.fpcf.PropertyStore.Default"

    /**
     * Used to specify the number of threads the property store should use. This
     * value is read only once when the property store is created.
     *
     * The value must be larger than 0 and should be smaller or equal to the number
     * of (hyperthreaded) cores.
     */
    @volatile var parallelismLevel: Int = Math.max(NumberOfThreadsForCPUBoundTasks, 2)

    /**
     * The [[PropertyStoreKey]] has no special prerequisites.
     *
     * @return `Nil`.
     */
    override def requirements(project: SomeProject): Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    /**
     * Creates a new empty property store using the current [[parallelismLevel]].
     */
    override def compute(project: SomeProject): PropertyStore = {
        implicit val logContext: LogContext = project.logContext

        val context: List[PropertyStoreContext[AnyRef]] = List(
            PropertyStoreContext(classOf[SomeProject], project),
            PropertyStoreContext(classOf[Config], project.config)
        )
        project.getProjectInformationKeyInitializationData(this) match {
            case Some(psFactory) =>
                OPALLogger.info(
                    "analysis configuration",
                    s"the PropertyStore is initialized using: $psFactory"
                )
                psFactory(context)
            case None =>
                val ps = project.config.as[Option[String]](configKey) match {
                    case Some("Sequential") =>
                        org.opalj.fpcf.seq.PKESequentialPropertyStore(context: _*)
                    case Some("Parallel") | None =>
                        org.opalj.fpcf.par.PKECPropertyStore(context: _*)
                    case Some(unknown) =>
                        OPALLogger.error(
                            "analysis configuration",
                            s"unknown PropertyStore $unknown configured,"+
                                " using PKECPropertyStore instead"
                        )
                        org.opalj.fpcf.par.PKECPropertyStore(context: _*)
                }
                ps
        }
    }
}
