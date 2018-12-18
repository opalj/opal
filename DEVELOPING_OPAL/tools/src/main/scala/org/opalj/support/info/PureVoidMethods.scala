/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.net.URL

import org.opalj.br.DefinedMethod
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport
import org.opalj.fpcf.PropertyStoreKey
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.fpcf.analyses.LazyStaticDataUsageAnalysis
import org.opalj.fpcf.analyses.LazyL0CompileTimeConstancyAnalysis
import org.opalj.fpcf.analyses.LazyFieldLocalityAnalysis
import org.opalj.fpcf.analyses.LazyClassImmutabilityAnalysis
import org.opalj.fpcf.analyses.LazyTypeImmutabilityAnalysis
import org.opalj.fpcf.analyses.LazyVirtualMethodStaticDataUsageAnalysis
import org.opalj.fpcf.analyses.LazyVirtualCallAggregatingEscapeAnalysis
import org.opalj.fpcf.analyses.LazyVirtualReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.LazyL1FieldMutabilityAnalysis
import org.opalj.fpcf.analyses.LazyVirtualMethodPurityAnalysis
import org.opalj.fpcf.analyses.escape.LazyInterProceduralEscapeAnalysis
import org.opalj.fpcf.analyses.escape.LazyReturnValueFreshnessAnalysis
import org.opalj.fpcf.analyses.purity.EagerL2PurityAnalysis
import org.opalj.fpcf.properties.Pure
import org.opalj.fpcf.properties.SideEffectFree
import org.opalj.fpcf.properties.CompileTimePure
import org.opalj.fpcf.FinalEP
import org.opalj.ai.fpcf.analyses.LazyL0BaseAIAnalysis
import org.opalj.tac.fpcf.analyses.TACAITransformer

/**
 * Identifies pure/side-effect free methods with a void return type.
 *
 * @author Dominik Helm
 */
object PureVoidMethods extends DefaultOneStepAnalysis {

    override def title: String = "Pure Void Methods Analysis"

    override def description: String = {
        "finds useless methods because they are side effect free and do not return a value (void)"
    }

    override def doAnalyze(
        project: Project[URL], parameters: Seq[String], isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val propertyStore = project.get(PropertyStoreKey)

        project.get(FPCFAnalysesManagerKey).runAll(
            LazyL0BaseAIAnalysis,
            TACAITransformer,
            LazyL0CompileTimeConstancyAnalysis,
            LazyStaticDataUsageAnalysis,
            LazyVirtualMethodStaticDataUsageAnalysis,
            LazyInterProceduralEscapeAnalysis,
            LazyVirtualCallAggregatingEscapeAnalysis,
            LazyReturnValueFreshnessAnalysis,
            LazyVirtualReturnValueFreshnessAnalysis,
            LazyFieldLocalityAnalysis,
            LazyL1FieldMutabilityAnalysis,
            LazyClassImmutabilityAnalysis,
            LazyTypeImmutabilityAnalysis,
            LazyVirtualMethodPurityAnalysis,
            EagerL2PurityAnalysis
        )

        val entities = propertyStore.entities(fpcf.properties.Purity.key)

        val voidReturn = entities.collect {
            case FinalEP(m: DefinedMethod, p @ (CompileTimePure | Pure | SideEffectFree)) // Do not report empty methods, they are e.g. used for base implementations of listeners
            // Empty methods still have a return instruction and therefore a body size of 1
            if m.definedMethod.returnType.isVoidType && !m.definedMethod.isConstructor &&
                m.definedMethod.body.isDefined && m.definedMethod.body.get.instructions.length != 1 ⇒
                (m, p)
        }

        BasicReport(
            voidReturn.toIterable map { mp ⇒
                val (m, p) = mp
                s"${m.toJava} has a void return type but it is $p"
            }
        )
    }
}
