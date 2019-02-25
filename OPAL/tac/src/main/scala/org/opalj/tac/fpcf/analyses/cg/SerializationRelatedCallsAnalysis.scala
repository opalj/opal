/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import scala.annotation.tailrec

import org.opalj.fpcf.EPK
import org.opalj.fpcf.FinalP
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.InterimUBP
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.UBP
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.fpcf.BasicFPCFEagerAnalysisScheduler
import org.opalj.br.fpcf.FPCFAnalysis
import org.opalj.br.fpcf.cg.properties.CallersProperty
import org.opalj.br.DeclaredMethod
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.DefinedMethod
import org.opalj.br.ElementReferenceType
import org.opalj.br.MethodDescriptor.JustReturnsObject
import org.opalj.br.MethodDescriptor.NoArgsAndReturnVoid
import org.opalj.br.ObjectType.{ObjectOutputStream ⇒ ObjectOutputStreamType}
import org.opalj.br.ObjectType.{ObjectInputStream ⇒ ObjectInputStreamType}
import org.opalj.br.fpcf.cg.properties.Callees
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.TheTACAI
import org.opalj.tac.VirtualFunctionCall

trait APIBasedCallGraphAnalysis extends FPCFAnalysis {
    val apiMethod: DeclaredMethod

    implicit val declaredMethods: DeclaredMethods = p.get(DeclaredMethodsKey)

    def handleNewCaller(caller: DefinedMethod, pc: Int): ProperPropertyComputationResult

    final def registerAPIMethod(): ProperPropertyComputationResult = {
        val seenCallers = Set.empty[(DeclaredMethod, Int)]
        val callersEOptP = ps(apiMethod, CallersProperty.key)
        c(seenCallers)(callersEOptP)
    }

    private[this] def c(
        seenCallers: Set[(DeclaredMethod, Int)]
    )(callersEOptP: SomeEOptionP): ProperPropertyComputationResult =
        (callersEOptP: @unchecked) match {
            case UBP(callersUB: CallersProperty) ⇒
                // IMPROVE: use better design in order to get new callers
                var newSeenCallers = seenCallers
                var results: List[ProperPropertyComputationResult] = Nil
                if (callersUB.size != 0) {
                    for ((caller, pc) ← callersUB.callers) {
                        // we can not analyze virtual methods
                        if (!newSeenCallers.contains((caller, pc)) && caller.hasSingleDefinedMethod) {
                            newSeenCallers += (caller → pc)

                            results ::= handleNewCaller(caller.asDefinedMethod, pc)
                        }
                    }
                }

                if (callersEOptP.isRefinable)
                    results ::= InterimPartialResult(
                        Some(callersEOptP), c(newSeenCallers)
                    )

                Results(results)
            case _: EPK[_, _] ⇒ InterimPartialResult(Some(callersEOptP), c(seenCallers))
        }
}

trait TACAIBasedAPIBasedCallGraphAnalysis extends APIBasedCallGraphAnalysis {
    final override def handleNewCaller(caller: DefinedMethod, pc: Int): ProperPropertyComputationResult = {
        val tacEOptP = ps(caller.definedMethod, TACAI.key)
        continueWithTAC(caller, pc)(tacEOptP)
    }

    private[this] def continueWithTAC(
        caller: DefinedMethod, pc: Int
    )(tacEOptP: SomeEOptionP): ProperPropertyComputationResult = tacEOptP match {
        case FinalP(tac: TheTACAI) ⇒
            processNewCaller(caller, pc, tac.theTAC)

        case InterimUBP(tac: TheTACAI) ⇒
            val result = processNewCaller(caller, pc, tac.theTAC)
            val continuationResult =
                InterimPartialResult(Some(tacEOptP), continueWithTAC(caller, pc))
            Results(result, continuationResult)

        case _ ⇒ InterimPartialResult(Some(tacEOptP), continueWithTAC(caller, pc))
    }

    def processNewCaller(
        caller: DefinedMethod, pc: Int, tac: TACode[TACMethodParameter, V]
    ): ProperPropertyComputationResult
}

class OOSWriteObjectAnalysis private[analyses] (
        final val project: SomeProject
) extends TACAIBasedAPIBasedCallGraphAnalysis {

    override val apiMethod: DeclaredMethod = declaredMethods(
        ObjectOutputStreamType,
        "",
        ObjectOutputStreamType,
        "writeObject",
        MethodDescriptor.JustTakesObject
    )

    final val ObjectOutputType = ObjectType("java/io/ObjectOutput")
    final val WriteExternalDescriptor = MethodDescriptor.JustTakes(ObjectOutputType)
    final val WriteObjectDescriptor = MethodDescriptor.JustTakes(ObjectOutputStreamType)

    override def processNewCaller(
        caller: DefinedMethod,
        pc:     UShort,
        tac:    TACode[TACMethodParameter, V]
    ): ProperPropertyComputationResult = {
        implicit val stmts: Array[Stmt[V]] = tac.stmts

        val indexOfWriteObject = tac.pcToIndex(pc)

        // todo this may fail
        val calleesAndCallers = new CalleesAndCallers()
        val VirtualMethodCall(_, _, _, "writeObject", _, receiver: V, params) = stmts(indexOfWriteObject)
        handleOOSWriteObject(
            caller,
            receiver,
            params.head.asVar,
            pc,
            calleesAndCallers
        )

        Results(
            calleesAndCallers.partialResultForCallees(caller, isIndirect = true),
            calleesAndCallers.partialResultsForCallers
        )
    }

    private[this] def handleOOSWriteObject(
        definedMethod:     DefinedMethod,
        outputStream:      V,
        param:             V,
        pc:                Int,
        calleesAndCallers: CalleesAndCallers
    )(
        implicit
        stmts: Array[Stmt[V]]
    ): Unit = {
        val parameterList = Seq(persistentUVar(param), persistentUVar(outputStream))

        for (rv ← param.value.asReferenceValue.allValues) {
            if (rv.isPrecise && rv.isNull.isNo) {
                val rt = rv.leastUpperType.get
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
                                WriteExternalDescriptor
                            )

                            calleesAndCallers.updateWithIndirectCallOrFallback(
                                definedMethod,
                                writeExternalMethod,
                                pc,
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
                            definedMethod,
                            writeReplaceMethod,
                            pc,
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
            } else {
                calleesAndCallers.addIncompleteCallsite(pc)
            }
        }
    }

}

class OISReadObjectAnalysis private[analyses] (
        final val project: SomeProject
) extends TACAIBasedAPIBasedCallGraphAnalysis {

    final val ObjectInputValidationType = ObjectType("java/io/ObjectInputValidation")
    final val ObjectInputType = ObjectType("java/io/ObjectInput")

    final val ReadObjectDescriptor = MethodDescriptor.JustTakes(ObjectInputStreamType)
    final val ReadExternalDescriptor = MethodDescriptor.JustTakes(ObjectInputType)

    final val UnknownParam = Seq(None)

    override val apiMethod: DeclaredMethod = declaredMethods(
        ObjectInputStreamType,
        "",
        ObjectInputStreamType,
        "readObject",
        MethodDescriptor.JustReturnsObject
    )

    override def processNewCaller(
        caller: DefinedMethod, pc: Int, tac: TACode[TACMethodParameter, V]
    ): ProperPropertyComputationResult = {
        implicit val stmts: Array[Stmt[V]] = tac.stmts

        val indexOfReadObject = tac.pcToIndex(pc)
        val calleesAndCallers = new CalleesAndCallers()

        val stmt = stmts(indexOfReadObject)
        if (stmt.isInstanceOf[Assignment[V]]) {
            // todo this may fail
            val VirtualFunctionCall(_, _, _, "readObject", _, receiver: V, _) = stmt.asAssignment.expr
            handleOISReadObject(caller, stmt.asAssignment.targetVar, receiver, pc, calleesAndCallers)

        } else {
            calleesAndCallers.addIncompleteCallsite(pc)
        }

        Results(
            calleesAndCallers.partialResultForCallees(caller, isIndirect = true),
            calleesAndCallers.partialResultsForCallers
        )
    }

    private[this] def handleOISReadObject(
        definedMethod:     DefinedMethod,
        targetVar:         V,
        inputStream:       V,
        pc:                Int,
        calleesAndCallers: CalleesAndCallers
    )(
        implicit
        stmts: Array[Stmt[V]]
    ): Unit = {
        var foundCast = false
        val parameterList = Seq(None, persistentUVar(inputStream))
        for { Checkcast(_, _, ElementReferenceType(castType)) ← stmts } {
            foundCast = true

            // for each subtype of the type declared at cast we add calls to the relevant methods
            for {
                t ← ch.allSubtypes(castType.asObjectType, reflexive = true)
                cf ← project.classFile(t) // we ignore cases were no class file exists
                if !cf.isInterfaceDeclaration
                if ch.isSubtypeOf(castType, ObjectType.Serializable)
            } {
                if (ch.isSubtypeOf(castType, ObjectType.Externalizable)) {
                    // call to `readExternal`
                    val readExternal = p.instanceCall(t, t, "readExternal", ReadExternalDescriptor)

                    calleesAndCallers.updateWithIndirectCallOrFallback(
                        definedMethod,
                        readExternal,
                        pc,
                        ObjectType.Externalizable.packageName,
                        ObjectType.Externalizable,
                        "readExternal",
                        ReadExternalDescriptor,
                        parameterList
                    )

                    // call to no-arg constructor
                    cf.findMethod("<init>", NoArgsAndReturnVoid) foreach { c ⇒
                        calleesAndCallers.updateWithIndirectCall(
                            definedMethod, declaredMethods(c), pc, UnknownParam
                        )
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

                    // for the type to be instantiated, we need to call a constructor of the type t
                    // in order to let the instantiated types be correct. Note, that the JVM would
                    // not call the constructor
                    // Note, that we assume that there is a constructor
                    val constructor = cf.constructors.next()
                    calleesAndCallers.updateWithIndirectCall(
                        definedMethod, declaredMethods(constructor), pc, UnknownParam
                    )

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
}

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

    def process(p: SomeProject): PropertyComputationResult = {
        val readObjectAnalysis = new OISReadObjectAnalysis(project)
        val readObjectResult = readObjectAnalysis.registerAPIMethod()
        val writeObjectAnalysis = new OOSWriteObjectAnalysis(project)
        val writeObjectResult = writeObjectAnalysis.registerAPIMethod()
        Results(readObjectResult, writeObjectResult)
    }
}

object TriggeredSerializationRelatedCallsAnalysis extends BasicFPCFEagerAnalysisScheduler {

    override def uses: Set[PropertyBounds] = PropertyBounds.ubs(
        CallersProperty,
        Callees,
        TACAI
    )

    override def derivesCollaboratively: Set[PropertyBounds] = PropertyBounds.ubs(
        CallersProperty, Callees
    )

    override def derivesEagerly: Set[PropertyBounds] = Set.empty

    override def start(p: SomeProject, ps: PropertyStore, i: Null): FPCFAnalysis = {

        val analysis = new SerializationRelatedCallsAnalysis(p)
        ps.scheduleEagerComputationForEntity(p)(analysis.process)
        analysis
    }
}

