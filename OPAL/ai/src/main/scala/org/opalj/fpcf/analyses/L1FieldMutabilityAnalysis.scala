/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.opalj.ai.common.DefinitionSitesKey
import org.opalj.ai.common.DefinitionSite
import org.opalj.br.ClassFile
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.PCs
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.ClosedPackagesKey
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.fpcf.properties.DeclaredFinalField
import org.opalj.fpcf.properties.EffectivelyFinalField
import org.opalj.fpcf.properties.FieldMutability
import org.opalj.fpcf.properties.NonFinalFieldByAnalysis
import org.opalj.fpcf.properties.NonFinalFieldByLackOfInformation
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.EscapeViaReturn
import org.opalj.fpcf.properties.EscapeInCallee
import org.opalj.fpcf.properties.AtMost
import org.opalj.fpcf.properties.NoEscape
import org.opalj.tac.DUVar
import org.opalj.tac.PutField
import org.opalj.tac.PutStatic
import org.opalj.tac.SelfReferenceParameter
import org.opalj.tac.Stmt
import org.opalj.tac.TACode
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.value.ValueInformation

/**
 * Simple analysis that checks if a private (static or instance) field is always initialized at
 * most once or if a field is or can be mutated after (lazy) initialization.
 *
 * @note Requires that the 3-address code's expressions are not deeply nested.
 *
 * @author Dominik Helm
 * @author Florian Kübler
 * @author Michael Eichberg
 */
class L1FieldMutabilityAnalysis private[analyses] (val project: SomeProject) extends FPCFAnalysis {

    class State(
            val field:           Field,
            var tacDependees:    Map[Method, (EOptionP[Method, TACAI], PCs)]   = Map.empty,
            var escapeDependees: Set[EOptionP[DefinitionSite, EscapeProperty]] = Set.empty
    )

    type V = DUVar[ValueInformation]

    final val typeExtensibility = project.get(TypeExtensibilityKey)
    final val closedPackages = project.get(ClosedPackagesKey)
    final val fieldAccessInformation = project.get(FieldAccessInformationKey)
    final val definitionSites = project.get(DefinitionSitesKey)

    def doDetermineFieldMutability(entity: Entity): PropertyComputationResult = {
        entity match {
            case field: Field ⇒ determineFieldMutability(field)
            case _ ⇒
                val m = entity.getClass.getName+"is not an org.opalj.br.Field"
                throw new IllegalArgumentException(m)
        }
    }

    /**
     * Analyzes the mutability of private non-final fields.
     *
     * This analysis is only ''soundy'' if the class file does not contain native methods.
     * If the analysis is schedulued using its companion object all class files with
     * native methods are filtered.
     */
    private[analyses] def determineFieldMutability(field: Field): PropertyComputationResult = {
        if (field.isFinal) {
            return Result(field, DeclaredFinalField)
        }

        val thisType = field.classFile.thisType

        if (field.isPublic) {
            return Result(field, NonFinalFieldByLackOfInformation);
        }

        val initialClasses =
            if (field.isProtected || field.isPackagePrivate) {
                if (!closedPackages.isClosed(thisType.packageName)) {
                    return Result(field, NonFinalFieldByLackOfInformation);
                }
                project.classesPerPackage(thisType.packageName)
            } else {
                Set(field.classFile)
            }

        val classesHavingAccess: Iterator[ClassFile] =
            if (field.isProtected) {
                if (typeExtensibility(thisType).isYesOrUnknown) {
                    return Result(field, NonFinalFieldByLackOfInformation);
                }
                val subclassesIterator: Iterator[ClassFile] =
                    classHierarchy.allSubclassTypes(thisType, reflexive = false).
                        flatMap { ot ⇒
                            project.classFile(ot).filter(cf ⇒ !initialClasses.contains(cf))
                        }
                initialClasses.iterator ++ subclassesIterator
            } else {
                initialClasses.iterator
            }

        if (classesHavingAccess.exists(_.methods.exists(_.isNative))) {
            return Result(field, NonFinalFieldByLackOfInformation);
        }

        // We now (compared to the simple one) have to analyze the static initializer as
        // the static initializer can be used to initialize a private field of an instance
        // of the class after the reference to the class and (an indirect) reference to the
        // field has become available. Consider the following example:
        // class X implements Y{
        //
        //     private Object o;
        //
        //     public Object getO() { return o; }
        //
        //     private static X instance;
        //     static {
        //         instance = new X();
        //         Z.register(instance);
        //         // when we reach this point o is now (via getO) publically accessible and
        //         // X is properly initialized!
        //         o = new Object(); // o is mutated...
        //     }
        // }

        implicit val state: State = new State(field)

        for {
            (method, pcs) ← fieldAccessInformation.writeAccesses(field)
            taCode ← getTACAI(method, pcs)
        } {
            if (methodUpdatesField(method, taCode, pcs))
                return Result(field, NonFinalFieldByAnalysis);
        }

        returnResult()
    }

    /**
     * Analyzes field writes for a single method, returning false if the field may still be
     * effectively final and true otherwise.
     */
    def methodUpdatesField(
        method: Method,
        taCode: TACode[TACMethodParameter, V],
        pcs:    PCs
    )(implicit state: State): Boolean = {
        val stmts = taCode.stmts
        for (pc ← pcs) {
            val index = taCode.pcToIndex(pc)
            if (index >= 0) {
                val stmtCandidate = stmts(index)
                if (stmtCandidate.pc == pc) {
                    stmtCandidate match {
                        case _: PutStatic[_] ⇒
                            if (!method.isStaticInitializer) {
                                return true;
                            };
                        case stmt: PutField[V] ⇒
                            val objRef = stmt.objRef.asVar
                            if ((!method.isConstructor ||
                                objRef.definedBy != SelfReferenceParameter) &&
                                !referenceHasNotEscaped(objRef, stmts, method)) {
                                // note that here we assume real three address code (flat hierarchy)

                                // for instance fields it is okay if they are written in the
                                // constructor (w.r.t. the currently initialized object!)

                                // If the field that is written is not the one referred to by the
                                // self reference, it is not effectively final.

                                // However, a method (e.g. clone) may instantiate a new object and
                                // write the field as long as that new object did not yet escape.
                                return true;
                            }
                        case _ ⇒ throw new RuntimeException("unexpected field access");
                    }
                } else {
                    // nothing to do as the put field is dead
                }
            }
        }
        false
    }

    /**
     * Returns the TACode for a method if available, registering dependencies as necessary.
     */
    def getTACAI(
        method: Method,
        pcs:    PCs
    )(implicit state: State): Option[TACode[TACMethodParameter, V]] = {
        propertyStore(method, TACAI.key) match {
            case finalP: FinalP[Method, TACAI] ⇒
                finalP.ub.tac
            case eps: IntermediateEP[Method, TACAI] ⇒
                state.tacDependees += method → ((eps, pcs))
                eps.ub.tac
            case epk ⇒
                state.tacDependees += method → ((epk, pcs))
                None
        }
    }

    def returnResult()(implicit state: State): PropertyComputationResult = {
        if (state.tacDependees.isEmpty && state.escapeDependees.isEmpty)
            Result(state.field, EffectivelyFinalField)
        else
            IntermediateResult(
                state.field,
                NonFinalFieldByAnalysis,
                EffectivelyFinalField,
                state.escapeDependees ++ state.tacDependees.valuesIterator.map(_._1),
                continuation,
                if (state.tacDependees.isEmpty) CheapPropertyComputation
                else DefaultPropertyComputation
            )
    }

    def continuation(eps: SomeEPS)(implicit state: State): PropertyComputationResult = {
        val isNonFinal = eps.e match {
            case ds: DefinitionSite ⇒
                val newEP = eps.asInstanceOf[EOptionP[DefinitionSite, EscapeProperty]]
                state.escapeDependees = state.escapeDependees.filter(_.e ne ds)
                handleEscapeProperty(newEP)
            case method: Method ⇒
                val newEP = eps.asInstanceOf[EOptionP[Method, TACAI]]
                val pcs = state.tacDependees(method)._2
                state.tacDependees -= method
                if (eps.isRefinable)
                    state.tacDependees += method → ((newEP, pcs))
                methodUpdatesField(method, newEP.ub.tac.get, pcs)
        }

        if (isNonFinal)
            Result(state.field, NonFinalFieldByAnalysis);
        else
            returnResult()
    }

    /**
     * Checks whether the object reference of a PutField does not escape (except for being
     * returned).
     */
    def referenceHasNotEscaped(
        ref:    V,
        stmts:  Array[Stmt[V]],
        method: Method
    )(implicit state: State): Boolean = {
        ref.definedBy.forall { defSite ⇒
            if (defSite < 0) false // Must be locally created
            else {
                val definition = stmts(defSite).asAssignment
                // Must either be null or freshly allocated
                if (definition.expr.isNullExpr) true
                else if (!definition.expr.isNew) false
                else {
                    val escape =
                        propertyStore(definitionSites(method, definition.pc), EscapeProperty.key)
                    !handleEscapeProperty(escape)
                }
            }
        }
    }

    /**
     * Handles the influence of an escape property on the field mutability.
     * @return true if the object - on which a field write occurred - escapes, false otherwise.
     * @note (Re-)Adds dependees as necessary.
     */
    def handleEscapeProperty(
        ep: EOptionP[DefinitionSite, EscapeProperty]
    )(implicit state: State): Boolean = ep match {
        case FinalP(_, NoEscape | EscapeInCallee | EscapeViaReturn) ⇒
            false

        case FinalP(_, AtMost(_)) ⇒
            true

        case FinalP(_, _) ⇒
            true // Escape state is worse than via return

        case IntermediateEP(_, _, NoEscape | EscapeInCallee | EscapeViaReturn) ⇒
            state.escapeDependees += ep
            false

        case IntermediateEP(_, _, AtMost(_)) ⇒
            true

        case IntermediateEP(_, _, _) ⇒
            true // Escape state is worse than via return

        case _ ⇒
            state.escapeDependees += ep
            false
    }
}

sealed trait L1FieldMutabilityAnalysisScheduler extends ComputationSpecification {

    final override def uses: Set[PropertyKind] = Set(TACAI, EscapeProperty)

    final override def derives: Set[PropertyKind] = Set(FieldMutability)

    final override type InitializationData = Null
    final def init(p: SomeProject, ps: PropertyStore): Null = null

    def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}

}

/**
 * Executor for the field mutability analysis.
 */
object EagerL1FieldMutabilityAnalysis
    extends L1FieldMutabilityAnalysisScheduler
    with FPCFEagerAnalysisScheduler {

    final override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L1FieldMutabilityAnalysis(p)
        val fields = p.allFields
        ps.scheduleEagerComputationsForEntities(fields)(analysis.determineFieldMutability)
        analysis
    }
}

/**
 * Executor for the lazy field mutability analysis.
 */
object LazyL1FieldMutabilityAnalysis
    extends L1FieldMutabilityAnalysisScheduler
    with FPCFLazyAnalysisScheduler {

    final override def startLazily(
        p:      SomeProject,
        ps:     PropertyStore,
        unused: Null
    ): FPCFAnalysis = {
        val analysis = new L1FieldMutabilityAnalysis(p)
        ps.registerLazyPropertyComputation(
            FieldMutability.key, analysis.determineFieldMutability
        )
        analysis
    }
}
