/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses
package cg

import scala.language.existentials
import scala.annotation.tailrec
import org.opalj.br.DeclaredMethod
import org.opalj.br.DefinedMethod
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.collection.immutable.UIDSet
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.fpcf.analyses.cg.SerializationRelatedCallsAnalysis.UnknownParam
import org.opalj.fpcf.cg.properties.SerializationRelatedCalleesImplementation
import org.opalj.fpcf.cg.properties.SerializationRelatedCallees
import org.opalj.fpcf.cg.properties.NoCallers
import org.opalj.fpcf.cg.properties.CallersProperty
import org.opalj.fpcf.cg.properties.NoSerializationRelatedCallees
import org.opalj.fpcf.cg.properties.InstantiatedTypes
import org.opalj.tac.Assignment
import org.opalj.tac.Checkcast
import org.opalj.tac.ExprStmt
import org.opalj.tac.SimpleTACAIKey
import org.opalj.tac.Stmt
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.VirtualMethodCall

import scala.collection.immutable.IntMap

/**
 * todo
 * @author Florian Kuebler
 * @author Dominik Helm
 */
class SerializationRelatedCallsAnalysis private[analyses] (
        final val project: SomeProject
) extends FPCFAnalysis {

    implicit private[this] val declaredMethods: DeclaredMethods = project.get(DeclaredMethodsKey)
    private[this] val tacai = project.get(SimpleTACAIKey)

    private[this] val WriteObjectDescriptor =
        MethodDescriptor.JustTakes(ObjectType.ObjectOutputStream)
    private[this] val ReadObjectDescriptor =
        MethodDescriptor.JustTakes(ObjectType.ObjectInputStream)

    def analyze(declaredMethod: DeclaredMethod): PropertyComputationResult = {
        // todo this is copy & past code from the RTACallGraphAnalysis -> refactor
        propertyStore(declaredMethod, CallersProperty.key) match {
            case FinalEP(_, NoCallers) ⇒
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

        // we only allow defined methods with declared type eq. to the class of the method
        if (method.classFile.thisType != declaredMethod.declaringClassType)
            return NoResult;

        if (method.body.isEmpty)
            // happens in particular for native methods
            return NoResult;

        var relevantPCs: IntMap[IntTrieSet] = IntMap.empty
        val insts = method.body.get.instructions
        var i = 0
        val max = insts.length
        while (i < max) {
            val inst = insts(i)
            if (inst != null)
                inst.opcode match {
                    case INVOKEVIRTUAL.opcode ⇒
                        val call = inst.asMethodInvocationInstruction
                        if (call.declaringClass == ObjectType.ObjectOutputStream
                            && call.name == "writeObject" ||
                            call.declaringClass == ObjectType.ObjectInputStream &&
                            call.name == "readObject")
                            relevantPCs += i → IntTrieSet.empty
                    case _ ⇒
                }
            i += 1
        }

        if (relevantPCs.isEmpty)
            return Result(declaredMethod, NoSerializationRelatedCallees);

        // the set of types that are definitely initialized at this point in time
        val instantiatedTypesEOptP = propertyStore(project, InstantiatedTypes.key)

        // the upper bound for type instantiations, seen so far
        // in case they are not yet computed, we use the initialTypes
        val instantiatedTypesUB: UIDSet[ObjectType] = instantiatedTypesEOptP match {
            case eps: EPS[_, _] ⇒ eps.ub.types

            case _              ⇒ InstantiatedTypes.initialTypes
        }

        val calleesAndCallers = new IndirectCalleesAndCallers()

        val tacode = tacai(method)
        implicit val stmts = tacode.stmts
        val pcToIndex = tacode.pcToIndex

        var newInstantiatedTypes = UIDSet.empty[ObjectType]
        for {
            pc ← relevantPCs.keysIterator
            index = pcToIndex(pc)
            if index != -1
            stmt = stmts(index)
        } {
            stmt match {
                case VirtualMethodCall(_, dc, _, "writeObject", md, receiver, params) if isOOSWriteObject(dc, md) ⇒
                    handleOOSWriteObject(
                        definedMethod,
                        receiver.asVar,
                        params.head.asVar,
                        pc,
                        calleesAndCallers
                    )

                case Assignment(_, targetVar, VirtualFunctionCall(_, dc, _, "readObject", md, receiver, _)) if isOISReadObject(dc, md) ⇒
                    newInstantiatedTypes = handleOISReadObject(
                        definedMethod,
                        targetVar.asVar,
                        receiver.asVar,
                        pc,
                        instantiatedTypesUB,
                        newInstantiatedTypes,
                        calleesAndCallers
                    )

                case ExprStmt(_, VirtualFunctionCall(_, dc, _, "readObject", md, _, _)) if isOISReadObject(dc, md) ⇒
                    calleesAndCallers.addIncompleteCallsite(pc)

                case _ ⇒

            }
        }

        returnResult(definedMethod, calleesAndCallers, newInstantiatedTypes)
    }

    @inline private[this] def isOOSWriteObject(
        declaredType: ReferenceType, methodDescriptor: MethodDescriptor
    ): Boolean = {
        classHierarchy.isSubtypeOf(declaredType, ObjectType.ObjectOutputStream) &&
            methodDescriptor == MethodDescriptor.JustTakesObject
    }

    @inline private[this] def isOISReadObject(
        declaredType: ReferenceType, methodDescriptor: MethodDescriptor
    ): Boolean = {
        classHierarchy.isSubtypeOf(declaredType, ObjectType.ObjectInputStream) &&
            methodDescriptor == MethodDescriptor.JustReturnsObject
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
            if (rv.isPrecise) {
                val rt = rv.valueType.get
                if (rt.isObjectType || rt.asArrayType.elementType.isObjectType) {
                    val paramType =
                        if (rt.isArrayType)
                            rt.asArrayType.elementType.asObjectType
                        else rt.asObjectType

                    if (classHierarchy.isSubtypeOf(paramType, ObjectType.Serializable)) {
                        if (classHierarchy.isSubtypeOf(paramType, ObjectType.Externalizable)) {
                            val writeExternalMethod = project.instanceCall(
                                paramType,
                                paramType,
                                "writeExternal",
                                MethodDescriptor.JustTakes(ObjectType("java/io/ObjectOutput"))
                            )

                            calleesAndCallers.updateWithIndirectCallOrFallback(
                                definedMethod, writeExternalMethod, pc,
                                ObjectType.Externalizable.packageName,
                                ObjectType.Externalizable,
                                "writeExternal",
                                MethodDescriptor.JustTakes(ObjectType("java/io/ObjectOutput")),
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
    )(implicit stmts: Array[Stmt[V]]): UIDSet[ObjectType] = {
        var resNewInstantiatedTypes = UIDSet.empty[ObjectType]
        var foundCast = false
        val parameterList = Seq(None, persistentUVar(inputStream))
        for {
            Checkcast(_, _, cmpTpe) ← stmts
            if cmpTpe.isObjectType || cmpTpe.asArrayType.elementType.isObjectType
        } {
            var castType = cmpTpe

            if (cmpTpe.isArrayType) {
                val elementType = cmpTpe.asArrayType.elementType
                if (elementType.isObjectType)
                    castType = elementType.asObjectType
            }
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
                    val readExternal = p.instanceCall(
                        t,
                        t,
                        "readExternal",
                        MethodDescriptor.JustTakes(ObjectType("java/io/ObjectInput"))
                    )

                    calleesAndCallers.updateWithIndirectCallOrFallback(
                        definedMethod, readExternal, pc,
                        ObjectType.Externalizable.packageName,
                        ObjectType.Externalizable,
                        "readExternal",
                        MethodDescriptor.JustTakes(ObjectType("java/io/ObjectInput")),
                        parameterList
                    )

                    // call to no-arg constructor
                    val constructor = p.classFile(t).flatMap { cf ⇒
                        cf.findMethod("<init>", MethodDescriptor.NoArgsAndReturnVoid)
                    }
                    // otherwise an exception will thrown at runtime
                    if (constructor.isDefined) {
                        calleesAndCallers.updateWithIndirectCall(
                            definedMethod, declaredMethods(constructor.get), pc, UnknownParam
                        )
                    }
                } else {
                    // call to `readObject`
                    val readObjectMethod = p.specialCall(
                        t, t, isInterface = false, "readObject", ReadObjectDescriptor
                    )
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
                            cf.findMethod("<init>", MethodDescriptor.NoArgsAndReturnVoid)
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
                val readResolve = p.specialCall(
                    t,
                    t,
                    isInterface = false,
                    "readResolve",
                    MethodDescriptor.JustReturnsObject
                )
                calleesAndCallers.updateWithIndirectCallOrFallback(
                    definedMethod, readResolve, pc,
                    ObjectType.Object.packageName,
                    ObjectType.Object,
                    "readResolve",
                    MethodDescriptor.JustReturnsObject,
                    UnknownParam
                )

                // call to `validateObject`
                if (ch.isSubtypeOf(t, ObjectType("java/io/ObjectInputValidation"))) {
                    val validateObject = p.instanceCall(
                        t, t, "validateObject", MethodDescriptor.JustReturnsObject
                    )
                    calleesAndCallers.updateWithIndirectCallOrFallback(
                        definedMethod, validateObject, pc,
                        ObjectType.Object.packageName,
                        ObjectType.Object,
                        "validateObject",
                        MethodDescriptor.JustReturnsObject,
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
        calleesAndCallers:    IndirectCalleesAndCallers,
        newInstantiatedTypes: UIDSet[ObjectType]
    ): PropertyComputationResult = {
        var res: List[PropertyComputationResult] = calleesAndCallers.partialResultsForCallers

        val calleesResult =
            if (calleesAndCallers.callees.isEmpty)
                Result(definedMethod, NoSerializationRelatedCallees)
            else
                Result(
                    definedMethod,
                    new SerializationRelatedCalleesImplementation(
                        calleesAndCallers.callees,
                        calleesAndCallers.incompleteCallsites,
                        calleesAndCallers.parameters
                    )
                )

        res ::= calleesResult

        if (newInstantiatedTypes.nonEmpty)
            res ::= RTACallGraphAnalysis.partialResultForInstantiatedTypes(p, newInstantiatedTypes)

        Results(res)
    }
}

object SerializationRelatedCallsAnalysis {
    final val UnknownParam = Seq(None)
}

object EagerSerializationRelatedCallsAnalysis extends FPCFEagerAnalysisScheduler {

    override type InitializationData = SerializationRelatedCallsAnalysis

    override def start(
        project:       SomeProject,
        propertyStore: PropertyStore,
        analysis:      SerializationRelatedCallsAnalysis
    ): FPCFAnalysis = {
        analysis
    }

    override def uses: Set[PropertyKind] = Set(CallersProperty, InstantiatedTypes)

    override def derives: Set[PropertyKind] = Set(CallersProperty, SerializationRelatedCallees)

    override def init(p: SomeProject, ps: PropertyStore): SerializationRelatedCallsAnalysis = {
        val analysis = new SerializationRelatedCallsAnalysis(p)
        ps.registerTriggeredComputation(CallersProperty.key, analysis.analyze)
        analysis
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}
}
