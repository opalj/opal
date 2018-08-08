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
import org.opalj.collection.immutable.UIDSet
import org.opalj.fpcf.properties.CallersProperty
import org.opalj.fpcf.properties.InstantiatedTypes
import org.opalj.fpcf.properties.NoCallers
import org.opalj.fpcf.properties.NoSerializationRelatedCallees
import org.opalj.fpcf.properties.SerializationRelatedCallees
import org.opalj.fpcf.properties.SerializationRelatedCalleesImplementation
import org.opalj.log.OPALLogger
import org.opalj.tac.Assignment
import org.opalj.tac.Checkcast
import org.opalj.tac.ExprStmt
import org.opalj.tac.SimpleTACAIKey
import org.opalj.tac.Stmt
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.VirtualMethodCall
import org.opalj.value.IsReferenceValue

/**
 * todo
 * @author Florian Kuebler
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
        if (method.classFile.thisType ne declaredMethod.declaringClassType)
            return NoResult;

        if (method.body.isEmpty)
            // happens in particular for native methods
            return NoResult;

        // the set of types that are definitely initialized at this point in time
        // in case the instantiatedTypes are not finally computed, we depend on them
        val instantiatedTypesEOptP = propertyStore(project, InstantiatedTypes.key)

        // the upper bound for type instantiations, seen so far
        // in case they are not yet computed, we use the initialTypes
        val instantiatedTypesUB: UIDSet[ObjectType] = instantiatedTypesEOptP match {
            case eps: EPS[_, _] ⇒ eps.ub.types

            case _              ⇒ InstantiatedTypes.initialTypes
        }

        val calleesAndCallers = new CalleesAndCallers()

        val stmts = tacai(method).stmts

        var newInstantiatedTypes = UIDSet.empty[ObjectType]
        for (stmt ← stmts) {
            stmt match {
                case VirtualMethodCall(pc, dc, _, "writeObject", md, _, params) if isOOSWriteObject(dc, md) ⇒
                    val param = params.head.asVar.value.asReferenceValue
                    handleOOSWriteObject(definedMethod, param, pc, calleesAndCallers)

                case Assignment(pc, targetVar, VirtualFunctionCall(_, dc, _, "readObject", md, _, _)) if isOISReadObject(dc, md) ⇒
                    newInstantiatedTypes = handleOISReadObject(
                        definedMethod,
                        targetVar.asVar,
                        pc,
                        stmts,
                        instantiatedTypesUB,
                        newInstantiatedTypes,
                        calleesAndCallers
                    )

                case ExprStmt(_, VirtualFunctionCall(pc, dc, _, "readObject", md, _, _)) if isOISReadObject(dc, md) ⇒
                    OPALLogger.warn("analysis", "missed call to readObject")

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
        param:             IsReferenceValue,
        pc:                Int,
        calleesAndCallers: CalleesAndCallers
    ): Unit = {
        for (rv ← param.allValues) {
            if (rv.isPrecise) {
                val rt = rv.valueType.get
                val paramType =
                    if (rt.isArrayType)
                        rt.asArrayType.elementType.asObjectType // todo this will crash
                    else rt.asObjectType

                if (classHierarchy.isSubtypeOf(paramType, ObjectType.Serializable)) {
                    if (classHierarchy.isSubtypeOf(paramType, ObjectType.Externalizable)) {
                        val writeExternalMethod = project.instanceCall(
                            paramType, paramType, "writeExternal", MethodDescriptor.JustTakes(ObjectType("java/io/ObjectOutput"))
                        )

                        calleesAndCallers.updateWithCallOrFallback(
                            definedMethod, writeExternalMethod, pc,
                            ObjectType.Externalizable.packageName,
                            ObjectType.Externalizable,
                            "writeExternal",
                            MethodDescriptor.JustTakes(ObjectType("java/io/ObjectOutput"))
                        )
                    } else {
                        val writeObjectMethod = project.specialCall(
                            paramType, false, "writeObject", WriteObjectDescriptor
                        )
                        calleesAndCallers.updateWithCallOrFallback(
                            definedMethod,
                            writeObjectMethod,
                            pc,
                            ObjectType.Object.packageName,
                            ObjectType.Object,
                            "writeObject",
                            WriteObjectDescriptor
                        )
                    }

                    val writeReplaceMethod = project.specialCall(
                        paramType, false, "writeReplace", WriteObjectDescriptor
                    )

                    calleesAndCallers.updateWithCallOrFallback(
                        definedMethod, writeReplaceMethod, pc,
                        ObjectType.Object.packageName,
                        ObjectType.Object,
                        "writeReplace",
                        WriteObjectDescriptor
                    )
                }

            } else {
                OPALLogger.warn("analysis", "missed call to writeObject")
            }
        }
    }

    private[this] def handleOISReadObject(
        definedMethod:        DefinedMethod,
        targetVar:            V,
        pc:                   Int,
        stmts:                Array[Stmt[V]],
        instantiatedTypesUB:  UIDSet[ObjectType],
        newInstantiatedTypes: UIDSet[ObjectType],
        calleesAndCallers:    CalleesAndCallers
    ): UIDSet[ObjectType] = {
        var resNewInstantiatedTypes = UIDSet.empty[ObjectType]
        var foundCast = false
        for {
            Checkcast(_, _, cmpTpe) ← stmts
        } {
            var castType = cmpTpe

            if (cmpTpe.isArrayType) {
                val elementType = cmpTpe.asArrayType.elementType
                if (elementType.isObjectType)
                    castType = elementType.asObjectType
                else {
                    // todo we will crash later... we have to handle that case
                }
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

                    calleesAndCallers.updateWithCallOrFallback(
                        definedMethod, readExternal, pc,
                        ObjectType.Externalizable.packageName,
                        ObjectType.Externalizable,
                        "readExternal",
                        MethodDescriptor.JustTakes(ObjectType("java/io/ObjectInput"))
                    )

                    // call to no-arg constructor
                    val constructor = p.classFile(t).flatMap { cf ⇒
                        cf.findMethod("<init>", MethodDescriptor.NoArgsAndReturnVoid)
                    }
                    // otherwise an exception will thrown at runtime
                    if (constructor.isDefined) {
                        calleesAndCallers.updateWithCall(
                            definedMethod, declaredMethods(constructor.get), pc
                        )
                    }
                } else {
                    // call to `readObject`
                    val readObjectMethod = p.specialCall(
                        t, isInterface = false, "readObject", ReadObjectDescriptor
                    )
                    calleesAndCallers.updateWithCallOrFallback(
                        definedMethod, readObjectMethod, pc,
                        ObjectType.Object.packageName,
                        ObjectType.Object,
                        "readObject",
                        ReadObjectDescriptor
                    )

                    // call to first super no-arg constructor
                    val nonSerializableSuperclass = firstNotSerializableSupertype(t)
                    if (nonSerializableSuperclass.isDefined) {
                        val constructor = p.classFile(nonSerializableSuperclass.get).flatMap { cf ⇒
                            cf.findMethod("<init>", MethodDescriptor.NoArgsAndReturnVoid)
                        }
                        // otherwise an exception will thrown at runtime
                        if (constructor.isDefined) {
                            calleesAndCallers.updateWithCall(
                                definedMethod, declaredMethods(constructor.get), pc
                            )
                        }
                    }
                }

                // call to `readResolve`
                val readResolve = p.specialCall(
                    t,
                    isInterface = false,
                    "readResolve",
                    MethodDescriptor.JustReturnsObject
                )
                calleesAndCallers.updateWithCallOrFallback(
                    definedMethod, readResolve, pc,
                    ObjectType.Object.packageName,
                    ObjectType.Object,
                    "readResolve",
                    MethodDescriptor.JustReturnsObject
                )

                // call to `validateObject`
                if (ch.isSubtypeOf(t, ObjectType("java/io/ObjectInputValidation"))) {
                    val readResolve = p.instanceCall(
                        t, t, "validateObject", MethodDescriptor.JustReturnsObject
                    )
                    calleesAndCallers.updateWithCallOrFallback(
                        definedMethod, readResolve, pc,
                        ObjectType.Object.packageName,
                        ObjectType.Object,
                        "readResolve",
                        MethodDescriptor.JustReturnsObject
                    )
                }
            }
        }

        if (!foundCast) {
            OPALLogger.warn("analysis", "missed call to readObject")
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
        calleesAndCallers:    CalleesAndCallers,
        newInstantiatedTypes: UIDSet[ObjectType]
    ): PropertyComputationResult = {
        var res: List[PropertyComputationResult] = calleesAndCallers.partialResultsForCallers

        val calleesResult =
            if (calleesAndCallers.callees.isEmpty)
                Result(definedMethod, NoSerializationRelatedCallees)
            else
                Result(
                    definedMethod,
                    new SerializationRelatedCalleesImplementation(calleesAndCallers.callees)
                )

        res ::= calleesResult

        if (newInstantiatedTypes.nonEmpty)
            res ::= RTACallGraphAnalysis.partialResultForInstantiatedTypes(p, newInstantiatedTypes)

        Results(res)
    }
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

    override def uses: Set[PropertyKind] = Set(CallersProperty)

    override def derives: Set[PropertyKind] = Set(CallersProperty, SerializationRelatedCallees)

    override def init(p: SomeProject, ps: PropertyStore): SerializationRelatedCallsAnalysis = {
        val analysis = new SerializationRelatedCallsAnalysis(p)
        ps.registerTriggeredComputation(CallersProperty.key, analysis.analyze)
        analysis
    }

    override def beforeSchedule(p: SomeProject, ps: PropertyStore): Unit = {}

    override def afterPhaseCompletion(p: SomeProject, ps: PropertyStore): Unit = {}
}
