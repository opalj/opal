/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.python

import org.opalj.br.{Method, ObjectType}
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.ifds.Dependees.Getter
import org.opalj.tac.fpcf.analyses.ifds.JavaIFDSProblem.{NO_MATCH, V}
import org.opalj.tac.{AITACode, Assignment, Call, ComputeTACAIKey, Expr, ReturnValue, TACMethodParameter, TACode}
import org.opalj.tac.fpcf.analyses.ifds.{JavaIFDSProblem, JavaMethod, JavaStatement}
import org.opalj.tac.fpcf.analyses.ifds.taint.{ArrayElement, FlowFact, ForwardTaintProblem, TaintFact, TaintNullFact, Variable}
import org.opalj.value.ValueInformation

import scala.annotation.nowarn

/**
 * Java IFDS analysis that is able to resolve calls to JavaScript.
 *
 * @param p project
 */
class PythonAwareTaintProblem(p: SomeProject) extends ForwardTaintProblem(p) {
    final type TACAICode = TACode[TACMethodParameter, JavaIFDSProblem.V]
    val tacaiKey: Method => AITACode[TACMethodParameter, ValueInformation] = p.get(ComputeTACAIKey)

    /**
     * Called, when the exit to return facts are computed for some `callee` with the null fact and
     * the callee's return value is assigned to a variable.
     * Creates a taint, if necessary.
     *
     * @param callee The called method.
     * @param call   The call.
     * @return Some variable fact, if necessary. Otherwise none.
     */
    override protected def createTaints(callee: Method, call: JavaStatement): Set[TaintFact] =
        if (callee.name == "source") Set(Variable(call.index))
        else Set.empty

    /**
     * Called, when the call to return facts are computed for some `callee`.
     * Creates a FlowFact, if necessary.
     *
     * @param callee The method, which was called.
     * @param call   The call.
     * @return Some FlowFact, if necessary. Otherwise None.
     */
    override protected def createFlowFact(
        callee: Method,
        call:   JavaStatement,
        in:     TaintFact
    ): Option[FlowFact] =
        if (callee.name == "sink" && in == Variable(-2))
            Some(FlowFact(Seq(JavaMethod(call.method), JavaMethod(callee))))
        else None

    /**
     * The entry points of this analysis.
     */
    override def entryPoints: Seq[(Method, TaintFact)] =
        for {
            m <- p.allMethodsWithBody
            if m.name == "main"
        } yield m -> TaintNullFact

    /**
     * Checks, if some `callee` is a sanitizer, which sanitizes its return value.
     * In this case, no return flow facts will be created.
     *
     * @param callee The method, which was called.
     * @return True, if the method is a sanitizer.
     */
    override protected def sanitizesReturnValue(callee: Method): Boolean = callee.name == "sanitize"

    /**
     * Called in callToReturnFlow. This method can return whether the input fact
     * will be removed after `callee` was called. I.e. the method could sanitize parameters.
     *
     * @param call The call statement.
     * @param in   The fact which holds before the call.
     * @return Whether in will be removed after the call.
     */
    override protected def sanitizesParameter(call: JavaStatement, in: TaintFact): Boolean = false

    override def callFlow(call: JavaStatement, callee: Method, in: TaintFact): Set[TaintFact] = {
        val callObject = JavaIFDSProblem.asCall(call.stmt)
        val allParams = callObject.allParams

        val allParamsWithIndices = allParams.zipWithIndex
        in match {
            case BindingFact(index, keyName) => allParamsWithIndices.flatMap {
                case (param, paramIndex) if param.asVar.definedBy.contains(index) =>
                    Some(BindingFact(JavaIFDSProblem.switchParamAndVariableIndex(
                        paramIndex,
                        callee.isStatic
                    ), keyName))
                case _ => None // Nothing to do
            }.toSet
            case _ => super.callFlow(call, callee, in)
        }
    }

    override def returnFlow(exit: JavaStatement, in: TaintFact, call: JavaStatement,
                            callFact: TaintFact, successor: JavaStatement): Set[TaintFact] = {
        if (!isPossibleReturnFlow(exit, successor)) return Set.empty
        val callee = exit.callable
        if (sanitizesReturnValue(callee)) return Set.empty
        val callStatement = JavaIFDSProblem.asCall(call.stmt)
        val allParams = callStatement.allParams

        in match {
            case BindingFact(index, keyName) =>
                var flows: Set[TaintFact] = Set.empty
                if (index < 0 && index > -100) {
                    val param = allParams(
                        JavaIFDSProblem.switchParamAndVariableIndex(index, callee.isStatic)
                    )
                    flows ++= param.asVar.definedBy.map(i => BindingFact(i, keyName))
                }
                if (exit.stmt.astID == ReturnValue.ASTID && call.stmt.astID == Assignment.ASTID) {
                    val returnValueDefinedBy = exit.stmt.asReturnValue.expr.asVar.definedBy
                    if (returnValueDefinedBy.contains(index))
                        flows += BindingFact(call.index, keyName)
                }
                flows
            case _ => super.returnFlow(exit, in, call, callFact, successor)
        }
    }

    /**
     * Kills the flow.
     *
     * @return empty set
     */
    def killFlow(
        @nowarn call:            JavaStatement,
        @nowarn successor:       JavaStatement,
        @nowarn in:              TaintFact,
        @nowarn dependeesGetter: Getter
    ): Set[TaintFact] = Set.empty

    val scriptEngineMethods: Map[ObjectType, List[String]] = Map(
        ObjectType("javax/script/Invocable") -> List("invokeFunction"),
        ObjectType("javax/script/ScriptEngine") -> List("put", "get", "eval"),
        ObjectType("javax/script/Bindings") -> List("put", "get", "putAll", "remove"),
    )

    /**
     * Checks whether we handle the method.
     *
     * @param objType type of the base object
     * @param methodName method name of the call
     * @return true if we have a rule for the method call
     */
    def invokesScriptFunction(objType: ObjectType, methodName: String): Boolean =
        scriptEngineMethods.exists(kv => objType.isSubtypeOf(kv._1)(p.classHierarchy) && kv._2.contains(methodName))

    def invokesScriptFunction(callStmt: Call[JavaIFDSProblem.V]): Boolean =
        invokesScriptFunction(callStmt.declaringClass.mostPreciseObjectType, callStmt.name)

    def invokesScriptFunction(method: Method): Boolean =
        invokesScriptFunction(method.classFile.thisType, method.name)

    /**
     * Kill the flow if we handle the call outside.
     */
    override def outsideAnalysisContext(callee: Method): Option[OutsideAnalysisContextHandler] = {
        if (invokesScriptFunction(callee)) {
            Some(killFlow _)
        } else {
            super.outsideAnalysisContext(callee)
        }
    }

    /**
     * Returns all possible constant strings. Contains the empty string if at least one was non-constant.
     *
     * @param method method
     * @param defSites def sites of the queried variable
     * @return
     */
    private def getPossibleStrings(method: Method, defSites: IntTrieSet): Set[String] = {
        val taCode = tacaiKey(method)

        // TODO: use string analysis here
        defSites.map(site => taCode.stmts.apply(site)).map {
            case a: Assignment[JavaIFDSProblem.V] if a.expr.isStringConst =>
                a.expr.asStringConst.value
            case _ => ""
        }
    }

    val analysisCaller = new PythonAnalysisCaller(p)

    override def callToReturnFlow(call: JavaStatement, in: TaintFact, successor: JavaStatement): Set[TaintFact] = {
        val callStmt = JavaIFDSProblem.asCall(call.stmt)
        val allParams = callStmt.allParams
        val allParamsWithIndex = callStmt.allParams.zipWithIndex

        if (!invokesScriptFunction(callStmt)) {
            in match {
                case BindingFact(index, _) =>
                    if (JavaIFDSProblem.getParameterIndex(allParamsWithIndex, index) == NO_MATCH)
                        Set(in)
                    else
                        Set()
                case _ => super.callToReturnFlow(call, in, successor)
            }
        } else {
            in match {
                /* Call to invokeFunction. The variable length parameter list is an array in TACAI. */
                case arrIn: ArrayElement if callStmt.name == "invokeFunction"
                    && JavaIFDSProblem.getParameterIndex(allParamsWithIndex, arrIn.index) == -3 =>
                    val fNames = getPossibleStrings(call.method, allParams(1).asVar.definedBy)
                    fNames.map(fName =>
                        if (fName == "")
                            /* Function name is unknown. We don't know what to call */
                            None
                        else
                            Some(analysisCaller.analyze(call, arrIn, fName))).filter(_.isDefined).flatMap(_.get) ++ Set(in)
                /* Call to eval. */
                case f: BindingFact if callStmt.name == "eval"
                    && (JavaIFDSProblem.getParameterIndex(allParamsWithIndex, f.index) == -1
                        || JavaIFDSProblem.getParameterIndex(allParamsWithIndex, f.index) == -3) =>
                    analysisCaller.analyze(call, f)
                case f: WildcardBindingFact if callStmt.name == "eval"
                    && (JavaIFDSProblem.getParameterIndex(allParamsWithIndex, f.index) == -1
                        || JavaIFDSProblem.getParameterIndex(allParamsWithIndex, f.index) == -3) =>
                    analysisCaller.analyze(call, f)
                /* Put obj in Binding */
                case Variable(index) if callStmt.name == "put" && JavaIFDSProblem.getParameterIndex(allParamsWithIndex, index) == -3 =>
                    val keyNames = getPossibleStrings(call.method, allParams(1).asVar.definedBy)
                    val defSites = callStmt.receiverOption.get.asVar.definedBy
                    keyNames.flatMap(keyName => defSites.map(i => if (keyName == "") WildcardBindingFact(i) else BindingFact(i, keyName))) ++ Set(in)
                /* putAll BindingFact to other BindingFact */
                case BindingFact(index, keyName) if callStmt.name == "putAll" && JavaIFDSProblem.getParameterIndex(allParamsWithIndex, index) == -2 =>
                    callStmt.receiverOption.get.asVar.definedBy.map(i => if (keyName == "") WildcardBindingFact(i) else BindingFact(i, keyName)) ++ Set(in)
                case WildcardBindingFact(index) if callStmt.name == "putAll" && JavaIFDSProblem.getParameterIndex(allParamsWithIndex, index) == -2 =>
                    callStmt.receiverOption.get.asVar.definedBy.map(i => WildcardBindingFact(i)) ++ Set(in)
                /* Overwrite BindingFact */
                case BindingFact(index, keyName) if callStmt.name == "put" && JavaIFDSProblem.getParameterIndex(allParamsWithIndex, index) == -1 =>
                    val possibleFields = getPossibleStrings(call.method, allParams(1).asVar.definedBy)
                    if (possibleFields.size == 1 && possibleFields.contains(keyName))
                        /* Key is definitely overwritten */
                        Set()
                    else
                        Set(in)
                case WildcardBindingFact(index) if callStmt.name == "put" && JavaIFDSProblem.getParameterIndex(allParamsWithIndex, index) == -1 =>
                    /* We never overwrite here as we don't know the key */
                    Set(in)
                /* Remove BindingFact */
                case BindingFact(index, keyName) if callStmt.name == "remove" && JavaIFDSProblem.getParameterIndex(allParamsWithIndex, index) == -1 =>
                    val possibleFields = getPossibleStrings(call.method, allParams(1).asVar.definedBy)
                    if (possibleFields.size == 1 && possibleFields.contains(keyName))
                        Set()
                    else
                        Set(in)
                case WildcardBindingFact(index) if callStmt.name == "remove" && JavaIFDSProblem.getParameterIndex(allParamsWithIndex, index) == -1 =>
                    /* We never kill here as we don't know the key */
                    Set(in)
                /* get from BindingFact */
                case BindingFact(index, keyName) if callStmt.name == "get" && JavaIFDSProblem.getParameterIndex(allParamsWithIndex, index) == -1 =>
                    val possibleFields = getPossibleStrings(call.method, allParams(1).asVar.definedBy)
                    if (possibleFields.size == 1 && possibleFields.contains(keyName))
                        Set(Variable(call.index), in)
                    else
                        Set(in)
                case WildcardBindingFact(index) if callStmt.name == "get" && JavaIFDSProblem.getParameterIndex(allParamsWithIndex, index) == -1 =>
                    Set(Variable(call.index), in)
                case _ => Set(in)
            }
        }
    }

    override def isTainted(expression: Expr[V], in: TaintFact): Boolean = {
        val definedBy = expression.asVar.definedBy
        expression.isVar && (in match {
            case BindingFact(index, _) => definedBy.contains(index)
            case _                     => super.isTainted(expression, in)
        })
    }
}
