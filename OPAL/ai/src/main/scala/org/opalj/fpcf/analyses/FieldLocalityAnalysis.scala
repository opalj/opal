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

    /**
     * Checks if the field locality can be determined trivially.
     * Otherwise it forwards to [[step2]].
     */
    def step1(field: Field): PropertyComputationResult = {
        val fieldType = field.fieldType
        val thisType = field.classFile.thisType
        // base types can be considered to be local
        if (fieldType.isBaseType)
            return Result(field, LocalField);

        if (field.isStatic)
            return Result(field, NoLocalField);

        // this analysis can not track public fields
        if (field.isPublic)
            return Result(field, NoLocalField);

        if (field.isProtected && typeExtensiblity(thisType).isYesOrUnknown) {
            return Result(field, NoLocalField);
        }

        if (field.isPackagePrivate || field.isProtected) {
            val closedPackages = project.get(ClosedPackagesKey)
            if (!closedPackages.isClosed(thisType.packageName)) {
                return Result(field, NoLocalField);
            }
        }

        step2(field)
    }

    /**
     * Checks if the class declaring the field overrides clone.
     * If this is not the case and the class implements [[java.lang.Cloneable]],
     * the field is not local.
     * In case it is not cloneable, there might be still a subtype doing so.
     * If there exists such a subtype or if the type can be extended, the state is
     * lifted to [[ExtensibleLocalField]].
     *
     * Afterwards it calls [[step3]].
     */
    private[this] def step2(field: Field): PropertyComputationResult = {
        implicit val state: FieldLocalityState = new FieldLocalityState(field)
        val thisType = field.classFile.thisType

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

            val subtypes = classHierarchy.allSubtypes(thisType, reflexive = false)
            val existsCloneableSubtype = subtypes.exists { subtype ⇒
                project.classHierarchy.isSubtypeOf(subtype, ObjectType.Cloneable).isYesOrUnknown
            }

            if (typeExtensiblity(thisType).isYesOrUnknown || existsCloneableSubtype) {
                state.updateWithMeet(ExtensibleLocalField)
            }
        }

        step3(field)
    }

    private[this] def step3(field: Field)(implicit state: FieldLocalityState): PropertyComputationResult = {
        val fieldName = field.name
        val fieldType = field.fieldType
        for {
            method ← allMethodsHavingAccess(field)
            tacai = tacaiProvider(method)
            stmts = tacai.stmts
            (stmt, index) ← stmts.zipWithIndex
        } {
            stmt match {
                // no read from field escapes, if it is returned, we have a getter
                case _@ Assignment(pc, _, GetField(_, declaredType, `fieldName`, `fieldType`, _)) ⇒
                    project.resolveFieldReference(declaredType, fieldName, fieldType) match {
                        case Some(`field`) ⇒
                            val escape = propertyStore(definitionSites(method, pc), EscapeProperty.key)
                            if (handleEscape(escape)) {
                                return Result(field, NoLocalField);
                            }
                        case None ⇒ throw new RuntimeException("unexpected case")
                        case _    ⇒
                    }

                // all assignments to the field have to be fresh and are not escaped.
                case PutField(pc, declaredFieldType, `fieldName`, `fieldType`, _, value) ⇒
                    project.resolveFieldReference(declaredFieldType, `fieldName`, `fieldType`) match {
                        case Some(`field`) ⇒
                            // value is fresh and does not escape.
                            // in case of escape via return, we have a local field with getter
                            if (checkFreshnessAndEscapeOfValue(value, pc, stmts, method)) {
                                return Result(field, NoLocalField);
                            }
                        case None ⇒ throw new RuntimeException("unexpected case")
                        case _    ⇒
                    }

                // always if there is a call to super.clone the field has to be override
                case Assignment(pc, left, NonVirtualFunctionCall(_, dc: ObjectType, false, "clone", descr, _, _)) ⇒
                    if (descr.parametersCount == 0 && field.classFile.superclassType.get == dc) {
                        if (handleSuperCloneCall(index, pc, method, left.usedBy, tacai)) {
                            return Result(field, NoLocalField);
                        }
                    }

                case _ ⇒
            }
        }

        returnResult
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

    private[this] def handleEscape(
        eOptionP: EOptionP[DefinitionSite, EscapeProperty]
    )(implicit state: FieldLocalityState): Boolean = eOptionP match {
        case FinalEP(_, NoEscape | EscapeInCallee) ⇒ false
        case FinalEP(e, EscapeViaReturn) ⇒
            if (isGetFieldOfReceiver(e)) {
                state.updateWithMeet(LocalFieldWithGetter)
                false
            } else
                true

        case IntermediateEP(_, _, NoEscape | EscapeInCallee) ⇒
            state.addDefinitionSiteDependee(eOptionP)
            false

        case IntermediateEP(e, _, EscapeViaReturn) ⇒
            if (isGetFieldOfReceiver(e)) {
                state.updateWithMeet(LocalFieldWithGetter)
                state.addDefinitionSiteDependee(eOptionP)
                false
            } else
                true

        case EPS(_, _, _) ⇒
            true

        case _ ⇒
            state.addDefinitionSiteDependee(eOptionP)
            false
    }

    private[this] def isGetFieldOfReceiver(defSite: DefinitionSite): Boolean = {
        val stmt = tacaiProvider(defSite.method).stmts.find(_.pc == defSite.pc)
        stmt match {
            case Some(Assignment(_, _, getField: GetField[V])) ⇒
                getField.objRef.asVar.definedBy == tac.SelfReferenceParameter
            case Some(_) | None ⇒
                // this method is also called for put fields
                false
        }
    }

    private[this] def checkFreshnessAndEscapeOfValue(
        value: Expr[V], putField: Int, stmts: Array[Stmt[V]], method: Method
    )(implicit state: FieldLocalityState): Boolean = {
        value.asVar.definedBy.exists { defSite ⇒
            if (defSite < 0)
                true
            else {
                val stmt = stmts(defSite)

                // is the def site not fresh?
                if (checkFreshnessOfDef(stmt, method)) {
                    true
                } else {
                    val filteredUses = aiResult(method).domain.usedBy(stmt.pc) - putField
                    val defSiteEntity = definitionSites(method, stmt.pc, filteredUses)
                    val escape = propertyStore(defSiteEntity, EscapeProperty.key)
                    // does the value escape?
                    handleEscape(escape)
                }
            }
        }
    }

    /**
     * We consider a defSite (represented by the stmt) as fresh iff it was allocated in this method,
     * is a constant or is the result of call to a method with fresh return value.
     */
    private[this] def checkFreshnessOfDef(
        stmt: Stmt[V], method: Method
    )(implicit state: FieldLocalityState): Boolean = {
        // the object stored in the field is fresh
        stmt match {
            case Assignment(_, _, New(_, _) | NewArray(_, _, _)) ⇒
                false // fresh by definition

            case Assignment(_, _, StaticFunctionCall(_, dc, isI, name, desc, _)) ⇒
                val callee = project.staticCall(dc, isI, name, desc)
                handleConcreteCall(callee)

            case Assignment(_, _, NonVirtualFunctionCall(_, dc, isI, name, desc, _, _)) ⇒
                val callee = project.specialCall(dc, isI, name, desc)
                handleConcreteCall(callee)

            case Assignment(_, _, VirtualFunctionCall(_, _, _, name, desc, receiver, _)) ⇒
                val value = receiver.asVar.value.asDomainReferenceValue

                if (value.isNull.isNotYes) {
                    val receiverType = project.classHierarchy.joinReferenceTypesUntilSingleUpperBound(
                        value.upperTypeBound
                    )

                    if (receiverType.isArrayType) {
                        val callee = project.instanceCall(ObjectType.Object, ObjectType.Object, name, desc)
                        handleConcreteCall(callee)

                    } else if (value.isPrecise) {
                        val preciseType = value.valueType.get
                        val callee = project.instanceCall(method.classFile.thisType, preciseType, name, desc)
                        handleConcreteCall(callee)

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
                                    return true;
                            }
                        }

                        if (callee.isEmpty || isOverridableMethod(callee.value).isYesOrUnknown)
                            true
                        else {
                            val dm = declaredMethods(callee.value)
                            val rvf = propertyStore(dm, VirtualMethodReturnValueFreshness.key)
                            handleReturnValueFreshness(rvf)
                        }
                    }
                } else {
                    false
                }
            case Assignment(_, _, _: Const) ⇒
                false

            case _ ⇒
                true

        }
    }

    /**
     * Checks whether a call to a concrete method (no virtual call with imprecise type) has a fresh
     * return value.
     */
    private[this] def handleConcreteCall(
        callee: org.opalj.Result[Method]
    )(implicit state: FieldLocalityState): Boolean = {
        // unkown method
        if (callee.isEmpty)
            false
        else {
            val dm = declaredMethods(callee.value)
            handleReturnValueFreshness(propertyStore(dm, ReturnValueFreshness.key))
        }
    }

    private[this] def handleReturnValueFreshness(
        eOptionP: EOptionP[DeclaredMethod, Property]
    )(implicit state: FieldLocalityState): Boolean = eOptionP match {
        case FinalEP(_, NoFreshReturnValue | VNoFreshReturnValue) ⇒
            true

        case FinalEP(_, Getter | VGetter) ⇒
            true

        case FinalEP(_, ExtensibleGetter | VExtensibleGetter) ⇒
            true

        case IntermediateEP(_, _, Getter | VGetter) ⇒
            true

        case IntermediateEP(_, _, ExtensibleGetter | VExtensibleGetter) ⇒
            true

        case FinalEP(_, FreshReturnValue | VFreshReturnValue)         ⇒ false

        case FinalEP(_, PrimitiveReturnValue | VPrimitiveReturnValue) ⇒ false

        case epkOrCnd ⇒
            state.addMethodDependee(epkOrCnd)
            false
    }

    private[this] def handleSuperCloneCall(
        defSite: ValueOrigin,
        pc:      Int,
        method:  Method,
        uses:    IntTrieSet,
        tacai:   TACode[TACMethodParameter, V]
    )(implicit state: FieldLocalityState): Boolean = {
        val field = state.field
        val thisType = field.classFile.thisType
        val fieldName = field.name
        val fieldType = field.fieldType

        val escape = propertyStore(definitionSites(method, pc), EscapeProperty.key)
        handleEscapeStateOfResultOfSuperClone(escape)

        var enqueuedBBs: Set[CFGNode] = Set(tacai.cfg.bb(defSite))
        var worklist: List[CFGNode] = List(tacai.cfg.bb(defSite))

        while (worklist.nonEmpty) {
            val currentBB = worklist.head
            worklist = worklist.tail

            var foundPut = false
            currentBB match {
                case currentBB: BasicBlock ⇒
                    for (index ← currentBB.startPC to currentBB.endPC) {
                        tacai.stmts(index) match {
                            case _@ PutField(_, `thisType`, `fieldName`, `fieldType`, _, _) ⇒
                                if (uses.contains(index)) {
                                    // we are done
                                    foundPut = true
                                }
                            case _ ⇒
                        }
                    }
                case exit: ExitNode ⇒
                    if (exit.isNormalReturnExitNode) {
                        return true;
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
        false
    }

    private[this] def handleEscapeStateOfResultOfSuperClone(
        eOptionP: EOptionP[DefinitionSite, EscapeProperty]
    )(implicit state: FieldLocalityState): Boolean = eOptionP match {
        case FinalEP(_, NoEscape | EscapeInCallee) ⇒ false

        case IntermediateEP(_, _, NoEscape | EscapeInCallee) ⇒
            state.addClonedDefinitionSiteDependee(eOptionP)
            false

        case FinalEP(_, EscapeViaReturn) ⇒ false

        case IntermediateEP(_, _, EscapeViaReturn) ⇒
            state.addClonedDefinitionSiteDependee(eOptionP)
            false

        case EPS(_, _, _) ⇒ true

        case _ ⇒
            state.addClonedDefinitionSiteDependee(eOptionP)
            false
    }

    private[this] def continuation(
        someEPS: SomeEPS
    )(implicit state: FieldLocalityState): PropertyComputationResult = {

        val isNotLocal = someEPS.e match {
            case _: DeclaredMethod ⇒
                val newEP = someEPS.asInstanceOf[EOptionP[DeclaredMethod, Property]]
                state.removeMethodDependee(newEP)
                handleReturnValueFreshness(newEP)

            case e: DefinitionSiteWithFilteredUses ⇒
                val newEP = someEPS.asInstanceOf[EOptionP[DefinitionSiteWithFilteredUses, EscapeProperty]]
                state.removeDefinitionSiteDependee(newEP)
                handleEscape(newEP)

            // order matters: normal allocation sites must not escape via return
            case e: DefinitionSite ⇒
                val newEP = someEPS.asInstanceOf[EOptionP[DefinitionSite, EscapeProperty]]
                state.removeDefinitionSiteDependee(newEP)
                if (state.isDefinitionSiteOfClone(e))
                    handleEscapeStateOfResultOfSuperClone(newEP)
                else
                    handleEscape(newEP)
        }
        if (isNotLocal)
            Result(state.field, NoLocalField)
        else
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
        propertyStore.scheduleForEntities(allFields)(analysis.step1)
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
            FieldLocality.key, analysis.step1
        )
        analysis
    }
}
