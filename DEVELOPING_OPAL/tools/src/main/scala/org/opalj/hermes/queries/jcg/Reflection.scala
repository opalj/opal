/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries
package jcg

import org.opalj.ai.domain.l1.ArrayValues
import org.opalj.br.ObjectType
import org.opalj.br.MethodWithBody
import org.opalj.br.MethodDescriptor
import org.opalj.br.ArrayType
import org.opalj.br.Method
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.MethodDescriptor.JustReturnsObject
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.LoadClass
import org.opalj.br.instructions.LoadClass_W
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.da.ClassFile
import org.opalj.tac.TACode
import org.opalj.tac.DefaultTACAIKey
import org.opalj.tac.Assignment
import org.opalj.tac.Stmt
import org.opalj.tac.DUVar
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.Expr
import org.opalj.tac.NewArray
import org.opalj.tac.ArrayStore
import org.opalj.tac.VirtualMethodCall
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.ExprStmt
import org.opalj.tac.MonitorExit
import org.opalj.tac.MonitorEnter
import org.opalj.tac.If
import org.opalj.tac.InstanceOf
import org.opalj.tac.Compare
import org.opalj.tac.PutField
import org.opalj.tac.Checkcast
import org.opalj.tac.GetField
import org.opalj.tac.ArrayLoad
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.StaticFunctionCall
import org.opalj.tac.NonVirtualFunctionCall
import org.opalj.tac.Call
import org.opalj.tac.GetStatic
import org.opalj.tac.New
import org.opalj.tac.TACMethodParameter
import org.opalj.value.KnownTypedValue

/**
 * Groups features that use the java reflection API.
 *
 * @note The features represent the __REFLECTION__ test cases from the Call Graph Test Project
 *       (JCG).
 *
 * @author Dominik Helm
 */
class Reflection(implicit hermes: HermesConfig) extends DefaultFeatureQuery {

    type V = DUVar[KnownTypedValue]

    val ClassT = ObjectType.Class
    val MethodT = ObjectType("java/lang/reflect/Method")
    val ConstructorT = ObjectType("java/lang/reflect/Constructor")
    val FieldT = ObjectType("java/lang/reflect/Field")

    val Invoke = MethodDescriptor(
        IndexedSeq(ObjectType.Object, ArrayType(ObjectType.Object)),
        ObjectType.Object
    )
    val GetMethodMD = MethodDescriptor(IndexedSeq(ObjectType.String, ArrayType(ClassT)), MethodT)
    val NewInstanceMD = MethodDescriptor(ArrayType(ObjectType.Object), ObjectType.Object)
    val GetFieldMD = MethodDescriptor(ObjectType.String, FieldT)
    val FieldGetMD = MethodDescriptor(ObjectType.Object, ObjectType.Object)
    val ForName1MD = MethodDescriptor("(Ljava/lang/String;)Ljava/lang/Class;")
    val ForName3MD =
        MethodDescriptor("(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;")

    override def featureIDs: Seq[String] = {
        Seq(
            "TR1", /* 0 --- loading of class constant */
            "TR2", /* 1 --- invoke static method */
            "TR3", /* 2 --- invoke instance method */
            "TR4", /* 3 --- invoke instance method acquired by getMethod */
            "TR5", /* 4 --- invoke method with parameters */
            "TR6", /* 5 --- Constructor.newInstance */
            "TR7", /* 6 --- Class.newInstance */
            "TR8", /* 7 --- call on result of getDeclaredField */
            "TR9", /* 8 --- call on result of getField */
            "TR10", /* 9 -- Class.forName */
            "LRR1", /* 10 - multiple constants for param */
            "LRR2", /* 11 - constant(s) from StringBuilder for param */
            "CSR1+CSR2", /* 12 - value from unknown source for param */
            "LRR3+CSS3" /* 13 -- value from field for param */
        )
    }

    override def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Traversable[(ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {

        implicit val locations: Array[LocationsContainer[S]] =
            Array.fill(featureIDs.size)(new LocationsContainer[S])

        implicit val p: SomeProject = project

        implicit val tacai: Method ⇒ TACode[TACMethodParameter, DUVar[KnownTypedValue]] =
            project.get(DefaultTACAIKey)

        for {
            (classFile, source) ← project.projectClassFilesWithSources
            if !isInterrupted()
            classFileLocation = ClassFileLocation(source, classFile)
            method @ MethodWithBody(body) ← classFile.methods
            methodLocation = MethodLocation(classFileLocation, method)
            pcAndInstruction ← body collect {
                case i: LoadClass ⇒ i
                case i: LoadClass_W ⇒ i
                case i @ INVOKEVIRTUAL(MethodT, "invoke", Invoke) ⇒ i
                case i @ INVOKEVIRTUAL(ClassT, "getMethod", GetMethodMD) ⇒ i
                case i @ INVOKEVIRTUAL(ClassT, "getDeclaredMethod", GetMethodMD) ⇒ i
                case i @ INVOKEVIRTUAL(ClassT, "newInstance", JustReturnsObject) ⇒ i
                case i @ INVOKEVIRTUAL(ConstructorT, "newInstance", NewInstanceMD) ⇒ i
                case i @ INVOKEVIRTUAL(ClassT, "getDeclaredField", GetFieldMD) ⇒ i
                case i @ INVOKEVIRTUAL(ClassT, "getField", GetFieldMD) ⇒ i
                case i @ INVOKEVIRTUAL(FieldT, "get", FieldGetMD) ⇒ i
                case i @ INVOKESTATIC(ClassT, false, "forName", ForName1MD | ForName3MD) ⇒ i
            }
            TACode(_, stmts, pcToIndex, _, _, _) = tacai(method)
        } {
            val pc = pcAndInstruction.pc
            val l = InstructionLocation(methodLocation, pc)

            if (!pcAndInstruction.value.isMethodInvocationInstruction)
                locations(0) += l
            else {
                implicit val body: Array[Stmt[V]] = stmts

                val index = pcToIndex(pc)
                if (index != -1) {
                    val stmt = stmts(index)

                    val call =
                        if (stmt.astID == Assignment.ASTID) stmt.asAssignment.expr.asFunctionCall
                        else stmt.asExprStmt.expr.asFunctionCall

                    call.declaringClass match {
                        case MethodT      ⇒ handleInvoke(call.asVirtualFunctionCall, l)
                        case ConstructorT ⇒ locations(5 /* Constructor.newInstance */ ) += l
                        case ClassT ⇒
                            call.name match {
                                case "getMethod" ⇒
                                    if (stmt.astID == Assignment.ASTID &&
                                        methodUsedForInvocation(index, stmt.asAssignment)) {
                                        locations(3 /* getMethod */ ) += l // invoke called directly
                                        handleParameterSources(call, l)
                                    }
                                case "getDeclaredMethod" ⇒
                                    if (stmt.astID == Assignment.ASTID &&
                                        methodUsedForInvocation(index, stmt.asAssignment)) {
                                        handleParameterSources(call, l)
                                    }
                                case "newInstance" ⇒ locations(6 /* Class.newInstance */ ) += l
                                case "getDeclaredField" ⇒
                                    if (stmt.astID == Assignment.ASTID &&
                                        getFieldUsedForInvokation(index, stmt.asAssignment)) {
                                        handleParameterSources(call, l)
                                    }
                                case "getField" ⇒
                                    if (stmt.astID == Assignment.ASTID &&
                                        getFieldUsedForInvokation(index, stmt.asAssignment)) {
                                        locations(8 /* getField */ ) += l
                                        handleParameterSources(call, l)
                                    }
                                case "forName" ⇒
                                    locations(9 /* Class.forName */ ) += l
                                    handleParameterSources(call, l)
                            }
                        case FieldT ⇒
                            if (stmt.astID == Assignment.ASTID)
                                if (fieldUsedForInvocation(index, stmt.asAssignment))
                                    locations(7 /* Field.get */ ) += l
                        case _ ⇒ throw new UnknownError("will not happen")
                    }
                }
            }
        }

        locations;
    }

    def handleInvoke[S](call: VirtualFunctionCall[V], l: Location[S])(
        implicit
        locations: Array[LocationsContainer[S]]
    ): Unit = {
        if (call.params.head.asVar.value.asReferenceValue.isNull.isYes)
            locations(1 /* static invoke */ ) += l
        else
            locations(2 /* instance invoke */ ) += l

        val paramTypes = call.params(1).asVar.value
        if (paramTypes.asReferenceValue.allValues.exists { v ⇒
            v.isNull.isYesOrUnknown || v.asInstanceOf[ArrayValues#DomainArrayValue].length.contains(0)
        })
            locations(4 /* method with parameters */ ) += l
    }

    def handleParameterSources[S](call: Call[V], l: Location[S])(
        implicit
        locations: Array[LocationsContainer[S]],
        stmts:     Array[Stmt[V]]
    ): Unit = {
        def simpleDefinition(definedBy: IntTrieSet): Option[Assignment[V]] = {
            if (definedBy.size == 1 && definedBy.head > 0 &&
                stmts(definedBy.head).astID == Assignment.ASTID)
                Some(stmts(definedBy.head).asAssignment)
            else None
        }

        def isNonEscapingStringBuilder(stmt: Assignment[V]): Boolean = {
            def isNonEscaping(stringBuilder: V): Boolean = {
                stringBuilder.usedBy.forall { useSite ⇒
                    val use = stmts(useSite)
                    use.astID match {
                        case Assignment.ASTID ⇒
                            use.asAssignment.expr.isVirtualFunctionCall &&
                                (use.asAssignment.expr.asVirtualFunctionCall.name == "append" ||
                                    use.asAssignment.expr.asVirtualFunctionCall.name == "toString")
                        case ExprStmt.ASTID ⇒
                            use.asExprStmt.expr.isVirtualFunctionCall &&
                                (use.asExprStmt.expr.asVirtualFunctionCall.name == "append" ||
                                    use.asExprStmt.expr.asVirtualFunctionCall.name == "toString")
                        case NonVirtualMethodCall.ASTID ⇒
                            use.asNonVirtualMethodCall.name == "<init>"
                        case _ ⇒ false // might escape here
                    }
                }
            }

            stmt match {
                case Assignment(_, sb, _: New) ⇒ isNonEscaping(sb)
                case Assignment(_, sb, VirtualFunctionCall(_, ObjectType.StringBuilder, false, "append", _, receiver, params)) ⇒
                    val stringBuilder = simpleDefinition(receiver.asVar.definedBy)
                    stringBuilder.exists(isNonEscapingStringBuilder) && isNonEscaping(sb)
                case _ ⇒ false
            }
        }

        val definedBy = call.params.head.asVar.definedBy
        if (simpleDefinition(definedBy).exists(_.expr.isConst)) {
            /* nothing to do, trivial string constant */
        } else {
            definedBy.foreach { defSite ⇒
                if (defSite < 0) {
                    locations(12 /* string string */ ) += l
                } else {
                    val definition = stmts(defSite).asAssignment.expr
                    if (definition.isConst) {
                        locations(10 /* multiple constants */ ) += l
                    } else if (definition.isGetField || definition.isGetStatic) {
                        locations(13 /* field */ ) += l
                    } else definition match {
                        case VirtualFunctionCall(_, ObjectType.StringBuilder, false, "toString", MethodDescriptor.JustReturnsString, receiver, _) ⇒
                            val stringBuilder = simpleDefinition(receiver.asVar.definedBy)
                            if (stringBuilder.exists(isNonEscapingStringBuilder)) {
                                locations(11 /* StringBuilder */ ) += l
                            } else {
                                locations(12 /* string unknown */ )
                            }
                        case _ ⇒ locations(12 /* string unknown */ )
                    }
                }
            }
        }
    }

    def methodUsedForInvocation[S](
        pc:   Int,
        stmt: Assignment[V]
    )(
        implicit
        project: SomeProject,
        tacai:   Method ⇒ TACode[TACMethodParameter, DUVar[KnownTypedValue]],
        stmts:   Array[Stmt[V]]
    ): Boolean = {
        if (stmt.targetVar.usedBy.exists { useSite ⇒
            val stmt = stmts(useSite)
            stmt.astID match {
                case Assignment.ASTID ⇒
                    stmt.asAssignment.expr.isVirtualFunctionCall &&
                        stmt.asAssignment.expr.asVirtualFunctionCall.name == "invoke"
                case ExprStmt.ASTID ⇒
                    stmt.asExprStmt.expr.isVirtualFunctionCall &&
                        stmt.asExprStmt.expr.asVirtualFunctionCall.name == "invoke"
                case _ ⇒ false
            }
        }) {
            true
        } else
            stmt.targetVar.usedBy.exists { useSite ⇒
                val stmt = stmts(useSite)
                stmt.astID match {
                    case VirtualMethodCall.ASTID | NonVirtualMethodCall.ASTID | StaticMethodCall.ASTID ⇒
                        mayUse(stmt.asMethodCall.params, pc) &&
                            projectContainsNonLocalCall(MethodT, "invoke")
                    case Assignment.ASTID ⇒
                        stmt.asAssignment.expr.astID match {
                            case VirtualFunctionCall.ASTID | NonVirtualFunctionCall.ASTID |
                                StaticFunctionCall.ASTID ⇒
                                mayUse(stmt.asAssignment.expr.asFunctionCall.params, pc) &&
                                    projectContainsNonLocalCall(MethodT, "invoke")
                            case InstanceOf.ASTID | Compare.ASTID ⇒ false
                            case _ ⇒
                                projectContainsNonLocalCall(MethodT, "invoke")
                        }
                    case ExprStmt.ASTID ⇒
                        stmt.asExprStmt.expr.astID match {
                            case VirtualFunctionCall.ASTID | NonVirtualFunctionCall.ASTID |
                                StaticFunctionCall.ASTID ⇒
                                mayUse(stmt.asExprStmt.expr.asFunctionCall.params, pc) &&
                                    projectContainsNonLocalCall(MethodT, "invoke")
                            case InstanceOf.ASTID | Compare.ASTID ⇒ false
                            case _ ⇒
                                projectContainsNonLocalCall(MethodT, "invoke")
                        }
                    case MonitorEnter.ASTID | MonitorExit.ASTID | If.ASTID | Checkcast.ASTID ⇒ false
                    case _ ⇒
                        projectContainsNonLocalCall(MethodT, "invoke")
                }
            }
    }

    def fieldUsedForInvocation[S](
        pc:         Int,
        assignment: Assignment[V]
    )(implicit stmts: Array[Stmt[V]]): Boolean = {
        if (assignment.targetVar.usedBy.exists { useSite ⇒
            val stmt = stmts(useSite)
            stmt.astID match {
                case VirtualMethodCall.ASTID ⇒
                    stmt.asVirtualMethodCall.receiver.asVar.definedBy.contains(pc)
                case Assignment.ASTID ⇒
                    stmt.asAssignment.expr.isVirtualFunctionCall &&
                        stmt.asAssignment.expr.asVirtualFunctionCall.receiver.asVar.definedBy.contains(pc)
                case ExprStmt.ASTID ⇒
                    stmt.asExprStmt.expr.isVirtualFunctionCall &&
                        stmt.asExprStmt.expr.asVirtualFunctionCall.receiver.asVar.definedBy.contains(pc)
                case _ ⇒ false
            }
        }) {
            true // direct invocation
        } else
            // Value loaded from field may escape and may be receiver of a call somewhere in project
            assignment.targetVar.usedBy.exists { useSite ⇒
                val stmt = stmts(useSite)
                stmt.astID match {
                    case PutField.ASTID   ⇒ stmt.asPutField.value.asVar.definedBy.contains(pc)
                    case ArrayStore.ASTID ⇒ stmt.asArrayStore.value.asVar.definedBy.contains(pc)
                    case Assignment.ASTID ⇒
                        stmt.asAssignment.expr.astID match {
                            case InstanceOf.ASTID | Compare.ASTID |
                                GetField.ASTID | ArrayLoad.ASTID ⇒
                                false
                            case _ ⇒ true
                        }
                    case ExprStmt.ASTID ⇒
                        stmt.asExprStmt.expr.astID match {
                            case InstanceOf.ASTID | Compare.ASTID |
                                GetField.ASTID | ArrayLoad.ASTID ⇒
                                false
                            case _ ⇒ true
                        }
                    case MonitorEnter.ASTID | MonitorExit.ASTID | If.ASTID | Checkcast.ASTID ⇒ false
                    case _ ⇒ true
                }
            }
    }

    def getFieldUsedForInvokation[S](
        pc:   Int,
        stmt: Assignment[V]
    )(
        implicit
        project: SomeProject,
        tacai:   Method ⇒ TACode[TACMethodParameter, DUVar[KnownTypedValue]],
        stmts:   Array[Stmt[V]]
    ): Boolean = {
        if (stmt.targetVar.usedBy.exists { useSite ⇒
            val stmt = stmts(useSite)
            stmt.astID match {
                case Assignment.ASTID ⇒
                    stmt.asAssignment.expr.isVirtualFunctionCall &&
                        stmt.asAssignment.expr.asVirtualFunctionCall.name == "get"
                    fieldUsedForInvocation(useSite, stmt.asAssignment)
                case ExprStmt.ASTID ⇒
                    stmt.asExprStmt.expr.isVirtualFunctionCall &&
                        stmt.asExprStmt.expr.asVirtualFunctionCall.name == "get" &&
                        fieldUsedForInvocation(useSite, stmt.asAssignment)
                case _ ⇒ false
            }
        }) {
            true
        } else
            // Field may escape and there is a non-local Field.get that might lead to an invocation
            stmt.targetVar.usedBy.exists { useSite ⇒
                val stmt = stmts(useSite)
                stmt.astID match {
                    case VirtualMethodCall.ASTID | NonVirtualMethodCall.ASTID |
                        StaticMethodCall.ASTID ⇒
                        mayUse(stmt.asMethodCall.params, pc) &&
                            projectContainsNonLocalCall(FieldT, "get")
                    case Assignment.ASTID ⇒
                        stmt.asAssignment.expr.astID match {
                            case VirtualFunctionCall.ASTID | NonVirtualFunctionCall.ASTID |
                                StaticFunctionCall.ASTID ⇒
                                mayUse(stmt.asAssignment.expr.asFunctionCall.params, pc) &&
                                    projectContainsNonLocalCall(FieldT, "get")
                            case InstanceOf.ASTID | Compare.ASTID ⇒ false
                            case _ ⇒
                                projectContainsNonLocalCall(FieldT, "get")
                        }
                    case ExprStmt.ASTID ⇒
                        stmt.asExprStmt.expr.astID match {
                            case VirtualFunctionCall.ASTID | NonVirtualFunctionCall.ASTID |
                                StaticFunctionCall.ASTID ⇒
                                mayUse(stmt.asExprStmt.expr.asFunctionCall.params, pc) &&
                                    projectContainsNonLocalCall(FieldT, "get")
                            case InstanceOf.ASTID | Compare.ASTID ⇒ false
                            case _ ⇒
                                projectContainsNonLocalCall(FieldT, "get")
                        }
                    case MonitorEnter.ASTID | MonitorExit.ASTID | If.ASTID | Checkcast.ASTID ⇒ false
                    case _ ⇒
                        projectContainsNonLocalCall(FieldT, "get")
                }
            }
    }

    def mayUse(uvars: Seq[Expr[V]], pc: Int): Boolean = {
        uvars.exists(_.asVar.definedBy.contains(pc))
    }

    def projectContainsNonLocalCall(
        declType: ObjectType,
        name:     String
    )(
        implicit
        project: SomeProject,
        tacai:   Method ⇒ TACode[TACMethodParameter, DUVar[KnownTypedValue]]
    ): Boolean = {
        project.allMethodsWithBody.exists { method ⇒
            val invokes = method.body.get.collect {
                case i @ INVOKEVIRTUAL(`declType`, `name`, _) ⇒ i
            }
            if (invokes.isEmpty) {
                false
            } else {
                val TACode(_, stmts, pcToIndex, _, _, _) = tacai(method)
                invokes.exists { pcAndInvocation ⇒
                    val stmt = stmts(pcToIndex(pcAndInvocation.pc))
                    val call =
                        if (stmt.astID == Assignment.ASTID)
                            stmt.asAssignment.expr.asVirtualFunctionCall
                        else
                            stmt.asExprStmt.expr.asVirtualFunctionCall
                    call.receiver.asVar.definedBy.exists { defSite ⇒
                        defSite < 0 || stmts(defSite).astID != New.ASTID
                    }
                }
            }
        }
    }

    def isConstant(uvar: V, allowArray: Boolean = true)(implicit stmts: Array[Stmt[V]]): Boolean = {
        uvar.definedBy.forall { definition ⇒
            if (definition < 0) false
            else {
                val assignment = stmts(definition).asAssignment
                val expr = assignment.expr
                expr.isConst || allowArray && expr.isNewArray &&
                    assignment.targetVar.usedBy.forall { usesite ⇒
                        val stmt = stmts(usesite)
                        stmt.astID != ArrayStore.ASTID ||
                            isConstant(stmt.asArrayStore.value.asVar, allowArray = false)
                    }
            }
        }
    }
}
