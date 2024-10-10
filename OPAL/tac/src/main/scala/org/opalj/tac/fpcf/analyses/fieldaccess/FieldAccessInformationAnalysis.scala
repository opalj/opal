/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package fieldaccess

import org.opalj.br.Method
import org.opalj.br.analyses.DeclaredFields
import org.opalj.br.analyses.DeclaredFieldsKey
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.fpcf.ContextProviderKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.analyses.ContextProvider
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.cg.Callers
import org.opalj.br.fpcf.properties.fieldaccess.DirectFieldAccesses
import org.opalj.br.fpcf.properties.fieldaccess.FieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.FieldWriteAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.MethodFieldReadAccessInformation
import org.opalj.br.fpcf.properties.fieldaccess.MethodFieldWriteAccessInformation
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.tac.fpcf.analyses.cg.BaseAnalysisState
import org.opalj.tac.fpcf.analyses.cg.DefinedBodyReachableMethodAnalysis
import org.opalj.tac.fpcf.analyses.cg.persistentUVar
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.TheTACAI
import org.opalj.value.ValueInformation

/**
 * A simple analysis that identifies every direct read and write access to a [[org.opalj.br.Field]].
 *
 * @note Fields which are not accessed at all are not further considered.
 *
 * @author Maximilian Rüsch
 */
sealed trait FieldAccessInformationAnalysis extends FPCFAnalysis {

    val project: SomeProject

    type ContextType <: Context

    protected[this] class State(
        override val callContext:                  ContextType,
        override protected[this] var _tacDependee: EOptionP[Method, TACAI]
    ) extends BaseAnalysisState with TACAIBasedAnalysisState[ContextType]

    protected[this] val declaredFields: DeclaredFields = project.get(DeclaredFieldsKey)

    protected[this] def handleTac(
        context: ContextType,
        tac:     TACode[TACMethodParameter, DUVar[ValueInformation]]
    )(implicit fieldAccesses: DirectFieldAccesses): Unit = {
        tac.stmts.foreach {
            case _ @Assignment(_, _, GetField(pc, declaringClass, fieldName, fieldType, objRef)) =>
                fieldAccesses.addFieldRead(
                    context,
                    pc,
                    declaredFields(declaringClass, fieldName, fieldType),
                    persistentUVar(objRef.asVar)(tac.stmts)
                )
            case _ @ExprStmt(_, GetField(pc, declaringClass, fieldName, fieldType, objRef)) =>
                fieldAccesses.addFieldRead(
                    context,
                    pc,
                    declaredFields(declaringClass, fieldName, fieldType),
                    persistentUVar(objRef.asVar)(tac.stmts)
                )

            case _ @Assignment(_, _, GetStatic(pc, declaringClass, fieldName, fieldType)) =>
                fieldAccesses.addFieldRead(context, pc, declaredFields(declaringClass, fieldName, fieldType), None)
            case _ @ExprStmt(_, GetStatic(pc, declaringClass, fieldName, fieldType)) =>
                fieldAccesses.addFieldRead(context, pc, declaredFields(declaringClass, fieldName, fieldType), None)

            case PutField(pc, declaringClass, fieldName, fieldType, objRef, value) =>
                fieldAccesses.addFieldWrite(
                    context,
                    pc,
                    declaredFields(declaringClass, fieldName, fieldType),
                    persistentUVar(objRef.asVar)(tac.stmts),
                    persistentUVar(value.asVar)(tac.stmts)
                )
            case PutStatic(pc, declaringClass, fieldName, fieldType, value) =>
                fieldAccesses.addFieldWrite(
                    context,
                    pc,
                    declaredFields(declaringClass, fieldName, fieldType),
                    None,
                    persistentUVar(value.asVar)(tac.stmts)
                )

            case _ => /* Nothing to do */
        }
    }
}

private[fieldaccess] class EagerTacBasedFieldAccessInformationAnalysis(
    val project: SomeProject
) extends FieldAccessInformationAnalysis {

    private[this] val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    private[this] val contextProvider: ContextProvider = project.get(ContextProviderKey)

    def analyzeMethod(method: Method): PropertyComputationResult = {
        val context = contextProvider.newContext(declaredMethods(method)).asInstanceOf[ContextType]

        val tacEP = propertyStore(method, TACAI.key)

        if (tacEP.hasUBP && tacEP.ub.tac.isDefined) {
            analyzeWithTAC(context, tacEP.asInstanceOf[EPS[Method, TACAI]])
        } else {
            InterimPartialResult(Set(tacEP), tac => analyzeWithTAC(context, tac.asInstanceOf[EPS[Method, TACAI]]))
        }
    }

    private def analyzeWithTAC(context: ContextType, tacEP: EPS[Method, TACAI]): ProperPropertyComputationResult = {
        implicit val state: State = new State(context, tacEP)
        implicit val fieldAccesses: DirectFieldAccesses = new DirectFieldAccesses()

        handleTac(context, tacEP.ub.tac.get)
        returnResult(context)
    }

    private def continuation(context: ContextType, state: State)(eps: SomeEPS): ProperPropertyComputationResult = {
        eps match {
            case UBP(tac: TheTACAI) =>
                val fieldAccesses = new DirectFieldAccesses()
                handleTac(context, tac.theTAC)(fieldAccesses)

                returnResult(context)(state, fieldAccesses)

            case _ =>
                InterimPartialResult(state.dependees, continuation(context, state))
        }
    }

    private def returnResult(context: ContextType)(implicit
        state:         State,
        fieldAccesses: DirectFieldAccesses
    ): ProperPropertyComputationResult = {
        if (state.hasOpenDependencies)
            Results(
                InterimPartialResult(state.dependees, continuation(context, state)),
                fieldAccesses.partialResults(context)
            )
        else
            Results(fieldAccesses.partialResults(context))
    }
}

private[fieldaccess] class ReachableTacBasedFieldAccessInformationAnalysis(
    val project: SomeProject
) extends FieldAccessInformationAnalysis with DefinedBodyReachableMethodAnalysis {

    override def processMethod(callContext: ContextType, tacEP: EPS[Method, TACAI]): ProperPropertyComputationResult = {
        implicit val state: State = new State(callContext, tacEP)
        implicit val fieldAccesses: DirectFieldAccesses = new DirectFieldAccesses()

        handleTac(callContext, tacEP.ub.tac.get)
        returnResult(callContext)
    }

    private def returnResult(context: ContextType)(implicit
        state:         State,
        fieldAccesses: DirectFieldAccesses
    ): ProperPropertyComputationResult = {
        if (state.hasOpenDependencies)
            Results(
                InterimPartialResult(state.dependees, continuationForTAC(context.method)),
                fieldAccesses.partialResults(context)
            )
        else
            Results(fieldAccesses.partialResults(context))
    }
}

private[fieldaccess] trait FieldAccessInformationAnalysisScheduler extends FPCFAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = Seq(DeclaredFieldsKey)

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        TACAI,
        FieldReadAccessInformation,
        FieldWriteAccessInformation,
        MethodFieldReadAccessInformation,
        MethodFieldWriteAccessInformation
    )

    protected def derivedProperties: Set[PropertyBounds] = PropertyBounds.ubs(
        FieldReadAccessInformation,
        FieldWriteAccessInformation,
        MethodFieldReadAccessInformation,
        MethodFieldWriteAccessInformation
    )
}

object EagerFieldAccessInformationAnalysis
    extends FieldAccessInformationAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def requiredProjectInformation: ProjectInformationKeys = super.requiredProjectInformation ++ Seq(
        DeclaredMethodsKey,
        ContextProviderKey
    )

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def derivesCollaboratively: Set[PropertyBounds] = derivedProperties

    override def start(p: SomeProject, ps: PropertyStore, i: InitializationData): FPCFAnalysis = {
        val analysis = new EagerTacBasedFieldAccessInformationAnalysis(p)
        ps.scheduleEagerComputationsForEntities(p.allMethodsWithBody)(analysis.analyzeMethod)
        analysis
    }
}

object TriggeredFieldAccessInformationAnalysis
    extends FieldAccessInformationAnalysisScheduler
    with BasicFPCFTriggeredAnalysisScheduler {

    override def triggeredBy: PropertyKind = Callers

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def derivesCollaboratively: Set[PropertyBounds] = derivedProperties

    override def register(p: SomeProject, ps: PropertyStore, i: InitializationData): FPCFAnalysis = {
        val analysis = new ReachableTacBasedFieldAccessInformationAnalysis(p)
        ps.registerTriggeredComputation(Callers.key, analysis.analyze)
        analysis
    }
}
