/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.immutability.reference

import org.opalj.br.BooleanType
import org.opalj.br.ByteType
import org.opalj.br.ClassFile
import org.opalj.br.DeclaredMethod
import org.opalj.br.Field
import org.opalj.br.FloatType
import org.opalj.br.IntegerType
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.PCs
import org.opalj.br.ShortType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.BasicFPCFLazyAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.FPCFAnalysisScheduler
import org.opalj.br.fpcf.properties.AtMost
import org.opalj.br.fpcf.properties.DeepImmutableField
import org.opalj.br.fpcf.properties.DependentImmutableType
import org.opalj.br.fpcf.properties.EscapeInCallee
import org.opalj.br.fpcf.properties.EscapeProperty
import org.opalj.br.fpcf.properties.EscapeViaReturn
import org.opalj.br.fpcf.properties.FieldImmutability
import org.opalj.br.fpcf.properties.FieldPrematurelyRead
import org.opalj.br.fpcf.properties.ImmutableReference
import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeButDeterministicReference
import org.opalj.br.fpcf.properties.LazyInitializedNotThreadSafeOrNotDeterministicReference
import org.opalj.br.fpcf.properties.LazyInitializedThreadSafeReference
import org.opalj.br.fpcf.properties.MutableReference
import org.opalj.br.fpcf.properties.NoEscape
import org.opalj.br.fpcf.properties.Purity
import org.opalj.br.fpcf.properties.ReferenceImmutability
import org.opalj.br.fpcf.properties.TypeImmutability_new
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
import org.opalj.tac.Assignment
import org.opalj.tac.DVar
import org.opalj.tac.GetField
import org.opalj.tac.If
import org.opalj.tac.MonitorEnter
import org.opalj.tac.MonitorExit
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.PutField
import org.opalj.tac.PutStatic
import org.opalj.tac.Stmt
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.UVar
import org.opalj.tac.common.DefinitionSite
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.SelfReferenceParameter

/**
 *
 *  Implementation is used from the old L2FieldMutability implementation
 *  but the lattice is mapped to the new reference immutability lattice.
 *
 * @note Requires that the 3-address code's expressions are not deeply nested.
 *
 * @author Dominik Helm
 * @author Florian Kübler
 * @author Michael Eichberg
 * @author Tobias Peter Roth
 */
class L0ReferenceImmutabilityAnalysis private[analyses] (val project: SomeProject)
    extends AbstractReferenceImmutabilityAnalysisLazyInitialization
    with AbstractReferenceImmutabilityAnalysis
    with FPCFAnalysis {

    def doDetermineReferenceImmutability(entity: Entity): PropertyComputationResult = entity match {
        case field: Field ⇒ determineReferenceImmutability(field)
        case _ ⇒
            val m = entity.getClass.getSimpleName+" is not an org.opalj.br.Field"
            throw new IllegalArgumentException(m)
    }

    /**
     * Analyzes the mutability of private non-final fields.
     *
     * This analysis is only ''soundy'' if the class file does not contain native methods.
     * If the analysis is schedulued using its companion object all class files with
     * native methods are filtered.
     */
    private[analyses] def determineReferenceImmutability(
        field: Field
    ): ProperPropertyComputationResult = {

        implicit val state: State = State(field)
        // Fields are not final if they are read prematurely!
        if (isPrematurelyRead(propertyStore(field, FieldPrematurelyRead.key))) {
            //("field is prematurely read")
            return Result(field, MutableReference)
        }; //Result(field, NonFinalFieldByAnalysis);

        state.referenceImmutability = ImmutableReference(true) //EffectivelyFinalField

        val thisType = field.classFile.thisType

        if ((field.isPublic || field.isPackagePrivate || field.isProtected)) {

            if (!field.isFinal) {

                return Result(field, MutableReference)
            } else {

                state.notEscapes = false
            }
        }; //Result(field, NonFinalFieldByLackOfInformation)

        if (field.isPublic)
            return Result(field, MutableReference)

        // Collect all classes that have access to the field, i.e. the declaring class and possibly
        // classes in the same package as well as subclasses
        // Give up if the set of classes having access to the field is not closed
        val initialClasses =
            if (field.isProtected || field.isPackagePrivate) {
                if (!closedPackages.isClosed(thisType.packageName)) {
                    return Result(field, MutableReference);
                }
                project.classesPerPackage(thisType.packageName)
            } else {
                Set(field.classFile)
            }

        val classesHavingAccess: Iterator[ClassFile] =
            if (field.isProtected) {
                if (typeExtensibility(thisType).isYesOrUnknown) {
                    return Result(field, MutableReference);
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
        if (classesHavingAccess.exists(_.methods.exists(_.isNative)))
            return Result(field, MutableReference);

        for {
            (method, pcs) ← fieldAccessInformation.readAccesses(field)
            taCode ← getTACAI(method, pcs)
        } {
            //TODO !!!!

            pcs.foreach(pc ⇒ {

                val index = taCode.pcToIndex(pc)

                val staticAddition = {
                    if (method.isStatic) {

                        1
                    } else {

                        0
                    }
                }

                if (index > (-1 + staticAddition)) {
                    taCode.stmts(index) match {
                        case a @ Assignment(pc3, targetVar, GetField(pc4, cl, name, _, value)) ⇒

                            if (name == field.name) {
                                targetVar.usedBy.foreach(i ⇒
                                    {

                                        taCode.stmts(i) match {
                                            case MonitorEnter(_, _)                     ⇒
                                            case MonitorExit(_, _)                      ⇒
                                            case If(pc, left, condition, right, target) ⇒
                                            case u @ _                                  ⇒ state.notEscapes = false;
                                        }
                                    })

                            }

                        case _ ⇒
                    }
                } else {

                    state.notEscapes = false
                }
            })
        }

        for {
            (method, pcs) ← fieldAccessInformation.writeAccesses(field)
            taCode ← getTACAI(method, pcs)
        } {

            for (pc ← pcs) {

                val index = taCode.pcToIndex(pc)
                val staticAddition = {
                    if (method.isStatic)
                        1
                    else
                        0
                }
                if (index > (-1 + staticAddition)) {
                    val stmt = taCode.stmts(index)

                    stmt match {
                        case PutField(_, _, _, _, _, value) ⇒
                            value match {
                                case v @ UVar(defSites, value2) ⇒ //SObjectValue(t,_,_,_) =>
                                    //if (!v.defSites.filter(_ < 1).isEmpty)
                                    //escape var
                                    {
                                        v.defSites.foreach(
                                            i ⇒ {

                                                if (i > 0) {
                                                    val stmt2 = taCode.stmts(i)

                                                    stmt2 match {
                                                        //case Assignment(pcAssigment, v @ DVar(useSites, value), expr) ⇒
                                                        case Assignment(pcAssignment, dv @ DVar(useSites, value), expr) ⇒
                                                            //useSites
                                                            dv.useSites.foreach(
                                                                x ⇒ {
                                                                    val innerstmt = taCode.stmts(x)
                                                                    innerstmt match {
                                                                        case NonVirtualMethodCall(
                                                                            pc,
                                                                            bdeclaringClass,
                                                                            isInterface,
                                                                            name,
                                                                            descriptor,
                                                                            receiver,
                                                                            params
                                                                            ) ⇒ {
                                                                            params.foreach(expr ⇒ {
                                                                                expr match {
                                                                                    case vrs @ UVar(defSites, value) ⇒ { //: SObjectValue(tpe,isNull,isPrecise)
                                                                                        //val isStaticIndex = if (method.isStatic) 0 else -1
                                                                                        vrs.defSites.foreach(
                                                                                            dfste ⇒ {
                                                                                                if (dfste < 0) {

                                                                                                    state.notEscapes = false
                                                                                                } //(0 + isStaticIndex))
                                                                                                else {
                                                                                                    val stmtDefSite = taCode.stmts(dfste)

                                                                                                    stmtDefSite match {
                                                                                                        case Assignment(
                                                                                                            pcA,
                                                                                                            targetVar,
                                                                                                            GetField(
                                                                                                                pc,
                                                                                                                declaringClass,
                                                                                                                name,
                                                                                                                declaredFieldType,
                                                                                                                objRef
                                                                                                                )
                                                                                                            ) ⇒ {
                                                                                                            val fs = project.allFields.filter(
                                                                                                                { f ⇒
                                                                                                                    f.name.equals(name) && f.fieldType.equals(
                                                                                                                        declaredFieldType
                                                                                                                    ) && f.classFile.equals(
                                                                                                                        state.field.classFile
                                                                                                                    )
                                                                                                                }
                                                                                                            )
                                                                                                            fs.foreach(f ⇒ {
                                                                                                                val result =
                                                                                                                    propertyStore(f, FieldImmutability.key)
                                                                                                                result match {
                                                                                                                    case FinalP(DeepImmutableField) ⇒
                                                                                                                    case _                          ⇒ state.notEscapes = false
                                                                                                                }
                                                                                                            })
                                                                                                        }
                                                                                                        case _ ⇒
                                                                                                    }
                                                                                                }

                                                                                            }
                                                                                        )
                                                                                    }
                                                                                }
                                                                            })
                                                                        }
                                                                        case _ ⇒
                                                                    }

                                                                }
                                                            )
                                                            if (expr.isNew || expr.isConst) {
                                                                if (expr.isNew) {

                                                                    /**
                                                                     * val NEW = expr.asNew
                                                                     *
                                                                     * val oType = NEW.tpe
                                                                     *
                                                                     *
                                                                     * if (oType == ObjectType.Object)
                                                                     * state.notEscapes = false
                                                                     * else {
                                                                     * val result = propertyStore(oType, TypeImmutability_new.key)
                                                                     * result match {
                                                                     * case FinalP(DependentImmutableType) ⇒ {
                                                                     *
                                                                     * state.notEscapes = false
                                                                     * }
                                                                     * case fp @ FinalP(_) ⇒ fp)
                                                                     * case ep @ _ ⇒ {
                                                                     *
                                                                     * state.typeDependees += ep
                                                                     * }
                                                                     * }
                                                                     * }*
                                                                     */

                                                                }
                                                                /**
                                                                 * propertyStore(
                                                                 * definitionSites(method, pc),
                                                                 * EscapeProperty.key
                                                                 * ) match {
                                                                 * case FinalP(NoEscape)                   ⇒ // nothing to do
                                                                 * case FinalP(AtMost(EscapeViaParameter)) ⇒ // introduced via this
                                                                 * case _ ⇒
                                                                 * state.notEscapes = false
                                                                 * }
                                                                 * } else {
                                                                 * state.notEscapes = false*
                                                                 */ //TODO
                                                            }
                                                        case _ ⇒
                                                            state.notEscapes = false
                                                    }
                                                } else {
                                                    //constructor ??

                                                    state.notEscapes = false
                                                }
                                            }
                                        )
                                    }
                                case _ ⇒
                                    state.notEscapes = false
                            }
                        case _ ⇒
                            state.notEscapes = false
                    }
                } else state.notEscapes = false
            }

            if (methodUpdatesField(method, taCode, pcs) && !state.field.isFinal) {

                return Result(field, MutableReference); //return Result(field, NonFinalFieldByAnfalysis);
            }
        }

        if (state.lazyInitInvocation.isDefined) {
            //handleCalls(calleesEOP)
        }

        createResult()
    }

    def handleCalls(
        calleesEOP: EOptionP[DeclaredMethod, Callees]
    )(
        implicit
        state: State
    ): Boolean = {
        calleesEOP match {
            case FinalP(callees) ⇒
                state.calleesDependee = None
                handleCallees(callees)
            case InterimUBP(callees) ⇒
                state.calleesDependee = Some(calleesEOP)
                handleCallees(callees)
            case _ ⇒
                state.calleesDependee = Some(calleesEOP)
                false
        }
    }

    def handleCallees(callees: Callees)(implicit state: State): Boolean = {
        val pc = state.lazyInitInvocation.get._2
        if (callees.isIncompleteCallSite(pc)) {
            state.referenceImmutability = MutableReference //NonFinalFieldByAnalysis
            true
        } else {
            val targets = callees.callees(pc).toTraversable
            if (targets.exists(target ⇒ isNonDeterministic(propertyStore(target, Purity.key)))) {
                state.referenceImmutability = MutableReference //NonFinalFieldByAnalysis
                true
            } else false
        }
    }

    /**
     * Returns the value the field will have after initialization or None if there may be multiple
     * values.
     */
    def getDefaultValue()(implicit state: State): Option[Any] = {
        Some(
            state.field.fieldType match {
                case FloatType     ⇒ 0.0f
                case IntegerType   ⇒ 0
                case ObjectType(_) ⇒ null
                case BooleanType   ⇒ false
                case ByteType      ⇒ 0
                case ShortType     ⇒ 0
                case _             ⇒
            }
        )
    }

    /**
     * Prepares the PropertyComputation result, either as IntermediateResult if there are still
     * dependees or as Result otherwise.
     */
    def createResult()(implicit state: State): ProperPropertyComputationResult = {

        if (state.hasDependees && (state.referenceImmutability ne MutableReference)) { //NonFinalFieldByAnalysis))
            InterimResult(
                state.field,
                MutableReference, //NonFinalFieldByAnalysis,
                state.referenceImmutability,
                state.dependees,
                c
            )
        } else {
            if (state.field.isFinal)
                Result(state.field, ImmutableReference(state.notEscapes))
            else
                state.referenceImmutability match {
                    case ImmutableReference(_) ⇒ Result(state.field, ImmutableReference(state.notEscapes))
                    case _                     ⇒ Result(state.field, state.referenceImmutability)
                }
        }
    }

    /**
     * Continuation function handling updates to the FieldPrematurelyRead property or to the purity
     * property of the method that initializes a (potentially) lazy initialized field.
     */
    def c(eps: SomeEPS)(implicit state: State): ProperPropertyComputationResult = {
        var isNotFinal = false
        eps.pk match {
            case EscapeProperty.key ⇒
                val newEP = eps.asInstanceOf[EOptionP[DefinitionSite, EscapeProperty]]
                state.escapeDependees = state.escapeDependees.filter(_.e ne newEP.e)
                val escapes = handleEscapeProperty(newEP)
                if (escapes)
                    state.notEscapes = false
                isNotFinal = escapes
            case TACAI.key ⇒
                val newEP = eps.asInstanceOf[EOptionP[Method, TACAI]]
                val method = newEP.e
                val pcs = state.tacDependees(method)._2
                state.tacDependees -= method
                if (eps.isRefinable)
                    state.tacDependees += method -> ((newEP, pcs))
                isNotFinal = methodUpdatesField(method, newEP.ub.tac.get, pcs)
            case Callees.key ⇒
                isNotFinal = handleCalls(eps.asInstanceOf[EOptionP[DeclaredMethod, Callees]])
            case FieldPrematurelyRead.key ⇒
                isNotFinal = isPrematurelyRead(eps.asInstanceOf[EOptionP[Field, FieldPrematurelyRead]])
            case Purity.key ⇒
                val newEP = eps.asInstanceOf[EOptionP[DeclaredMethod, Purity]]
                state.purityDependees = state.purityDependees.filter(_.e ne newEP.e)
                val nonDeterministicResult = isNonDeterministic(newEP)
                //if (!r) state.referenceImmutability = LazyInitializedReference
                if (state.referenceImmutability != LazyInitializedNotThreadSafeOrNotDeterministicReference &&
                    state.referenceImmutability != LazyInitializedThreadSafeReference) { // both dont need determinism
                    isNotFinal = nonDeterministicResult
                }

            case ReferenceImmutability.key ⇒
                val newEP = eps.asInstanceOf[EOptionP[Field, ReferenceImmutability]]
                state.referenceImmutabilityDependees =
                    state.referenceImmutabilityDependees.filter(_.e ne newEP.e)
                isNotFinal = !isImmutableReference(newEP)
            case TypeImmutability_new.key ⇒
                val newEP = eps.asInstanceOf[EOptionP[ObjectType, TypeImmutability_new]]
                state.typeDependees = state.typeDependees.filter(_.e ne newEP.e)
                newEP match {
                    case FinalP(DependentImmutableType) ⇒ state.notEscapes = false
                    case FinalP(_)                      ⇒
                    case ep @ _                         ⇒ state.typeDependees += ep
                }
        }

        if (isNotFinal && !state.field.isFinal) {
            Result(state.field, MutableReference) //Result(state.field, NonFinalFieldByAnalysis)
        } else
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
        for (pc ← pcs) {
            val index = taCode.pcToIndex(pc)
            val staticAddition = {
                if (method.isStatic)
                    1
                else
                    0
            }
            if (index > (-1 + staticAddition)) {
                val stmt = stmts(index)

                if (stmt.pc == pc) {
                    stmt.astID match {
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
                                    if (state.referenceImmutability == LazyInitializedThreadSafeReference ||
                                        state.referenceImmutability == LazyInitializedNotThreadSafeButDeterministicReference) //LazyInitializedField)
                                        return true;
                                    // A lazily initialized instance field must be initialized only
                                    // by its owning instance
                                    if (!field.isStatic &&
                                        stmt.asPutField.objRef.asVar.definedBy != SelfReferenceParameter)
                                        return true;
                                    val defaultValue = getDefaultValue()
                                    if (defaultValue.isEmpty)
                                        return true;

                                    // A field written outside an initializer must be lazily
                                    // initialized or it is non-final
                                    val dcl = isThreadSafeLazyInitialisation(
                                        index,
                                        defaultValue.get,
                                        method,
                                        taCode.stmts,
                                        taCode.cfg,
                                        taCode.pcToIndex,
                                        taCode
                                    )
                                    val li = isLazyInitialization(
                                        index,
                                        defaultValue.get,
                                        method,
                                        taCode.stmts,
                                        taCode.cfg,
                                        taCode.pcToIndex
                                    )
                                    dcl match {
                                        case MutableReference if (!li) ⇒ return true;
                                        case MutableReference if (li)  ⇒ state.referenceImmutability = LazyInitializedNotThreadSafeButDeterministicReference
                                        case LazyInitializedNotThreadSafeButDeterministicReference ⇒
                                            state.referenceImmutability = LazyInitializedNotThreadSafeButDeterministicReference
                                        case LazyInitializedNotThreadSafeOrNotDeterministicReference ⇒
                                            if (li) state.referenceImmutability = LazyInitializedNotThreadSafeButDeterministicReference
                                            else state.referenceImmutability = LazyInitializedNotThreadSafeOrNotDeterministicReference
                                        case lits @ LazyInitializedThreadSafeReference ⇒ state.referenceImmutability = lits
                                    }

                                } else if (referenceHasEscaped(stmt.asPutField.objRef.asVar, stmts, method)) {

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
     * Checks whether the object reference of a PutField does escape (except for being returned).
     */
    def referenceHasEscaped(
        ref:    V,
        stmts:  Array[Stmt[V]],
        method: Method
    )(implicit state: State): Boolean = {

        ref.definedBy.forall { defSite ⇒
            if (defSite < 0) true // Must be locally created
            else {
                val definition = stmts(defSite).asAssignment
                // Must either be null or freshly allocated
                if (definition.expr.isNullExpr) false
                else if (!definition.expr.isNew) true
                else {
                    val escape =
                        propertyStore(definitionSites(method, definition.pc), EscapeProperty.key)
                    handleEscapeProperty(escape)
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

trait L0ReferenceImmutabilityAnalysisScheduler extends FPCFAnalysisScheduler {

    final override def uses: Set[PropertyBounds] = Set(
        PropertyBounds.lub(Purity),
        PropertyBounds.lub(FieldPrematurelyRead),
        PropertyBounds.ub(TACAI),
        PropertyBounds.lub(ReferenceImmutability),
        PropertyBounds.ub(EscapeProperty),
        PropertyBounds.lub(TypeImmutability_new)
    )

    final def derivedProperty: PropertyBounds = PropertyBounds.lub(ReferenceImmutability)

}

/**
 * Executor for the field mutability analysis.
 */
object EagerL0ReferenceImmutabilityAnalysis
    extends L0ReferenceImmutabilityAnalysisScheduler
    with BasicFPCFEagerAnalysisScheduler {

    final override def start(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new L0ReferenceImmutabilityAnalysis(p)
        val fields = p.allProjectClassFiles.flatMap(_.fields)
        ps.scheduleEagerComputationsForEntities(fields)(analysis.determineReferenceImmutability)
        analysis
    }

    override def derivesEagerly: Set[PropertyBounds] = Set(derivedProperty)

    override def derivesCollaboratively: Set[PropertyBounds] = Set.empty
}

/**
 * Executor for the lazy field mutability analysis.
 */
object LazyL0ReferenceImmutabilityAnalysis
    extends L0ReferenceImmutabilityAnalysisScheduler
    with BasicFPCFLazyAnalysisScheduler {

    final override def register(
        p:      SomeProject,
        ps:     PropertyStore,
        unused: Null
    ): FPCFAnalysis = {
        val analysis = new L0ReferenceImmutabilityAnalysis(p)
        ps.registerLazyPropertyComputation(
            ReferenceImmutability.key,
            analysis.determineReferenceImmutability
        )
        analysis
    }

    override def derivesLazily: Some[PropertyBounds] = Some(derivedProperty)
}
