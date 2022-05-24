/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.net.URL

import org.opalj.fpcf.FinalEP
import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.br.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.br.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.br.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.br.fpcf.properties.CompileTimePure
import org.opalj.br.fpcf.properties.Pure
import org.opalj.br.fpcf.properties.SideEffectFree
import org.opalj.tac.cg.RTACallGraphKey
import org.opalj.tac.fpcf.analyses.purity.EagerL2PurityAnalysis
import org.opalj.tac.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.tac.fpcf.analyses.LazyL1FieldMutabilityAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.tac.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis

/**
 * Identifies pure/side-effect free methods with a void return type.
 *
 * @author Dominik Helm
 */
object PureVoidMethods extends ProjectAnalysisApplication {

    override def title: String = "Pure Void Methods Analysis"

    override def description: String = {
        "finds useless methods because they are side effect free and do not return a value (void)"
    }

    override def doAnalyze(
        project: Project[URL], parameters: Seq[String], isInterrupted: () => Boolean
    ): BasicReport = {

        val propertyStore = project.get(PropertyStoreKey)

        project.get(RTACallGraphKey)

        project.get(FPCFAnalysesManagerKey).runAll(
            LazyL0CompileTimeConstancyAnalysis,
            LazyStaticDataUsageAnalysis,
            LazyInterProceduralEscapeAnalysis,
            LazyReturnValueFreshnessAnalysis,
            LazyFieldLocalityAnalysis,
            LazyL1FieldMutabilityAnalysis,
            LazyClassImmutabilityAnalysis,
            LazyTypeImmutabilityAnalysis,
            EagerL2PurityAnalysis
        )

        val entities = propertyStore.entities(br.fpcf.properties.Purity.key)

        val voidReturn = entities.collect {
            case FinalEP(m: DefinedMethod, p @ (CompileTimePure | Pure | SideEffectFree)) // Do not report empty methods, they are e.g. used for base implementations of listeners
            // Empty methods still have a return instruction and therefore a body size of 1
            if m.definedMethod.returnType.isVoidType && !m.definedMethod.isConstructor &&
                m.definedMethod.body.isDefined && m.definedMethod.body.get.instructions.length != 1 =>
                (m, p)
        }

        BasicReport(
            voidReturn.iterator.to(Iterable) map { mp =>
                val (m, p) = mp
                s"${m.toJava} has a void return type but it is $p"
            }
        )
    }
}
