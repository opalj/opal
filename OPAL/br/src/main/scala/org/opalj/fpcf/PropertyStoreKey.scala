/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf

import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.concurrent.NumberOfThreadsForCPUBoundTasks
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
    extends ProjectInformationKey[PropertyStore, (List[PropertyStoreContext[AnyRef]]) ⇒ PropertyStore] {

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
    override protected def requirements: Seq[ProjectInformationKey[Nothing, Nothing]] = Nil

    /**
     * Creates a new empty property store using the current [[parallelismLevel]].
     */
    override protected def compute(project: SomeProject): PropertyStore = {
        implicit val logContext: LogContext = project.logContext

        val context: List[PropertyStoreContext[AnyRef]] = List(
            PropertyStoreContext(classOf[SomeProject], project)
        )
        project.getProjectInformationKeyInitializationData(this) match {
            case Some(psFactory) ⇒
                OPALLogger.info(
                    "analysis configuration",
                    "the PropertyStore is created using project information key initialization data"
                )(project.logContext)
                psFactory(context)
            case None ⇒
                val ps = seq.PKESequentialPropertyStore(context: _*)
                // val ps = par.PKEParallelTasksPropertyStore(context: _*)
                ps
        }
    }
}
