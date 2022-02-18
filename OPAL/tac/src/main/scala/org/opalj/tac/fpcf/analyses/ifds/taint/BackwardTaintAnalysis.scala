/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds.taint

import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.tac._
import org.opalj.tac.fpcf.analyses.ifds.AbstractIFDSAnalysis
import org.opalj.tac.fpcf.analyses.ifds.AbstractIFDSAnalysis.V
import org.opalj.tac.fpcf.analyses.ifds.BackwardIFDSAnalysis
import org.opalj.tac.fpcf.analyses.ifds.Statement
import org.opalj.tac.fpcf.analyses.ifds.UnbalancedReturnFact
import org.opalj.tac.fpcf.properties.IFDSProperty
import org.opalj.tac.fpcf.properties.IFDSPropertyMetaInformation

/**
 * The unbalanced return fact of this analysis.
 *
 * @param index The index, at which the analyzed method is called by some caller.
 * @param innerFact The fact, which will hold in the caller context after the call.
 * @param callChain The current call chain from the sink.
 */
case class UnbalancedTaintFact(index: Int, innerFact: Fact, callChain: Seq[Method])
    extends UnbalancedReturnFact[Fact] with Fact

/**
 * An analysis that checks, if the return value of a `source` method can flow to the parameter of a
 * `sink` method.
 *
 * @param project The project, that is analyzed
 * @author Mario Trageser
 */
abstract class BackwardTaintAnalysis(implicit val project: SomeProject)
    extends BackwardIFDSAnalysis[Fact, UnbalancedTaintFact] with TaintAnalysis {

    override val propertyKey: IFDSPropertyMetaInformation[Fact] = Taint

    /**
     * Called in callToReturnFlow. Creates a FlowFact if necessary.
     *
     * @param call The call.
     * @param in The facts, which hold before the call.
     * @param source The entity, which is analyzed.
     * @return Some FlowFact, if necessary. Otherwise None.
     */
    protected def createFlowFactAtCall(call: Statement, in: Set[Fact],
                                       source: (DeclaredMethod, Fact)): Option[FlowFact]

    /**
     * Called, when a FlowFact holds at the start node of a callee. Creates a FlowFact in the caller
     * context if necessary.
     *
     * @param calleeFact The FlowFact, which holds at the start node of the callee.
     * @param source The analyzed entity.
     * @return Some FlowFact, if necessary. Otherwise None.
     */
    protected def applyFlowFactFromCallee(
        calleeFact: FlowFact,
        source:     (DeclaredMethod, Fact)
    ): Option[FlowFact]

    /**
     * Called, when new FlowFacts are found at the beginning of a method.
     * Creates a FlowFact if necessary.
     *
     * @param in The newly found facts.
     * @param source The entity, which is analyzed.
     * @return Some FlowFact, if necessary. Otherwise None.
     */
    protected def createFlowFactAtBeginningOfMethod(
        in:     Set[Fact],
        source: (DeclaredMethod, Fact)
    ): Option[FlowFact]

    override protected def nullFact: Fact = NullFact

    override protected def createPropertyValue(result: Map[Statement, Set[Fact]]): IFDSProperty[Fact] =
        new Taint(result)

    /**
     * If a tainted variable gets assigned a value, this value will be tainted.
     */
    override protected def normalFlow(statement: Statement, successor: Statement,
                                      in: Set[Fact]): Set[Fact] = {
        val stmt = statement.stmt
        stmt.astID match {
            case Assignment.ASTID if isTainted(statement.index, in) ⇒
                in ++ createNewTaints(stmt.asAssignment.expr, statement)
            case ArrayStore.ASTID ⇒
                val arrayStore = stmt.asArrayStore
                val arrayIndex = TaintAnalysis.getIntConstant(arrayStore.index, statement.code)
                val arrayDefinedBy = arrayStore.arrayRef.asVar.definedBy
                val result = in ++ in.collect {
                    // In this case, we taint the whole array.
                    case Variable(index) if arrayDefinedBy.contains(index) ⇒
                        createNewTaints(arrayStore.value, statement)
                    // In this case, we taint exactly the stored element.
                    case ArrayElement(index, taintedElement) if arrayDefinedBy.contains(index) &&
                        (arrayIndex.isEmpty || arrayIndex.get == taintedElement) ⇒
                        createNewTaints(arrayStore.value, statement)
                }.flatten
                if (arrayDefinedBy.size == 1 && arrayIndex.isDefined)
                    result - ArrayElement(arrayDefinedBy.head, arrayIndex.get)
                else result
            case PutField.ASTID ⇒
                val putField = stmt.asPutField
                val objectDefinedBy = putField.objRef.asVar.definedBy
                if (in.exists {
                    case InstanceField(index, declaringClass, name) if objectDefinedBy.contains(index) &&
                        putField.declaringClass == declaringClass && putField.name == name ⇒
                        true
                    case _ ⇒ false
                }) in ++ createNewTaints(putField.value, statement)
                else in
            case PutStatic.ASTID ⇒
                val putStatic = stmt.asPutStatic
                if (in.exists {
                    case StaticField(declaringClass, name) if putStatic.declaringClass == declaringClass && putStatic.name == name ⇒
                        true
                    case _ ⇒ false
                }) in ++ createNewTaints(putStatic.value, statement)
                else in
            case _ ⇒ in
        }
    }

    /**
     * Taints the actual parameters in the caller context if the formal parameters in the callee
     * context were tainted.
     * Does not taint anything, if the sanitize method was called.
     */
    override protected def callFlow(call: Statement, callee: DeclaredMethod,
                                    in: Set[Fact], source: (DeclaredMethod, Fact)): Set[Fact] =
        if (sanitizesReturnValue(callee)) Set.empty
        else taintActualsIfFormalsTainted(callee, call, in, source)

    /**
     * If the returned value on in the caller context is tainted, the returned values in the callee
     * context will be tainted. If an actual pass-by-reference-parameter in the caller context is
     * tainted, the formal parameter in the callee context will be tainted.
     */
    override protected def returnFlow(call: Statement, callee: DeclaredMethod, exit: Statement,
                                      successor: Statement, in: Set[Fact]): Set[Fact] = {
        val callObject = asCall(call.stmt)
        val staticCall = callee.definedMethod.isStatic
        val flow = collection.mutable.Set.empty[Fact]
        if (call.stmt.astID == Assignment.ASTID && exit.stmt.astID == ReturnValue.ASTID)
            in.foreach {
                case Variable(index) if index == call.index ⇒
                    flow ++= createNewTaints(exit.stmt.asReturnValue.expr, exit)
                case ArrayElement(index, taintedElement) if index == call.index ⇒
                    flow ++= createNewArrayElementTaints(exit.stmt.asReturnValue.expr, taintedElement,
                        call)
                case InstanceField(index, declaringClass, name) if index == call.index ⇒
                    flow ++= createNewInstanceFieldTaints(exit.stmt.asReturnValue.expr, declaringClass,
                        name, call)
                case _ ⇒ // Nothing to do
            }
        val thisOffset = if (callee.definedMethod.isStatic) 0 else 1
        callObject.allParams.iterator.zipWithIndex
            .filter(pair ⇒ (pair._2 == 0 && !staticCall) ||
                callObject.descriptor.parameterTypes(pair._2 - thisOffset).isReferenceType)
            .foreach { pair ⇒
                val param = pair._1.asVar
                val paramIndex = pair._2
                in.foreach {
                    case Variable(index) if param.definedBy.contains(index) ⇒
                        flow += Variable(AbstractIFDSAnalysis.switchParamAndVariableIndex(paramIndex, staticCall))
                    case ArrayElement(index, taintedElement) if param.definedBy.contains(index) ⇒
                        flow += ArrayElement(
                            AbstractIFDSAnalysis.switchParamAndVariableIndex(paramIndex, staticCall), taintedElement
                        )
                    case InstanceField(index, declaringClass, name) if param.definedBy.contains(index) ⇒
                        flow += InstanceField(
                            AbstractIFDSAnalysis.switchParamAndVariableIndex(paramIndex, staticCall),
                            declaringClass, name
                        )
                    case staticField: StaticField ⇒ flow += staticField
                    case _                        ⇒ // Nothing to do
                }
            }
        flow.toSet
    }

    /**
     * Adds a FlowFact, if `createFlowFactAtCall` creates one.
     * Removes taints according to `sanitizeParamters`.
     */
    override protected def callToReturnFlow(call: Statement, successor: Statement,
                                            in:     Set[Fact],
                                            source: (DeclaredMethod, Fact)): Set[Fact] = {
        val flowFact = createFlowFactAtCall(call, in, source)
        val result = in -- sanitizeParamters(call, in)
        if (flowFact.isDefined) result + flowFact.get
        else result
    }

    /**
     * If the returned value is tainted, all actual parameters will be tainted.
     */
    override protected def callOutsideOfAnalysisContext(call: Statement, callee: DeclaredMethod,
                                                        successor: Statement,
                                                        in:        Set[Fact]): Set[Fact] = {
        val callStatement = asCall(call.stmt)
        in ++ in.collect {
            case Variable(index) if index == call.index ⇒
                callStatement.allParams.flatMap(createNewTaints(_, call))
            case ArrayElement(index, _) if index == call.index ⇒
                callStatement.allParams.flatMap(createNewTaints(_, call))
            case InstanceField(index, _, _) if index == call.index ⇒
                callStatement.allParams.flatMap(createNewTaints(_, call))
        }.flatten
    }

    /**
     * Creates an UnbalancedTaintFact for each actual parameter on the caller side, of the formal
     * parameter of this method is tainted.
     */
    override protected def unbalancedReturnFlow(facts: Set[Fact], call: Statement,
                                                caller: DeclaredMethod,
                                                source: (DeclaredMethod, Fact)): Set[UnbalancedTaintFact] =
        taintActualsIfFormalsTainted(source._1, call, facts, source, isCallFlow = false)
            .map(UnbalancedTaintFact(call.index, _, currentCallChain(source)))
            // Avoid infinite loops.
            .filter(unbalancedTaintFact ⇒ !containsHeadTwice(unbalancedTaintFact.callChain))

    /**
     * Propagates the call to createFlowFactAtBeginningOfMethod.
     */
    override protected def createFactsAtStartNode(
        in:     Set[Fact],
        source: (DeclaredMethod, Fact)
    ): Set[Fact] = {
        val flowFact = createFlowFactAtBeginningOfMethod(in, source)
        if (flowFact.isDefined) Set(flowFact.get)
        else Set.empty
    }

    /**
     * Gets the current call chain.
     * If the input fact for the analysis is an UnbalancedTaintFact, this is the current method
     * concatenated with the fact's call chain.
     * Otherwise, it is just this method.
     *
     * @param source The entity, which is analyzed.
     * @return The current call chain.
     */
    protected def currentCallChain(source: (DeclaredMethod, Fact)): Seq[Method] = {
        val method = source._1.definedMethod
        val sourceFact = source._2
        sourceFact match {
            case fact: UnbalancedTaintFact ⇒ method +: fact.callChain
            case _                         ⇒ Seq(method)
        }
    }

    /**
     * Checks, if the head of a sequence is contained in its tail.
     *
     * @param callChain The sequence.
     * @return True, if the head of `callChain` is contained in its tail.
     */
    protected def containsHeadTwice(callChain: Seq[Method]): Boolean =
        callChain.tail.contains(callChain.head)

    /**
     * Checks, if some variable or array element is tainted.
     *
     * @param index The index of the variable or array element.
     * @param in The current data flow facts.
     * @param taintedElement If present, the taint of a specific array element will be checked.
     * @return True, if the variable or array element is tainted.
     */
    private def isTainted(index: Int, in: Set[Fact], taintedElement: Option[Int] = None): Boolean = in.exists {
        case Variable(variableIndex) ⇒ variableIndex == index
        case ArrayElement(variableIndex, element) ⇒
            variableIndex == index && (taintedElement.isEmpty || taintedElement.get == element)
        case _ ⇒ false
    }

    /**
     * Taints all variables in an `expression`.
     *
     * @param expression The expression.
     * @param statement The statement, which contains the expression.
     * @return The new taints.
     */
    private def createNewTaints(expression: Expr[V], statement: Statement): Set[Fact] =
        expression.astID match {
            case Var.ASTID ⇒ expression.asVar.definedBy.map(Variable)
            case ArrayLoad.ASTID ⇒
                val arrayLoad = expression.asArrayLoad
                val arrayIndex = TaintAnalysis.getIntConstant(expression.asArrayLoad.index, statement.code)
                val arrayDefinedBy = arrayLoad.arrayRef.asVar.definedBy
                if (arrayIndex.isDefined) arrayDefinedBy.map(ArrayElement(_, arrayIndex.get))
                else arrayDefinedBy.map(Variable)
            case BinaryExpr.ASTID | PrefixExpr.ASTID | Compare.ASTID |
                PrimitiveTypecastExpr.ASTID | NewArray.ASTID | ArrayLength.ASTID ⇒
                (0 until expression.subExprCount).foldLeft(Set.empty[Fact])((acc, subExpr) ⇒
                    acc ++ createNewTaints(expression.subExpr(subExpr), statement))
            case GetField.ASTID ⇒
                val getField = expression.asGetField
                getField.objRef.asVar.definedBy
                    .map(InstanceField(_, getField.declaringClass, getField.name))
            /*case GetStatic.ASTID ⇒
                val getStatic = expression.asGetStatic
                Set(StaticField(getStatic.declaringClass, getStatic.name))*/
            case _ ⇒ Set.empty
        }

    /**
     * Creates some taints for an array, but only taints some element.
     *
     * @param expression The expression, referring to the array.
     * @param taintedElement The array element, which will be tainted.
     * @param statement The statement, containing the expression.
     * @return An ArrayElement fact for the expression and the tainted element.
     */
    private def createNewArrayElementTaints(expression: Expr[V], taintedElement: Int,
                                            statement: Statement): Set[Fact] =
        createNewTaints(expression, statement).map {
            case Variable(variableIndex)            ⇒ ArrayElement(variableIndex, taintedElement)
            // We do not nest taints. Instead, we taint the whole inner array.
            case ArrayElement(variableIndex, _)     ⇒ ArrayElement(variableIndex, taintedElement)
            case InstanceField(variableIndex, _, _) ⇒ ArrayElement(variableIndex, taintedElement)
        }

    private def createNewInstanceFieldTaints(expression: Expr[V], declaringClass: ObjectType,
                                             name: String, statement: Statement): Set[Fact] =
        createNewTaints(expression, statement).map {
            case Variable(variableIndex)        ⇒ InstanceField(variableIndex, declaringClass, name)
            // We do not nest taints. Instead, taint the whole field.
            case ArrayElement(variableIndex, _) ⇒ InstanceField(variableIndex, declaringClass, name)
            case InstanceField(variableIndex, _, _) ⇒
                InstanceField(variableIndex, declaringClass, name)
        }

    /**
     * Taints actual parameters on in the caller context, if the corresponding formal parameter in
     * the `callee` context is tainted.
     * Additionally propagates taints of static fields.
     *
     * @param callee The callee.
     * @param call The call, calling the `callee`.
     * @param calleeFacts The facts in the `callee` context.
     * @param source The entity, which is analyzed.
     * @param isCallFlow True, if this method is called in callFlow. In this case, FlowFacts will
     *                   be propagated to the caller context.
     * @return The data flow facts in the caller context.
     */
    private def taintActualsIfFormalsTainted(
        callee:      DeclaredMethod,
        call:        Statement,
        calleeFacts: Set[Fact],
        source:      (DeclaredMethod, Fact),
        isCallFlow:  Boolean                = true
    ): Set[Fact] = {
        val stmt = call.stmt
        val callStatement = asCall(stmt)
        val staticCall = callee.definedMethod.isStatic
        val thisOffset = if (staticCall) 0 else 1
        val formalParameterIndices = (0 until callStatement.descriptor.parametersCount)
            .map(index ⇒ AbstractIFDSAnalysis.switchParamAndVariableIndex(index + thisOffset, staticCall))
        val facts = scala.collection.mutable.Set.empty[Fact]
        calleeFacts.foreach {
            case Variable(index) if formalParameterIndices.contains(index) ⇒
                facts ++= createNewTaints(
                    callStatement.allParams(AbstractIFDSAnalysis.switchParamAndVariableIndex(index, staticCall)), call
                )
            case ArrayElement(index, taintedElement) if formalParameterIndices.contains(index) ⇒
                facts ++= createNewArrayElementTaints(
                    callStatement.allParams(AbstractIFDSAnalysis.switchParamAndVariableIndex(index, staticCall)),
                    taintedElement, call
                )
            case InstanceField(index, declaringClass, name) if formalParameterIndices.contains(index) ⇒
                facts ++= createNewInstanceFieldTaints(
                    callStatement.allParams(AbstractIFDSAnalysis.switchParamAndVariableIndex(index, staticCall)),
                    declaringClass, name, call
                )
            case staticField: StaticField ⇒ facts += staticField
            // If the source was reached in a callee, create a flow fact from this method to the sink.
            case calleeFact: FlowFact if isCallFlow ⇒
                val callerFact = applyFlowFactFromCallee(calleeFact, source)
                if (callerFact.isDefined) facts += callerFact.get
            case _ ⇒ // Nothing to do
        }
        facts.toSet
    }
}