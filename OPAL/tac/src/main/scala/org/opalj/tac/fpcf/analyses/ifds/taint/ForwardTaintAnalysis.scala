/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.ifds.taint

import org.opalj.fpcf.PropertyKey
import org.opalj.br.DeclaredMethod
import org.opalj.br.analyses.SomeProject
import org.opalj.tac._
import org.opalj.tac.fpcf.analyses.ifds.AbstractIFDSAnalysis
import org.opalj.tac.fpcf.analyses.ifds.AbstractIFDSAnalysis.V
import org.opalj.tac.fpcf.analyses.ifds.ForwardIFDSAnalysis
import org.opalj.tac.fpcf.analyses.ifds.Statement
import org.opalj.tac.fpcf.properties.IFDSProperty
import org.opalj.tac.fpcf.properties.IFDSPropertyMetaInformation

/**
 * An analysis that checks, if the return value of a `source` method can flow to the parameter of a
 * `sink` method.
 *
 * @param project The project, that is analyzed
 * @author Mario Trageser
 */
abstract class ForwardTaintAnalysis(implicit val project: SomeProject)
    extends ForwardIFDSAnalysis[Fact] with TaintAnalysis {

    override val propertyKey: IFDSPropertyMetaInformation[Fact] = Taint

    /**
     * Called, when the exit to return facts are computed for some `callee` with the null fact and
     * the callee's return value is assigned to a vairbale.
     * Creates a taint, if necessary.
     *
     * @param callee The called method.
     * @param call The call.
     * @return Some variable fact, if necessary. Otherwise none.
     */
    protected def createTaints(callee: DeclaredMethod, call: Statement): Set[Fact]

    /**
     * Called, when the call to return facts are computed for some `callee`.
     * Creates a FlowFact, if necessary.
     *
     * @param callee The method, which was called.
     * @param call The call.
     * @return Some FlowFact, if necessary. Otherwise None.
     */
    protected def createFlowFact(callee: DeclaredMethod, call: Statement,
                                 in: Set[Fact]): Option[FlowFact]

    override protected def nullFact: Fact = NullFact

    override protected def createPropertyValue(result: Map[Statement, Set[Fact]]): IFDSProperty[Fact] =
        new Taint(result)

    /**
     * If a variable gets assigned a tainted value, the variable will be tainted.
     */
    override protected def normalFlow(statement: Statement, successor: Statement,
                                      in: Set[Fact]): Set[Fact] =
        statement.stmt.astID match {
            case Assignment.ASTID ⇒
                in ++ createNewTaints(statement.stmt.asAssignment.expr, statement, in)
            case ArrayStore.ASTID ⇒
                val store = statement.stmt.asArrayStore
                val definedBy = store.arrayRef.asVar.definedBy
                val arrayIndex = TaintAnalysis.getIntConstant(store.index, statement.code)
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
            case PutField.ASTID ⇒
                val put = statement.stmt.asPutField
                val definedBy = put.objRef.asVar.definedBy
                if (isTainted(put.value, in))
                    definedBy.foldLeft(in) { (in, defSite) ⇒
                        in + InstanceField(defSite, put.declaringClass, put.name)
                    }
                else
                    in
            case PutStatic.ASTID ⇒
                val put = statement.stmt.asPutStatic
                if (isTainted(put.value, in))
                    in + StaticField(put.declaringClass, put.name)
                else
                    in
            case _ ⇒ in
        }

    /**
     * Propagates tainted parameters to the callee. If a call to the sink method with a tainted
     * parameter is detected, no call-to-start
     * edges will be created.
     */
    override protected def callFlow(call: Statement, callee: DeclaredMethod,
                                    in: Set[Fact], source: (DeclaredMethod, Fact)): Set[Fact] = {
        val callObject = asCall(call.stmt)
        val allParams = callObject.allParams
        var facts = Set.empty[Fact]

        if (relevantCallee(callee)) {
            val allParamsWithIndices = allParams.zipWithIndex
            in.foreach {
                // Taint formal parameter if actual parameter is tainted
                case Variable(index) ⇒
                    allParamsWithIndices.foreach {
                        case (param, paramIndex) if param.asVar.definedBy.contains(index) ⇒
                            facts += Variable(AbstractIFDSAnalysis.switchParamAndVariableIndex(
                                paramIndex,
                                callee.definedMethod.isStatic
                            ))
                        case _ ⇒ // Nothing to do
                    }

                // Taint element of formal parameter if element of actual parameter is tainted
                case ArrayElement(index, taintedIndex) ⇒
                    allParamsWithIndices.foreach {
                        case (param, paramIndex) if param.asVar.definedBy.contains(index) ⇒
                            facts += ArrayElement(
                                AbstractIFDSAnalysis.switchParamAndVariableIndex(paramIndex, callee.definedMethod.isStatic),
                                taintedIndex
                            )
                        case _ ⇒ // Nothing to do
                    }

                case InstanceField(index, declClass, taintedField) ⇒
                    // Taint field of formal parameter if field of actual parameter is tainted
                    // Only if the formal parameter is of a type that may have that field!
                    allParamsWithIndices.foreach {
                        case (param, pIndex) if param.asVar.definedBy.contains(index) &&
                            (AbstractIFDSAnalysis.switchParamAndVariableIndex(pIndex, callee.definedMethod.isStatic) != -1 ||
                                classHierarchy.isSubtypeOf(declClass, callee.declaringClassType)) ⇒
                            facts += InstanceField(
                                AbstractIFDSAnalysis.switchParamAndVariableIndex(pIndex, callee.definedMethod.isStatic),
                                declClass, taintedField
                            )
                        case _ ⇒ // Nothing to do
                    }

                case sf: StaticField ⇒ facts += sf

                case _               ⇒ // Nothing to do
            }
        }

        facts
    }

    /**
     * Taints an actual parameter, if the corresponding formal parameter was tainted in the callee.
     * If the callee's return value was tainted and it is assigned to a variable in the callee, the
     * variable will be tainted.
     * If a FlowFact held in the callee, this method will be appended to a new FlowFact, which holds
     * at this method.
     * Creates new taints and FlowFacts, if necessary.
     * If the sanitize method was called, nothing will be tainted.
     */
    override protected def returnFlow(call: Statement, callee: DeclaredMethod, exit: Statement,
                                      successor: Statement, in: Set[Fact]): Set[Fact] = {

        /**
         * Checks whether the callee's formal parameter is of a reference type.
         */
        def isRefTypeParam(index: Int): Boolean =
            if (index == -1) true
            else {
                val parameterOffset = if (callee.definedMethod.isStatic) 0 else 1
                callee.descriptor.parameterType(
                    AbstractIFDSAnalysis.switchParamAndVariableIndex(index, callee.definedMethod.isStatic)
                        - parameterOffset
                ).isReferenceType
            }

        if (sanitizesReturnValue(callee)) return Set.empty
        val callStatement = asCall(call.stmt)
        val allParams = callStatement.allParams
        var flows: Set[Fact] = Set.empty
        in.foreach {
            // Taint actual parameter if formal parameter is tainted
            case Variable(index) if index < 0 && index > -100 && isRefTypeParam(index) ⇒
                val param = allParams(
                    AbstractIFDSAnalysis.switchParamAndVariableIndex(index, callee.definedMethod.isStatic)
                )
                flows ++= param.asVar.definedBy.iterator.map(Variable)

            // Taint element of actual parameter if element of formal parameter is tainted
            case ArrayElement(index, taintedIndex) if index < 0 && index > -100 ⇒
                val param = allParams(
                    AbstractIFDSAnalysis.switchParamAndVariableIndex(index, callee.definedMethod.isStatic)
                )
                flows ++= param.asVar.definedBy.iterator.map(ArrayElement(_, taintedIndex))

            case InstanceField(index, declClass, taintedField) if index < 0 && index > -10 ⇒
                // Taint field of actual parameter if field of formal parameter is tainted
                val param =
                    allParams(AbstractIFDSAnalysis.switchParamAndVariableIndex(index, callee.definedMethod.isStatic))
                param.asVar.definedBy.foreach { defSite ⇒
                    flows += InstanceField(defSite, declClass, taintedField)
                }

            case sf: StaticField ⇒ flows += sf

            // Track the call chain to the sink back
            case FlowFact(flow) if !flow.contains(call.method) ⇒
                flows += FlowFact(call.method +: flow)
            case _ ⇒
        }

        // Propagate taints of the return value
        if (exit.stmt.astID == ReturnValue.ASTID && call.stmt.astID == Assignment.ASTID) {
            val returnValueDefinedBy = exit.stmt.asReturnValue.expr.asVar.definedBy
            in.foreach {
                case Variable(index) if returnValueDefinedBy.contains(index) ⇒
                    flows += Variable(call.index)
                case ArrayElement(index, taintedIndex) if returnValueDefinedBy.contains(index) ⇒
                    flows += ArrayElement(call.index, taintedIndex)
                case InstanceField(index, declClass, taintedField) if returnValueDefinedBy.contains(index) ⇒
                    flows += InstanceField(call.index, declClass, taintedField)
                case NullFact ⇒
                    val taints = createTaints(callee, call)
                    if (taints.nonEmpty) flows ++= taints
                case _ ⇒ // Nothing to do
            }
        }
        val flowFact = createFlowFact(callee, call, in)
        if (flowFact.isDefined) flows += flowFact.get

        flows
    }

    /**
     * Removes taints according to `sanitizeParamters`.
     */
    override protected def callToReturnFlow(call: Statement, successor: Statement,
                                            in:     Set[Fact],
                                            source: (DeclaredMethod, Fact)): Set[Fact] =
        in -- sanitizeParamters(call, in)

    /**
     * If a parameter is tainted, the result will also be tainted.
     * We assume that the callee does not call the source method.
     */
    override protected def callOutsideOfAnalysisContext(statement: Statement, callee: DeclaredMethod,
                                                        successor: Statement,
                                                        in:        Set[Fact]): Set[Fact] = {
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

    /**
     * Checks, if a `callee` should be analyzed, i.e. callFlow and returnFlow should create facts.
     * True by default. This method can be overwritten by a subclass.
     *
     * @param callee The callee.
     * @return True, by default.
     */
    protected def relevantCallee(callee: DeclaredMethod): Boolean = true

    /**
     * Creates new facts for an assignment. A new fact for the assigned variable will be created,
     * if the expression contains a tainted variable
     *
     * @param expression The source expression of the assignment
     * @param statement The assignment statement
     * @param in The incoming facts
     * @return The new facts, created by the assignment
     */
    private def createNewTaints(expression: Expr[V], statement: Statement, in: Set[Fact]): Set[Fact] =
        expression.astID match {
            case Var.ASTID ⇒
                val definedBy = expression.asVar.definedBy
                in.collect {
                    case Variable(index) if definedBy.contains(index) ⇒
                        Variable(statement.index)
                }
            case ArrayLoad.ASTID ⇒
                val loadExpression = expression.asArrayLoad
                val arrayDefinedBy = loadExpression.arrayRef.asVar.definedBy
                if (in.exists {
                    // One specific array element may be tainted
                    case ArrayElement(index, taintedElement) ⇒
                        val loadedIndex = TaintAnalysis.getIntConstant(loadExpression.index, statement.code)
                        arrayDefinedBy.contains(index) &&
                            (loadedIndex.isEmpty || taintedElement == loadedIndex.get)
                    // Or the whole array
                    case Variable(index) ⇒ arrayDefinedBy.contains(index)
                    case _               ⇒ false
                }) Set(Variable(statement.index))
                else
                    Set.empty
            case GetField.ASTID ⇒
                val get = expression.asGetField
                val objectDefinedBy = get.objRef.asVar.definedBy
                if (in.exists {
                    // The specific field may be tainted
                    case InstanceField(index, _, taintedField) ⇒
                        taintedField == get.name && objectDefinedBy.contains(index)
                    // Or the whole object
                    case Variable(index) ⇒ objectDefinedBy.contains(index)
                    case _               ⇒ false
                })
                    Set(Variable(statement.index))
                else
                    Set.empty
            case GetStatic.ASTID ⇒
                val get = expression.asGetStatic
                if (in.contains(StaticField(get.declaringClass, get.name)))
                    Set(Variable(statement.index))
                else Set.empty
            case BinaryExpr.ASTID | PrefixExpr.ASTID | Compare.ASTID | PrimitiveTypecastExpr.ASTID |
                NewArray.ASTID | ArrayLength.ASTID ⇒
                (0 until expression.subExprCount).foldLeft(Set.empty[Fact])((acc, subExpr) ⇒
                    acc ++ createNewTaints(expression.subExpr(subExpr), statement, in))
            case _ ⇒ Set.empty
        }

    /**
     * Checks, if the result of some variable expression could be tainted.
     *
     * @param expression The variable expression.
     * @param in The current data flow facts.
     * @return True, if the expression could be tainted
     */
    private def isTainted(expression: Expr[V], in: Set[Fact]): Boolean = {
        val definedBy = expression.asVar.definedBy
        expression.isVar && in.exists {
            case Variable(index)            ⇒ definedBy.contains(index)
            case ArrayElement(index, _)     ⇒ definedBy.contains(index)
            case InstanceField(index, _, _) ⇒ definedBy.contains(index)
            case _                          ⇒ false
        }
    }
}

/**
 * The IFDSProperty for this analysis.
 */
case class Taint(flows: Map[Statement, Set[Fact]]) extends IFDSProperty[Fact] {

    override type Self = Taint

    override def key: PropertyKey[Taint] = Taint.key
}

object Taint extends IFDSPropertyMetaInformation[Fact] {

    override type Self = Taint

    val key: PropertyKey[Taint] = PropertyKey.create("Taint", new Taint(Map.empty))
}
