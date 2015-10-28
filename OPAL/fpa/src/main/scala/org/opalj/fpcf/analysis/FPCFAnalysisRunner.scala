package org.opalj
package fpcf
package analysis

import org.opalj.br.analyses.SomeProject

/**
 * 
 * The results of the analysis which the `FPCFAnalysisRunner` object run are saved
 * within the [[PropertyStore]] of the [[Project]].
 * 
 * @author Michael Reif
 */
trait FPCFAnalysisRunner[T <: FPCFAnalysis[_ <: Entity]] {
    
    final val uniqueId: Int = FPCFAnalysisRunner.nextId
    
    /**
     * Returns the information which other analyses strictly need to be executed
     * before this analysis can be performed.
     *
     * @note A analysis has only to be added to the requirements if and only if this analysis 
     * depends on the computed property of the analysis and the property key has no fallback
     * such that it is only available if the regarding analysis is executed.
     */
    def requirements: Set[FPCFAnalysis[_ <: Entity]] = Set.empty

    /**
     * Returns the information which analyses should be executed to achieve
     * the most precise analysis result.
     *
     * @note These analyses are not required. Hence, the analysis will always compute a correct
     * result. If the set of recommendations is not empty, you may lose precision for every analysis
     * that is not executed in parallel.
     */
    def recommendations: Set[FPCFAnalysis[_ <: Entity]] = Set.empty
    
    /**
     * Trigger the start of the analysis.
     */
    protected def start(project: SomeProject): Unit
}

/**
 * 
 * Companion object of FPCFAnalysisRunner.
 * 
 * @author Michael Reif
 */
private object FPCFAnalysisRunner {
    private[this] val idGenerator = new java.util.concurrent.atomic.AtomicInteger(0)
    private[this] var executedAnalyses : Set[_ <: FPCFAnalysisRunner[_]] = Set.empty
    
    private[FPCFAnalysisRunner] def nextId: Int = {
        idGenerator.getAndIncrement()
    }
    
    private[FPCFAnalysisRunner] def alreadyExecuted[T <: FPCFAnalysisRunner[_]](
            analysis: T) : Boolean = this.synchronized {
        executedAnalyses contains analysis
    }
    
    private[FPCFAnalysisRunner] def addAsExecuted[T <: FPCFAnalysisRunner[_]](
            analysis: T) : Unit = this.synchronized {
        executedAnalyses += analysis
    }
}