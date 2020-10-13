/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package immutability
package fieldreference

import scala.annotation.switch

import org.opalj.br.BooleanType
import org.opalj.br.ClassFile
import org.opalj.br.DeclaredMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.PCs
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.AtMost
import org.opalj.br.fpcf.properties.EscapeInCallee
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.br.fpcf.properties.EscapeViaReturn
import org.opalj.br.fpcf.properties.FieldPrematurelyRead
import org.opalj.br.fpcf.properties.ImmutableFieldReference
import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeButDeterministicFieldReference
import org.opalj.br.fpcf.properties.LazyInitializedThreadSafeFieldReference
import org.opalj.br.fpcf.properties.MutableFieldReference
import org.opalj.br.fpcf.properties.NoEscape
import org.opalj.br.fpcf.properties.Purity
import org.opalj.br.fpcf.properties.FieldReferenceImmutability
import org.opalj.br.fpcf.properties.cg.Callees
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.Entity
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimEP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEPS
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.br.ReferenceType
import org.opalj.br.ByteType
import org.opalj.br.CharType
import org.opalj.br.DoubleType
import org.opalj.br.FloatType
import org.opalj.br.IntegerType
import org.opalj.br.LongType
import org.opalj.br.ObjectType
import org.opalj.br.ShortType

/**
 *
 * Determines the immutability of the reference of a class' field.
 * A field reference can either refer to an reference object or have a value.
 *
 * Examples:
 * class ... {
 * ...
 * final Object o;
 * int n;
 * ...
 * }
 *
 * In both cases we consider o and n as a field reference.
 * o refers to a reference object with type [[Object]] and n is storing an a value with type int.
 *
 * @note Requires that the 3-address code's expressions are not deeply nested.
 *
 * @author Tobias Roth
 * @author Dominik Helm
 * @author Florian Kübler
 * @author Michael Eichberg
 *
 */
class L0FieldReferenceImmutabilityAnalysis private[analyses] (val project: SomeProject)
    extends AbstractFieldReferenceImmutabilityAnalysisLazyInitialization
    with AbstractFieldReferenceImmutabilityAnalysis
    with FPCFAnalysis {

    def doDetermineFieldReferenceImmutability(entity: Entity): PropertyComputationResult = {
        entity match {
            case field: Field ⇒ {
                determineFieldReferenceImmutability(field)
            }
            case _ ⇒
                val m = entity.getClass.getSimpleName+" is not an org.opalj.br.Field"
                throw new IllegalArgumentException(m)
        }
    }

    /**
     * Analyzes the immutability fields references.
     *
     * This analysis is only ''soundy'' if the class file does not contain native methods.
     * If the analysis is schedulued using its companion object all class files with
     * native methods are filtered.
     */
    private[analyses] def determineFieldReferenceImmutability(
        field: Field
    ): ProperPropertyComputationResult = {
        /* println(
            s"""
             | determine field reference immutability:
             | field ${field}
             |
             |
             |""".stripMargin
        )*/

        if (field.isFinal)
            return Result(field, ImmutableFieldReference);

        if (field.isPublic)
            return Result(field, MutableFieldReference);

        implicit val state: State = State(field)

        val thisType = field.classFile.thisType

        // Fields are not final if they are read prematurely!
        if (isPrematurelyRead(propertyStore(field, FieldPrematurelyRead.key)))
            return Result(field, MutableFieldReference)

        // Collect all classes that have access to the field, i.e. the declaring class and possibly
        // classes in the same package as well as subclasses
        // Give up if the set of classes having access to the field is not closed
        val initialClasses =
            if (field.isProtected || field.isPackagePrivate) {
                project.classesPerPackage(thisType.packageName)
            } else {
                Set(field.classFile)
            }
        val classesHavingAccess: Iterator[ClassFile] =
            if (field.isProtected) {
                if (typeExtensibility(thisType).isYesOrUnknown) {
                    return Result(field, MutableFieldReference);
                }
                val subclassesIterator: Iterator[ClassFile] =
                    classHierarchy.allSubclassTypes(thisType, reflexive = false).flatMap { ot ⇒
                        project.classFile(ot).filter(cf ⇒ !initialClasses.contains(cf))
                    }
                initialClasses.iterator ++ subclassesIterator
            } else {
                initialClasses.iterator
            }
        // If there are native methods, we give up
        if (classesHavingAccess.exists(_.methods.exists(_.isNative))) {
            if (!field.isFinal)
                return Result(field, MutableFieldReference)
        }
        for {
            (method, pcs) ← fieldAccessInformation.writeAccesses(field)
            taCode ← getTACAI(method, pcs)
        } {
            if (methodUpdatesField(method, taCode, pcs)) {
                return Result(field, MutableFieldReference);
            }
        }
        if (state.lazyInitInvocation.isDefined) {
            val calleesEOP = propertyStore(state.lazyInitInvocation.get._1, Callees.key)
            doCallsIntroduceNonDeterminism(calleesEOP, state.lazyInitInvocation.get._2)
        }
        createResult()
    }

    /**
     * Returns the value the field will have after initialization or None if there may be multiple
     * values.
     */
    def getDefaultValue()(implicit state: State): Any = {

        state.field.fieldType match {
            case FloatType | ObjectType.Float     ⇒ 0.0f
            case DoubleType | ObjectType.Double   ⇒ 0.0d
            case LongType | ObjectType.Long       ⇒ 0L
            case CharType | ObjectType.Character  ⇒ '\u0000'
            case BooleanType | ObjectType.Boolean ⇒ false
            case IntegerType | ObjectType.Integer |
                ByteType | ObjectType.Byte |
                ShortType | ObjectType.Short ⇒ 0
            case _: ReferenceType ⇒ null
        }
    }

    /**
     * Prepares the PropertyComputation result, either as IntermediateResult if there are still
     * dependees or as Result otherwise.
     */
    def createResult()(implicit state: State): ProperPropertyComputationResult = {

        if (state.hasDependees && (state.referenceImmutability ne MutableFieldReference)) {
            InterimResult(
                state.field,
                MutableFieldReference,
                state.referenceImmutability,
                state.dependees,
                c
            )
        } else {
            Result(state.field, state.referenceImmutability)
        }
    }

    /**
     * Continuation function handling updates to the FieldPrematurelyRead property or to the purity
     * property of the method that initializes a (potentially) lazy initialized field.
     */
    def c(eps: SomeEPS)(implicit state: State): ProperPropertyComputationResult = {

        var isNotFinal = false
        println(
            s"""
               | enter continuation
               | eps: $eps
               |""".stripMargin
        )
        eps.pk match {
            case EscapeProperty.key ⇒
                val newEP = eps.asInstanceOf[EOptionP[DefinitionSite, EscapeProperty]]
                state.escapeDependees = state.escapeDependees.iterator.filter(_.e ne newEP.e).toSet
                isNotFinal = handleEscapeProperty(newEP)
            case TACAI.key ⇒
                val newEP = eps.asInstanceOf[EOptionP[Method, TACAI]]
                val method = newEP.e
                val pcs = state.tacDependees(method)._2
                state.tacDependees -= method
                if (eps.isRefinable)
                    state.tacDependees += method -> ((newEP, pcs))
                isNotFinal = methodUpdatesField(method, newEP.ub.tac.get, pcs)
            case Callees.key ⇒
                //if (eps.e.isInstanceOf[DeclaredMethod])

                //state.lazyInitInvocation
                val newEPS = eps.asInstanceOf[EOptionP[DeclaredMethod, Callees]]
                val pcs = state.calleesDependee(newEPS.e)._2
                isNotFinal = pcs.forall(pc ⇒ doCallsIntroduceNonDeterminism(newEPS, pc))
            //state.calleesDependee+= (calleesEOP.e → (calleesEOP,pc :: state.calleesDependee(calleesEOP.e)._2))
            //    isNotFinal = handleCalls()
            //else {
            //-T ODO callees handling
            //}
            case FieldPrematurelyRead.key ⇒
                isNotFinal = isPrematurelyRead(eps.asInstanceOf[EOptionP[Field, FieldPrematurelyRead]])
            case Purity.key ⇒
                val newEP = eps.asInstanceOf[EOptionP[DeclaredMethod, Purity]]
                state.purityDependees = state.purityDependees.iterator.filter(_.e ne newEP.e).toSet
                val nonDeterministicResult = isNonDeterministic(newEP)
                //if (!r) state.referenceImmutability = LazyInitializedReference
                //if (state.referenceImmutability != LazyInitializedNotThreadSafeReference &&
                //    state.referenceImmutability != LazyInitializedThreadSafeReference) { // both dont need determinism
                isNotFinal = nonDeterministicResult
            //}

            case FieldReferenceImmutability.key ⇒
                val newEP = eps.asInstanceOf[EOptionP[Field, FieldReferenceImmutability]]
                state.referenceImmutabilityDependees =
                    state.referenceImmutabilityDependees.iterator.filter(_.e ne newEP.e).toSet
                isNotFinal = !isImmutableReference(newEP)
        }

        println("result is not final: "+isNotFinal)
        if (isNotFinal)
            state.referenceImmutability = MutableFieldReference
        createResult()
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
        val field = state.field
        val stmts = taCode.stmts
        for (pc ← pcs.iterator) {
            val index = taCode.pcToIndex(pc)
            if (index > -1) { //TODO actually, unnecessary but required because there are '-1'; dead
                val stmt = stmts(index)
                if (stmt.pc == pc) {
                    (stmt.astID: @switch) match {
                        case PutStatic.ASTID | PutField.ASTID ⇒
                            if (method.isInitializer) {
                                if (field.isStatic) {
                                    if (method.isConstructor)
                                        return true;
                                } else {
                                    val receiverDefs = stmt.asPutField.objRef.asVar.definedBy
                                    if (receiverDefs != SelfReferenceParameter)
                                        return true;
                                }
                            } else {
                                if (field.isStatic ||
                                    stmt.asPutField.objRef.asVar.definedBy == SelfReferenceParameter) {
                                    // We consider lazy initialization if there is only single write
                                    // outside an initializer, so we can ignore synchronization
                                    if (state.referenceImmutability == LazyInitializedThreadSafeFieldReference ||
                                        state.referenceImmutability == LazyInitializedNotThreadSafeButDeterministicFieldReference) //LazyInitializedField)
                                        return true;
                                    // A lazily initialized instance field must be initialized only
                                    // by its owning instance
                                    if (!field.isStatic &&
                                        stmt.asPutField.objRef.asVar.definedBy != SelfReferenceParameter)
                                        return true;

                                    // A field written outside an initializer must be lazily
                                    // initialized or it is non-final
                                    val result = handleLazyInitialization(
                                        index,
                                        getDefaultValue(),
                                        method,
                                        taCode.stmts,
                                        taCode.cfg,
                                        taCode.pcToIndex,
                                        taCode
                                    )
                                    if (result) {
                                        println("result7: "+result)
                                        return result
                                    };

                                } else if (referenceHasEscaped(stmt.asPutField.objRef.asVar, stmts, method)) {
                                    println("reference has escaped")
                                    // note that here we assume real three address code (flat hierarchy)

                                    // for instance fields it is okay if they are written in the
                                    // constructor (w.r.t. the currently initialized object!)

                                    // If the field that is written is not the one referred to by the
                                    // self reference, it is not effectively final.

                                    // However, a method (e.g. clone) may instantiate a new object and
                                    // write the field as long as that new object did not yet escape.
                                    return true;
                                }

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
     *
     * Checks whether the object reference of a PutField does escape (except for being returned).
     */
    def referenceHasEscaped(
        ref:    V,
        stmts:  Array[Stmt[V]],
        method: Method
    )(implicit state: State): Boolean = {
        ref.definedBy.forall { defSite ⇒
            if (defSite < 0) true
            // Must be locally created
            else {
                val definition = stmts(defSite).asAssignment
                // Must either be null or freshly allocated
                if (definition.expr.isNullExpr) false
                else if (!definition.expr.isNew) true
                else {
                    handleEscapeProperty(propertyStore(definitionSites(method, definition.pc), EscapeProperty.key))
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
    )(implicit state: State): Boolean = {
        ep match {
            case FinalP(NoEscape | EscapeInCallee | EscapeViaReturn) ⇒
                false

            case FinalP(AtMost(_)) ⇒
                true

            case _: FinalEP[DefinitionSite, EscapeProperty] ⇒
                true // Escape state is worse than via return

            case InterimUBP(NoEscape | EscapeInCallee | EscapeViaReturn) ⇒
                state.escapeDependees += ep
                false

            case InterimUBP(AtMost(_)) ⇒
                true

            case _: InterimEP[DefinitionSite, EscapeProperty] ⇒
                true // Escape state is worse than via return

            case _ ⇒
                state.escapeDependees += ep
                false
        }
    }

}

trait L0FieldReferenceImmutabilityAnalysisScheduler extends FPCFAnalysisScheduler {

    import org.opalj.br.fpcf.properties.FieldImmutability

    final override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.lub(Purity),
        PropertyBounds.lub(FieldPrematurelyRead),
        PropertyBounds.ub(TACAI),
        PropertyBounds.ub(EscapeProperty),
        PropertyBounds.ub(FieldReferenceImmutability),
        PropertyBounds.ub(FieldImmutability)
    )

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(FieldReferenceImmutability)
}

/**
 * Executor for the eager field reference immutability analysis.
 */
object EagerL0FieldReferenceImmutabilityAnalysis extends L0FieldReferenceImmutabilityAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty

    final override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L0FieldReferenceImmutabilityAnalysis(p)
        val fields = p.allFields
        ps.scheduleEagerComputationsForEntities(fields)(analysis.determineFieldReferenceImmutability)
        analysis
    }
}

/**
 * Executor for the lazy field reference immutability analysis.
 */
object LazyL0FieldReferenceImmutabilityAnalysis extends L0FieldReferenceImmutabilityAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)

    final override def register(
        p:      SomeProject,
        ps:     PropertyStore,
        unused: Null
    ): FPCFAnalysis = {
        val analysis = new L0FieldReferenceImmutabilityAnalysis(p)
        ps.registerLazyPropertyComputation(
            FieldReferenceImmutability.key,
            analysis.determineFieldReferenceImmutability
        )
        analysis
    }
}
