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
package purity

import scala.annotation.switch
import org.opalj.ai.Domain
import org.opalj.ai.ValueOrigin
import org.opalj.ai.isImmediateVMException
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.ComputationalTypeReference
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.collection.immutable.EmptyIntTrieSet
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.properties.ClassImmutability
import org.opalj.fpcf.properties.FieldMutability
import org.opalj.fpcf.properties.FinalField
import org.opalj.fpcf.properties.ImmutableObject
import org.opalj.fpcf.properties.ImmutableType
import org.opalj.fpcf.properties.ImpureByLackOfInformation
import org.opalj.fpcf.properties.ImpureByAnalysis
import org.opalj.fpcf.properties.SideEffectFree
import org.opalj.fpcf.properties.Purity
import org.opalj.fpcf.properties.TypeImmutability
import org.opalj.fpcf.properties.VirtualMethodPurity
import org.opalj.fpcf.properties.Pure
import org.opalj.fpcf.properties.CompileTimePure
import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger
import org.opalj.tac.ArrayLength
import org.opalj.tac.ArrayLoad
import org.opalj.tac.ArrayStore
import org.opalj.tac.Assignment
import org.opalj.tac.BinaryExpr
import org.opalj.tac.Call
import org.opalj.tac.CaughtException
import org.opalj.tac.Checkcast
import org.opalj.tac.ClassConst
import org.opalj.tac.Compare
import org.opalj.tac.DUVar
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.DoubleConst
import org.opalj.tac.Expr
import org.opalj.tac.ExprStmt
import org.opalj.tac.FloatConst
import org.opalj.tac.GetField
import org.opalj.tac.GetStatic
import org.opalj.tac.Goto
import org.opalj.tac.If
import org.opalj.tac.InstanceOf
import org.opalj.tac.IntConst
import org.opalj.tac.Invokedynamic
import org.opalj.tac.JSR
import org.opalj.tac.LongConst
import org.opalj.tac.MethodHandleConst
import org.opalj.tac.MethodTypeConst
import org.opalj.tac.MonitorEnter
import org.opalj.tac.MonitorExit
import org.opalj.tac.New
import org.opalj.tac.NewArray
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.Nop
import org.opalj.tac.NullExpr
import org.opalj.tac.Param
import org.opalj.tac.PrefixExpr
import org.opalj.tac.PrimitiveTypecastExpr
import org.opalj.tac.PutField
import org.opalj.tac.PutStatic
import org.opalj.tac.Ret
import org.opalj.tac.Return
import org.opalj.tac.ReturnValue
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.Stmt
import org.opalj.tac.StringConst
import org.opalj.tac.Switch
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.Throw
import org.opalj.tac.Var
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.FieldRead

/**
 * Base trait for analyses that analyze the purity of methods.
 *
 * Provides types and methods needed for purity analyses.
 */
trait AbstractPurityAnalysis extends FPCFAnalysis {

    /** The type of the TAC domain. */
    type V = DUVar[(Domain with RecordDefUse)#DomainValue]

    /**
     * The state of the analysis.
     * Analyses are expected to extend this trait with the information they need.
     *
     * lbPurity - The current minimum possible purity level for the method
     * ubPurity - The current maximum purity level for the method
     * method - The currently analyzed method
     * declClass - The declaring class of the currently analyzed method
     * code - The code of the currently analyzed method
     */
    trait AnalysisState {
        var lbPurity: Purity
        var ubPurity: Purity
        val method: Method
        val declClass: ObjectType
        val code: Array[Stmt[V]]
    }

    type StateType <: AnalysisState

    protected[this] def raterFqn: String

    val rater: DomainSpecificRater

    protected[this] val tacai: Method ⇒ TACode[TACMethodParameter, V] = project.get(DefaultTACAIKey)
    protected[this] val isMethodOverridable: Method ⇒ Answer = project.get(IsOverridableMethodKey)
    protected[this] val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)

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

                case NonVirtualFunctionCall.ASTID | VirtualFunctionCall.ASTID ⇒
                    val rcvr = expr.asInstanceFunctionCall.receiver
                    !rcvr.isVar || rcvr.asVar.value.asDomainReferenceValue.isNull.isYesOrUnknown

                case StaticFunctionCall.ASTID ⇒ false

                case _                        ⇒ true
            }
        }

        val stmt = state.code(origin)
        (stmt.astID: @switch) match {
            case StaticMethodCall.ASTID ⇒ false // We are looking for implicit exceptions only

            case Throw.ASTID ⇒
                stmt.asThrow.exception.asVar.value.asDomainReferenceValue.isNull.isNotNo

            case NonVirtualMethodCall.ASTID | VirtualMethodCall.ASTID ⇒
                val rcvr = stmt.asInstanceMethodCall.receiver
                !rcvr.isVar || rcvr.asVar.value.asDomainReferenceValue.isNull.isNotNo

            case Assignment.ASTID ⇒ evaluationMayCauseVMLevelException(stmt.asAssignment.expr)

            case ExprStmt.ASTID   ⇒ evaluationMayCauseVMLevelException(stmt.asExprStmt.expr)

            case _                ⇒ true
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
        implicit val code: Array[Stmt[V]] = state.code
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
            // For method calls, purity depends on purity of the called method
            case StaticMethodCall.ASTID ⇒
                val call = stmt.asStaticMethodCall
                if (!isDomainSpecificCall(call, None)) {
                    val StaticMethodCall(_, declClass, _, name, descr, params) = stmt
                    val callee = call.resolveCallTarget
                    checkPurityOfCall(declClass, name, descr, None, params, callee)
                } else true
            case NonVirtualMethodCall.ASTID ⇒
                val call = stmt.asNonVirtualMethodCall
                if (!isDomainSpecificCall(call, Some(call.receiver))) {
                    val NonVirtualMethodCall(_, declClass, _, name, descr, rcvr, params) = stmt
                    val callee = stmt.asNonVirtualMethodCall.resolveCallTarget
                    checkPurityOfCall(declClass, name, descr, Some(rcvr), params, callee)
                } else true
            case VirtualMethodCall.ASTID ⇒
                val call = stmt.asVirtualMethodCall
                if (!isDomainSpecificCall(call, Some(call.receiver))) {
                    val VirtualMethodCall(_, declClass, isInterface, name, descr, rcvr, params) =
                        stmt
                    checkPurityOfVirtualCall(declClass, isInterface, name, rcvr, params, descr)
                } else true

            // Returning objects/arrays is pure, if the returned object/array is locally initialized
            // and non-escaping or the object is immutable
            case ReturnValue.ASTID ⇒
                checkPurityOfReturn(stmt.asReturnValue.expr)
                true
            case Throw.ASTID ⇒
                checkPurityOfReturn(stmt.asThrow.exception)
                true

            // Synchronization on non-escaping locally initialized objects/arrays is pure (and
            // useless...)
            case MonitorEnter.ASTID | MonitorExit.ASTID ⇒
                isLocal(stmt.asSynchronizationStmt.objRef, ImpureByAnalysis)

            // Storing into non-escaping locally initialized objects/arrays is pure
            case ArrayStore.ASTID ⇒ isLocal(stmt.asArrayStore.arrayRef, ImpureByAnalysis)
            case PutField.ASTID   ⇒ isLocal(stmt.asPutField.objRef, ImpureByAnalysis)

            case PutStatic.ASTID ⇒
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
            case CaughtException.ASTID ⇒
                for {
                    origin ← stmt.asCaughtException.origins
                    if isImmediateVMException(origin)
                } {
                    val baseOrigin = state.code(ai.underlyingPC(origin))
                    val ratedResult = rater.handleException(baseOrigin)
                    if (ratedResult.isDefined) atMost(ratedResult.get)
                    else atMost(SideEffectFree)
                }
                true

            // Reference comparisons may have different results for structurally equal values
            case If.ASTID ⇒
                val If(_, left, _, right, _) = stmt
                if (left.cTpe eq ComputationalTypeReference)
                    if (!(isLocal(left, CompileTimePure) || isLocal(right, CompileTimePure)))
                        atMost(SideEffectFree)
                true

            // The following statements do not further influence purity
            case Goto.ASTID | JSR.ASTID | Ret.ASTID | Switch.ASTID | Assignment.ASTID |
                Return.ASTID | Nop.ASTID | ExprStmt.ASTID | Checkcast.ASTID ⇒
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
            // For function calls, purity depends on purity of the method called
            case StaticFunctionCall.ASTID ⇒
                if (!isDomainSpecificCall(expr.asStaticFunctionCall, None)) {
                    val StaticFunctionCall(_, declClass, _, name, descr, params) = expr
                    val callee = expr.asStaticFunctionCall.resolveCallTarget
                    checkPurityOfCall(declClass, name, descr, None, params, callee)
                } else true
            case NonVirtualFunctionCall.ASTID ⇒
                val call = expr.asNonVirtualFunctionCall
                if (!isDomainSpecificCall(call, Some(call.receiver))) {
                    val NonVirtualFunctionCall(_, declClass, _, name, descr, rcvr, params) = expr
                    val callee = call.resolveCallTarget
                    checkPurityOfCall(declClass, name, descr, Some(rcvr), params, callee)
                } else true
            case VirtualFunctionCall.ASTID ⇒
                val call = expr.asVirtualFunctionCall
                if (!isDomainSpecificCall(call, Some(call.receiver))) {
                    val VirtualFunctionCall(_, declClass, interface, name, descr, rcvr, params) =
                        expr
                    checkPurityOfVirtualCall(declClass, interface, name, rcvr, params, descr)
                } else true

            // Field/array loads are pure if the field is (effectively) final or the object/array is
            // local and non-escaping
            case GetStatic.ASTID ⇒
                implicit val code: Array[Stmt[V]] = state.code
                val ratedResult = rater.handleGetStatic(expr.asGetStatic)
                if (ratedResult.isDefined) atMost(ratedResult.get)
                else checkPurityOfFieldRef(expr.asGetStatic)
                true
            case GetField.ASTID ⇒
                checkPurityOfFieldRef(expr.asGetField)
                true
            case ArrayLoad.ASTID ⇒
                if (state.ubPurity.isDeterministic)
                    isLocal(expr.asArrayLoad.arrayRef, SideEffectFree)
                true

            // We don't handle unresolved Invokedynamic
            // - either OPAL removes it or we forget about it
            case Invokedynamic.ASTID ⇒
                atMost(ImpureByAnalysis)
                false

            // The following expressions do not further influence purity, potential exceptions are
            // handled explicitly
            case New.ASTID | NewArray.ASTID | InstanceOf.ASTID | Compare.ASTID | Param.ASTID |
                MethodTypeConst.ASTID | MethodHandleConst.ASTID | IntConst.ASTID | LongConst.ASTID |
                FloatConst.ASTID | DoubleConst.ASTID | StringConst.ASTID | ClassConst.ASTID |
                NullExpr.ASTID | BinaryExpr.ASTID | PrefixExpr.ASTID | PrimitiveTypecastExpr.ASTID |
                ArrayLength.ASTID | Var.ASTID ⇒
                true

        }

        isExprNotImpure && expr.forallSubExpressions(checkPurityOfExpr)
    }

    /**
     * Examines a virtual call for its influence on the method's purity.
     * Resolves the call and uses [[checkPurityOfCall]] to examine the individual possible callees.
     * This method will return false for impure calls, so evaluation can be terminated early.
     */
    def checkPurityOfVirtualCall(
        rcvrType:  ReferenceType,
        interface: Boolean,
        name:      String,
        rcvr:      Expr[V],
        params:    Seq[Expr[V]],
        descr:     MethodDescriptor
    )(implicit state: StateType): Boolean = {
        onVirtualMethod(rcvrType, interface, name, rcvr, params, descr,
            callee ⇒ checkPurityOfCall(rcvrType, name, descr, Some(rcvr), params, callee),
            dm ⇒ checkMethodPurity(
                propertyStore(dm, VirtualMethodPurity.key), (Some(rcvr), params)
            ),
            () ⇒ { atMost(ImpureByAnalysis); false })
    }

    def onVirtualMethod(
        receiverType: ReferenceType,
        interface:    Boolean,
        name:         String,
        receiver:     Expr[V],
        params:       Seq[Expr[V]],
        descr:        MethodDescriptor,
        onPrecise:    org.opalj.Result[Method] ⇒ Boolean,
        onMultiple:   DeclaredMethod ⇒ Boolean,
        onUnknown:    () ⇒ Boolean
    )(implicit state: StateType): Boolean = {
        val rcvrType =
            if (receiver.isVar) receiver.asVar.value.asDomainReferenceValue.valueType
            else Some(receiverType)

        if (rcvrType.isEmpty) {
            // IMPROVE Just use the CFG to check if we have a normal successor
            true // We don't have to examine calls that will result in an NPE
        } else if (rcvrType.get.isArrayType) {
            val callee = project.instanceCall(state.declClass, ObjectType.Object, name, descr)
            onPrecise(callee)
        } else if (receiver.asVar.value.asDomainReferenceValue.isPrecise) {
            // The receiver could refer to further expressions in a non-flat representation.
            // To avoid special handling, we just fallback to the general case of virtual/interface
            // calls here as the analysis is intended to be used on flat representations anyway.
            val callee = project.instanceCall(state.declClass, rcvrType.get, name, descr)
            onPrecise(callee)
        } else {
            val callee =
                declaredMethods(state.declClass.packageName, rcvrType.get.asObjectType, name, descr)

            if (!callee.hasDefinition || isMethodOverridable(callee.methodDefinition).isNotNo) {
                onUnknown() // We don't know all overrides
            } else {
                onMultiple(callee)
            }
        }
    }

    /**
     * Examines a call to a given callee for its influence on the method's purity.
     * This method will return false for impure calls, so evaluation can be terminated early.
     */
    def checkPurityOfCall(
        receiverClass: ReferenceType,
        name:          String,
        descriptor:    MethodDescriptor,
        receiver:      Option[Expr[V]],
        params:        Seq[Expr[V]],
        methodResult:  org.opalj.Result[Method]
    )(implicit state: StateType): Boolean = {
        val receiverType =
            if (receiverClass.isArrayType) ObjectType.Object else receiverClass.asObjectType

        val dm = methodResult match {
            case Success(callee) if callee eq state.method ⇒
                return true; // Self-recursive calls don't need to be checked
            case Success(callee) ⇒ declaredMethods(callee)
            case _ ⇒
                declaredMethods(state.declClass.packageName, receiverType, name, descriptor)
        }
        val calleePurity = propertyStore(dm, Purity.key)
        checkMethodPurity(calleePurity, (receiver, params))
    }

    /**
     * Examines the influence of the purity property of a method on the examined method's purity.
     *
     * @note Adds dependendies when necessary.
     */
    def checkMethodPurity(
        ep:     EOptionP[DeclaredMethod, Property],
        params: (Option[Expr[V]], Seq[Expr[V]])    = (None, Seq.empty)
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
                case Some(field) if field.isStatic ⇒
                    checkFieldMutability(propertyStore(field, FieldMutability.key), None)
                case Some(field) ⇒
                    checkFieldMutability(
                        propertyStore(field, FieldMutability.key), Some(fieldRef.asGetField.objRef)
                    )
                case _ ⇒ // Unknown field
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
        case EPS(_, _: FinalField, _) ⇒ // Final fields don't impede purity
        case FinalEP(_, _) ⇒ // Mutable field
            if (objRef.isDefined) {
                if (state.ubPurity.isDeterministic)
                    isLocal(objRef.get, SideEffectFree)
            } else atMost(SideEffectFree)
        case _ ⇒
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

        val value = returnValue.asVar.value.asDomainReferenceValue
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

            returnTypes.forall { returnType ⇒
                val typeImmutability =
                    propertyStore(
                        returnType,
                        TypeImmutability.key
                    ).asInstanceOf[EOptionP[ObjectType, TypeImmutability]]
                checkTypeMutability(typeImmutability, returnValue)
            }
        }
    }

    def checkTypeMutability(
        ep:   EOptionP[ObjectType, Property],
        expr: Expr[V]
    )(implicit state: StateType): Boolean = ep match {
        // Returning immutable object is pure
        case EPS(_, ImmutableType | ImmutableObject, _) ⇒ true
        case FinalEP(_, _) ⇒
            atMost(Pure) // Can not be compile time pure if mutable object is returned
            if (state.ubPurity.isDeterministic)
                isLocal(expr, SideEffectFree)
            false // Return early if we are already side-effect free
        case _ ⇒
            reducePurityLB(SideEffectFree)
            if (state.ubPurity.isDeterministic)
                handleUnknownTypeMutability(ep, expr)
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
     * Retrieves and commits the methods purity as calculated for its declaring class type for the
     * current DefinedMethod that represents the non-overwritten method in a subtype.
     */
    def baseMethodPurity(dm: DefinedMethod): PropertyComputationResult = {

        def c(eps: SomeEOptionP): PropertyComputationResult = eps match {
            case FinalEP(_, p)                  ⇒ Result(dm, p)
            case ep @ IntermediateEP(_, lb, ub) ⇒ IntermediateResult(dm, lb, ub, Seq(ep), c)
            case epk                            ⇒ IntermediateResult(dm, ImpureByAnalysis, CompileTimePure, Seq(epk), c)
        }

        c(propertyStore(declaredMethods(dm.definedMethod), Purity.key))
    }

    /**
     * Determines the purity of the given method.
     *
     * @param definedMethod A defined method with a body.
     */
    def determinePurity(definedMethod: DefinedMethod): PropertyComputationResult

    /** Called when the analysis is scheduled lazily. */
    def doDeterminePurity(e: Entity): PropertyComputationResult = {
        e match {
            case dm: DefinedMethod if dm.definedMethod.body.isDefined ⇒
                determinePurity(dm)
            case dm: DeclaredMethod ⇒ Result(dm, ImpureByLackOfInformation)
            case _ ⇒
                throw new UnknownError("purity is only defined for declared methods")
        }
    }

    def resolveDomainSpecificRater(fqn: String): DomainSpecificRater = {
        import scala.reflect.runtime.universe.runtimeMirror
        val mirror = runtimeMirror(getClass.getClassLoader)
        try {
            val module = mirror.staticModule(fqn)
            mirror.reflectModule(module).instance.asInstanceOf[DomainSpecificRater]
        } catch {
            case ex @ (_: ScalaReflectionException | _: ClassCastException) ⇒
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
