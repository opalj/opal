/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package cg

import scala.annotation.tailrec

import org.opalj.fpcf.EOptionP
import org.opalj.fpcf.EPK
import org.opalj.fpcf.EPS
import org.opalj.fpcf.InterimPartialResult
import org.opalj.fpcf.ProperPropertyComputationResult
import org.opalj.fpcf.PropertyBounds
import org.opalj.fpcf.PropertyComputationResult
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.Results
import org.opalj.fpcf.SomeEOptionP
import org.opalj.fpcf.UBP
import org.opalj.fpcf.UBPS
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
import org.opalj.br.Method
import org.opalj.tac.fpcf.properties.TACAI
import org.opalj.tac.fpcf.properties.TheTACAI

trait APIBasedCallGraphAnalysis extends FPCFAnalysis {
    val apiMethod: DeclaredMethod

    implicit val declaredMethods: DeclaredMethods = p.get(DeclaredMethodsKey)

    def handleNewCaller(
        caller: DefinedMethod, pc: Int, isDirect: Boolean
    ): ProperPropertyComputationResult

    final def registerAPIMethod(): ProperPropertyComputationResult = {
        val seenCallers = Set.empty[(DeclaredMethod, Int, Boolean)]
        val callersEOptP = ps(apiMethod, CallersProperty.key)
        c(seenCallers)(callersEOptP)
    }

    private[this] def c(
        seenCallers: Set[(DeclaredMethod, Int, Boolean)]
    )(callersEOptP: SomeEOptionP): ProperPropertyComputationResult =
        (callersEOptP: @unchecked) match {
            case UBP(callersUB: CallersProperty) ⇒
                // IMPROVE: use better design in order to get new callers
                var newSeenCallers = seenCallers
                var results: List[ProperPropertyComputationResult] = Nil
                if (callersUB.size != 0) {
                    for ((caller, pc, isDirect) ← callersUB.callers) {
                        // we can not analyze virtual methods
                        if (!newSeenCallers.contains((caller, pc, isDirect)) && caller.hasSingleDefinedMethod) {
                            newSeenCallers += ((caller, pc, isDirect))

                            results ::= handleNewCaller(caller.asDefinedMethod, pc, isDirect)
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
    final override def handleNewCaller(
        caller: DefinedMethod, pc: Int, isDirect: Boolean
    ): ProperPropertyComputationResult = {
        val tacEOptP = ps(caller.definedMethod, TACAI.key)
        if (isDirect)
            continueDirectCallWithTAC(caller, pc)(tacEOptP)
        else {
            val calleesEOptP = ps(caller, Callees.key)
            continueIndirectCallWithTACOrCallees(caller, pc, tacEOptP, calleesEOptP)(tacEOptP)
        }
    }

    private[this] def continueDirectCallWithTAC(
        caller: DefinedMethod, pc: Int
    )(tacEOptP: SomeEOptionP): ProperPropertyComputationResult = tacEOptP match {
        case UBPS(tac: TheTACAI, isFinal) ⇒
            val theTAC = tac.theTAC
            val callStmt = theTAC.stmts(theTAC.pcToIndex(pc))
            val call = retrieveCall(callStmt)
            val tgtVarOpt =
                if (callStmt.isAssignment)
                    Some(callStmt.asAssignment.targetVar)
                else
                    None
            val result = processNewCaller(
                caller,
                pc,
                theTAC,
                call.receiverOption,
                call.params.map(Some(_)),
                tgtVarOpt,
                isDirect = true
            )
            if (isFinal)
                result
            else {
                val continuationResult =
                    InterimPartialResult(Some(tacEOptP), continueDirectCallWithTAC(caller, pc))
                Results(result, continuationResult)
            }

        case _ ⇒ InterimPartialResult(Some(tacEOptP), continueDirectCallWithTAC(caller, pc))
    }

    private[this] def processNewCaller(
        caller:     DefinedMethod,
        pc:         Int,
        calleesEPS: EPS[DeclaredMethod, Callees],
        tacEPS:     EPS[Method, TACAI]
    ): ProperPropertyComputationResult = {
        val tac = tacEPS.ub.tac.get
        val callees = calleesEPS.ub
        val receiverOption = callees.indirectCallReceiver(pc, apiMethod).map(uVarForDefSites(_, tac.pcToIndex))
        val params = callees.indirectCallParameters(pc, apiMethod).map(_.map(uVarForDefSites(_, tac.pcToIndex)))

        val callStmt = tac.stmts(tac.pcToIndex(pc))
        val tgtVarOpt =
            if (callStmt.isAssignment)
                Some(callStmt.asAssignment.targetVar)
            else
                None

        val result = processNewCaller(caller, pc, tac, receiverOption, params, tgtVarOpt, isDirect = false)
        if (tacEPS.isFinal)
            result
        else {
            val continuationResult =
                InterimPartialResult(
                    Some(tacEPS),
                    continueIndirectCallWithTACOrCallees(
                        caller, pc, tacEPS, calleesEPS
                    )
                )
            Results(result, continuationResult)
        }
    }

    private[this] def continueIndirectCallWithTACOrCallees(
        caller:       DefinedMethod,
        pc:           Int,
        tacEOptP:     EOptionP[Method, TACAI],
        calleesEOptP: EOptionP[DeclaredMethod, Callees]
    )(someEOptionP: SomeEOptionP): ProperPropertyComputationResult = someEOptionP match {
        case UBP(_: TheTACAI) if calleesEOptP.isEPS && calleesEOptP.ub.indirectCallees(pc).contains(apiMethod) ⇒
            processNewCaller(
                caller, pc, calleesEOptP.asEPS, someEOptionP.asInstanceOf[EPS[Method, TACAI]]
            )

        case UBP(callees: Callees) if tacEOptP.isEPS && tacEOptP.ub.tac.isDefined && callees.indirectCallees(pc).contains(apiMethod) ⇒
            processNewCaller(
                caller, pc, someEOptionP.asInstanceOf[EPS[DeclaredMethod, Callees]], tacEOptP.asEPS
            )

        case _ ⇒
            InterimPartialResult(
                List(tacEOptP, calleesEOptP),
                continueIndirectCallWithTACOrCallees(caller, pc, tacEOptP, calleesEOptP)
            )
    }

    private[this] def retrieveCall(callStmt: Stmt[V]): Call[V] = callStmt match {
        case VirtualFunctionCallStatement(call)    ⇒ call
        case NonVirtualFunctionCallStatement(call) ⇒ call
        case StaticFunctionCallStatement(call)     ⇒ call
        case call: MethodCall[V]                   ⇒ call
    }

    def processNewCaller(
        caller:          DefinedMethod,
        pc:              Int,
        tac:             TACode[TACMethodParameter, V],
        receiverOption:  Option[Expr[V]],
        params:          Seq[Option[Expr[V]]],
        targetVarOption: Option[V],
        isDirect:        Boolean
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
        caller:         DefinedMethod,
        pc:             Int,
        tac:            TACode[TACMethodParameter, V],
        receiverOption: Option[Expr[V]],
        params:         Seq[Option[Expr[V]]],
        tgtVarOption:   Option[V],
        isDirect:       Boolean
    ): ProperPropertyComputationResult = {
        implicit val stmts: Array[Stmt[V]] = tac.stmts

        val calleesAndCallers = new IndirectCalls()

        if (params.nonEmpty && params.head.isDefined) {
            handleOOSWriteObject(
                caller,
                receiverOption,
                params.head.get.asVar,
                pc,
                calleesAndCallers
            )

        } else {
            calleesAndCallers.addIncompleteCallSite(pc)
        }
        Results(calleesAndCallers.partialResults(caller))
    }

    private[this] def handleOOSWriteObject(
        definedMethod:     DefinedMethod,
        outputStream:      Option[Expr[V]],
        param:             V,
        pc:                Int,
        calleesAndCallers: IndirectCalls
    )(
        implicit
        stmts: Array[Stmt[V]]
    ): Unit = {
        val receiver = persistentUVar(param)
        val parameterList = Seq(
            outputStream.flatMap(os ⇒ persistentUVar(os.asVar))
        )

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

                            calleesAndCallers.addCallOrFallback(
                                definedMethod,
                                writeExternalMethod,
                                pc,
                                ObjectType.Externalizable.packageName,
                                ObjectType.Externalizable,
                                "writeExternal",
                                WriteExternalDescriptor,
                                parameterList,
                                receiver
                            )
                        } else {
                            val writeObjectMethod = project.specialCall(
                                paramType,
                                paramType,
                                isInterface = false,
                                "writeObject",
                                WriteObjectDescriptor
                            )
                            calleesAndCallers.addCallOrFallback(
                                definedMethod,
                                writeObjectMethod,
                                pc,
                                ObjectType.Object.packageName,
                                ObjectType.Object,
                                "writeObject",
                                WriteObjectDescriptor,
                                parameterList,
                                receiver
                            )
                        }

                        val writeReplaceMethod = project.specialCall(
                            paramType,
                            paramType,
                            isInterface = false,
                            "writeReplace",
                            WriteObjectDescriptor
                        )

                        calleesAndCallers.addCallOrFallback(
                            definedMethod,
                            writeReplaceMethod,
                            pc,
                            ObjectType.Object.packageName,
                            ObjectType.Object,
                            "writeReplace",
                            WriteObjectDescriptor,
                            parameterList,
                            receiver
                        )
                    }
                } else {
                    calleesAndCallers.addIncompleteCallSite(pc)
                }
            } else {
                calleesAndCallers.addIncompleteCallSite(pc)
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
        caller:         DefinedMethod,
        pc:             Int,
        tac:            TACode[TACMethodParameter, V],
        receiverOption: Option[Expr[V]],
        params:         Seq[Option[Expr[V]]],
        tgtVarOption:   Option[V],
        isDirect:       Boolean
    ): ProperPropertyComputationResult = {
        implicit val stmts: Array[Stmt[V]] = tac.stmts

        val calleesAndCallers = new IndirectCalls()

        if (tgtVarOption.isDefined) {
            handleOISReadObject(
                caller, tgtVarOption.get, receiverOption, pc, calleesAndCallers
            )

        } else {
            calleesAndCallers.addIncompleteCallSite(pc)
        }

        Results(calleesAndCallers.partialResults(caller))
    }

    private[this] def handleOISReadObject(
        definedMethod:     DefinedMethod,
        targetVar:         V,
        inputStream:       Option[Expr[V]],
        pc:                Int,
        calleesAndCallers: IndirectCalls
    )(
        implicit
        stmts: Array[Stmt[V]]
    ): Unit = {
        var foundCast = false
        val parameterList = Seq(inputStream.flatMap(is ⇒ persistentUVar(is.asVar)))
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

                    calleesAndCallers.addCallOrFallback(
                        definedMethod,
                        readExternal,
                        pc,
                        ObjectType.Externalizable.packageName,
                        ObjectType.Externalizable,
                        "readExternal",
                        ReadExternalDescriptor,
                        parameterList,
                        None
                    )

                    // call to no-arg constructor
                    cf.findMethod("<init>", NoArgsAndReturnVoid) foreach { c ⇒
                        calleesAndCallers.addCall(
                            definedMethod, declaredMethods(c), pc, UnknownParam, None
                        )
                    }
                } else {

                    // call to `readObject`
                    val readObjectMethod =
                        p.specialCall(t, t, isInterface = false, "readObject", ReadObjectDescriptor)
                    calleesAndCallers.addCallOrFallback(
                        definedMethod, readObjectMethod, pc,
                        ObjectType.Object.packageName,
                        ObjectType.Object,
                        "readObject",
                        ReadObjectDescriptor,
                        parameterList,
                        None
                    )

                    // call to first super no-arg constructor
                    val nonSerializableSuperclass = firstNotSerializableSupertype(t)
                    if (nonSerializableSuperclass.isDefined) {
                        val constructor = p.classFile(nonSerializableSuperclass.get).flatMap { cf ⇒
                            cf.findMethod("<init>", NoArgsAndReturnVoid)
                        }
                        // otherwise an exception will thrown at runtime
                        if (constructor.isDefined) {
                            calleesAndCallers.addCall(
                                definedMethod,
                                declaredMethods(constructor.get),
                                pc,
                                UnknownParam,
                                None
                            )
                        }
                    }

                    // for the type to be instantiated, we need to call a constructor of the type t
                    // in order to let the instantiated types be correct. Note, that the JVM would
                    // not call the constructor
                    // Note, that we assume that there is a constructor
                    val constructor = cf.constructors.next()
                    calleesAndCallers.addCall(
                        definedMethod, declaredMethods(constructor), pc, UnknownParam, None
                    )

                }

                // call to `readResolve`
                val readResolve =
                    p.specialCall(t, t, isInterface = false, "readResolve", JustReturnsObject)
                calleesAndCallers.addCallOrFallback(
                    definedMethod, readResolve, pc,
                    ObjectType.Object.packageName,
                    ObjectType.Object,
                    "readResolve",
                    JustReturnsObject,
                    UnknownParam,
                    None
                )

                // call to `validateObject`
                if (ch.isSubtypeOf(t, ObjectInputValidationType)) {
                    val validateObject =
                        p.instanceCall(t, t, "validateObject", JustReturnsObject)
                    calleesAndCallers.addCallOrFallback(
                        definedMethod, validateObject, pc,
                        ObjectType.Object.packageName,
                        ObjectType.Object,
                        "validateObject",
                        JustReturnsObject,
                        UnknownParam,
                        None
                    )
                }
            }
        }

        if (!foundCast) {
            calleesAndCallers.addIncompleteCallSite(pc)
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

