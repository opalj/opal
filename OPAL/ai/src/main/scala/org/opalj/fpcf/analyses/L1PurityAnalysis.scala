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

import scala.annotation.switch
import org.opalj.ai.Domain
import org.opalj.ai.isVMLevelValue
import org.opalj.ai.pcOfVMLevelValue
import org.opalj.ai.ValueOrigin
import org.opalj.ai.VMLevelValuesOriginOffset
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.ComputationalTypeReference
import org.opalj.br.FieldType
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.fpcf.properties.AtLeastConditionallyImmutableObject
import org.opalj.fpcf.properties.AtLeastConditionallyImmutableType
import org.opalj.fpcf.properties.ClassImmutability
import org.opalj.fpcf.properties.CLBPure
import org.opalj.fpcf.properties.CLBSideEffectFree
import org.opalj.fpcf.properties.EffectivelyFinalField
import org.opalj.fpcf.properties.FieldMutability
import org.opalj.fpcf.properties.ImmutableObject
import org.opalj.fpcf.properties.ImmutableType
import org.opalj.fpcf.properties.LBImpure
import org.opalj.fpcf.properties.LBPure
import org.opalj.fpcf.properties.Purity
import org.opalj.fpcf.properties.LBSideEffectFree
import org.opalj.fpcf.properties.TypeImmutability
import org.opalj.fpcf.properties.FinalField
import org.opalj.tac._

/**
 * An inter-procedural analysis to determine a method's purity.
 *
 * @note This analysis is sound only up to the usual standards, i.e. it does not cope with
 *       VirtualMachineErrors and may be unsound in the presence of native code, reflection or
 *       `sun.misc.Unsafe`. Calls to native methods are generally
 *       handled soundly as they are considered [[org.opalj.fpcf.properties.LBImpure]].
 *       There are no soundness guarantees in the presence of load-time transformation.
 *       Soundness in general depends on the soundness of the analyses that compute properties used
 *       by this analysis, e.g. field mutability.
 *
 * @note This analysis is sound even if the three address code hierarchy is not flat, it will
 *       produce better results for a flat hierarchy, though. This is because it will not assess the
 *       types of expressions other than [[org.opalj.tac.Var]]s.
 *
 * @note This analysis only derives the properties [[org.opalj.fpcf.properties.LBPure]],
 *       [[org.opalj.fpcf.properties.LBSideEffectFree]] and
 *       [[org.opalj.fpcf.properties.LBImpure]].
 *       Compared to the `L0PurityAnalysis`, it deals with all methods, even if their reference type
 *       parameters are mutable. It can handle accesses of (effectively) final instance fields,
 *       array loads, array length and virtual/interface calls. Array stores and field writes as
 *       well as (useless) synchronization on locally created, non-escaping objects/arrays are also
 *       handled. Newly allocated objects/arrays returned from callees are not identified.
 *       VMExceptions are treated as `SideEffectFree`, explicit exceptions are treated as
 *       `LBImpure`, as the `Throwable` constructor calls the overridable `fillInStackTrace` method.
 *
 * @author Dominik Helm
 */
class L1PurityAnalysis private (val project: SomeProject) extends FPCFAnalysis {

    type V = DUVar[(Domain with RecordDefUse)#DomainValue]

    val tacai: Method ⇒ TACode[TACMethodParameter, V] = project.get(DefaultTACAIKey)

    val isOverridable: Method ⇒ Answer = project.get(IsOverridableMethodKey)
    val declaredMethods = project.get(DeclaredMethodsKey)

    /**
     * Checks whether the statement, which is the origin of an exception, directly created the
     * exception or if the VM instantiated the exception. Here, we are only concerned about the
     * exceptions thrown by the instructions not about exceptions that are transitively thrown;
     * e.g. if a method is called.
     */
    def isImmediateVMException(origin: ValueOrigin)(implicit code: Array[Stmt[V]]): Boolean = {
        if (VMLevelValuesOriginOffset < origin && origin < 0)
            return false; // Parameters aren't implicit exceptions

        def evaluationMayCauseVMLevelException(expr: Expr[V]): Boolean = {
            (expr.astID: @switch) match {

                case NonVirtualFunctionCall.ASTID | VirtualFunctionCall.ASTID ⇒
                    val rcvr = expr.asInstanceFunctionCall.receiver
                    !rcvr.isVar || rcvr.asVar.value.asDomainReferenceValue.isNull.isNotNo

                case StaticFunctionCall.ASTID ⇒ false

                case _                        ⇒ true
            }
        }

        val pc = if (isVMLevelValue(origin)) pcOfVMLevelValue(origin) else origin
        val stmt = code(pc)
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
     * Determines the purity of the given method.
     *
     * @param method A method with a body.
     */
    def determinePurity(method: Method): PropertyComputationResult = {
        val definedMethod = declaredMethods(method)

        // We treat all synchronized methods as impure
        if (method.isSynchronized)
            return Result(definedMethod, LBImpure);

        implicit val TACode(_, code, cfg, _, _) = tacai(method)
        val declClass = method.classFile.thisType

        var dependees: Set[EOptionP[Entity, Property]] = Set.empty
        /**
         * Current purity level for this method.
         * The checkPurityOfX methods will assign to this var to aggregate the purity.
         */
        var maxPurity: Purity = LBPure

        // Creating implicit exceptions is side-effect free (except for the fillInStackTrace)
        val bbsCausingExceptions = cfg.abnormalReturnNode.predecessors
        if (bbsCausingExceptions.exists(bb ⇒ isImmediateVMException(bb.asBasicBlock.endPC))) {
            maxPurity = LBSideEffectFree
        }

        /**
         * Checks if a reference was created locally, hence actions on it might not
         * influence purity.
         *
         * @note Fresh references can be treated as non-escaping as the analysis result will be
         *       impure if anything escapes the method via parameters, static field
         *       assignments or calls.
         */
        def isLocal(expr: Expr[V]): Boolean = {
            if (expr.isConst)
                true
            else if (expr.isVar) {
                expr.asVar.definedBy.forall { defSite ⇒
                    if (defSite >= 0) {
                        val astID = code(defSite).asAssignment.expr.astID
                        astID == New.ASTID || astID == NewArray.ASTID
                    } else if (isVMLevelValue(defSite)) {
                        true // VMLevelValues are freshly created
                    } else {
                        // In initializers the self reference (this) is local
                        method.isConstructor && defSite == OriginOfThis
                    }
                }
            } else {
                // The expression could refer to further expressions in a non-flat representation.
                // In that case it could be, e.g., a GetStatic. In that case the reference is
                // not locally created and/or initialized. To avoid special handling, we just
                // fallback to false here as the analysis is intended to be used on flat
                // representations anyway.
                false
            }
        }

        /**
         * Examines a statement for its influence on the method's purity.
         * This method will return false for impure statements,
         * so evaluation can be terminated early.
         */
        def checkPurityOfStmt(stmt: Stmt[V]): Boolean = {
            val isStmtNotImpure = (stmt.astID: @switch) match {
                // For method calls, purity depends on purity of the called method
                case StaticMethodCall.ASTID ⇒
                    val StaticMethodCall(_, declClass, isInterface, name, descr, _) = stmt
                    val callee = project.staticCall(declClass, isInterface, name, descr)
                    checkPurityOfCall(declClass, name, callee)
                case NonVirtualMethodCall.ASTID ⇒
                    val NonVirtualMethodCall(_, declClass, isInterface, name, descr, _, _) =
                        stmt
                    val callee = project.specialCall(declClass, isInterface, name, descr)
                    checkPurityOfCall(declClass, name, callee)
                case VirtualMethodCall.ASTID ⇒
                    val VirtualMethodCall(_, declClass, isInterface, name, descr, rcvr, _) = stmt
                    checkPurityOfVirtualCall(declClass, isInterface, name, rcvr, descr)

                // Returning objects/arrays is pure, if the returned object/array is locally
                // initialized and non-escaping or the object is immutable
                case ReturnValue.ASTID ⇒
                    val value = stmt.asReturnValue.expr
                    if (!isLocal(value)) checkPurityOfReturn(value)
                    true
                case Throw.ASTID ⇒
                    val ex = stmt.asThrow.exception
                    if (!isLocal(ex)) checkPurityOfReturn(ex)
                    true

                // Synchronization on non-escaping locally initialized objects/arrays is pure (and
                // useless...)
                case MonitorEnter.ASTID ⇒ isLocal(stmt.asMonitorEnter.objRef)
                case MonitorExit.ASTID  ⇒ isLocal(stmt.asMonitorExit.objRef)

                // Storing into non-escaping locally initialized objects/arrays is pure
                case ArrayStore.ASTID   ⇒ isLocal(stmt.asArrayStore.arrayRef)
                case PutField.ASTID     ⇒ isLocal(stmt.asPutField.objRef)

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
                    false

                case CaughtException.ASTID ⇒
                    // Creating implicit exceptions is side-effect free because the Throwable
                    // constructor will add the non-deterministic stack trace via fillInStackTrace
                    if (stmt.asCaughtException.origins.exists(isImmediateVMException))
                        maxPurity = LBSideEffectFree
                    true

                // The following statements do not further influence purity
                case If.ASTID | Goto.ASTID | JSR.ASTID | Ret.ASTID | Switch.ASTID |
                    Assignment.ASTID | Return.ASTID | Nop.ASTID | ExprStmt.ASTID |
                    Checkcast.ASTID ⇒
                    true
            }

            isStmtNotImpure && stmt.forallSubExpressions(checkPurityOfExpr)
        }

        /**
         * Examines an expression for its influence on the method purity.
         * This method will return false for impure expressions,
         * so evaluation can be terminated early.
         */
        def checkPurityOfExpr(expr: Expr[V]): Boolean = {
            val isExprNotImpure = (expr.astID: @switch) match {
                // For function calls, purity depends on purity of the method called
                case StaticFunctionCall.ASTID ⇒
                    val StaticFunctionCall(_, declClass, interface, name, descr, params) = expr
                    val callee = project.staticCall(declClass, interface, name, descr)
                    checkPurityOfCall(declClass, name, callee)
                case NonVirtualFunctionCall.ASTID ⇒
                    val NonVirtualFunctionCall(_, declClass, interface, name, descr, rcvr, params) =
                        expr
                    val callee = project.specialCall(declClass, interface, name, descr)
                    checkPurityOfCall(declClass, name, callee)
                case VirtualFunctionCall.ASTID ⇒
                    val VirtualFunctionCall(_, declClass, interface, name, descr, rcvr, params) =
                        expr
                    checkPurityOfVirtualCall(declClass, interface, name, rcvr, descr)

                // Field/array loads are pure if the field is (effectively) final or the
                // object/array is local and non-escaping
                case GetStatic.ASTID ⇒
                    val GetStatic(_, declClass, name, fieldType) = expr
                    checkPurityOfFieldRef(declClass, name, fieldType)
                    true
                case GetField.ASTID ⇒
                    val GetField(_, declClass, name, fieldType, objRef) = expr
                    if (!isLocal(objRef)) checkPurityOfFieldRef(declClass, name, fieldType)
                    true
                case ArrayLoad.ASTID ⇒
                    val arrayRef = expr.asArrayLoad.arrayRef
                    if (!isLocal(arrayRef)) maxPurity = LBSideEffectFree
                    true

                // We don't handle unresolved Invokedynamic - either OPAL removes it or we forget about it
                case Invokedynamic.ASTID        ⇒ false

                // "just" pure - not "allocation free pure"!
                // IMPROVE also derive PureWithoutAllocations where possible
                case New.ASTID | NewArray.ASTID ⇒ true

                // The following expressions do not further influence purity
                case InstanceOf.ASTID | Compare.ASTID | Param.ASTID | MethodTypeConst.ASTID |
                    MethodHandleConst.ASTID | IntConst.ASTID | LongConst.ASTID | FloatConst.ASTID |
                    DoubleConst.ASTID | StringConst.ASTID | ClassConst.ASTID | NullExpr.ASTID |
                    BinaryExpr.ASTID | PrefixExpr.ASTID | PrimitiveTypecastExpr.ASTID |
                    ArrayLength.ASTID | Var.ASTID ⇒
                    true
            }

            isExprNotImpure && expr.forallSubExpressions(checkPurityOfExpr)
        }

        /**
         * Examines a virtual call for its influence on the method purity.
         * Resolves the call and uses [[checkPurityOfCall]] to examine the individual possible
         * callees. This method will return false for impure calls, so evaluation can be terminated
         * early.
         */
        def checkPurityOfVirtualCall(
            rcvrType:    ReferenceType, // TODO Rename declaringClassType(?)
            isInterface: Boolean,
            name:        String,
            receiver:    Expr[V],
            descr:       MethodDescriptor
        ): Boolean = {
            if (receiver.isVar && receiver.asVar.value.asDomainReferenceValue.isPrecise) {
                // The receiver could refer to further expressions in a non-flat representation.
                // To avoid special handling, we just fallback to the general case of
                // virtual/interface calls here as the analysis is intended to be used on flat
                // representations anyway.
                // IMPROVE Just use the CFG to check if we have a normal successor
                val rcvr = receiver.asVar.value.asDomainReferenceValue
                if (rcvr.isNull.isYes)
                    true // We don't have to examine calls that will result in an NPE
                else {
                    val callee = project.instanceCall(declClass, rcvr.valueType.get, name, descr)
                    checkPurityOfCall(rcvrType, name, callee)
                }
            } else if (rcvrType.isObjectType && (
                isInterface || {
                    val mOpt = project.resolveMethodReference(rcvrType, name, descr, false)
                    mOpt.isEmpty || isOverridable(mOpt.get).isYes
                }
            )) {
                false // We don't know all overrides, so we are impure
            } else {
                val callees =
                    if (isInterface) project.interfaceCall(rcvrType.asObjectType, name, descr)
                    else project.virtualCall(declClass.packageName, rcvrType, name, descr)
                if (callees.isEmpty)
                    // We know nothing about the target methods
                    // (they are not in the scope of the current project)
                    false
                else
                    callees.forall { callee ⇒
                        /* Remember that checkPurityOfCall returns false if call is impure for
                        early termination */
                        checkPurityOfCall(rcvrType, name, Success(callee))
                    }
            }
        }

        /**
         * Examines a call to a given callee for its influence on the method purity.
         * This method will return false for impure calls, so evaluation can be terminated early.
         *
         * @note Adds dependendies when necessary.
         */
        def checkPurityOfCall(
            receiverClass: ReferenceType,
            name:          String,
            methodResult:  org.opalj.Result[Method]
        ): Boolean = {
            if (receiverClass == ObjectType.Object && name == "<init>") {
                true // The java.lang.Object constructor is pure
            } else {
                methodResult match {
                    case Success(callee) if declaredMethods.contains(callee) ⇒
                        if (callee == method) true // Self-recursive don't need to be checked
                        else {
                            val calleePurity = propertyStore(declaredMethods(callee), Purity.key)
                            calleePurity match {
                                case EP(_, LBPure) ⇒ true
                                case EP(_, LBSideEffectFree) ⇒
                                    maxPurity = LBSideEffectFree
                                    true
                                case ep @ EP(_, CLBSideEffectFree) ⇒
                                    dependees += ep
                                    maxPurity = LBSideEffectFree
                                    true
                                case ep @ EP(_, CLBPure) ⇒
                                    dependees += ep
                                    true
                                case EP(_, _) ⇒ false // Impure or unknown purity level
                                case epk ⇒
                                    dependees += epk
                                    true
                            }
                        }
                    case _ ⇒ false // Target method unknown (not in scope of current project)
                }
            }
        }

        /**
         * Examines whether a field read influences a method's purity.
         * Reading values from fields that are not (effectively) final may cause nondeterministic
         * behavior, so the method can only be side-effect free.
         */
        def checkPurityOfFieldRef(
            declaringClass: ObjectType,
            name:           String,
            fieldType:      FieldType
        ): Unit = {
            if (maxPurity != LBSideEffectFree) { // Don't do dependee checks if already not pure
                project.resolveFieldReference(declaringClass, name, fieldType) match {
                    case Some(field) if field.isFinal ⇒ // constants do not impede purity!
                    case Some(field) if field.isPrivate /*&& field.isNonFinal*/ ⇒
                        val fieldMutability = propertyStore(field, FieldMutability.key)
                        fieldMutability match {
                            case EP(_, _: FinalField) ⇒ // Final fields don't impede purity
                            case EP(_, _)             ⇒ maxPurity = LBSideEffectFree
                            case epk                  ⇒ dependees += epk
                        }
                    case _ ⇒ maxPurity = LBSideEffectFree // Mutable or unknown field
                }
            }
        }

        /**
         * Examines the effect of returning a value on the method purity.
         * Returning a reference to a mutable object or array may cause nondeterministic behavior
         * as the object/array may be modified between invocations of the method, so the method can
         * only be side-effect free. E.g., a given parameter which references a mutable object is
         * returned (and not otherwise accessed).
         */
        def checkPurityOfReturn(returnValue: Expr[V]): Unit = {
            // Only non-primitive return values influence purity.
            if (returnValue.cTpe != ComputationalTypeReference)
                return ;

            // If the method can't be pure, the return value is not important.
            if (maxPurity == LBSideEffectFree)
                return ;

            if (!returnValue.isVar) {
                // The expression could refer to further expressions in a non-flat
                // representation. To avoid special handling, we just fallback to SideEffectFree
                // here as the analysis is intended to be used on flat representations anyway.
                maxPurity = LBSideEffectFree
                return ;
            }

            val value = returnValue.asVar.value.asDomainReferenceValue
            if (value.isNull.isYes)
                // Null is immutable
                return ;

            if (value.upperTypeBound.exists(_.isArrayType)) {
                // Arrays are always mutable
                maxPurity = LBSideEffectFree
                return ;
            }

            if (value.isPrecise) { // Precise class known, use ClassImmutability
                val cfo = project.classFile(value.upperTypeBound.head.asObjectType)
                if (cfo.isEmpty)
                    maxPurity = LBSideEffectFree // Unknown class, might be mutable
                else
                    propertyStore(cfo.get, ClassImmutability.key) match {
                        case EP(_, ImmutableObject) ⇒
                        // Returning immutable objects is pure
                        case ep @ EP(_, AtLeastConditionallyImmutableObject) ⇒
                            dependees += ep
                        case EP(_, _) ⇒ maxPurity = LBSideEffectFree
                        case epk      ⇒ dependees += epk
                    }
            } else { // Precise class unknown, use TypeImmutability
                // IMPROVE Use ObjectType once we attach the respective information to ObjectTypes
                val cfos = value.upperTypeBound.map { tpe ⇒
                    project.classFile(tpe.asObjectType)
                }
                if (cfos.exists(_.isEmpty))
                    maxPurity = LBSideEffectFree // Unknown class, might be mutable
                else {
                    cfos.forall { cfo ⇒
                        propertyStore(cfo.get, TypeImmutability.key) match {
                            case EP(_, ImmutableType) ⇒
                                true // Returning immutable objects is pure
                            case ep @ EP(_, AtLeastConditionallyImmutableType) ⇒
                                dependees += ep
                                true
                            case EP(_, _) ⇒
                                maxPurity = LBSideEffectFree
                                false // Return early if we are already side-effect free
                            case epk ⇒
                                dependees += epk
                                true
                        }
                    }
                }
            }
        }

        /**
         * Continuation to handle updates to properties of dependees.
         * Dependees may be
         *     - methods called (for their purity)
         *     - fields read (for their mutability)
         *     - classes files for class types returned (for their mutability)
         */
        def c(e: Entity, p: Property, u: UpdateType): PropertyComputationResult = {
            dependees = dependees.filter(_.e ne e)
            p match {
                // Cases resulting in conditional purity
                case CLBPure |
                    AtLeastConditionallyImmutableType | AtLeastConditionallyImmutableObject ⇒
                    val newEP = EP(e, p)
                    dependees += newEP // For conditional result, keep the dependence

                // Cases resulting in conditional side-effect freeness
                case CLBSideEffectFree ⇒
                    val newEP = EP(e, p)
                    dependees += newEP // For conditional result, keep the dependence
                    maxPurity = LBSideEffectFree

                // Cases that are pure
                case LBPure | // Call to pure method
                    EffectivelyFinalField | // Reading eff. final fields
                    ImmutableType | ImmutableObject ⇒ // Returning immutable reference

                // Cases resulting in side-effect freeness
                case LBSideEffectFree | // Call to side-effect free method
                    _: FieldMutability | // Reading non-final field
                    _: TypeImmutability | _: ClassImmutability ⇒ // Returning mutable reference
                    maxPurity = LBSideEffectFree

                // Cases resulting in impurity LBImpure ...  call to impure method
                case _ ⇒
                    return Result(definedMethod, LBImpure)
            }

            if (dependees.isEmpty) {
                Result(definedMethod, maxPurity)
            } else if (maxPurity == LBPure) {
                IntermediateResult(definedMethod, CLBPure, dependees, c)
            } else {
                IntermediateResult(definedMethod, CLBSideEffectFree, dependees, c)
            }
        }

        val stmtCount = code.length
        var s = 0
        while (s < stmtCount) {
            if (!checkPurityOfStmt(code(s))) // Early return for impure statements
                return Result(definedMethod, LBImpure)
            s += 1
        }

        // Every method that is not identified as being impure is (conditionally) pure or
        // (conditionally) side-effect free.
        if (dependees.isEmpty) {
            Result(definedMethod, maxPurity)
        } else if (maxPurity == LBPure) {
            IntermediateResult(definedMethod, CLBPure, dependees, c)
        } else {
            IntermediateResult(definedMethod, CLBSideEffectFree, dependees, c)
        }
    }
}

object L1PurityAnalysis extends FPCFAnalysisScheduler {

    override def derivedProperties: Set[PropertyKind] = Set(Purity.key)

    override def usedProperties: Set[PropertyKind] = {
        Set(FieldMutability, ClassImmutability, TypeImmutability)
    }

    def start(p: SomeProject, ps: PropertyStore): FPCFAnalysis = {
        val analysis = new L1PurityAnalysis(p)
        ps.scheduleForEntities(p.allMethodsWithBody)(analysis.determinePurity)
        analysis
    }

    def startLazily(p: SomeProject, ps: PropertyStore): FPCFAnalysis = {
        val analysis = new L1PurityAnalysis(p)
        val propertyComputation = (e: Entity) ⇒ analysis.determinePurity(e.asInstanceOf[Method])
        ps.scheduleLazyPropertyComputation(Purity.key, propertyComputation)
        analysis
    }
}
