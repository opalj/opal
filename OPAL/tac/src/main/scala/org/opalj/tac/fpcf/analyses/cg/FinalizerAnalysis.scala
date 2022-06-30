/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimEUBP
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.br.MethodDescriptor
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.DeclaredMethod
import org.opalj.tac.fpcf.properties.cg.Callers
import org.opalj.tac.fpcf.properties.cg.NoCallers
import org.opalj.tac.fpcf.properties.cg.OnlyVMLevelCallers

/**
 * Computes the set of finalize methods that are being called by the VM during the execution of the
 * `project`.
 * Extends the call graph analysis ([[org.opalj.tac.fpcf.analyses.cg.CallGraphAnalysis]]) to include
 * the calls to these methods.
 *
 * @author Florian Kuebler
 */
class FinalizerAnalysis private[analyses] ( final val project: SomeProject) extends FPCFAnalysis {

    implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

    def analyze(dm: DeclaredMethod): PropertyComputationResult = {
        // the method is a constructor?
        if (dm.name != "<init>")
            return NoResult;

        // is the method reachable?
        (propertyStore(dm, Callers.key): @unchecked) match {
            case FinalP(NoCallers) =>
                // nothing to do, since there is no caller
                return NoResult;

            case eps: EPS[_, _] =>
                if (eps.ub eq NoCallers) {
                    // we can not create a dependency here, so the analysis is not allowed to create
                    // such a result
                    throw new IllegalStateException("illegal immediate result for callers")
                }
        }

        val instantiatedType = dm.declaringClassType

        val finalizers = project.resolveAllMethodReferences(
            instantiatedType,
            "finalize",
            MethodDescriptor.NoArgsAndReturnVoid
        )
        assert(finalizers.size < 2)

        val r = finalizers.map { finalizerMethod =>
            val finalizer = declaredMethods(finalizerMethod)
            PartialResult[DeclaredMethod, Callers](finalizer, Callers.key, {
                case InterimUBP(ub: Callers) =>
                    if (!ub.hasVMLevelCallers)
                        Some(InterimEUBP(finalizer, ub.updatedWithVMLevelCall()))
                    else None
                case _: EPK[_, _] =>
                    Some(InterimEUBP(finalizer, OnlyVMLevelCallers))
                case r =>
                    throw new IllegalStateException(s"unexpected previous result $r")
            })
        }

        Results(r)
    }
}

object FinalizerAnalysisScheduler extends BasicFPCFTriggeredAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys =
        Seq(DeclaredMethodsKey)

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(Callers)

    override def derivesCollaboratively: Set[PropertyBounds] = Set(
        PropertyBounds.ub(Callers)
    )

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FinalizerAnalysis = {
        val analysis = new FinalizerAnalysis(p)
        ps.registerTriggeredComputation(Callers.key, analysis.analyze)
        analysis
    }

    override def triggeredBy: PropertyKind = Callers
}
