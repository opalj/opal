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
import org.opalj.ai.common.SimpleAIKey
import org.opalj.ai.domain.RecordDefUse
import org.opalj.br.ClassFile
import org.opalj.br.DeclaredMethod
import org.opalj.ai.DefinitionSite
import org.opalj.ai.DefinitionSiteWithFilteredUses
import org.opalj.br.Field
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.ai.DefinitionSites
import org.opalj.ai.DefinitionSitesKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.ClosedPackagesKey
import org.opalj.br.analyses.cg.IsOverridableMethodKey
import org.opalj.br.analyses.cg.TypeExtensibilityKey
import org.opalj.br.cfg.BasicBlock
import org.opalj.br.cfg.CFGNode
import org.opalj.br.cfg.ExitNode
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.properties.EscapeInCallee
import org.opalj.fpcf.properties.EscapeProperty
import org.opalj.fpcf.properties.EscapeViaReturn
import org.opalj.fpcf.properties.ExtensibleGetter
import org.opalj.fpcf.properties.ExtensibleLocalField
import org.opalj.fpcf.properties.FieldLocality
import org.opalj.fpcf.properties.FreshReturnValue
import org.opalj.fpcf.properties.Getter
import org.opalj.fpcf.properties.LocalField
import org.opalj.fpcf.properties.LocalFieldWithGetter
import org.opalj.fpcf.properties.NoEscape
import org.opalj.fpcf.properties.NoFreshReturnValue
import org.opalj.fpcf.properties.NoLocalField
import org.opalj.fpcf.properties.PrimitiveReturnValue
import org.opalj.fpcf.properties.ReturnValueFreshness
import org.opalj.fpcf.properties.VExtensibleGetter
import org.opalj.fpcf.properties.VFreshReturnValue
import org.opalj.fpcf.properties.VGetter
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
class FieldLocalityAnalysis private[analyses] ( final val project: SomeProject) extends FPCFAnalysis {
    type V = DUVar[(Domain with RecordDefUse)#DomainValue]
    private[this] val tacaiProvider: (Method) ⇒ TACode[TACMethodParameter, V] = project.get(DefaultTACAIKey)
    private[this] val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    private[this] val typeExtensiblity = project.get(TypeExtensibilityKey)
    private[this] val definitionSites: DefinitionSites = project.get(DefinitionSitesKey)
    private[this] val aiResult = project.get(SimpleAIKey)
    private[this] val isOverridableMethod: Method ⇒ Answer = project.get(IsOverridableMethodKey)

    def determineLocality(field: Field): PropertyComputationResult = {
        implicit val state = new FieldLocalityState(field)
        val thisType = field.classFile.thisType
        val fieldName = field.name
        val fieldType = field.fieldType

        earlyExit(field).foreach(return _)

        val methodsOfThisType = field.classFile.methodsWithBody.map(_._1)

        // if there is no clone method, the class must not be extensible to be non local
        // extensible classes can still be extensible local if they dont implement cloneable
        if (!methodsOfThisType.exists(
            m ⇒ m.name == "clone" &&
                m.descriptor.parametersCount == 0 &&
                !m.isSynthetic
        )) {
            if (project.classHierarchy.isSubtypeOf(field.classFile.thisType, ObjectType.Cloneable).isYesOrUnknown)
                return Result(field, NoLocalField)

            if (typeExtensiblity(thisType).isNo) {
                val subtypes = classHierarchy.allSubtypes(thisType, reflexive = false)
                if (subtypes.exists { subtype ⇒
                    project.classHierarchy.isSubtypeOf(subtype, ObjectType.Cloneable).isYesOrUnknown

                }) {
                    state.updateWithMeet(ExtensibleLocalField)
                }
            } else {
                state.updateWithMeet(ExtensibleLocalField)
            }
        }

        for {
            method ← allMethodsHavingAccess(field)
            tacai = tacaiProvider(method)
            stmts = tacai.stmts
            (stmt, index) ← stmts.zipWithIndex
        } {
            stmt match {
                // all assignments to the field have to be fresh
                case put @ PutField(_, declaredFieldType, `fieldName`, `fieldType`, objRef, value) ⇒
                    project.resolveFieldReference(declaredFieldType, `fieldName`, `fieldType`) match {
                        case Some(`field`) ⇒
                            checkFreshnessAndEscapeOfValue(
                                value, put, stmts, method, isClone = false
                            ).foreach(return _)
                        case None ⇒ throw new RuntimeException("unexpected case")
                        case _    ⇒
                    }

                // no read from field escapes, if it is returned, we have a getter
                case stmt @ Assignment(_, _, GetField(_, declaredType, `fieldName`, `fieldType`, _)) ⇒
                    project.resolveFieldReference(declaredType, fieldName, fieldType) match {
                        case Some(`field`) ⇒
                            handleGetField(stmt, stmts, method).foreach(return _)
                        case None ⇒ throw new RuntimeException("unexpected case")
                        case _    ⇒
                    }

                // always if there is a call to super.clone the field has to be override
                case Assignment(_, left, NonVirtualFunctionCall(_, dc: ObjectType, false, "clone", descr, _, _)) ⇒
                    if (descr.parametersCount == 0 && field.classFile.superclassType.get == dc) {
                        handleSuperCloneCall(index, method, left.usedBy, tacai).foreach(return _)
                    }

                case _ ⇒
            }
        }

        returnResult
    }

    def handleGetField(
        definingStmt: Assignment[V], stmts: Array[Stmt[V]], method: Method
    )(implicit state: FieldLocalityState): Option[PropertyComputationResult] = {
        val escape = propertyStore(definitionSites(method, definingStmt.pc), EscapeProperty.key)
        handleEscapeOfGetField(escape)

    }

    def handleSuperCloneCall(
        index:  ValueOrigin,
        method: Method,
        uses:   IntTrieSet,
        tacai:  TACode[TACMethodParameter, V]
    )(implicit state: FieldLocalityState): Option[PropertyComputationResult] = {
        val field = state.field
        val thisType = field.classFile.thisType
        val fieldName = field.name
        val fieldType = field.fieldType

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
                        tacai.stmts(index) match {
                            case putStmt @ PutField(_, `thisType`, `fieldName`, `fieldType`, objRef, _) ⇒
                                if (uses.contains(index)) {
                                    checkFreshnessAndEscapeOfValue(
                                        objRef, putStmt, tacai.stmts, method, isClone = true
                                    ).foreach(x ⇒ return Some(x))
                                    // we are done
                                    foundPut = true
                                }
                            case _ ⇒
                        }
                    }
                case exit: ExitNode ⇒
                    if (exit.isNormalReturnExitNode) {
                        return Some(Result(state.field, NoLocalField))
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
        None
    }

    /**
     * Checks whether a call to a concrete method (no virtual call with imprecise type) has a fresh
     * return value.
     */
    private[this] def handleConcreteCall(
        callee: org.opalj.Result[Method]
    )(implicit state: FieldLocalityState): Option[PropertyComputationResult] = {
        // unkown method
        if (callee.isEmpty)
            return Some(Result(state.field, NoLocalField))

        val dm = declaredMethods(callee.value)
        handleReturnValueFreshness(propertyStore(dm, ReturnValueFreshness.key))
    }

    /**
     * We consider a defSite (represented by the stmt) as fresh iff it was allocated in this method,
     * is a constant or is the result of call to a method with fresh return value.
     */
    private[this] def checkFreshnessOfDef(
        stmt: Stmt[V], method: Method, isClone: Boolean
    )(implicit state: FieldLocalityState): Option[PropertyComputationResult] = {
        // the object stored in the field is fresh

        val superType = state.field.classFile.superclassType.get
        stmt match {
            case Assignment(_, _, New(_, _) | NewArray(_, _, _)) ⇒ // fresh by definition

            case Assignment(_, _, StaticFunctionCall(_, dc, isI, name, desc, _)) ⇒
                val callee = project.staticCall(dc, isI, name, desc)
                return handleConcreteCall(callee)

            case Assignment(_, _, NonVirtualFunctionCall(_, `superType`, _, "clone", descr, _, _)) if isClone && descr.parametersCount == 0 ⇒
                None

            case Assignment(_, _, NonVirtualFunctionCall(_, dc, isI, name, desc, _, _)) ⇒
                val callee = project.specialCall(dc, isI, name, desc)
                return handleConcreteCall(callee)

            case Assignment(_, _, VirtualFunctionCall(_, _, _, name, desc, receiver, _)) ⇒

                val value = receiver.asVar.value.asDomainReferenceValue

                if (value.isNull.isNotYes) {
                    val receiverType = project.classHierarchy.joinReferenceTypesUntilSingleUpperBound(
                        value.upperTypeBound
                    )

                    if (receiverType.isArrayType) {
                        val callee = project.instanceCall(ObjectType.Object, ObjectType.Object, name, desc)
                        return handleConcreteCall(callee);
                    } else if (value.isPrecise) {
                        val preciseType = value.valueType.get
                        val callee = project.instanceCall(method.classFile.thisType, preciseType, name, desc)
                        return handleConcreteCall(callee);
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
                                    return Some(Result(state.field, NoLocalField));
                            }
                        }

                        if (callee.isEmpty || isOverridableMethod(callee.value).isYesOrUnknown)
                            return Some(Result(state.field, NoLocalField))

                        val dm = declaredMethods(callee.value)
                        val rvf = propertyStore(dm, VirtualMethodReturnValueFreshness.key)
                        val rvff = handleReturnValueFreshness(rvf)
                        if (rvff.isDefined) return rvff;
                    }
                }
            case Assignment(_, _, _: Const) ⇒

            case _ ⇒
                return Some(Result(state.field, NoLocalField))

        }
        None
    }

    private[this] def handleReturnValueFreshness(
        eOptionP: EOptionP[DeclaredMethod, Property]
    )(implicit state: FieldLocalityState): Option[PropertyComputationResult] = eOptionP match {
        case FinalEP(_, NoFreshReturnValue | VNoFreshReturnValue) ⇒
            Some(Result(state.field, NoLocalField))

        case FinalEP(_, Getter | VGetter) ⇒
            Some(Result(state.field, NoLocalField))

        case FinalEP(_, ExtensibleGetter | VExtensibleGetter) ⇒
            Some(Result(state.field, NoLocalField))

        case IntermediateEP(_, _, Getter | VGetter) ⇒
            Some(Result(state.field, NoLocalField))

        case IntermediateEP(_, _, ExtensibleGetter | VExtensibleGetter) ⇒
            Some(Result(state.field, NoLocalField))

        case FinalEP(_, FreshReturnValue | VFreshReturnValue)         ⇒ None

        case FinalEP(_, PrimitiveReturnValue | VPrimitiveReturnValue) ⇒ None

        case epkOrCnd ⇒
            state.addMethodDependee(epkOrCnd)
            None
    }

    private[this] def handleEscapeState(
        eOptionP: EOptionP[DefinitionSiteWithFilteredUses, EscapeProperty], isClone: Boolean
    )(implicit state: FieldLocalityState): Option[PropertyComputationResult] = eOptionP match {
        case FinalEP(_, NoEscape | EscapeInCallee) ⇒ None

        case IntermediateEP(_, _, NoEscape | EscapeInCallee) ⇒
            if (isClone)
                state.addClonedDefinitionSiteDependee(eOptionP)
            else
                state.addDefinitionSiteDependee(eOptionP)
            None

        case FinalEP(_, EscapeViaReturn) if isClone ⇒ None

        case IntermediateEP(_, _, EscapeViaReturn) if isClone ⇒
            state.addClonedDefinitionSiteDependee(eOptionP)
            None

        case EPS(_, _, _) ⇒ Some(Result(state.field, NoLocalField))

        case _ if isClone ⇒
            state.addClonedDefinitionSiteDependee(eOptionP)
            None
        case _ ⇒
            state.addDefinitionSiteDependee(eOptionP)
            None
    }

    private[this] def handleEscapeOfGetField(
        eOptionP: EOptionP[DefinitionSite, EscapeProperty]
    )(implicit state: FieldLocalityState): Option[PropertyComputationResult] = eOptionP match {
        case FinalEP(_, NoEscape | EscapeInCallee) ⇒ None
        case FinalEP(e, EscapeViaReturn) ⇒
            if (isGetFieldOfReceiver(e)) {
                state.updateWithMeet(LocalFieldWithGetter)
                None
            } else
                Some(Result(state.field, NoLocalField))

        case IntermediateEP(_, _, NoEscape | EscapeInCallee) ⇒
            state.addDefinitionSiteDependee(eOptionP)
            None

        case IntermediateEP(e, _, EscapeViaReturn) ⇒
            if (isGetFieldOfReceiver(e)) {
                state.updateWithMeet(LocalFieldWithGetter)
                state.addDefinitionSiteDependee(eOptionP)
                None
            } else
                Some(Result(state.field, NoLocalField))
            None

        case EPS(_, _, _) ⇒
            Some(Result(state.field, NoLocalField))

        case _ ⇒
            state.addDefinitionSiteDependee(eOptionP)
            None

    }

    private[this] def isGetFieldOfReceiver(defSite: DefinitionSite): Boolean = {
        val stmt = tacaiProvider(defSite.method).stmts.find(_.pc == defSite.pc)
        val objRef = stmt match {

            case Some(Assignment(_, _, getField: GetField[V])) ⇒ getField.objRef
            case Some(_) | None ⇒
                throw new RuntimeException("unexpected case")
        }
        objRef.asVar.definedBy == tac.SelfReferenceParameter
    }

    private[this] def checkFreshnessAndEscapeOfValue(
        value: Expr[V], putField: PutField[V], stmts: Array[Stmt[V]], method: Method, isClone: Boolean
    )(implicit state: FieldLocalityState): Option[PropertyComputationResult] = {
        for (defSite1 ← value.asVar.definedBy) {
            //TODO what about exceptions
            if (defSite1 < 0)
                return Some(Result(state.field, NoLocalField));
            val stmt = stmts(defSite1)

            // is the def-site fresh?
            checkFreshnessOfDef(stmt, method, isClone).foreach(r ⇒ return Some(r))

            val filteredUses = aiResult(method).domain.usedBy(stmt.pc).filter(_ != putField.pc)
            val defSiteEntity = definitionSites(method, stmt.pc, filteredUses)
            val escape = propertyStore(defSiteEntity, EscapeProperty.key)
            handleEscapeState(escape, isClone).foreach(r ⇒ return Some(r))
        }
        None
    }

    def earlyExit(field: Field): Option[PropertyComputationResult] = {
        val fieldType = field.fieldType
        val thisType = field.classFile.thisType
        // base types can be considered to be local
        if (fieldType.isBaseType)
            return Some(Result(field, LocalField))

        if (field.isStatic)
            return Some(Result(field, NoLocalField))

        // this analysis can not track public fields
        if (field.isPublic)
            return Some(Result(field, NoLocalField))

        if (field.isProtected && typeExtensiblity(thisType).isYesOrUnknown) {
            return Some(Result(field, NoLocalField))
        }

        if (field.isPackagePrivate || field.isProtected) {
            val closedPackages = project.get(ClosedPackagesKey)
            if (!closedPackages.isClosed(thisType.packageName)) {
                return Some(Result(field, NoLocalField))
            }
        }
        None
    }

    def allMethodsHavingAccess(field: Field): Set[Method] = {
        val thisType = field.classFile.thisType
        var classes: Set[ClassFile] = Set(field.classFile)
        if (field.isPackagePrivate || field.isProtected) {
            classes ++= project.allClassFiles.filter(_.thisType.packageName == thisType.packageName)
        }
        if (field.isProtected) {
            classes ++= project.classHierarchy.allSubclassTypes(thisType, reflexive = false).map(project.classFile(_).get)
        }
        classes.flatMap(_.methodsWithBody.map(_._1))
    }

    private[this] def continuation(
        someEPS: SomeEPS
    )(implicit state: FieldLocalityState): PropertyComputationResult = {

        someEPS.e match {
            case _: DeclaredMethod ⇒
                val newEP = someEPS.asInstanceOf[EOptionP[DeclaredMethod, Property]]
                state.removeMethodDependee(newEP)
                handleReturnValueFreshness(newEP).foreach(return _)

            case e: DefinitionSiteWithFilteredUses ⇒
                val newEP = someEPS.asInstanceOf[EOptionP[DefinitionSiteWithFilteredUses, EscapeProperty]]
                state.removeDefinitionSiteDependee(newEP)
                handleEscapeState(newEP, state.isDefinitionSiteOfClone(e)).foreach(return _)

            // order matters: normal allocation sites must not escape via return
            case e: DefinitionSite ⇒
                val newEP = someEPS.asInstanceOf[EOptionP[DefinitionSite, EscapeProperty]]
                state.removeDefinitionSiteDependee(newEP)
                handleEscapeOfGetField(newEP).foreach(return _)
        }
        returnResult
    }

    private[this] def returnResult(implicit state: FieldLocalityState): PropertyComputationResult = {
        if (state.hasNoDependees)
            Result(state.field, state.temporaryState)
        else
            IntermediateResult(
                state.field,
                NoLocalField,
                state.temporaryState,
                state.dependees,
                continuation
            )
    }
}

trait FieldLocalityAnalysisScheduler extends ComputationSpecification {
    override def derives: Set[PropertyKind] = Set(FieldLocality)

    override def uses: Set[PropertyKind] =
        Set(ReturnValueFreshness, VirtualMethodReturnValueFreshness)
}

object EagerFieldLocalityAnalysis extends FieldLocalityAnalysisScheduler with FPCFEagerAnalysisScheduler {

    def start(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val allFields = project.allFields
        val analysis = new FieldLocalityAnalysis(project)
        propertyStore.scheduleForEntities(allFields)(analysis.determineLocality)
        analysis
    }
}

object LazyFieldLocalityAnalysis extends FieldLocalityAnalysisScheduler with FPCFLazyAnalysisScheduler {

    /**
     * Registers the analysis as a lazy computation, that is, the method
     * will call `ProperytStore.scheduleLazyComputation`.
     */
    def startLazily(project: SomeProject, propertyStore: PropertyStore): FPCFAnalysis = {
        val analysis = new FieldLocalityAnalysis(project)
        propertyStore.registerLazyPropertyComputation(
            FieldLocality.key, analysis.determineLocality
        )
        analysis
    }
}
