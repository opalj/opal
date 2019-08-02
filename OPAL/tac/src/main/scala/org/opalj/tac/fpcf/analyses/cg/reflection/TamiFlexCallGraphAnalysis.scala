/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg
package reflection

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.RefArray
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.value.ValueInformation
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.ArrayType
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.tac.fpcf.analyses.pointsto.TamiFlexKey
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Adds the specified calls from the tamiflex.log to the call graph.
 * TODO: Merge with reflection analysis
 * TODO: Also handle class-forName -> Loaded classes
 * @author Florian Kuebler
 */
class TamiFlexCallGraphAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {
    val declaredMethods = project.get(DeclaredMethodsKey)
    val ConstructorT = ObjectType("java/lang/reflect/Constructor")
    def process(p: SomeProject): PropertyComputationResult = {
        val analyses = List(
            new TamiFlexMethodInvokeAnalysis(
                project,
                declaredMethods(
                    ObjectType.Method,
                    "",
                    ObjectType.Method,
                    "invoke",
                    MethodDescriptor.apply(
                        RefArray(ObjectType.Object, ArrayType.ArrayOfObject), ObjectType.Object
                    )
                )
            ),
            new TamiFlexMethodInvokeAnalysis(
                project,
                declaredMethods(
                    ObjectType.Class,
                    "",
                    ObjectType.Class,
                    "newInstance",
                    MethodDescriptor.JustReturnsObject
                )
            ),
            new TamiFlexMethodInvokeAnalysis(
                project,
                declaredMethods(
                    ConstructorT,
                    "",
                    ConstructorT,
                    "newInstance",
                    MethodDescriptor(ArrayType.ArrayOfObject, ObjectType.Object)
                )
            )
        )
        Results(analyses.map(_.registerAPIMethod()))
    }
}

class TamiFlexMethodInvokeAnalysis private[analyses] (
        final val project: SomeProject, override val apiMethod: DeclaredMethod
) extends TACAIBasedAPIBasedCallGraphAnalysis {
    final private[this] val tamiFlexLogData = project.get(TamiFlexKey)

    override def processNewCaller(
        caller:          DefinedMethod,
        pc:              Int,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
    ): ProperPropertyComputationResult = {
        implicit val indirectCalls: IndirectCalls = new IndirectCalls()

        if (receiverOption.isDefined)
            handleMethodInvoke(caller, pc, receiverOption.get, params, tac)
        else
            indirectCalls.addIncompleteCallSite(pc)

        Results(indirectCalls.partialResults(caller))
    }
    private[this] def handleMethodInvoke(
        caller:       DefinedMethod,
        pc:           Int,
        receiver:     Expr[V],
        methodParams: Seq[Option[Expr[V]]],
        tac:          TACode[TACMethodParameter, V]
    )(implicit indirectCalls: IndirectCalls): Unit = {
        val line = caller.definedMethod.body.flatMap(b ⇒ b.lineNumber(pc)).getOrElse(-1)
        val targets = tamiFlexLogData.methodInvocations(caller, line)
        if (targets.isEmpty)
            return ;

        val (methodInvokeReceiver, methodInvokeActualParamsOpt) = if (methodParams.size == 2) { // Method.invoke
            // TODO we should probably match the method receiver information (e.g. points-to) to
            // each of the target methods to prevent spurious invocations (e.g. because of unknown
            // source line number
            (
                methodParams.head.map(_.asVar),
                methodParams(1).flatMap(p ⇒ VarargsUtil.getParamsFromVararg(p, tac.stmts, tac.cfg))
            )
        } else if (methodParams.size == 1) { // Constructor.newInstance
            (
                None,
                methodParams.head.flatMap(p ⇒ VarargsUtil.getParamsFromVararg(p, tac.stmts, tac.cfg))
            )
        } else { // Class.newInstance
            (None, Some(Seq.empty))
        }
        val persistentMethodInvokeReceiver = methodInvokeReceiver.flatMap(r ⇒ persistentUVar(r)(tac.stmts))
        val persistentMethodInvokeActualParams: Seq[Option[(ValueInformation, IntTrieSet)]] =
            methodInvokeActualParamsOpt.map(_.map(persistentUVar(_)(tac.stmts))).getOrElse(Seq.empty)

        for (target ← targets) {
            indirectCalls.addCall(
                caller,
                target,
                pc,
                persistentMethodInvokeActualParams,
                persistentMethodInvokeReceiver
            )
        }
    }
}

object TamiFlexCallGraphAnalysisScheduler extends BasicFPCFEagerAnalysisScheduler {

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        Callers,
        Callees,
        TACAI
    )

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(
        Callers,
        Callees
    )

    override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new TamiFlexCallGraphAnalysis(p)
        ps.scheduleEagerComputationForEntity(p)(analysis.process)
        analysis
    }

    override def derivesEagerly: Set[PropertyBounds] = Set.empty
}
