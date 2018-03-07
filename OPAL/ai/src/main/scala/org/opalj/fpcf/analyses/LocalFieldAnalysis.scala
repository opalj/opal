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

import org.opalj.ai.Domain
import org.opalj.ai.ValueOrigin
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.AllocationSite
import org.opalj.br.ClassFile
import org.opalj.br.DeclaredMethod
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.ClassExtensibilityKey
import org.opalj.br.analyses.cg.ClosedPackagesKey
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CFGNode
import org.opalj.br.cfg.ExitNode
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.analyses.escape.DefaultEntityEscapeAnalysis
import org.opalj.fpcf.properties.Conditional
import org.opalj.fpcf.properties.ConditionalFreshReturnValue
import org.opalj.fpcf.properties.EscapeInCallee
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.EscapeViaReturn
import org.opalj.fpcf.properties.ExtensibleLocalField
import org.opalj.fpcf.properties.FieldLocality
import org.opalj.fpcf.properties.FreshReturnValue
import org.opalj.fpcf.properties.LocalField
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.NoFreshReturnValue
import org.opalj.fpcf.properties.NoLocalField
import org.opalj.fpcf.properties.PrimitiveReturnValue
import org.opalj.fpcf.properties.ReturnValueFreshness
import org.opalj.fpcf.properties.VConditionalFreshReturnValue
import org.opalj.fpcf.properties.VFreshReturnValue
import org.opalj.fpcf.properties.VNoFreshReturnValue
import org.opalj.fpcf.properties.VPrimitiveReturnValue
import org.opalj.fpcf.properties.VirtualMethodReturnValueFreshness
import org.opalj.tac.Assignment
import org.opalj.tac.Const
import org.opalj.tac.DUVar
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.Expr
import org.opalj.tac.GetField
import org.opalj.tac.New
import org.opalj.tac.NewArray
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.PutField
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.Stmt
import org.opalj.tac.TACMethodParameter
import org.opalj.tac.TACode
import org.opalj.tac.VirtualFunctionCall

/**
 * TODO
 * @author Florian Kuebler
 */
class LocalFieldAnalysis private ( final val project: SomeProject) extends FPCFAnalysis {

    class State(val field: Field) {
        private[this] var declaredMethodsDependees: Set[EOptionP[DeclaredMethod, Property]] = Set.empty
        private[this] var allocationSiteDependees: Set[EOptionP[Entity, EscapeProperty]] = Set.empty
        private[this] var clonedDependees: Set[EOptionP[Entity, EscapeProperty]] = Set.empty

        var temporary: FieldLocality = LocalField

        def dependees: Set[EOptionP[Entity, Property]] = {
            declaredMethodsDependees ++ allocationSiteDependees ++ clonedDependees
        }

        def hasNoDependees: Boolean = {
            declaredMethodsDependees.isEmpty &&
                allocationSiteDependees.isEmpty &&
                clonedDependees.isEmpty
        }

        def isNormalAllocationSite(e: AllocationSite): Boolean =
            allocationSiteDependees.exists(_.e == e)
        def isAllocationSiteOfClone(e: AllocationSite): Boolean =
            clonedDependees.exists(_.e == e)

        def addMethodDependee(ep: EOptionP[DeclaredMethod, Property]): Unit =
            declaredMethodsDependees += ep

        def removeMethodDependee(ep: EOptionP[DeclaredMethod, Property]): Unit =
            declaredMethodsDependees = declaredMethodsDependees.filter(other ⇒ (other.e ne ep.e) || other.pk != ep.pk)

        def updateMethodDependee(ep: EOptionP[DeclaredMethod, Property]): Unit = {
            removeMethodDependee(ep)
            addMethodDependee(ep)
        }

        def addClonedAllocationSiteDependee(ep: EOptionP[Entity, EscapeProperty]): Unit =
            clonedDependees += ep

        def removeClonedAllocationSiteDependee(ep: EOptionP[Entity, EscapeProperty]): Unit =
            clonedDependees = clonedDependees.filter(other ⇒ (other.e ne ep.e) || other.pk != ep.pk)

        def updateClonedAllocationSiteDependee(ep: EOptionP[Entity, EscapeProperty]): Unit = {
            removeClonedAllocationSiteDependee(ep)
            addClonedAllocationSiteDependee(ep)
        }

        def addAllocationSiteDependee(ep: EOptionP[Entity, EscapeProperty]): Unit =
            allocationSiteDependees += ep

        def removeAllocationSiteDependee(ep: EOptionP[Entity, EscapeProperty]): Unit =
            allocationSiteDependees = allocationSiteDependees.filter(other ⇒ (other.e ne ep.e) || other.pk != ep.pk)

        def updateAllocationSiteDependee(ep: EOptionP[Entity, EscapeProperty]): Unit = {
            removeAllocationSiteDependee(ep)
            addAllocationSiteDependee(ep)
        }

        def updateWithMeet(f: FieldLocality): Unit = temporary = temporary.meet(f)
    }

    type V = DUVar[(Domain with RecordDefUse)#DomainValue]
    private[this] val tacaiProvider: (Method) ⇒ TACode[TACMethodParameter, V] = project.get(DefaultTACAIKey)
    private[this] val declaredMethods: DeclaredMethods = propertyStore.context[DeclaredMethods]
    private[this] val classExtensibility = project.get(ClassExtensibilityKey)
    private[this] val typeExtensiblity = project.get(TypeExtensibilityKey)
    //private[this] val allocationSites: AllocationSites = propertyStore.context[AllocationSites]

    /**
     * Checks whether a call to a concrete method (no virtual call with imprecise type) has a fresh
     * return value.
     */
    private[this] def handleConcreteCall(callee: org.opalj.Result[Method], state: State): Option[PropertyComputationResult] = {
        // unkown method
        if (callee.isEmpty)
            return Some(Result(state.field, NoLocalField))

        propertyStore(declaredMethods(callee.value), ReturnValueFreshness.key) match {
            case EP(_, NoFreshReturnValue)   ⇒ return Some(Result(state.field, NoLocalField))
            case EP(_, FreshReturnValue)     ⇒
            case EP(_, PrimitiveReturnValue) ⇒
            case epkOrCond                   ⇒ state.addMethodDependee(epkOrCond)
        }
        None
    }

    /**
     * We consider a defSite (represented by the stmt) as fresh iff it was allocated in this method,
     * is a constant or is the result of call to a method with fresh return value.
     */
    private[this] def checkFreshnessOfDef(
        stmt: Stmt[V], method: Method, state: State
    ): Option[PropertyComputationResult] = {
        // the object stored in the field is fresh
        stmt match {
            case Assignment(_, _, New(_, _) | NewArray(_, _, _)) ⇒ // fresh by definition

            case Assignment(_, _, StaticFunctionCall(_, dc, isI, name, desc, _)) ⇒
                val callee = project.staticCall(dc, isI, name, desc)
                return handleConcreteCall(callee, state)

            case Assignment(_, _, NonVirtualFunctionCall(_, dc, isI, name, desc, _, _)) ⇒
                val callee = project.specialCall(dc, isI, name, desc)
                return handleConcreteCall(callee, state)

            case Assignment(_, _, VirtualFunctionCall(_, _, _, name, desc, receiver, _)) ⇒

                val value = receiver.asVar.value.asDomainReferenceValue

                if (value.isNull.isNotYes) {
                    val receiverType = project.classHierarchy.joinReferenceTypesUntilSingleUpperBound(
                        value.upperTypeBound
                    )

                    if (receiverType.isArrayType) {
                        val callee = project.instanceCall(ObjectType.Object, ObjectType.Object, name, desc)
                        return handleConcreteCall(callee, state)
                    } else if (value.isPrecise) {
                        val preciseType = value.valueType.get
                        val callee = project.instanceCall(method.classFile.thisType, preciseType, name, desc)
                        return handleConcreteCall(callee, state)
                    } else {
                        var callee = project.instanceCall(method.classFile.thisType, receiverType, name, desc)

                        // handle abstract method
                        if (callee.isEmpty) {
                            project.classFile(receiverType.asObjectType) match {
                                case Some(cf) ⇒
                                    callee = if (cf.isInterfaceDeclaration) {
                                        org.opalj.Result(
                                            project.resolveInterfaceMethodReference(receiverType.asObjectType, name, desc)
                                        )
                                    } else {
                                        project.resolveClassMethodReference(receiverType.asObjectType, name, desc)
                                    }
                                case None ⇒
                                    return Some(Result(state.field, NoLocalField))
                            }
                        }

                        if (callee.isEmpty)
                            return Some(Result(state.field, NoLocalField))

                        propertyStore(declaredMethods(callee.value), VirtualMethodReturnValueFreshness.key) match {
                            case EP(_, VNoFreshReturnValue) ⇒
                                return Some(Result(state.field, NoLocalField))
                            case EP(_, VFreshReturnValue)     ⇒
                            case EP(_, VPrimitiveReturnValue) ⇒
                            case epkOrCnd                     ⇒ state.addMethodDependee(epkOrCnd)
                        }
                    }
                }
            case Assignment(_, _, _: Const) ⇒

            case _ ⇒
                return Some(Result(state.field, NoLocalField))

        }
        None
    }

    /*private[this]*/ def cloneCheckFreshnessOfDef(
        stmt: Stmt[V], method: Method, state: State
    ): Option[PropertyComputationResult] = {
        val superType = state.field.classFile.superclassType.get
        stmt match {
            case Assignment(_, _, NonVirtualFunctionCall(_, `superType`, _, "clone", descr, _, _)) if descr.parametersCount == 0 ⇒ None
            case _ ⇒ checkFreshnessOfDef(stmt, method, state)
        }
    }

    private[this] def normalCheckEscape(
        escapeState: EOptionP[Entity, EscapeProperty], state: State
    ): Option[PropertyComputationResult] =
        escapeState match {
            case EP(_, NoEscape | EscapeInCallee) ⇒ None
            case EP(_, Conditional(NoEscape) | Conditional(EscapeInCallee)) ⇒
                state.addAllocationSiteDependee(escapeState)
                None
            case EP(_, _) ⇒ Some(Result(state.field, NoLocalField))
            case _ ⇒
                state.addAllocationSiteDependee(escapeState)
                None
        }

    /*private[this]*/ def cloneCheckEscape(
        escapeState: EOptionP[Entity, EscapeProperty], state: State
    ): Option[PropertyComputationResult] =
        escapeState match {
            //TODO is no escape ok?
            case EP(_, EscapeViaReturn) ⇒ None
            case EP(_, Conditional(EscapeViaReturn)) ⇒
                state.addClonedAllocationSiteDependee(escapeState)
                None
            case EP(_, _) ⇒ Some(Result(state.field, NoLocalField))
            case _ ⇒
                state.addClonedAllocationSiteDependee(escapeState)
                None
        }

    private[this] def checkFreshnessAndEscapeOfValue(
        value: Expr[V], useStmt: Stmt[V], stmts: Array[Stmt[V]], method: Method, state: State,
        checkFreshnessOfDef: (Stmt[V], Method, State) ⇒ Option[PropertyComputationResult],
        checkEscapeOfDef:    (EOptionP[Entity, EscapeProperty], State) ⇒ Option[PropertyComputationResult]
    ): Option[PropertyComputationResult] = {
        for (defSite1 ← value.asVar.definedBy) {
            //TODO what about exceptions
            if (defSite1 < 0)
                return Some(Result(state.field, NoLocalField))
            val stmt = stmts(defSite1)

            val uses1 = stmt match {
                //TODO this is unsound, as the constructor call might let the object escape
                case Assignment(_, tgt, _: New) ⇒ tgt.usedBy filter {
                    use ⇒ use != stmts.indexOf(useStmt) && use != tgt.usedBy.filter(_ > defSite1).toChain.min
                }
                case Assignment(_, tgt, _) ⇒ tgt.usedBy.filter(_ != stmts.indexOf(useStmt))
                case _                     ⇒ throw new Error("unexpected def-Site")
            }

            // is the def-site fresh?
            checkFreshnessOfDef(stmt, method, state).foreach(r ⇒ return Some(r))

            //TODO remove me
            val result = {
                new DefaultEntityEscapeAnalysis {
                    override val code: Array[Stmt[V]] = stmts
                    override val defSite: ValueOrigin = defSite1
                    override val uses: IntTrieSet = uses1
                    override val entity: Entity = null
                }.doDetermineEscape() match {
                    case Result(e, escapeState: EscapeProperty) ⇒
                        EP(e, escapeState)
                    case RefinableResult(e, escapeState: EscapeProperty) ⇒
                        EP(e, escapeState)
                    case _ ⇒ throw new Error()
                }
            }

            checkEscapeOfDef(result, state).foreach(r ⇒ return Some(r))
        }
        None
    }

    def determineLocality(field: Field): PropertyComputationResult = {
        val state = new State(field)
        val thisType = field.classFile.thisType
        val fieldName = field.name
        val fieldType = field.fieldType

        // base types can be considered to be local
        // TODO
        if (fieldType.isBaseType)
            return Result(field, LocalField)

        // this analysis can not track public fields
        if (field.isPublic)
            return Result(field, NoLocalField)

        var classes: Set[ClassFile] = Set.empty
        if (field.isPackagePrivate || field.isProtected) {

            val closedPackages = project.get(ClosedPackagesKey)
            if (!closedPackages.isClosed(thisType.packageName)) {
                return Result(field, NoLocalField)
            }

            classes ++= project.allClassFiles.filter(_.thisType.packageName == thisType.packageName)
            // todo check if all classes in package: package name ==
        }
        if (field.isProtected) {
            if (typeExtensiblity(thisType).isNotNo) {
                return Result(field, NoLocalField)
            }
            classes ++= project.classHierarchy.allSubclassTypes(thisType, reflexive = false).map(project.classFile(_).get)
        }

        val methodsOfThisType = field.classFile.methodsWithBody.map(_._1).toList

        val allMethodsHavingAccess = methodsOfThisType ++ classes.flatMap(_.methodsWithBody.map(_._1))

        // if there is no clone method, the class must not be extensible to be non local
        // extensible classes can still be extensible local if they dont implement cloneable
        if (!methodsOfThisType.exists(
            m ⇒ m.name == "clone" &&
                m.descriptor.parametersCount == 0 &&
                !m.isSynthetic
        )) {
            if (classExtensibility.isClassExtensible(thisType).isNotNo) {
                if (project.classHierarchy.isSubtypeOf(field.classFile.thisType, ObjectType.Cloneable).isYesOrUnknown) {
                    return Result(field, NoLocalField)
                } else {
                    state.updateWithMeet(ExtensibleLocalField)
                }
            }
            // else class is not extensible and does not override clone, which is okay
        }
        //TODO clonable?

        for {
            method ← allMethodsHavingAccess
            tacai = tacaiProvider(method)
            stmts = tacai.stmts
            (stmt, index) ← stmts.zipWithIndex
        } {
            stmt match {
                // all assignments to the field have to be fresh
                case PutField(_, `thisType`, `fieldName`, `fieldType`, objRef, value) ⇒
                    checkFreshnessAndEscapeOfValue(
                        value, stmt, stmts, method, state, checkFreshnessOfDef, normalCheckEscape
                    ).foreach(return _)

                // no read from field escapes
                case Assignment(_, tgt, GetField(_, `thisType`, `fieldName`, `fieldType`, _)) ⇒

                    // TODO later use real escape analysis
                    val result = new DefaultEntityEscapeAnalysis {
                        override val code: Array[Stmt[V]] = stmts
                        override val defSite: ValueOrigin = stmts.indexOf(stmt)
                        override val uses: IntTrieSet = tgt.usedBy
                        override val entity: Entity = null
                    }.doDetermineEscape()

                    result match {
                        case Result(_, NoEscape) ⇒
                        case _                   ⇒ return Result(field, NoLocalField)
                    }

                // always if there is a call to super.clone the field has to be override
                case Assignment(pc, left, NonVirtualFunctionCall(_, dc: ObjectType, false, "clone", descr, _, _)) ⇒
                    if (descr.parametersCount == 0 && field.classFile.superclassType.get == dc) {
                        //TODO check escape of left

                        var enqueuedBBs: Set[CFGNode] = Set(tacai.cfg.bb(index))
                        var worklist: List[CFGNode] = List(tacai.cfg.bb(index))

                        while (worklist.nonEmpty) {
                            val currentBB = worklist.head
                            worklist = worklist.tail

                            var foundPut = false
                            currentBB match {
                                case currentBB: BasicBlock ⇒
                                    for (index ← currentBB.startPC to currentBB.endPC) {
                                        stmts(index) match {
                                            case putStmt @ PutField(_, `thisType`, `fieldName`, `fieldType`, objRef, _) ⇒
                                                if (left.usedBy.contains(index)) {
                                                    //TODO move me upwards
                                                    val x = checkFreshnessAndEscapeOfValue(objRef, putStmt, stmts, method, state, cloneCheckFreshnessOfDef, cloneCheckEscape)
                                                    x.foreach(return _)
                                                    // we are done
                                                    foundPut = true
                                                }
                                            case _ ⇒
                                        }
                                    }
                                case exit: ExitNode ⇒
                                    if (exit.isNormalReturnExitNode) {
                                        return Result(field, NoLocalField)
                                    }
                                case _ ⇒
                            }

                            // exit nodes should have empty successors
                            if (!foundPut) {
                                val successors = currentBB.successors.filter(!enqueuedBBs.contains(_))
                                worklist ++= successors
                                enqueuedBBs ++= successors

                            }
                        }

                    }

                case _ ⇒
            }
        }

        returnResult(state)
    }

    private[this] def continuation(
        e: Entity, p: Property, ut: UpdateType, state: State
    ): PropertyComputationResult = {
        e match {
            case e: DeclaredMethod ⇒
                p match {
                    case NoFreshReturnValue | VNoFreshReturnValue ⇒
                        Result(state.field, NoLocalField)

                    case FreshReturnValue | VFreshReturnValue | PrimitiveReturnValue | VPrimitiveReturnValue ⇒
                        state.removeMethodDependee(EP(e, p))
                        returnResult(state)

                    case ConditionalFreshReturnValue | VConditionalFreshReturnValue ⇒
                        val newEP = EP(e, p)
                        state.updateMethodDependee(newEP)
                        IntermediateResult(
                            state.field,
                            state.temporary.asConditional,
                            state.dependees,
                            continuation(_, _, _, state)
                        )

                    case PropertyIsLazilyComputed ⇒
                        IntermediateResult(
                            state.field,
                            state.temporary.asConditional,
                            state.dependees,
                            continuation(_, _, _, state)
                        )
                }

            // order matters: normal allocation sites must not escape via return
            case e: AllocationSite if state.isNormalAllocationSite(e) ⇒
                p match {
                    case NoEscape | EscapeInCallee ⇒
                        state.removeAllocationSiteDependee(EP(e, p.asInstanceOf[EscapeProperty]))
                        returnResult(state)

                    case p @ Conditional(NoEscape | EscapeInCallee) ⇒
                        state.updateAllocationSiteDependee(EP(e, p))
                        IntermediateResult(
                            state.field,
                            state.temporary.asConditional,
                            state.dependees,
                            continuation(_, _, _, state)
                        )

                    case PropertyIsLazilyComputed ⇒
                        IntermediateResult(
                            state.field,
                            state.temporary.asConditional,
                            state.dependees,
                            continuation(_, _, _, state)
                        )

                    case _ ⇒ Result(state.field, NoLocalField)
                }

            case e: AllocationSite if state.isAllocationSiteOfClone(e) ⇒
                p match {
                    case EscapeViaReturn ⇒
                        state.removeClonedAllocationSiteDependee(EP(e, p.asInstanceOf[EscapeProperty]))
                        returnResult(state)

                    case p @ Conditional(EscapeViaReturn) ⇒
                        state.updateClonedAllocationSiteDependee(EP(e, p))
                        IntermediateResult(
                            state.field,
                            state.temporary.asConditional,
                            state.dependees,
                            continuation(_, _, _, state)
                        )

                    case PropertyIsLazilyComputed ⇒
                        IntermediateResult(
                            state.field,
                            state.temporary.asConditional,
                            state.dependees,
                            continuation(_, _, _, state)
                        )

                    case _ ⇒ Result(state.field, NoLocalField)
                }
        }
    }

    private[this] def returnResult(state: State) = {
        if (state.hasNoDependees)
            Result(state.field, state.temporary)
        else
            IntermediateResult(
                state.field,
                state.temporary.asConditional,
                state.dependees,
                continuation(_, _, _, state)
            )
    }
}

object LocalFieldAnalysis extends FPCFAnalysisScheduler {

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val allFields = project.allFields
        val analysis = new LocalFieldAnalysis(project)
        propertyStore.scheduleForEntities(allFields)(analysis.determineLocality)
        analysis
    }

    /**
     * Registers the analysis as a lazy computation, that is, the method
     * will call `ProperytStore.scheduleLazyComputation`.
     */
    def startLazily(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new LocalFieldAnalysis(project)
        propertyStore.scheduleLazyPropertyComputation(
            FieldLocality.key, analysis.determineLocality
        )
        analysis
    }

    override def derivedProperties: Set[PropertyKind] = Set(FieldLocality)

    override def usedProperties: Set[PropertyKind] =
        Set(ReturnValueFreshness, VirtualMethodReturnValueFreshness)

}
