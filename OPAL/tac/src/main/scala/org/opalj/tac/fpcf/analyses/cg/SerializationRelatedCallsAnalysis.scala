/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import scala.annotation.tailrec

import org.opalj.collection.immutable.IntTrieSet
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPS
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimResult
import org.opalj.fpcf.NoResult
import org.opalj.fpcf.PartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Result
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEPS
import org.opalj.fpcf.UBP
import org.opalj.br.fpcf.cg.properties.NoSerializationRelatedCallees
import org.opalj.br.fpcf.cg.properties.SerializationRelatedCallees
import org.opalj.br.fpcf.cg.properties.SerializationRelatedCalleesImplementation
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.ElementReferenceType
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.MethodDescriptor.JustReturnsObject
import org.opalj.br.MethodDescriptor.JustTakesObject
import org.opalj.br.MethodDescriptor.NoArgsAndReturnVoid
import org.opalj.br.ObjectType
import org.opalj.br.ObjectType.{ObjectOutputStream ⇒ ObjectOutputStreamType}
import org.opalj.br.ObjectType.{ObjectInputStream ⇒ ObjectInputStreamType}
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InitialInstantiatedTypesKey
import org.opalj.br.fpcf.cg.properties.InstantiatedTypes
import org.opalj.br.fpcf.cg.properties.NoCallers
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.cg.properties.CallersProperty
import org.opalj.br.fpcf.BasicFPCFTriggeredAnalysisScheduler
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.tac.fpcf.analyses.cg.SerializationRelatedCallsAnalysis.UnknownParam
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.TheTACAI

/**
 * Handles the effect of serialization to the call graph.
 * As an example models the invocation of constructors when `readObject` is called, if there is a
 * cast afterwards.
 *
 * @author Florian Kuebler
 * @author Dominik Helm
 */
class SerializationRelatedCallsAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    final val ObjectInputValidationType = ObjectType("java/io/ObjectInputValidation")
    final val ObjectOutputType = ObjectType("java/io/ObjectOutput")
    final val ObjectInputType = ObjectType("java/io/ObjectInput")

    final val WriteObjectDescriptor = MethodDescriptor.JustTakes(ObjectOutputStreamType)
    final val ReadObjectDescriptor = MethodDescriptor.JustTakes(ObjectInputStreamType)
    final val WriteExternalDescriptor = MethodDescriptor.JustTakes(ObjectOutputType)
    final val ReadExternalDescriptor = MethodDescriptor.JustTakes(ObjectInputType)

    implicit private[this] val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    private[this] val initialInstantiatedTypes: UIDSet[ObjectType] =
        UIDSet(project.get(InitialInstantiatedTypesKey).toSeq: _*)

    def analyze(declaredMethod: DeclaredMethod): PropertyComputationResult = {
        // todo this is copy & past code from the RTACallGraphAnalysis -> refactor
        (propertyStore(declaredMethod, CallersProperty.key): @unchecked) match {
            case FinalP(NoCallers) ⇒
                // nothing to do, since there is no caller
                return NoResult;

            case eps: EPS[_, _] ⇒
                if (eps.ub eq NoCallers) {
                    // we can not create a dependency here, so the analysis is not allowed to create
                    // such a result
                    throw new IllegalStateException("illegal immediate result for callers")
                }
            // the method is reachable, so we analyze it!
        }

        // we only allow defined methods
        if (!declaredMethod.hasSingleDefinedMethod)
            return NoResult;

        val definedMethod = declaredMethod.asDefinedMethod

        val method = declaredMethod.definedMethod

        // we only allow defined methods with declared type equal to the class of the method
        if (method.classFile.thisType != declaredMethod.declaringClassType)
            return NoResult;

        if (method.body.isEmpty)
            // happens in particular for native methods
            return NoResult;

        var relevantPCs = IntTrieSet.empty
        method.body.get.iterate(INVOKEVIRTUAL) { (pc, instruction) ⇒
            val call = instruction.asMethodInvocationInstruction
            if (call.declaringClass == ObjectOutputStreamType && call.name == "writeObject" ||
                call.declaringClass == ObjectInputStreamType && call.name == "readObject")
                relevantPCs += pc
        }

        if (relevantPCs.isEmpty)
            return Result(declaredMethod, NoSerializationRelatedCallees);

        val tacEP = propertyStore(method, TACAI.key)

        if (tacEP.hasUBP && tacEP.ub.tac.isDefined) {
            processMethod(definedMethod, relevantPCs, tacEP.asEPS)
        } else {
            InterimResult.forUB(
                declaredMethod,
                NoSerializationRelatedCallees,
                List(tacEP),
                c(definedMethod, relevantPCs)
            )
        }
    }

    private[this] def processMethod(
        definedMethod: DefinedMethod,
        relevantPCs:   IntTrieSet,
        tacEP:         EPS[Method, TACAI]
    ): ProperPropertyComputationResult = {

        val tacode = tacEP.ub.tac.get

        // Let's get the types that are definitely initialized at this point in time;
        // the upper bound for type instantiations, seen so far in case they are not yet
        // computed, we use the initialTypes.
        val instantiatedTypes: UIDSet[ObjectType] = ps(project, InstantiatedTypes.key) match {
            case UBP(instantiatedTypes) ⇒ instantiatedTypes.types
            case _                      ⇒ initialInstantiatedTypes
        }

        val calleesAndCallers = new IndirectCalleesAndCallers()

        implicit val stmts: Array[Stmt[V]] = tacode.stmts
        val pcToIndex = tacode.pcToIndex

        var newInstantiatedTypes = UIDSet.empty[ObjectType]
        for (pc ← relevantPCs) {
            val index = pcToIndex(pc)
            if (index != -1) {

                stmts(index) match {
                    case VirtualMethodCall(_, dc, _, "writeObject", md, receiver: V, params) if isOOSWriteObject(dc, md) ⇒
                        handleOOSWriteObject(
                            definedMethod,
                            receiver,
                            params.head.asVar,
                            pc,
                            calleesAndCallers
                        )

                    case Assignment(_, targetVar: V, VirtualFunctionCall(_, dc, _, "readObject", md, receiver: V, _)) if isOISReadObject(dc, md) ⇒
                        newInstantiatedTypes = handleOISReadObject(
                            definedMethod,
                            targetVar,
                            receiver,
                            pc,
                            instantiatedTypes,
                            newInstantiatedTypes,
                            calleesAndCallers
                        )

                    case ExprStmt(_, VirtualFunctionCall(_, dc, _, "readObject", md, _, _)) if isOISReadObject(dc, md) ⇒
                        calleesAndCallers.addIncompleteCallsite(pc)

                    case _ ⇒ /* irrelevant */

                }
            }
        }

        returnResult(definedMethod, relevantPCs, calleesAndCallers, newInstantiatedTypes, tacEP)
    }

    @inline private[this] def isOOSWriteObject(
        declaredType: ReferenceType, methodDescriptor: MethodDescriptor
    ): Boolean = {
        ch.isSubtypeOf(declaredType, ObjectOutputStreamType) && methodDescriptor == JustTakesObject
    }

    @inline private[this] def isOISReadObject(
        declaredType: ReferenceType, methodDescriptor: MethodDescriptor
    ): Boolean = {
        ch.isSubtypeOf(declaredType, ObjectInputStreamType) && methodDescriptor == JustReturnsObject
    }

    @inline private[this] def handleOOSWriteObject(
        definedMethod:     DefinedMethod,
        outputStream:      V,
        param:             V,
        pc:                Int,
        calleesAndCallers: IndirectCalleesAndCallers
    )(
        implicit
        stmts: Array[Stmt[V]]
    ): Unit = {
        val parameterList = Seq(persistentUVar(param), persistentUVar(outputStream))

        for (rv ← param.value.asReferenceValue.allValues) {
            if (rv.isPrecise && rv.isNull.isNo) {
                val ElementReferenceType(paramType) = rv.leastUpperType.get

                if (classHierarchy.isSubtypeOf(paramType, ObjectType.Serializable)) {
                    if (classHierarchy.isSubtypeOf(paramType, ObjectType.Externalizable)) {
                        val writeExternalMethod = project.instanceCall(
                            paramType,
                            paramType,
                            "writeExternal",
                            WriteExternalDescriptor
                        )

                        calleesAndCallers.updateWithIndirectCallOrFallback(
                            definedMethod, writeExternalMethod, pc,
                            ObjectType.Externalizable.packageName,
                            ObjectType.Externalizable,
                            "writeExternal",
                            WriteExternalDescriptor,
                            parameterList
                        )
                    } else {
                        val writeObjectMethod = project.specialCall(
                            paramType,
                            paramType,
                            isInterface = false,
                            "writeObject",
                            WriteObjectDescriptor
                        )
                        calleesAndCallers.updateWithIndirectCallOrFallback(
                            definedMethod,
                            writeObjectMethod,
                            pc,
                            ObjectType.Object.packageName,
                            ObjectType.Object,
                            "writeObject",
                            WriteObjectDescriptor,
                            parameterList
                        )
                    }

                    val writeReplaceMethod = project.specialCall(
                        paramType,
                        paramType,
                        isInterface = false,
                        "writeReplace",
                        WriteObjectDescriptor
                    )

                    calleesAndCallers.updateWithIndirectCallOrFallback(
                        definedMethod, writeReplaceMethod, pc,
                        ObjectType.Object.packageName,
                        ObjectType.Object,
                        "writeReplace",
                        WriteObjectDescriptor,
                        parameterList
                    )
                }

            } else {
                calleesAndCallers.addIncompleteCallsite(pc)
            }
        }
    }

    private[this] def handleOISReadObject(
        definedMethod:        DefinedMethod,
        targetVar:            V,
        inputStream:          V,
        pc:                   Int,
        instantiatedTypesUB:  UIDSet[ObjectType],
        newInstantiatedTypes: UIDSet[ObjectType],
        calleesAndCallers:    IndirectCalleesAndCallers
    )(
        implicit
        stmts: Array[Stmt[V]]
    ): UIDSet[ObjectType] = {
        var resNewInstantiatedTypes = UIDSet.empty[ObjectType]
        var foundCast = false
        val parameterList = Seq(None, persistentUVar(inputStream))
        for { Checkcast(_, _, ElementReferenceType(castType)) ← stmts } {
            foundCast = true

            // for each subtype of the type declared at cast we add calls to the relevant methods
            for {
                t ← ch.allSubtypes(castType.asObjectType, reflexive = true)
                if ch.isSubtypeOf(castType, ObjectType.Serializable)
            } {

                // the object will be created
                if (!instantiatedTypesUB.contains(t))
                    resNewInstantiatedTypes += t

                if (ch.isSubtypeOf(castType, ObjectType.Externalizable)) {
                    // call to `readExternal`
                    val readExternal = p.instanceCall(t, t, "readExternal", ReadExternalDescriptor)

                    calleesAndCallers.updateWithIndirectCallOrFallback(
                        definedMethod, readExternal, pc,
                        ObjectType.Externalizable.packageName,
                        ObjectType.Externalizable,
                        "readExternal",
                        ReadExternalDescriptor,
                        parameterList
                    )

                    // call to no-arg constructor
                    p.classFile(t) foreach { cf ⇒
                        cf.findMethod("<init>", NoArgsAndReturnVoid) foreach { c ⇒
                            calleesAndCallers.updateWithIndirectCall(
                                definedMethod, declaredMethods(c), pc, UnknownParam
                            )
                        }
                    }
                } else {
                    // call to `readObject`
                    val readObjectMethod =
                        p.specialCall(t, t, isInterface = false, "readObject", ReadObjectDescriptor)
                    calleesAndCallers.updateWithIndirectCallOrFallback(
                        definedMethod, readObjectMethod, pc,
                        ObjectType.Object.packageName,
                        ObjectType.Object,
                        "readObject",
                        ReadObjectDescriptor,
                        parameterList
                    )

                    // call to first super no-arg constructor
                    val nonSerializableSuperclass = firstNotSerializableSupertype(t)
                    if (nonSerializableSuperclass.isDefined) {
                        val constructor = p.classFile(nonSerializableSuperclass.get).flatMap { cf ⇒
                            cf.findMethod("<init>", NoArgsAndReturnVoid)
                        }
                        // otherwise an exception will thrown at runtime
                        if (constructor.isDefined) {
                            calleesAndCallers.updateWithIndirectCall(
                                definedMethod, declaredMethods(constructor.get), pc, UnknownParam
                            )
                        }
                    }
                }

                // call to `readResolve`
                val readResolve =
                    p.specialCall(t, t, isInterface = false, "readResolve", JustReturnsObject)
                calleesAndCallers.updateWithIndirectCallOrFallback(
                    definedMethod, readResolve, pc,
                    ObjectType.Object.packageName,
                    ObjectType.Object,
                    "readResolve",
                    JustReturnsObject,
                    UnknownParam
                )

                // call to `validateObject`
                if (ch.isSubtypeOf(t, ObjectInputValidationType)) {
                    val validateObject =
                        p.instanceCall(t, t, "validateObject", JustReturnsObject)
                    calleesAndCallers.updateWithIndirectCallOrFallback(
                        definedMethod, validateObject, pc,
                        ObjectType.Object.packageName,
                        ObjectType.Object,
                        "validateObject",
                        JustReturnsObject,
                        UnknownParam
                    )
                }
            }
        }

        if (!foundCast) {
            calleesAndCallers.addIncompleteCallsite(pc)
        }
        resNewInstantiatedTypes
    }

    @tailrec private[this] def firstNotSerializableSupertype(t: ObjectType): Option[ObjectType] = {
        ch.superclassType(t) match {
            case None ⇒ None
            case Some(superType) ⇒
                if (ch.isSubtypeOf(superType, ObjectType.Serializable)) {
                    firstNotSerializableSupertype(superType)
                } else {
                    Some(superType)
                }
        }
    }

    @inline private[this] def returnResult(
        definedMethod:        DefinedMethod,
        relevantPCs:          IntTrieSet,
        calleesAndCallers:    IndirectCalleesAndCallers,
        newInstantiatedTypes: UIDSet[ObjectType],
        tacaiEP:              EOptionP[Method, TACAI]
    ): ProperPropertyComputationResult = {
        var res: List[ProperPropertyComputationResult] = calleesAndCallers.partialResultsForCallers

        val tmpResult =
            if (calleesAndCallers.callees.isEmpty) NoSerializationRelatedCallees
            else
                new SerializationRelatedCalleesImplementation(
                    calleesAndCallers.callees,
                    calleesAndCallers.incompleteCallsites,
                    calleesAndCallers.parameters
                )

        val calleesResult =
            if (tacaiEP.isRefinable)
                InterimResult.forUB(
                    definedMethod,
                    tmpResult,
                    Some(tacaiEP),
                    c(definedMethod, relevantPCs)
                )
            else
                Result(definedMethod, tmpResult)

        res ::= calleesResult

        if (newInstantiatedTypes.nonEmpty)
            res ::= PartialResult(
                p,
                InstantiatedTypes.key,
                InstantiatedTypesAnalysis.update(p, newInstantiatedTypes)
            )

        Results(res)
    }

    private[this] def c(
        definedMethod: DefinedMethod, relevantPCs: IntTrieSet
    )(
        eps: SomeEPS
    ): ProperPropertyComputationResult = eps match {
        case UBP(_: TheTACAI) ⇒
            processMethod(definedMethod, relevantPCs, eps.asInstanceOf[EPS[Method, TACAI]])
        case UBP(_: TACAI) ⇒
            InterimResult.forUB(
                definedMethod,
                NoSerializationRelatedCallees,
                List(eps),
                c(definedMethod, relevantPCs)
            )
    }
}

object SerializationRelatedCallsAnalysis {
    final val UnknownParam = Seq(None)
}

object TriggeredSerializationRelatedCallsAnalysis extends BasicFPCFTriggeredAnalysisScheduler {

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        CallersProperty,
        InstantiatedTypes,
        TACAI
    )

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(
        CallersProperty,
        InstantiatedTypes
    )

    override def derivesEagerly: Set[PropertyBounds] = PropertyBounds.ubs(
        SerializationRelatedCallees
    )

    override def register(p: SomeProject, ps: PropertyStore, unused: Null): FPCFAnalysis = {
        val analysis = new SerializationRelatedCallsAnalysis(p)
        ps.registerTriggeredComputation(CallersProperty.key, analysis.analyze)
        analysis
    }

}
