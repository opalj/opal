/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package purity

import scala.annotation.switch

import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger
import org.opalj.collection.immutable.EmptyIntTrieSet
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.Entity
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.FinalEP
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimLUBP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.LBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.Property
import org.opalj.fpcf.Result
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.UBPS
import org.opalj.value.ValueInformation
import org.opalj.br.fpcf.properties.ClassImmutability
import org.opalj.br.fpcf.properties.CompileTimePure
import org.opalj.br.fpcf.properties.FieldMutability
import org.opalj.br.fpcf.properties.FinalField
import org.opalj.br.fpcf.properties.ImmutableObject
import org.opalj.br.fpcf.properties.ImmutableType
import org.opalj.br.fpcf.properties.ImpureByAnalysis
import org.opalj.br.fpcf.properties.ImpureByLackOfInformation
import org.opalj.br.fpcf.properties.Pure
import org.opalj.br.fpcf.properties.SideEffectFree
import org.opalj.br.fpcf.properties.TypeImmutability
import org.opalj.br.ComputationalTypeReference
import org.opalj.br.DeclaredMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.analyses.ConfiguredPurity
import org.opalj.br.fpcf.analyses.ConfiguredPurityKey
import org.opalj.br.fpcf.properties.Context
import org.opalj.br.fpcf.properties.Purity
import org.opalj.br.fpcf.properties.SimpleContexts
import org.opalj.br.fpcf.properties.SimpleContextsKey
import org.opalj.tac.fpcf.properties.cg.Callees
import org.opalj.ai.ValueOrigin
import org.opalj.ai.isImmediateVMException
import org.opalj.tac.cg.TypeProviderKey
import org.opalj.tac.fpcf.analyses.cg.uVarForDefSites
import org.opalj.tac.fpcf.analyses.cg.TypeProvider
import org.opalj.tac.fpcf.properties.TACAI

/**
 * Base trait for analyses that analyze the purity of methods.
 *
 * Provides types and methods needed for purity analyses.
 */
trait AbstractPurityAnalysis extends FPCFAnalysis {

    /** The type of the TAC domain. */
    type V = DUVar[ValueInformation]

    /**
     * The state of the analysis.
     * Analyses are expected to extend this trait with the information they need.
     *
     * lbPurity - The current minimum possible purity level for the method
     * ubPurity - The current maximum purity level for the method
     * method - The currently analyzed method
     * context - The corresponding Context to report results for
     * declClass - The declaring class of the currently analyzed method
     * code - The code of the currently analyzed method
     */
    trait AnalysisState {
        var lbPurity: Purity
        var ubPurity: Purity
        val method: Method
        val context: Context
        val declClass: ObjectType
        var tac: TACode[TACMethodParameter, V]
    }

    type StateType <: AnalysisState

    protected[this] def raterFqn: String

    val rater: DomainSpecificRater

    protected[this] implicit val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    private[this] val simpleContexts: Option[SimpleContexts] = project.has(SimpleContextsKey)
    protected[this] implicit val typeProvider: TypeProvider = project.get(TypeProviderKey)

    val configuredPurity: ConfiguredPurity = project.get(ConfiguredPurityKey)

    /**
     * Reduces the maxPurity of the current method to at most the given purity level.
     */
    def reducePurityLB(newLevel: Purity)(implicit state: StateType): Unit = {
        state.lbPurity = state.lbPurity meet newLevel
    }

    /**
     * Reduces the minPurity and maxPurity of the current method to at most the given purity level.
     */
    def atMost(newLevel: Purity)(implicit state: StateType): Unit = {
        state.lbPurity = state.lbPurity meet newLevel
        state.ubPurity = state.ubPurity meet newLevel
    }

    /**
     * Examines whether the given expression denotes an object/array that is local to the current
     * method, i.e. the method has control over the object/array and actions on it might not
     * influence purity.
     *
     * @param otherwise The maxPurity will be reduced to at most this level if the expression is not
     *                  local.
     */
    def isLocal(
        expr:             Expr[V],
        otherwise:        Purity,
        excludedDefSites: IntTrieSet = EmptyIntTrieSet
    )(implicit state: StateType): Boolean

    /**
     * Checks whether the statement, which is the origin of an exception, directly created the
     * exception or if the VM instantiated the exception. Here, we are only concerned about the
     * exceptions thrown by the instructions not about exceptions that are transitively thrown;
     * e.g. if a method is called.
     * TODO We need this method because currently, for exceptions that terminate the method, no
     * definitions are recorded. Once this is done, use that information instead to determine
     * whether it may be an immediate exception or not.
     */
    def isSourceOfImmediateException(origin: ValueOrigin)(implicit state: StateType): Boolean = {

        def evaluationMayCauseVMLevelException(expr: Expr[V]): Boolean = {
            (expr.astID: @switch) match {

                case NonVirtualFunctionCall.ASTID | VirtualFunctionCall.ASTID =>
                    val rcvr = expr.asInstanceFunctionCall.receiver
                    !rcvr.isVar || rcvr.asVar.value.asReferenceValue.isNull.isYesOrUnknown

                case StaticFunctionCall.ASTID => false

                case _                        => true
            }
        }

        val stmt = state.tac.stmts(origin)
        (stmt.astID: @switch) match {
            case StaticMethodCall.ASTID => false // We are looking for implicit exceptions only

            case Throw.ASTID =>
                stmt.asThrow.exception.asVar.value.asReferenceValue.isNull.isNotNo

            case NonVirtualMethodCall.ASTID | VirtualMethodCall.ASTID =>
                val rcvr = stmt.asInstanceMethodCall.receiver
                !rcvr.isVar || rcvr.asVar.value.asReferenceValue.isNull.isNotNo

            case Assignment.ASTID => evaluationMayCauseVMLevelException(stmt.asAssignment.expr)

            case ExprStmt.ASTID   => evaluationMayCauseVMLevelException(stmt.asExprStmt.expr)

            case _                => true
        }
    }

    /**
     * Examines whether a call constitutes a domain-specific action using the domain-specific rater.
     * If it is, the maxPurity will be reduced to at most the domain-specific purity returned by the
     * domain-specific rater.
     */
    def isDomainSpecificCall(
        call:     Call[V],
        receiver: Option[Expr[V]]
    )(implicit state: StateType): Boolean = {
        implicit val code: Array[Stmt[V]] = state.tac.stmts
        val ratedResult = rater.handleCall(call, receiver)
        if (ratedResult.isDefined)
            atMost(ratedResult.get)
        ratedResult.isDefined
    }

    /**
     * Examines a statement for its influence on the method's purity.
     * This method will return false for impure statements, so evaluation can be terminated early.
     */
    def checkPurityOfStmt(stmt: Stmt[V])(implicit state: StateType): Boolean = {
        val isStmtNotImpure = (stmt.astID: @switch) match {
            // For method calls, purity will be checked later
            case StaticMethodCall.ASTID | NonVirtualMethodCall.ASTID | VirtualMethodCall.ASTID |
                InvokedynamicMethodCall.ASTID =>
                true

            // Returning objects/arrays is pure, if the returned object/array is locally initialized
            // and non-escaping or the object is immutable
            case ReturnValue.ASTID =>
                checkPurityOfReturn(stmt.asReturnValue.expr)
                true
            case Throw.ASTID =>
                checkPurityOfReturn(stmt.asThrow.exception)
                true

            // Synchronization on non-escaping locally initialized objects/arrays is pure (and
            // useless...)
            case MonitorEnter.ASTID | MonitorExit.ASTID =>
                isLocal(stmt.asSynchronizationStmt.objRef, ImpureByAnalysis)

            // Storing into non-escaping locally initialized objects/arrays is pure
            case ArrayStore.ASTID => isLocal(stmt.asArrayStore.arrayRef, ImpureByAnalysis)
            case PutField.ASTID   => isLocal(stmt.asPutField.objRef, ImpureByAnalysis)

            case PutStatic.ASTID =>
                // Note that a putstatic is not necessarily pure/sideeffect free, even if it
                // is executed within a static initializer to initialize a field of
                // `the` class; it is possible that the initialization triggers the
                // initialization of another class which reads the value of this static field.
                // See
                // https://stackoverflow.com/questions/6416408/static-circular-dependency-in-java
                // for an in-depth discussion.
                // (Howevever, if we would check for cycles, we could determine that it is pure,
                // but this is not considered to be too useful...)
                atMost(ImpureByAnalysis)
                false

            // Creating implicit exceptions is side-effect free (because of fillInStackTrace)
            // but it may be ignored as domain-specific
            case CaughtException.ASTID =>
                for {
                    origin <- stmt.asCaughtException.origins
                    if isImmediateVMException(origin)
                } {
                    val baseOrigin = state.tac.stmts(ai.underlyingPC(origin))
                    val ratedResult = rater.handleException(baseOrigin)
                    if (ratedResult.isDefined) atMost(ratedResult.get)
                    else atMost(SideEffectFree)
                }
                true

            // Reference comparisons may have different results for structurally equal values
            case If.ASTID =>
                val If(_, left, _, right, _) = stmt
                if (left.cTpe eq ComputationalTypeReference)
                    if (!(isLocal(left, CompileTimePure) || isLocal(right, CompileTimePure)))
                        atMost(SideEffectFree)
                true

            // The following statements do not further influence purity
            case Goto.ASTID | JSR.ASTID | Ret.ASTID | Switch.ASTID | Assignment.ASTID |
                Return.ASTID | Nop.ASTID | ExprStmt.ASTID | Checkcast.ASTID =>
                true
        }

        isStmtNotImpure && stmt.forallSubExpressions(checkPurityOfExpr)
    }

    /**
     * Examines an expression for its influence on the method's purity.
     * This method will return false for impure expressions, so evaluation can be terminated early.
     */
    def checkPurityOfExpr(expr: Expr[V])(implicit state: StateType): Boolean = {
        val isExprNotImpure = (expr.astID: @switch) match {
            // For function calls, purity will be checked later
            case StaticFunctionCall.ASTID | NonVirtualFunctionCall.ASTID |
                VirtualFunctionCall.ASTID =>
                true

            // Field/array loads are pure if the field is (effectively) final or the object/array is
            // local and non-escaping
            case GetStatic.ASTID =>
                implicit val code: Array[Stmt[V]] = state.tac.stmts
                val ratedResult = rater.handleGetStatic(expr.asGetStatic)
                if (ratedResult.isDefined) atMost(ratedResult.get)
                else checkPurityOfFieldRef(expr.asGetStatic)
                true
            case GetField.ASTID =>
                checkPurityOfFieldRef(expr.asGetField)
                true
            case ArrayLoad.ASTID =>
                if (state.ubPurity.isDeterministic)
                    isLocal(expr.asArrayLoad.arrayRef, SideEffectFree)
                true

            // We don't handle unresolved Invokedynamic
            // - either OPAL removes it or we forget about it
            case InvokedynamicFunctionCall.ASTID =>
                atMost(ImpureByAnalysis)
                false

            // The following expressions do not further influence purity, potential exceptions are
            // handled explicitly
            case New.ASTID | NewArray.ASTID | InstanceOf.ASTID | Compare.ASTID | Param.ASTID |
                MethodTypeConst.ASTID | MethodHandleConst.ASTID | IntConst.ASTID | LongConst.ASTID |
                FloatConst.ASTID | DoubleConst.ASTID | StringConst.ASTID | ClassConst.ASTID |
                NullExpr.ASTID | BinaryExpr.ASTID | PrefixExpr.ASTID | PrimitiveTypecastExpr.ASTID |
                ArrayLength.ASTID | Var.ASTID =>
                true

        }

        isExprNotImpure && expr.forallSubExpressions(checkPurityOfExpr)
    }

    def checkPurityOfMethod(
        callee: Context,
        params: Seq[Expr[V]]
    )(implicit state: StateType): Boolean = {
        if (callee eq state.context) {
            true
        } else {
            val calleePurity = propertyStore(callee, Purity.key)
            checkMethodPurity(calleePurity, params)
        }
    }

    def getCall(stmt: Stmt[V]): Call[V] = stmt.astID match {
        case StaticMethodCall.ASTID     => stmt.asStaticMethodCall
        case NonVirtualMethodCall.ASTID => stmt.asNonVirtualMethodCall
        case VirtualMethodCall.ASTID    => stmt.asVirtualMethodCall
        case Assignment.ASTID           => stmt.asAssignment.expr.asFunctionCall
        case ExprStmt.ASTID             => stmt.asExprStmt.expr.asFunctionCall
        case _ =>
            throw new IllegalStateException(s"unexpected stmt $stmt")
    }

    /**
     * Examines the influence of the purity property of a method on the examined method's purity.
     *
     * @note Adds dependendies when necessary.
     */
    def checkMethodPurity(
        ep:     EOptionP[Context, Purity],
        params: Seq[Expr[V]]              = Seq.empty
    )(implicit state: StateType): Boolean

    /**
     * Examines whether a field read influences a method's purity.
     * Reading values from fields that are not (effectively) final may cause nondeterministic
     * behavior, so the method can only be side-effect free.
     */
    def checkPurityOfFieldRef(
        fieldRef: FieldRead[V]
    )(implicit state: StateType): Unit = {
        // Don't do dependee checks if already non-deterministic
        if (state.ubPurity.isDeterministic) {
            fieldRef.asFieldRead.resolveField match {
                case Some(field) if field.isStatic =>
                    checkFieldMutability(propertyStore(field, FieldMutability.key), None)
                case Some(field) =>
                    checkFieldMutability(
                        propertyStore(field, FieldMutability.key), Some(fieldRef.asGetField.objRef)
                    )
                case _ => // Unknown field
                    if (fieldRef.isGetField) isLocal(fieldRef.asGetField.objRef, SideEffectFree)
                    else atMost(SideEffectFree)
            }
        }
    }

    /**
     * Examines the influence that a given field mutability has on the method's purity.
     */
    def checkFieldMutability(
        ep:     EOptionP[Field, FieldMutability],
        objRef: Option[Expr[V]]
    )(implicit state: StateType): Unit = ep match {
        case LBP(_: FinalField) => // Final fields don't impede purity
        case _: FinalEP[Field, FieldMutability] => // Mutable field
            if (objRef.isDefined) {
                if (state.ubPurity.isDeterministic)
                    isLocal(objRef.get, SideEffectFree)
            } else atMost(SideEffectFree)
        case _ =>
            reducePurityLB(SideEffectFree)
            if (state.ubPurity.isDeterministic)
                handleUnknownFieldMutability(ep, objRef)
    }

    /**
     * Handles what to do when the mutability of a field is not yet known.
     * Analyses must implement this method with the behavior they need, e.g. registering dependees.
     */
    def handleUnknownFieldMutability(
        ep:     EOptionP[Field, FieldMutability],
        objRef: Option[Expr[V]]
    )(implicit state: StateType): Unit

    /**
     * Examines the effect of returning a value on the method's purity.
     * Returning a reference to a mutable object or array may cause nondeterministic behavior
     * as the object/array may be modified between invocations of the method, so the method can
     * only be side-effect free. E.g., a given parameter which references a mutable object is
     * returned (and not otherwise accessed).
     */
    def checkPurityOfReturn(returnValue: Expr[V])(implicit state: StateType): Unit = {
        if (returnValue.cTpe != ComputationalTypeReference)
            return ; // Only non-primitive return values influence purity.

        if (!state.ubPurity.isDeterministic)
            return ; // If the method can't be pure, the return value is not important.

        if (!returnValue.isVar) {
            // The expression could refer to further expressions in a non-flat representation. To
            // avoid special handling, we just fallback to SideEffectFreeWithoutAllocations here if
            // the return value is not local as the analysis is intended to be used on flat
            // representations anyway.
            isLocal(returnValue, SideEffectFree)
            return ;
        }

        val value = returnValue.asVar.value.asReferenceValue
        if (value.isNull.isYes)
            return ; // Null is immutable

        if (value.upperTypeBound.exists(_.isArrayType)) {
            // Arrays are always mutable
            isLocal(returnValue, SideEffectFree)
            return ;
        }

        if (value.isPrecise) { // Precise class known, use ClassImmutability
            val returnType = value.upperTypeBound.head

            val classImmutability =
                propertyStore(
                    returnType,
                    ClassImmutability.key
                ).asInstanceOf[EOptionP[ObjectType, ClassImmutability]]
            checkTypeMutability(classImmutability, returnValue)

        } else { // Precise class unknown, use TypeImmutability
            // IMPROVE Use ObjectType once we attach the respective information to ObjectTypes
            val returnTypes = value.upperTypeBound

            returnTypes.forall { returnType =>
                val typeImmutability =
                    propertyStore(
                        returnType,
                        TypeImmutability.key
                    ).asInstanceOf[EOptionP[ObjectType, TypeImmutability]]
                checkTypeMutability(typeImmutability, returnValue)
            }
        }
    }

    /**
     * Examines the effect that the mutability of a returned value's type has on the method's
     * purity.
     */
    def checkTypeMutability(
        ep:          EOptionP[ObjectType, Property],
        returnValue: Expr[V]
    )(implicit state: StateType): Boolean = ep match {
        // Returning immutable object is pure
        case LBP(ImmutableType | ImmutableObject) => true
        case _: FinalEP[ObjectType, Property] =>
            atMost(Pure) // Can not be compile time pure if mutable object is returned
            if (state.ubPurity.isDeterministic)
                isLocal(returnValue, SideEffectFree)
            false // Return early if we are already side-effect free
        case _ =>
            reducePurityLB(SideEffectFree)
            if (state.ubPurity.isDeterministic)
                handleUnknownTypeMutability(ep, returnValue)
            true
    }

    /**
     * Handles what to do when the mutability of a type is not yet known.
     * Analyses must implement this method with the behavior they need, e.g. registering dependees.
     */
    def handleUnknownTypeMutability(
        ep:   EOptionP[ObjectType, Property],
        expr: Expr[V]
    )(implicit state: StateType): Unit

    /**
     * Examines the effect that the purity of all potential callees has on the purity of the method.
     */
    def checkPurityOfCallees(
        calleesEOptP: EOptionP[DeclaredMethod, Callees]
    )(
        implicit
        state: StateType
    ): Boolean = {
        handleCalleesUpdate(calleesEOptP)
        calleesEOptP match {
            case UBPS(p: Callees, isFinal) =>
                if (!isFinal) reducePurityLB(ImpureByAnalysis)

                val hasIncompleteCallSites =
                    p.incompleteCallSites(state.context).exists { pc =>
                        val index = state.tac.properStmtIndexForPC(pc)
                        if (index < 0)
                            false // call will not be executed
                        else {
                            val call = getCall(state.tac.stmts(index))
                            !isDomainSpecificCall(call, call.receiverOption)
                        }
                    }

                if (hasIncompleteCallSites) {
                    atMost(ImpureByAnalysis)
                    return false;
                }

                val noDirectCalleeIsImpure = p.directCallSites(state.context).forall {
                    case (pc, callees) =>
                        val index = state.tac.properStmtIndexForPC(pc)
                        if (index < 0)
                            true // call will not be executed
                        else {
                            val call = getCall(state.tac.stmts(index))
                            isDomainSpecificCall(call, call.receiverOption) ||
                                callees.forall { callee =>
                                    checkPurityOfMethod(
                                        callee,
                                        call.receiverOption.orNull +: call.params
                                    )
                                }
                        }
                }

                if (!noDirectCalleeIsImpure)
                    return false;

                val noIndirectCalleeIsImpure = p.indirectCallSites(state.context).forall {
                    case (pc, callees) =>
                        val index = state.tac.properStmtIndexForPC(pc)
                        if (index < 0)
                            true // call will not be executed
                        else {
                            val call = getCall(state.tac.stmts(index))
                            isDomainSpecificCall(call, call.receiverOption) ||
                                callees.forall { callee =>
                                    checkPurityOfMethod(
                                        callee,
                                        p.indirectCallReceiver(state.context, pc, callee).map(
                                            receiver =>
                                                uVarForDefSites(receiver, state.tac.pcToIndex)
                                        ).orNull +:
                                            p.indirectCallParameters(state.context, pc, callee).map {
                                                paramO =>
                                                    paramO.map(
                                                        uVarForDefSites(_, state.tac.pcToIndex)
                                                    ).orNull
                                            }
                                    )
                                }
                        }
                }

                noIndirectCalleeIsImpure

            case _ =>
                reducePurityLB(ImpureByAnalysis)
                true
        }
    }

    /**
     * Handles what to do when the set of potential callees changes.
     * Analyses must implement this method with the behavior they need, e.g. registering dependees.
     */
    def handleCalleesUpdate(
        callees: EOptionP[DeclaredMethod, Callees]
    )(implicit state: StateType): Unit

    /**
     * Handles what to do if the TACAI is not yet final.
     */
    def handleTACAI(ep: EOptionP[Method, TACAI])(implicit state: StateType): Unit

    /**
     * Retrieves and commits the methods purity as calculated for its declaring class type for the
     * current DefinedMethod that represents the non-overwritten method in a subtype.
     */
    def baseMethodPurity(context: Context): ProperPropertyComputationResult = {

        def c(eps: SomeEOptionP): ProperPropertyComputationResult = eps match {
            case FinalP(p) => Result(context, p)
            case ep @ InterimLUBP(lb, ub) =>
                InterimResult.create(context, lb, ub, Set(ep), c)
            case epk =>
                InterimResult(context, ImpureByAnalysis, CompileTimePure, Set(epk), c)
        }

        c(propertyStore(
            simpleContexts.get(declaredMethods(context.method.definedMethod)),
            Purity.key
        ))
    }

    /**
     * Determines the purity of the given method.
     *
     * @param context A method call context
     */
    def determinePurity(context: Context): ProperPropertyComputationResult

    /** Called when the analysis is scheduled lazily. */
    def doDeterminePurity(e: Entity): ProperPropertyComputationResult = {
        e match {
            case context: Context if context.method.definedMethod.body.isDefined =>
                determinePurity(context)
            case context: Context => Result(context, ImpureByLackOfInformation)
            case _ =>
                throw new IllegalArgumentException(s"$e is not a declared method")
        }
    }

    /**
     * Returns the TACode for a method if available, registering dependencies as necessary.
     */
    def getTACAI(
        method: Method
    )(implicit state: StateType): Option[TACode[TACMethodParameter, V]] = {
        propertyStore(method, TACAI.key) match {
            case finalEP: FinalEP[Method, TACAI] =>
                handleTACAI(finalEP)
                finalEP.ub.tac
            case eps @ InterimUBP(ub: TACAI) =>
                reducePurityLB(ImpureByAnalysis)
                handleTACAI(eps)
                ub.tac
            case epk =>
                reducePurityLB(ImpureByAnalysis)
                handleTACAI(epk)
                None
        }
    }

    def resolveDomainSpecificRater(fqn: String): DomainSpecificRater = {
        import scala.reflect.runtime.universe.runtimeMirror
        val mirror = runtimeMirror(getClass.getClassLoader)
        try {
            val module = mirror.staticModule(fqn)
            mirror.reflectModule(module).instance.asInstanceOf[DomainSpecificRater]
        } catch {
            case ex @ (_: ScalaReflectionException | _: ClassCastException) =>
                OPALLogger.error(
                    "analysis configuration",
                    "resolve of domain specific rater failed, change "+
                        s"org.opalj.fpcf.${this.getClass.getName}.domainSpecificRater in "+
                        "ai/reference.conf to an existing DomainSpecificRater implementation",
                    ex
                )(GlobalLogContext)
                new BaseDomainSpecificRater // Provide a safe default if resolution failed
        }
    }

}
