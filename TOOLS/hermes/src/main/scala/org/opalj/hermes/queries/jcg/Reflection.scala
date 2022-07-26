/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries
package jcg

import org.opalj.ai.domain.l1.ArrayValues
import org.opalj.br.MethodDescriptor.JustReturnsObject
import org.opalj.br._
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.LoadClass
import org.opalj.br.instructions.LoadClass_W
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.da.ClassFile
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger
import org.opalj.tac._
import org.opalj.value.ValueInformation

import scala.collection.immutable.ArraySeq

/**
 * Groups features that use the java reflection API.
 *
 * @note The features represent the __REFLECTION__ test cases from the Call Graph Test Project
 *       (JCG).
 *
 * @author Dominik Helm
 */
class Reflection(implicit hermes: HermesConfig) extends DefaultFeatureQuery {

    type V = DUVar[ValueInformation]

    val ClassT = ObjectType.Class
    val MethodT = ObjectType("java/lang/reflect/Method")
    val ConstructorT = ObjectType("java/lang/reflect/Constructor")
    val FieldT = ObjectType("java/lang/reflect/Field")

    val PropertiesT = ObjectType("java/util/Properties")

    val Invoke = MethodDescriptor(
        ArraySeq(ObjectType.Object, ArrayType(ObjectType.Object)),
        ObjectType.Object
    )
    val GetMethodMD = MethodDescriptor(ArraySeq(ObjectType.String, ArrayType(ClassT)), MethodT)
    val NewInstanceMD = MethodDescriptor(ArrayType(ObjectType.Object), ObjectType.Object)
    val GetFieldMD = MethodDescriptor(ObjectType.String, FieldT)
    val FieldGetMD = MethodDescriptor(ObjectType.Object, ObjectType.Object)
    val ForName1MD = MethodDescriptor("(Ljava/lang/String;)Ljava/lang/Class;")
    val ForName3MD =
        MethodDescriptor("(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;")

    val GetProperty1MD = MethodDescriptor(ObjectType.String, ObjectType.String)
    val GetProperty2MD =
        MethodDescriptor(ArraySeq(ObjectType.String, ObjectType.String), ObjectType.String)
    val GetMD = MethodDescriptor(ObjectType.Object, ObjectType.Object)

    override def featureIDs: Seq[String] = {
        Seq(
            "TR1", /* 0 --- invoke static method */
            "TR2", /* 1 --- invoke instance method */
            "TR3", /* 2 --- invoke instance method acquired by getMethod */
            "TR4", /* 3 --- invoke method with parameters */
            "TR5", /* 4 --- Constructor.newInstance */
            "TR6", /* 5 --- Class.newInstance */
            "TR7", /* 6 --- call on result of getDeclaredField */
            "TR8", /* 7 --- call on result of getField */
            "TR9", /* 8 --- Class.forName */
            "LRR1", /* 9 -- multiple constants for param */
            "LRR2", /* 10 - constant(s) from StringBuilder for param */
            "CSR1+CSR2", /* 11 - value from unknown source for param */
            "LRR3+CSR3", /* 12 -- value from field for param */
            "CSR4" /* 13 -- value from Properties */
        )
    }

    override def evaluate[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(ClassFile, S)]
    ): IndexedSeq[LocationsContainer[S]] = {

        implicit val locations: Array[LocationsContainer[S]] =
            Array.fill(featureIDs.size)(new LocationsContainer[S])

        implicit val p: SomeProject = project

        implicit val tacai: Method => TACode[TACMethodParameter, V] =
            project.get(LazyTACUsingAIKey)

        for {
            (classFile, source) <- project.projectClassFilesWithSources
            if !isInterrupted()
            classFileLocation = ClassFileLocation(source, classFile)
            method @ MethodWithBody(body) <- classFile.methods
            methodLocation = MethodLocation(classFileLocation, method)
            pcAndInstruction <- body collect ({
                case i: LoadClass => i
                case i: LoadClass_W => i
                case i @ INVOKEVIRTUAL(MethodT, "invoke", Invoke) => i
                case i @ INVOKEVIRTUAL(ClassT, "getMethod", GetMethodMD) => i
                case i @ INVOKEVIRTUAL(ClassT, "getDeclaredMethod", GetMethodMD) => i
                case i @ INVOKEVIRTUAL(ClassT, "newInstance", JustReturnsObject) => i
                case i @ INVOKEVIRTUAL(ConstructorT, "newInstance", NewInstanceMD) => i
                case i @ INVOKEVIRTUAL(ClassT, "getDeclaredField", GetFieldMD) => i
                case i @ INVOKEVIRTUAL(ClassT, "getField", GetFieldMD) => i
                case i @ INVOKEVIRTUAL(FieldT, "get", FieldGetMD) => i
                case i @ INVOKESTATIC(ClassT, false, "forName", ForName1MD | ForName3MD) => i
            }: PartialFunction[Instruction, Instruction])
        } {
            val tac = try {
                tacai(method)
            } catch {
                case e: Exception =>
                    implicit val logContext: LogContext = p.logContext
                    OPALLogger.error("analysis", s"unable to create 3-address code for: ${method.toJava}")
                    throw e
            }
            val pc = pcAndInstruction.pc
            val l = InstructionLocation(methodLocation, pc)

            if (pcAndInstruction.value.isMethodInvocationInstruction) {
                implicit val body: Array[Stmt[V]] = tac.stmts

                val index = tac.properStmtIndexForPC(pc)
                if (index != -1) {
                    val stmt = tac.stmts(index)

                    val call =
                        if (stmt.astID == Assignment.ASTID) stmt.asAssignment.expr.asFunctionCall
                        else stmt.asExprStmt.expr.asFunctionCall

                    call.declaringClass match {
                        case MethodT      => handleInvoke(call.asVirtualFunctionCall, l)
                        case ConstructorT => locations(4 /* Constructor.newInstance */ ) += l
                        case ClassT =>
                            call.name match {
                                case "getMethod" =>
                                    if (stmt.astID == Assignment.ASTID &&
                                        methodUsedForInvocation(index, stmt.asAssignment)) {
                                        locations(2 /* getMethod */ ) += l // invoke called directly
                                        handleParameterSources(call, l)
                                    }
                                case "getDeclaredMethod" =>
                                    if (stmt.astID == Assignment.ASTID &&
                                        methodUsedForInvocation(index, stmt.asAssignment)) {
                                        handleParameterSources(call, l)
                                    }
                                case "newInstance" => locations(5 /* Class.newInstance */ ) += l
                                case "getDeclaredField" =>
                                    if (stmt.astID == Assignment.ASTID &&
                                        getFieldUsedForInvokation(index, stmt.asAssignment)) {
                                        handleParameterSources(call, l)
                                    }
                                case "getField" =>
                                    if (stmt.astID == Assignment.ASTID &&
                                        getFieldUsedForInvokation(index, stmt.asAssignment)) {
                                        locations(7 /* getField */ ) += l
                                        handleParameterSources(call, l)
                                    }
                                case "forName" =>
                                    locations(8 /* Class.forName */ ) += l
                                    handleParameterSources(call, l)
                            }
                        case FieldT =>
                            if (stmt.astID == Assignment.ASTID)
                                if (fieldUsedForInvocation(index, stmt.asAssignment))
                                    locations(6 /* Field.get */ ) += l
                        case _ => throw new UnknownError("will not happen")
                    }
                }
            }
        }

        ArraySeq.unsafeWrapArray(locations)
    }

    def handleInvoke[S](call: VirtualFunctionCall[V], l: Location[S])(
        implicit
        locations: Array[LocationsContainer[S]]
    ): Unit = {
        if (call.params.head.asVar.value.asReferenceValue.isNull.isYes)
            locations(0 /* static invoke */ ) += l
        else
            locations(1 /* instance invoke */ ) += l

        val paramTypes = call.params(1).asVar.value
        if (paramTypes.asReferenceValue.allValues.exists { v =>
            v.isNull.isYesOrUnknown || v.asInstanceOf[ArrayValues#DomainArrayValue].length.contains(0)
        })
            locations(3 /* method with parameters */ ) += l
    }

    def handleParameterSources[S](call: Call[V], l: Location[S])(
        implicit
        project:   SomeProject,
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
                stringBuilder.usedBy.forall { useSite =>
                    val use = stmts(useSite)
                    use.astID match {
                        case Assignment.ASTID =>
                            use.asAssignment.expr.isVirtualFunctionCall &&
                                (use.asAssignment.expr.asVirtualFunctionCall.name == "append" ||
                                    use.asAssignment.expr.asVirtualFunctionCall.name == "toString")
                        case ExprStmt.ASTID =>
                            use.asExprStmt.expr.isVirtualFunctionCall &&
                                (use.asExprStmt.expr.asVirtualFunctionCall.name == "append" ||
                                    use.asExprStmt.expr.asVirtualFunctionCall.name == "toString")
                        case NonVirtualMethodCall.ASTID =>
                            use.asNonVirtualMethodCall.name == "<init>"
                        case _ => false // might escape here
                    }
                }
            }

            stmt match {
                case Assignment(_, sb, _: New) => isNonEscaping(sb)
                case Assignment(_, sb, VirtualFunctionCall(_, ObjectType.StringBuilder, false, "append", _, receiver, _)) =>
                    val stringBuilder = simpleDefinition(receiver.asVar.definedBy)
                    stringBuilder.exists(isNonEscapingStringBuilder) && isNonEscaping(sb)
                case _ => false
            }
        }

        val definedBy = call.params.head.asVar.definedBy
        if (simpleDefinition(definedBy).exists(_.expr.isConst)) {
            /* nothing to do, trivial string constant */
        } else {
            val ch = project.classHierarchy
            definedBy.foreach { defSite =>
                if (defSite < 0) {
                    locations(11 /* string string */ ) += l
                } else {
                    val definition = stmts(defSite).asAssignment.expr
                    if (definition.isConst) {
                        locations(9 /* multiple constants */ ) += l
                    } else if (definition.isGetField || definition.isGetStatic) {
                        locations(12 /* field */ ) += l
                    } else definition match {
                        case VirtualFunctionCall(_, ObjectType.StringBuilder, false, "toString", MethodDescriptor.JustReturnsString, receiver, _) =>
                            val stringBuilder = simpleDefinition(receiver.asVar.definedBy)
                            if (stringBuilder.exists(isNonEscapingStringBuilder)) {
                                locations(10 /* StringBuilder */ ) += l
                            } else {
                                locations(11 /* string unknown */ ) += l
                            }
                        case StaticFunctionCall(_, ObjectType.System, _, "getProperty", GetProperty1MD, _) =>
                            locations(13 /* string from Properties */ ) += l
                        case VirtualFunctionCall(_, dc, _, "getProperty", GetProperty1MD, _, _) if ch.isSubtypeOf(dc, PropertiesT) =>
                            locations(13 /* string from Properties */ ) += l
                        case VirtualFunctionCall(_, dc, _, "getProperty", GetProperty2MD, _, _) if ch.isSubtypeOf(dc, PropertiesT) =>
                            locations(13 /* string from Properties */ ) += l
                        case VirtualFunctionCall(_, dc, _, "get", GetMD, _, _) if ch.isSubtypeOf(dc, PropertiesT) =>
                            locations(13 /* string from Properties */ ) += l
                        case _ => locations(11 /* string unknown */ ) += l
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
        tacai:   Method => TACode[TACMethodParameter, V],
        stmts:   Array[Stmt[V]]
    ): Boolean = {
        if (stmt.targetVar.usedBy.exists { useSite =>
            val stmt = stmts(useSite)
            stmt.astID match {
                case Assignment.ASTID =>
                    stmt.asAssignment.expr.isVirtualFunctionCall &&
                        stmt.asAssignment.expr.asVirtualFunctionCall.name == "invoke"
                case ExprStmt.ASTID =>
                    stmt.asExprStmt.expr.isVirtualFunctionCall &&
                        stmt.asExprStmt.expr.asVirtualFunctionCall.name == "invoke"
                case _ => false
            }
        }) {
            true
        } else
            stmt.targetVar.usedBy.exists { useSite =>
                val stmt = stmts(useSite)
                stmt.astID match {
                    case VirtualMethodCall.ASTID | NonVirtualMethodCall.ASTID | StaticMethodCall.ASTID =>
                        mayUse(stmt.asMethodCall.params, pc) &&
                            projectContainsNonLocalCall(MethodT, "invoke")
                    case Assignment.ASTID =>
                        stmt.asAssignment.expr.astID match {
                            case VirtualFunctionCall.ASTID | NonVirtualFunctionCall.ASTID |
                                StaticFunctionCall.ASTID =>
                                mayUse(stmt.asAssignment.expr.asFunctionCall.params, pc) &&
                                    projectContainsNonLocalCall(MethodT, "invoke")
                            case InstanceOf.ASTID | Compare.ASTID => false
                            case _ =>
                                projectContainsNonLocalCall(MethodT, "invoke")
                        }
                    case ExprStmt.ASTID =>
                        stmt.asExprStmt.expr.astID match {
                            case VirtualFunctionCall.ASTID | NonVirtualFunctionCall.ASTID |
                                StaticFunctionCall.ASTID =>
                                mayUse(stmt.asExprStmt.expr.asFunctionCall.params, pc) &&
                                    projectContainsNonLocalCall(MethodT, "invoke")
                            case InstanceOf.ASTID | Compare.ASTID => false
                            case _ =>
                                projectContainsNonLocalCall(MethodT, "invoke")
                        }
                    case MonitorEnter.ASTID | MonitorExit.ASTID | If.ASTID | Checkcast.ASTID => false
                    case _ =>
                        projectContainsNonLocalCall(MethodT, "invoke")
                }
            }
    }

    def fieldUsedForInvocation[S](
        pc:         Int,
        assignment: Assignment[V]
    )(implicit stmts: Array[Stmt[V]]): Boolean = {
        if (assignment.targetVar.usedBy.exists { useSite =>
            val stmt = stmts(useSite)
            stmt.astID match {
                case VirtualMethodCall.ASTID =>
                    stmt.asVirtualMethodCall.receiver.asVar.definedBy.contains(pc)
                case Assignment.ASTID =>
                    stmt.asAssignment.expr.isVirtualFunctionCall &&
                        stmt.asAssignment.expr.asVirtualFunctionCall.receiver.asVar.definedBy.contains(pc)
                case ExprStmt.ASTID =>
                    stmt.asExprStmt.expr.isVirtualFunctionCall &&
                        stmt.asExprStmt.expr.asVirtualFunctionCall.receiver.asVar.definedBy.contains(pc)
                case _ => false
            }
        }) {
            true // direct invocation
        } else
            // Value loaded from field may escape and may be receiver of a call somewhere in project
            assignment.targetVar.usedBy.exists { useSite =>
                val stmt = stmts(useSite)
                stmt.astID match {
                    case PutField.ASTID   => stmt.asPutField.value.asVar.definedBy.contains(pc)
                    case ArrayStore.ASTID => stmt.asArrayStore.value.asVar.definedBy.contains(pc)
                    case Assignment.ASTID =>
                        stmt.asAssignment.expr.astID match {
                            case InstanceOf.ASTID | Compare.ASTID |
                                GetField.ASTID | ArrayLoad.ASTID =>
                                false
                            case _ => true
                        }
                    case ExprStmt.ASTID =>
                        stmt.asExprStmt.expr.astID match {
                            case InstanceOf.ASTID | Compare.ASTID |
                                GetField.ASTID | ArrayLoad.ASTID =>
                                false
                            case _ => true
                        }
                    case MonitorEnter.ASTID | MonitorExit.ASTID | If.ASTID | Checkcast.ASTID => false
                    case _ => true
                }
            }
    }

    def getFieldUsedForInvokation[S](
        pc:   Int,
        stmt: Assignment[V]
    )(
        implicit
        project: SomeProject,
        tacai:   Method => TACode[TACMethodParameter, V],
        stmts:   Array[Stmt[V]]
    ): Boolean = {
        if (stmt.targetVar.usedBy.exists { useSite =>
            val stmt = stmts(useSite)
            stmt.astID match {
                case Assignment.ASTID =>
                    stmt.asAssignment.expr.isVirtualFunctionCall &&
                        stmt.asAssignment.expr.asVirtualFunctionCall.name == "get"
                    fieldUsedForInvocation(useSite, stmt.asAssignment)
                case ExprStmt.ASTID =>
                    stmt.asExprStmt.expr.isVirtualFunctionCall &&
                        stmt.asExprStmt.expr.asVirtualFunctionCall.name == "get" &&
                        fieldUsedForInvocation(useSite, stmt.asAssignment)
                case _ => false
            }
        }) {
            true
        } else
            // Field may escape and there is a non-local Field.get that might lead to an invocation
            stmt.targetVar.usedBy.exists { useSite =>
                val stmt = stmts(useSite)
                stmt.astID match {
                    case VirtualMethodCall.ASTID | NonVirtualMethodCall.ASTID |
                        StaticMethodCall.ASTID =>
                        mayUse(stmt.asMethodCall.params, pc) &&
                            projectContainsNonLocalCall(FieldT, "get")
                    case Assignment.ASTID =>
                        stmt.asAssignment.expr.astID match {
                            case VirtualFunctionCall.ASTID | NonVirtualFunctionCall.ASTID |
                                StaticFunctionCall.ASTID =>
                                mayUse(stmt.asAssignment.expr.asFunctionCall.params, pc) &&
                                    projectContainsNonLocalCall(FieldT, "get")
                            case InstanceOf.ASTID | Compare.ASTID => false
                            case _ =>
                                projectContainsNonLocalCall(FieldT, "get")
                        }
                    case ExprStmt.ASTID =>
                        stmt.asExprStmt.expr.astID match {
                            case VirtualFunctionCall.ASTID | NonVirtualFunctionCall.ASTID |
                                StaticFunctionCall.ASTID =>
                                mayUse(stmt.asExprStmt.expr.asFunctionCall.params, pc) &&
                                    projectContainsNonLocalCall(FieldT, "get")
                            case InstanceOf.ASTID | Compare.ASTID => false
                            case _ =>
                                projectContainsNonLocalCall(FieldT, "get")
                        }
                    case MonitorEnter.ASTID | MonitorExit.ASTID | If.ASTID | Checkcast.ASTID => false
                    case _ =>
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
        tacai:   Method => TACode[TACMethodParameter, V]
    ): Boolean = {
        project.allMethodsWithBody.exists { method =>
            val invokes = method.body.get.collect({
                case i @ INVOKEVIRTUAL(`declType`, `name`, _) => i
            }: PartialFunction[Instruction, INVOKEVIRTUAL])
            if (invokes.isEmpty) {
                false
            } else {
                val tac = tacai(method)
                val stmts = tac.stmts
                invokes.exists { pcAndInvocation =>
                    val stmt = stmts(tac.properStmtIndexForPC(pcAndInvocation.pc))
                    val call =
                        if (stmt.astID == Assignment.ASTID)
                            stmt.asAssignment.expr.asVirtualFunctionCall
                        else
                            stmt.asExprStmt.expr.asVirtualFunctionCall
                    call.receiver.asVar.definedBy.exists { defSite =>
                        defSite < 0 || stmts(defSite).astID != New.ASTID
                    }
                }
            }
        }
    }
}
