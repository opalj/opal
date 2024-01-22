/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package fpcf
package analyses
package ifds
package taint

import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.analyses.SomeProject
import org.opalj.ifds.Callable
import org.opalj.ifds.Dependees.Getter
import org.opalj.tac.fpcf.analyses.ifds.JavaBackwardIFDSProblem
import org.opalj.tac.fpcf.analyses.ifds.JavaIFDSProblem
import org.opalj.tac.fpcf.analyses.ifds.JavaIFDSProblem.V
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement

/**
 * Implementation of a backward taint analysis for Java code.
 *
 * @author Nicolas Gross
 */
abstract class JavaBackwardTaintProblem(project: SomeProject)
    extends JavaBackwardIFDSProblem[TaintFact](project)
    with TaintProblem[Method, JavaStatement, TaintFact] {

    override def nullFact: TaintFact = TaintNullFact

    override def enableUnbalancedReturns: Boolean = true

    /**
     * If a tainted variable gets assigned a value, this value will be tainted.
     */
    override def normalFlow(jstmt: JavaStatement, in: TaintFact, predecessor: Option[JavaStatement]): Set[TaintFact] = {
        jstmt.stmt.astID match {
            case Assignment.ASTID =>
                if (in match {
                        case Variable(index)        => index == jstmt.index
                        case ArrayElement(index, _) => index == jstmt.index
                        case _                      => false
                    }
                ) {
                    Set(in) ++ createNewTaints(jstmt.stmt.asAssignment.expr, jstmt)
                } else Set(in)
            case ArrayStore.ASTID =>
                val arrayStore = jstmt.stmt.asArrayStore
                val arrayIndex = TaintProblem.getIntConstant(arrayStore.index, jstmt.code)
                val arrayDefinedBy = arrayStore.arrayRef.asVar.definedBy
                if (in match {
                        // check if array is tainted
                        case Variable(index) => arrayDefinedBy.contains(index) // whole variable/array is tainted
                        case ArrayElement(index, taintedElement) => arrayDefinedBy.contains(index) && // only specific array element is tainted
                                (arrayIndex.isEmpty || arrayIndex.get == taintedElement)
                        case _ => false
                    }
                ) {
                    if (arrayIndex.isDefined && arrayDefinedBy.size == 1 &&
                        in == ArrayElement(arrayDefinedBy.head, arrayIndex.get)
                    ) {
                        // tainted array element is overwritten -> untaint
                        createNewTaints(arrayStore.value, jstmt)
                    } else Set(in) ++ createNewTaints(arrayStore.value, jstmt)
                } else Set(in)
            case PutField.ASTID =>
                val putField = jstmt.stmt.asPutField
                val definedBy = putField.objRef.asVar.definedBy
                if (in match {
                        case InstanceField(index, classType, fieldName) =>
                            definedBy.contains(index) &&
                                putField.declaringClass == classType &&
                                putField.name == fieldName
                        case _ => false
                    }
                ) {
                    Set(in) ++ createNewTaints(putField.value, jstmt)
                } else Set(in)
            case PutStatic.ASTID =>
                val putStatic = jstmt.stmt.asPutStatic
                in match {
                    case StaticField(classType, fieldName)
                        if putStatic.declaringClass == classType &&
                            putStatic.name == fieldName => Set(in) ++ createNewTaints(putStatic.value, jstmt)
                    case _ => Set(in)
                }
            case _ => Set(in)
        }
    }

    /**
     * If the returned value in the caller context is tainted, the returned values in the callee
     * context will be tainted. If an actual pass-by-reference-parameter in the caller context is
     * tainted, the formal parameter in the callee context will be tainted.
     */
    override def callFlow(start: JavaStatement, in: TaintFact, call: JavaStatement, callee: Method): Set[TaintFact] = {
        // taint expression of return value in callee if return value in caller is tainted
        val callObject = JavaIFDSProblem.asCall(call.stmt)
        val flow = scala.collection.mutable.Set.empty[TaintFact]
        if (call.stmt.astID == Assignment.ASTID && start.stmt.astID == ReturnValue.ASTID) {
            in match {
                case Variable(index) if index == call.index =>
                    flow ++= createNewTaints(start.stmt.asReturnValue.expr, start)
                case ArrayElement(index, taintedElement) if index == call.index =>
                    flow ++= createNewArrayElementTaints(start.stmt.asReturnValue.expr, taintedElement, call)
                case InstanceField(index, declaringClass, name) if index == call.index =>
                    flow ++= createNewInstanceFieldTaints(start.stmt.asReturnValue.expr, declaringClass, name, call)
                case _ => // Nothing to do
            }
        }

        // check for tainted 'this' and pass-by-reference parameters
        val thisOffset = if (callee.isStatic) 0 else 1
        callObject.allParams.iterator.zipWithIndex
            .filter(pair =>
                (pair._2 == 0 && !callee.isStatic) || // this
                    callObject.descriptor.parameterTypes(pair._2 - thisOffset).isReferenceType
            ) // pass-by-reference parameters
            .foreach { pair =>
                val param = pair._1.asVar
                val paramIndex = pair._2
                in match {
                    case Variable(index) if param.definedBy.contains(index) =>
                        flow += Variable(JavaIFDSProblem.remapParamAndVariableIndex(paramIndex, callee.isStatic))
                    case ArrayElement(index, taintedElement) if param.definedBy.contains(index) =>
                        flow += ArrayElement(
                            JavaIFDSProblem.remapParamAndVariableIndex(paramIndex, callee.isStatic),
                            taintedElement
                        )
                    case InstanceField(index, declaringClass, name) if param.definedBy.contains(index) =>
                        flow += InstanceField(
                            JavaIFDSProblem.remapParamAndVariableIndex(paramIndex, callee.isStatic),
                            declaringClass,
                            name
                        )
                    case staticField: StaticField => flow += staticField
                    case _                        => // Nothing to do
                }
            }
        flow.toSet
    }

    /**
     * Taints the actual parameters in the caller context if the formal parameters in the callee
     * context were tainted.
     * Does not taint anything, if the sanitize method was called.
     */
    override def returnFlow(
        exit:         JavaStatement,
        in:           TaintFact,
        call:         JavaStatement,
        successor:    Option[JavaStatement],
        unbCallChain: Seq[Callable]
    ): Set[TaintFact] = {
        val callee = exit.callable
        if (sanitizesReturnValue(callee)) return Set.empty

        val callStatement = JavaIFDSProblem.asCall(call.stmt)
        val staticCall = callee.isStatic
        val thisOffset = if (staticCall) 0 else 1
        val formalParameterIndices = (0 until callStatement.descriptor.parametersCount)
            .map(index => JavaIFDSProblem.remapParamAndVariableIndex(index + thisOffset, staticCall))
        val facts = scala.collection.mutable.Set.empty[TaintFact]
        in match {
            case Variable(index) if formalParameterIndices.contains(index) =>
                facts.addAll(createNewTaints(
                    callStatement.allParams(JavaIFDSProblem.remapParamAndVariableIndex(index, staticCall)),
                    call
                ))
            case ArrayElement(index, taintedElement) if formalParameterIndices.contains(index) =>
                facts.addAll(createNewArrayElementTaints(
                    callStatement.allParams(JavaIFDSProblem.remapParamAndVariableIndex(index, staticCall)),
                    taintedElement,
                    call
                ))
            case InstanceField(index, declaringClass, name) if formalParameterIndices.contains(index) =>
                facts.addAll(createNewInstanceFieldTaints(
                    callStatement.allParams(JavaIFDSProblem.remapParamAndVariableIndex(index, staticCall)),
                    declaringClass,
                    name,
                    call
                ))
            case staticField: StaticField => facts.add(staticField)
            // If the source was reached in a callee, create a flow fact from this method to the sink.
            case calleeFact: FlowFact =>
                val callerFact = applyFlowFactFromCallee(calleeFact, call.callable, in, unbCallChain)
                if (callerFact.isDefined) facts.add(callerFact.get)
            case _ => // Nothing to do
        }
        facts
    }.toSet

    /**
     * Adds a FlowFact, if `createFlowFactAtCall` creates one.
     * Removes taints according to `sanitizeParamters`.
     */
    override def callToReturnFlow(
        call:         JavaStatement,
        in:           TaintFact,
        successor:    Option[JavaStatement],
        unbCallChain: Seq[Callable]
    ): Set[TaintFact] = {
        val flowFact = createFlowFactAtCall(call, in, unbCallChain)
        val result = scala.collection.mutable.Set.empty[TaintFact]
        if (!sanitizesParameter(call, in)) result.add(in)
        if (flowFact.isDefined) result.add(flowFact.get)
        result.toSet
    }

    /**
     * Called in callToReturnFlow. Creates a fact if necessary.
     *
     * @param call The call.
     * @param in   The fact, which holds before the call.
     * @return Some fact, if necessary. Otherwise None.
     */
    protected def createFlowFactAtCall(
        call:      JavaStatement,
        in:        TaintFact,
        callChain: Seq[Callable]
    ): Option[TaintFact] = None

    /**
     * If the returned value is tainted, all actual parameters will be tainted.
     */
    override def outsideAnalysisContextCall(callee: Method): Option[OutsideAnalysisContextCallHandler] = {
        super.outsideAnalysisContextCall(callee) match {
            case Some(_) => Some(
                    (
                        call:         JavaStatement,
                        _:            Option[JavaStatement],
                        in:           TaintFact,
                        unbCallChain: Seq[Callable],
                        _:            Getter
                    ) => {
                        val callStatement = JavaIFDSProblem.asCall(call.stmt)
                        Set(in) ++ (in match {
                            case Variable(index) if index == call.index =>
                                callStatement.allParams.flatMap(createNewTaints(_, call))
                            case ArrayElement(index, _) if index == call.index =>
                                callStatement.allParams.flatMap(createNewTaints(_, call))
                            case InstanceField(index, _, _) if index == call.index =>
                                callStatement.allParams.flatMap(createNewTaints(_, call))
                        })
                    }
                )
            case None => None
        }
    }

    /**
     * Taints all variables in an `expression`.
     *
     * @param expression The expression.
     * @param statement  The statement, which contains the expression.
     * @return The new taints.
     */
    private def createNewTaints(expression: Expr[V], statement: JavaStatement): Set[TaintFact] = {
        /* TODO alias references and nested objects are not correctly handled, same for forward analysis
         if new variable is tainted, check if it holds a reference type value, if yes:
          1. taint aliases of that variable
          2. taint variables holding references to inner objects/arrays
         repeat 1 and 2 for variables found in 1 and 2

         code example showcasing the problem:
         ============================================================
         def nested_ret_array():
            a = new int[4][4]               "
            b = a[3]                        "
            c = source()                    "
            b[0] = c                        "
            d = a[3]                        {d[2], a[3]}
            return d                        {arr[2] -> d[2]}

         arr = nested_ret_array()            {arr[2]}
         sink(arr[2])
         ============================================================
         */
        expression.astID match {
            case Var.ASTID => expression.asVar.definedBy.map(Variable)
            case ArrayLoad.ASTID =>
                val arrayLoad = expression.asArrayLoad
                val arrayIndex = TaintProblem.getIntConstant(arrayLoad.index, statement.code)
                val arrayDefinedBy = arrayLoad.arrayRef.asVar.definedBy
                if (arrayIndex.isDefined) arrayDefinedBy.map(ArrayElement(_, arrayIndex.get))
                else arrayDefinedBy.map(Variable)
            case BinaryExpr.ASTID | PrefixExpr.ASTID | Compare.ASTID |
                PrimitiveTypecastExpr.ASTID | NewArray.ASTID | ArrayLength.ASTID =>
                (0 until expression.subExprCount).foldLeft(Set.empty[TaintFact])((acc, subExpr) =>
                    acc ++ createNewTaints(expression.subExpr(subExpr), statement)
                )
            case GetField.ASTID =>
                val getField = expression.asGetField
                getField.objRef.asVar.definedBy
                    .map(InstanceField(_, getField.declaringClass, getField.name))
            case GetStatic.ASTID =>
                val getStatic = expression.asGetStatic
                Set(StaticField(getStatic.declaringClass, getStatic.name))
            case _ => Set.empty
        }
    }

    /**
     * Keeps information about the tainted array element when mapping params/retvals in/out-of methods.
     *
     * @param expression     The expression, referring to the array.
     * @param taintedElement The array element, which will be tainted.
     * @param statement      The statement, containing the expression.
     * @return a set of facts with preserved information about the tainted element of arrays.
     */
    private def createNewArrayElementTaints(
        expression:     Expr[V],
        taintedElement: Int,
        statement:      JavaStatement
    ): Set[TaintFact] =
        createNewTaints(expression, statement).map {
            // Keep information about tainted array element
            case Variable(variableIndex) => ArrayElement(variableIndex, taintedElement)
            case taintFact               => taintFact
        }

    /**
     * Keeps information about the tainted instance field when mapping params/retvals in/out-of methods.
     *
     * @param expression the expression referring to the mapped instance.
     * @param declaringClass the declaring class of the instance.
     * @param name the name of the instance field.
     * @param statement the statement containing the expression.
     * @return a set of facts with preserved information about the tainted field of instances.
     */
    private def createNewInstanceFieldTaints(
        expression:     Expr[V],
        declaringClass: ObjectType,
        name:           String,
        statement:      JavaStatement
    ): Set[TaintFact] =
        createNewTaints(expression, statement).map {
            // keep information about instance field
            case Variable(variableIndex) => InstanceField(variableIndex, declaringClass, name)
            case taintFact               => taintFact
        }

    /**
     * Called, when a FlowFact holds at the index 0 node of a callee. Creates a FlowFact in the caller
     * context if necessary.
     *
     * @param calleeFact The FlowFact, which holds at the start node of the callee.
     * @param caller The caller.
     * @param in the fact
     * @param callChain the current call chain
     * @return Some FlowFact, if necessary. Otherwise None.
     */
    protected def applyFlowFactFromCallee(
        calleeFact: FlowFact,
        caller:     Method,
        in:         TaintFact,
        callChain:  Seq[Callable]
    ): Option[FlowFact]
}
