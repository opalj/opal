/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package fpcf
package analyses

import net.ceedubs.ficus.Ficus._
import org.opalj.ai.isVMLevelValue
import org.opalj.ai.pcOfVMLevelValue
import org.opalj.br.ComputationalTypeReference
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.MethodDescriptor
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.EmptyIntTrieSet
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.properties.ClassImmutability
import org.opalj.fpcf.properties.ClassifiedImpure
import org.opalj.fpcf.properties.EffectivelyFinalField
import org.opalj.fpcf.properties.FieldMutability
import org.opalj.fpcf.properties.ImmutableObject
import org.opalj.fpcf.properties.ImmutableType
import org.opalj.fpcf.properties.LBImpure
import org.opalj.fpcf.properties.LBSideEffectFree
import org.opalj.fpcf.properties.Purity
import org.opalj.fpcf.properties.TypeImmutability
import org.opalj.fpcf.properties.VirtualMethodPurity
import org.opalj.fpcf.properties.LBPure
import org.opalj.tac.ArrayLoad
import org.opalj.tac.Expr
import org.opalj.tac.GetField
import org.opalj.tac.New
import org.opalj.tac.NewArray
import org.opalj.tac.OriginOfThis
import org.opalj.tac.Stmt
import org.opalj.tac.TACode

/**
 * An inter-procedural analysis to determine a method's purity.
 *
 * @note This analysis is sound only up to the usual standards, i.e. it does not cope with
 *       VirtualMachineErrors and may be unsound in the presence of native code, reflection or
 *       `sun.misc.Unsafe`. Calls to native methods are generally handled soundly as they are
 *       considered [[org.opalj.fpcf.properties.LBImpure]]. There are no soundness guarantees in the
 *       presence of load-time transformation. Soundness in general depends on the soundness of the
 *       analyses that compute properties used by this analysis, e.g. field mutability.
 *
 * @note This analysis is sound even if the three address code hierarchy is not flat, it will
 *       produce better results for a flat hierarchy, though. This is because it will not assess the
 *       types of expressions other than [[org.opalj.tac.Var]]s.
 *
 * @note This analysis derives all purity levels except for the `Externally` variants. A
 *       configurable [[DomainSpecificRater]] is used to identify calls, expressions and exceptions
 *       that are `LBDPure` instead of `LBImpure` or any `SideEffectFree` purity level.
 *       Compared to the `L0PurityAnalysis`, it deals with all methods, even if their reference type
 *       parameters are mutable. It can handle accesses of (effectively) final instance fields,
 *       array loads, array length and virtual/interface calls. Array stores and field writes as
 *       well as (useless) synchronization on locally created, non-escaping objects/arrays are also
 *       handled. Newly allocated objects/arrays returned from callees are not identified.
 *
 * @author Dominik Helm
 */
class L1PurityAnalysis private[analyses] (val project: SomeProject) extends AbstractPurityAnalysis {

    /**
     * Holds the state of this analysis.
     * @param lbPurity The current minimum purity level for the method
     * @param ubPurity The current maximum purity level for the method that will be assigned by
     *                  checkPurityOfX methods to aggregrate the purity
     * @param dependees The set of entities/properties the purity depends on
     * @param method The currently analyzed method
     * @param definedMethod The corresponding DefinedMethod we report results for
     * @param declClass The declaring class of the currently analyzed method
     * @param code The code of the currently analyzed method
     */
    class State(
            var lbPurity:      Purity,
            var ubPurity:      Purity,
            var dependees:     Set[EOptionP[Entity, Property]],
            val method:        Method,
            val definedMethod: DeclaredMethod,
            val declClass:     ObjectType,
            val code:          Array[Stmt[V]]
    ) extends AnalysisState

    override type StateType = State

    val raterFqn: String = project.config.as[String](
        "org.opalj.fpcf.analyses.L1PurityAnalysis.domainSpecificRater"
    )

    val rater: DomainSpecificRater =
        L1PurityAnalysis.rater.getOrElse(resolveDomainSpecificRater(raterFqn))

    /**
     * Checks if a reference was created locally, hence actions on it might not influence purity.
     *
     * @note Fresh references can be treated as non-escaping as the analysis result will be impure
     *       if anything escapes the method via parameters, static field assignments or calls.
     */
    override def isLocal(expr: Expr[V], otherwise: Purity, excludedDefSites: IntTrieSet = EmptyIntTrieSet)(implicit state: State): Boolean = {
        if (expr.isConst)
            true
        else if (expr.asVar.value.computationalType ne ComputationalTypeReference) {
            // Primitive values are always local (required for parameters of contextually pure calls)
            true
        } else if (expr.isVar) {
            val defSites = expr.asVar.definedBy -- excludedDefSites
            if (defSites.forall { defSite ⇒
                if (defSite >= 0) {
                    val rhs = state.code(defSite).asAssignment.expr
                    if (rhs.isConst)
                        true
                    else {
                        val astID = rhs.astID
                        astID match {
                            case New.ASTID | NewArray.ASTID ⇒ true
                            case GetField.ASTID ⇒
                                val objRef = rhs.asGetField.objRef
                                isLocal(objRef, otherwise, excludedDefSites ++ defSites)
                            case ArrayLoad.ASTID ⇒
                                val arrayRef = rhs.asArrayLoad.arrayRef
                                isLocal(arrayRef, otherwise, excludedDefSites ++ defSites)
                            case _ ⇒ false
                        }
                    }
                } else if (isVMLevelValue(defSite)) {
                    true // VMLevelValues are freshly created
                } else {
                    // In initializers the self reference (this) is local
                    state.method.isConstructor && defSite == OriginOfThis
                }
            }) {
                true
            } else {
                atMost(otherwise)
                false
            }
        } else {
            // The expression could refer to further expressions in a non-flat representation.
            // In that case it could be, e.g., a GetStatic. In that case the reference is
            // not locally created and/or initialized. To avoid special handling, we just
            // fallback to false here as the analysis is intended to be used on flat
            // representations anyway.
            atMost(otherwise)
            false
        }
    }

    /**
     * Examines the influence of the purity property of a method on the examined method's purity.
     *
     * @note Adds dependendies when necessary.
     */
    def checkMethodPurity(
        ep:     EOptionP[DeclaredMethod, Property],
        params: (Option[Expr[V]], Seq[Expr[V]])
    )(implicit state: State): Boolean = ep match {
        case EPS(_, _, _: ClassifiedImpure | VirtualMethodPurity(_: ClassifiedImpure)) ⇒
            atMost(LBImpure)
            false
        case eps @ EPS(_, lb: Purity, ub: Purity) ⇒
            if (ub.modifiesReceiver) {
                atMost(LBImpure)
                false
            } else {
                if (eps.isRefinable && ((lb meet state.ubPurity) ne state.ubPurity)) {
                    state.dependees += ep // On Conditional, keep dependence
                    reducePurityLB(lb)
                }
                atMost(ub)
                true
            }
        case eps @ EPS(_, VirtualMethodPurity(lb: Purity), VirtualMethodPurity(ub: Purity)) ⇒
            if (ub.modifiesReceiver) {
                atMost(LBImpure)
                false
            } else {
                if (eps.isRefinable && ((lb meet state.ubPurity) ne state.ubPurity)) {
                    state.dependees += ep // On Conditional, keep dependence
                    reducePurityLB(lb)
                }
                atMost(ub)
                true
            }
        case _ ⇒
            state.dependees += ep
            reducePurityLB(LBImpure)
            true
    }

    /**
     * If the given objRef is not local, adds the dependee necessary if the field mutability is not
     * known yet.
     */
    override def handleUnknownFieldMutability(
        ep:     EOptionP[Field, FieldMutability],
        objRef: Option[Expr[V]]
    )(implicit state: State): Unit = {
        if (objRef.isEmpty || !isLocal(objRef.get, LBPure)) state.dependees += ep
    }

    /**
     * If the given expression is not local, adds the dependee necessary if the type mutability is
     * not known yet.
     */
    override def handleUnknownTypeMutability(
        ep:   EOptionP[ObjectType, Property],
        expr: Expr[V]
    )(implicit state: State): Unit = {
        if (!isLocal(expr, LBPure)) state.dependees += ep
    }

    def cleanupDependees()(implicit state: State): Unit = {
        // Remove unnecessary dependees
        if (!state.ubPurity.isDeterministic) {
            state.dependees = state.dependees.filter { ep ⇒
                ep.pk == Purity.key || ep.pk == VirtualMethodPurity.key
            }
        }
        //IMPROVE: We could filter Purity/VPurity dependees with an lb not less than maxPurity
    }

    /**
     * Continuation to handle updates to properties of dependees.
     * Dependees may be
     *     - methods called (for their purity)
     *     - fields read (for their mutability)
     *     - classes files for class types returned (for their mutability)
     */
    def continuation(eps: SomeEPS)(implicit state: State): PropertyComputationResult = {
        state.dependees = state.dependees.filter(_.e ne eps.e)
        val oldPurity = state.ubPurity

        eps match {
            // Cases dealing with other purity values
            case EPS(_, _, _: Purity | _: VirtualMethodPurity) ⇒
                if (!checkMethodPurity(eps.asInstanceOf[EOptionP[DeclaredMethod, Property]]))
                    return Result(state.definedMethod, LBImpure)

            // Cases that are pure
            case FinalEP(_, EffectivelyFinalField | // Reading eff. final fields
                ImmutableType | ImmutableObject) ⇒ // Returning immutable reference

            // Cases resulting in side-effect freeness
            case FinalEP(_, _: FieldMutability | // Reading non-final field
                _: TypeImmutability | _: ClassImmutability) ⇒ // Returning mutable reference
                atMost(LBSideEffectFree)

            case IntermediateEP(_, _, _) ⇒ state.dependees += eps
        }

        if (state.ubPurity ne oldPurity)
            cleanupDependees()

        if (state.dependees.isEmpty || (state.lbPurity eq state.ubPurity)) {
            Result(state.definedMethod, state.ubPurity)
        } else {
            IntermediateResult(
                state.definedMethod,
                state.lbPurity,
                state.ubPurity,
                state.dependees,
                continuation
            )
        }
    }

    /**
     * Determines the purity of the given method.
     *
     * @param definedMethod a defined method with body.
     */
    def determinePurity(definedMethod: DefinedMethod): PropertyComputationResult = {
        val method = definedMethod.methodDefinition
        val declClass = method.classFile.thisType

        // If this is not the method's declaration, but a non-overwritten method in a subtype,
        // don't re-analyze the code
        if (declClass ne definedMethod.declaringClassType)
            return baseMethodPurity(definedMethod);

        // We treat all synchronized methods as impure
        if (method.isSynchronized)
            return Result(definedMethod, LBImpure);

        val TACode(_, code, _, cfg, _, _) = tacai(method)

        implicit val state: State =
            new State(LBPure, LBPure, Set.empty, method, definedMethod, declClass, code)

        // Special case: The Throwable constructor is `LBSideEffectFree`, but subtype constructors
        // may not be because of overridable fillInStackTrace method
        if (method.isConstructor && declClass.isSubtypeOf(ObjectType.Throwable).isYes)
            project.instanceMethods(declClass).foreach { mdc ⇒
                if (mdc.name == "fillInStackTrace" &&
                    mdc.method.classFile.thisType != ObjectType.Throwable) {
                    val impureFillInStackTrace = !checkPurityOfCall(
                        declClass,
                        "fillInStackTrace",
                        MethodDescriptor("()Ljava/lang/Throwable;"),
                        None,
                        List.empty,
                        Success(mdc.method)
                    )
                    if (impureFillInStackTrace) { // Early return for impure statements
                        return Result(definedMethod, state.ubPurity);
                    }
                }
            }

        // Creating implicit except
        // ions is side-effect free (because of fillInStackTrace)
        // but it may be ignored as domain-specific
        val bbsCausingExceptions = cfg.abnormalReturnNode.predecessors
        for {
            bb ← bbsCausingExceptions
            pc = bb.asBasicBlock.endPC
            if isImmediateVMException(pc)
        } {
            val origin = state.code(if (isVMLevelValue(pc)) pcOfVMLevelValue(pc) else pc)
            val ratedResult = rater.handleException(origin)
            if (ratedResult.isDefined) atMost(ratedResult.get)
            else atMost(LBSideEffectFree)
        }

        val stmtCount = code.length
        var s = 0
        while (s < stmtCount) {
            if (!checkPurityOfStmt(code(s))) // Early return for impure statements
                return Result(definedMethod, state.ubPurity)
            s += 1
        }

        // Remove unnecessary dependees
        if (state.ubPurity ne LBPure) {
            cleanupDependees()
        }

        if (state.dependees.isEmpty || (state.lbPurity eq state.ubPurity)) {
            Result(definedMethod, state.ubPurity)
        } else {
            IntermediateResult(
                definedMethod,
                state.lbPurity,
                state.ubPurity,
                state.dependees,
                continuation
            )
        }
    }

}

object L1PurityAnalysis {
    /**
     * Domain-specific rater used to examine whether certain statements and expressions are
     * domain-specific.
     * If the Option is None, a rater is created from a config file option.
     */
    var rater: Option[DomainSpecificRater] = None

    def setRater(newRater: Option[DomainSpecificRater]): Unit = {
        rater = newRater
    }
}

trait L1PurityAnalysisScheduler extends ComputationSpecification {

    override def derives: Set[PropertyKind] = Set(Purity)

    override def uses: Set[PropertyKind] = {
        Set(VirtualMethodPurity, FieldMutability, ClassImmutability, TypeImmutability)
    }
}

object EagerL1PurityAnalysis extends L1PurityAnalysisScheduler with FPCFEagerAnalysisScheduler {
    def start(p: SomeProject, ps: PropertyStore): FPCFAnalysis = {
        val analysis = new L1PurityAnalysis(p)
        val dms = p.get(DeclaredMethodsKey).declaredMethods
        val methodsWithBody = dms.collect {
            case dm if dm.hasDefinition && dm.methodDefinition.body.isDefined ⇒ dm.asDefinedMethod
        }
        ps.scheduleEagerComputationsForEntities(methodsWithBody.filterNot(analysis.configuredPurity.wasSet))(
            analysis.determinePurity
        )
        analysis
    }
}

object LazyL1PurityAnalysis extends L1PurityAnalysisScheduler with FPCFLazyAnalysisScheduler {

    def startLazily(p: SomeProject, ps: PropertyStore): FPCFAnalysis = {
        val analysis = new L1PurityAnalysis(p)
        ps.registerLazyPropertyComputation(Purity.key, analysis.doDeterminePurity)
        analysis
    }
}
