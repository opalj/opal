/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package si
package flowanalysis

/**
 * A mapping from [[PDUWeb]] to [[Data]], used to identify the state of variables during a given fixed point of the
 * [[org.opalj.si.flowanalysis.DataFlowAnalysis]].
 *
 * @author Dominik Helm
 */
trait DataFlowEnvironment[Data, T <: DataFlowEnvironment[Data, T]] { self =>

    val top: Data
    val bottom: Data

    def join(other: T): T

    def joinMany(envs: Iterable[T]): T

    def updateAll(value: Data): T

}
