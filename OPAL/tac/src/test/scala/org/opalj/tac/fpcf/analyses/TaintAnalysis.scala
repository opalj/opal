/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses

import org.opalj.fpcf.PropertyKey
import org.opalj.fpcf.PropertyStore
import org.opalj.br.DeclaredMethod
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.tac._
import org.opalj.tac.fpcf.analyses.AbstractIFDSAnalysis.V
import org.opalj.tac.fpcf.properties.IFDSProperty
import org.opalj.tac.fpcf.properties.IFDSPropertyMetaInformation

trait Fact extends AbstractIFDSFact
case object NullFact extends Fact with AbstractIFDSNullFact
case class Variable(index: Int) extends Fact
case class ArrayElement(index: Int, element: Int) extends Fact
case class InstanceField(index: Int, classType: ObjectType, fieldName: String) extends Fact
case class FlowFact(flow: Seq[Method]) extends Fact {
    override val hashCode: Int = {
        var r = 1
        flow.foreach(f ⇒ r = (r + f.hashCode()) * 31)
        r
    }
}

/**
 * An analysis that checks, if the return value of a `source` method can flow to the parameter of a
 * `sink` method.
 *
 * @param project The project, that is analyzed
 * @author Mario Trageser
 */
class TaintAnalysis private (implicit val project: SomeProject) extends AbstractIFDSAnalysis[Fact] {

    override val propertyKey: IFDSPropertyMetaInformation[Fact] = Taint

    /**
     * The analysis starts at the TaintAnalysisTestClass.
     * TODO Make the entry points variable
     */
    override val entryPoints: Map[DeclaredMethod, Fact] = p.allProjectClassFiles.filter(classFile ⇒
        classFile.thisType.fqn == "org/opalj/fpcf/fixtures/taint/TaintAnalysisTestClass")
        .flatMap(classFile ⇒ classFile.methods)
        .filter(method ⇒ method.isPublic)
        .map(method ⇒ declaredMethods(method) → NullFact).toMap

    override def createPropertyValue(result: Map[Statement, Set[Fact]]): IFDSProperty[Fact] = {
        new Taint(result)
    }

    /**
     * If a variable gets assigned a tainted value, the variable will be tainted.
     */
    override def normalFlow(statement: Statement, successor: Statement, in: Set[Fact]): Set[Fact] =
        statement.stmt.astID match {
            case Assignment.ASTID ⇒
                handleAssignment(statement, in)
            case ArrayStore.ASTID ⇒
                val store = statement.stmt.asArrayStore
                val definedBy = store.arrayRef.asVar.definedBy
                val arrayIndex = getIntConstant(store.index, statement.code)
                if (isTainted(store.value, in)) {
                    if (arrayIndex.isDefined)
                        // Taint a known array index
                        definedBy.foldLeft(in) { (c, n) ⇒
                            c + ArrayElement(n, arrayIndex.get)
                        }
                    else
                        // Taint the whole array if the index is unknown
                        definedBy.foldLeft(in) { (c, n) ⇒
                            c + Variable(n)
                        }
                } else if (arrayIndex.isDefined && definedBy.size == 1)
                    // Untaint if possible
                    in - ArrayElement(definedBy.head, arrayIndex.get)
                else in
            case _ ⇒ in
        }

    /**
     * Handles assignment statements. Propagates all incoming facts.
     * A new fact for the assigned variable will be created,
     * if the expression contains a tainted variable.
     *
     * @param statement The assignment
     * @param in The incoming facts
     * @return The incoming and the new facts.
     *
     * TODO Why don't we untaint the assigned variable? (Do not forget that source and target variable can be the same)
     */
    def handleAssignment(statement: Statement, in: Set[Fact]): Set[Fact] = {
        in ++ createNewTaints(statement.stmt.asAssignment.expr, statement, in)
    }

    /**
     * Creates new facts for an assignment. A new fact for the assigned variable will be created,
     * if the expression contains a tainted variable
     *
     * @param expr The source expression of the assignment
     * @param statement The assignment statement
     * @param in The incoming facts
     * @return The new facts, created by the assignment
     */
    def createNewTaints(expr: Expr[V], statement: Statement, in: Set[Fact]): Set[Fact] =
        expr.astID match {
            case Var.ASTID ⇒
                val definedBy = expr.asVar.definedBy
                in ++ in.collect {
                    case Variable(index) if definedBy.contains(index) ⇒
                        Some(Variable(statement.index))
                    case ArrayElement(index, taintedElement) if definedBy.contains(index) ⇒
                        Some(ArrayElement(statement.index, taintedElement))
                    case _ ⇒ None
                }.flatten
            case ArrayLoad.ASTID ⇒
                val loadExpr = expr.asArrayLoad
                val arrayDefinedBy = loadExpr.arrayRef.asVar.definedBy
                if (in.exists {
                    // One specific array element may be tainted
                    case ArrayElement(index, taintedElement) ⇒
                        val loadedIndex = getIntConstant(loadExpr.index, statement.code)
                        arrayDefinedBy.contains(index) &&
                            (loadedIndex.isEmpty || taintedElement == loadedIndex.get)
                    // Or the whole array
                    case Variable(index) ⇒ arrayDefinedBy.contains(index)
                    case _               ⇒ false
                }) Set(Variable(statement.index))
                else
                    Set.empty
            case BinaryExpr.ASTID | PrefixExpr.ASTID | Compare.ASTID | PrimitiveTypecastExpr.ASTID | NewArray.ASTID | ArrayLength.ASTID ⇒
                (0 until expr.subExprCount).foldLeft(Set.empty[Fact])((acc, subExpr) ⇒
                    acc ++ createNewTaints(expr.subExpr(subExpr), statement, in))
            // TODO GetField, GetStatic
            case _ ⇒ Set.empty
        }

    /**
     * Checks, if some expression always evaluates to the same int constant.
     *
     * @param expr The expression.
     * @param code The TAC code, which contains the expression.
     * @return Some int, if this analysis is sure that `expr` always evaluates to the same int constant, None otherwise.
     */
    def getIntConstant(expr: Expr[V], code: Array[Stmt[V]]): Option[Int] = {
        if (expr.isIntConst) Some(expr.asIntConst.value)
        else if (expr.isVar) {
            // TODO The following looks optimizable!
            val constVals = expr.asVar.definedBy.iterator
                .map[Option[Int]] { idx ⇒
                    if (idx >= 0) {
                        val stmt = code(idx)
                        if (stmt.astID == Assignment.ASTID && stmt.asAssignment.expr.isIntConst)
                            Some(stmt.asAssignment.expr.asIntConst.value)
                        else
                            None
                    } else None
                }
                .toIterable
            if (constVals.forall(option ⇒ option.isDefined && option.get == constVals.head.get))
                constVals.head
            else None
        } else None
    }

    /**
     * Checks, if the result of some variable expression could be tainted.
     *
     * @param expr The variable expression.
     * @param in The current data flow facts.
     * @return True, if the expression could be tainted
     */
    def isTainted(expr: Expr[V], in: Set[Fact]): Boolean = {
        expr.isVar && in.exists {
            case Variable(index)            ⇒ expr.asVar.definedBy.contains(index)
            case ArrayElement(index, _)     ⇒ expr.asVar.definedBy.contains(index)
            case InstanceField(index, _, _) ⇒ expr.asVar.definedBy.contains(index)
            case _                          ⇒ false
        }
    }

    /**
     * Propagates tainted parameters to the callee. If a call to the sink method with a tainted parameter is detected, no
     * call-to-start edges will be created.
     */
    override def callFlow(call: Statement, callee: DeclaredMethod, in: Set[Fact]): Set[Fact] = {
        val allParams = asCall(call.stmt).receiverOption ++ asCall(call.stmt).params
        // Do not analyze the internals of source and sink.
        if (callee.name == "source" || callee.name == "sink") {
            Set.empty
        } else {
            in.collect {

                // Taint formal parameter if actual parameter is tainted
                case Variable(index) ⇒
                    allParams.zipWithIndex.collect {
                        case (param, paramIndex) if param.asVar.definedBy.contains(index) ⇒
                            Variable(switchParamAndVariableIndex(paramIndex, !callee.definedMethod.isStatic))
                    }

                // Taint element of formal parameter if element of actual parameter is tainted
                case ArrayElement(index, taintedIndex) ⇒
                    allParams.zipWithIndex.collect {
                        case (param, paramIndex) if param.asVar.definedBy.contains(index) ⇒
                            ArrayElement(
                                switchParamAndVariableIndex(paramIndex, !callee.definedMethod.isStatic),
                                taintedIndex
                            )
                    }
            }.flatten
        }
    }

    /**
     * Propagates the taints. If the sink method was called with a tainted parameter, a FlowFact will be created to track
     * the call chain back.
     */
    override def callToReturnFlow(call: Statement, successor: Statement, in: Set[Fact]): Set[Fact] = {
        val callStatement = asCall(call.stmt)
        // Taint assigned variable, if source was called
        if (callStatement.name == "source") call.stmt.astID match {
            case Assignment.ASTID ⇒ in + Variable(call.index)
            case _                ⇒ in
        }
        // Create a flow fact, if sink was called with a tainted parameter
        else if (callStatement.name == "sink") {
            if (in.exists {
                case Variable(index) ⇒
                    asCall(call.stmt).params.exists(p ⇒ p.asVar.definedBy.contains(index))
                case _ ⇒ false
            }) {
                in ++ Set(FlowFact(Seq(call.method)))
            } else {
                in
            }
        } else {
            in
        }
    }

    /**
     * Taints an actual parameter, if the corresponding formal parameter was tainted in the callee.
     * If the callee's return value was tainted and it is assigned to a variable in the callee,
     * the variable will be tainted.
     * If a FlowFact held in the callee, this method will be appended to a new FlowFact,
     * which holds at this method.
     */
    override def returnFlow(
        call:      Statement,
        callee:    DeclaredMethod,
        exit:      Statement,
        successor: Statement,
        in:        Set[Fact]
    ): Set[Fact] = {

        /**
         * Checks whether the callee's formal parameter is of a reference type.
         */
        def isRefTypeParam(index: Int): Boolean =
            if (index == -1) true
            else {
                callee.descriptor
                    .parameterType(switchParamAndVariableIndex(index, isStaticMethod = false))
                    .isReferenceType
            }

        val allParams = (asCall(call.stmt).receiverOption ++ asCall(call.stmt).params).toSeq
        var flows: Set[Fact] = Set.empty
        for (fact ← in) {
            fact match {

                // Taint actual parameter if formal parameter is tainted
                case Variable(index) if index < 0 && index > -100 && isRefTypeParam(index) ⇒
                    val param =
                        allParams(switchParamAndVariableIndex(index, !callee.definedMethod.isStatic))
                    flows ++= param.asVar.definedBy.iterator.map(Variable)

                // Taint element of actual parameter if element of formal parameter is tainted
                case ArrayElement(index, taintedIndex) if index < 0 && index > -100 ⇒
                    val param =
                        allParams(switchParamAndVariableIndex(index, !callee.definedMethod.isStatic))
                    flows ++= param.asVar.definedBy.iterator.map(ArrayElement(_, taintedIndex))

                // Taint field of actual parameter if field of formal parameter is tainted
                case InstanceField(index, declClass, taintedField) if index < 0 && index > -10 ⇒
                    val param =
                        allParams(switchParamAndVariableIndex(index, !callee.definedMethod.isStatic))
                    flows ++= param.asVar.definedBy.iterator.map(InstanceField(_, declClass, taintedField))

                // Track the call chain to the sink back
                case FlowFact(flow) ⇒
                    flows += FlowFact(call.method +: flow)
                case _ ⇒
            }
        }

        // Propagate taints of the return value
        if (exit.stmt.astID == ReturnValue.ASTID && call.stmt.astID == Assignment.ASTID) {
            val returnValue = exit.stmt.asReturnValue.expr.asVar
            flows ++= in.collect {
                case Variable(index) if returnValue.definedBy.contains(index) ⇒
                    Variable(call.index)
                case ArrayElement(index, taintedIndex) if returnValue.definedBy.contains(index) ⇒
                    ArrayElement(call.index, taintedIndex)
                case InstanceField(index, declClass, taintedField) if returnValue.definedBy.contains(index) ⇒
                    InstanceField(call.index, declClass, taintedField)
            }
        }

        flows
    }

    /**
     * Converts the index of a method's formal parameter to its variable index in the method's scope and vice versa.
     *
     * @param index The index of a formal parameter in the parameter list or of a variable.
     * @param isStaticMethod States, whether the method is static
     * @return A variable index if a parameter index was passed or a parameter index if a variable index was passed.
     */
    def switchParamAndVariableIndex(index: Int, isStaticMethod: Boolean): Int =
        (if (isStaticMethod) -1 else -2) - index

    /**
     * If a parameter is tainted, the result will also be tainted.
     * We assume that the callee does not call the source method.
     */
    override def nativeCall(statement: Statement, callee: DeclaredMethod, successor: Statement, in: Set[Fact]): Set[Fact] = {
        val allParams = asCall(statement.stmt).receiverOption ++ asCall(statement.stmt).params
        if (statement.stmt.astID == Assignment.ASTID && in.exists {
            case Variable(index) ⇒
                allParams.zipWithIndex.exists {
                    case (param, _) if param.asVar.definedBy.contains(index) ⇒ true
                    case _                                                   ⇒ false
                }
            case ArrayElement(index, _) ⇒
                allParams.zipWithIndex.exists {
                    case (param, _) if param.asVar.definedBy.contains(index) ⇒ true
                    case _                                                   ⇒ false
                }
            case _ ⇒ false
        }) Set(Variable(statement.index))
        else Set.empty
    }
}

object TaintAnalysis extends IFDSAnalysis[Fact] {

    override def init(p: SomeProject, ps: PropertyStore) = new TaintAnalysis()(p)

    override def property: IFDSPropertyMetaInformation[Fact] = Taint
}

/**
 * The IFDSProperty for this analysis.
 *
 * @param flows Maps a statement to the facts, which hold at the statement.
 */
class Taint(val flows: Map[Statement, Set[Fact]]) extends IFDSProperty[Fact] {

    override type Self = Taint

    override def key: PropertyKey[Taint] = Taint.key
}

object Taint extends IFDSPropertyMetaInformation[Fact] {

    override type Self = Taint

    val key: PropertyKey[Taint] = PropertyKey.create("Taint", new Taint(Map.empty))
}