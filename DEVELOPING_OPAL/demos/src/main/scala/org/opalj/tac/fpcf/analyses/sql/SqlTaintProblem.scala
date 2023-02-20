/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tac.fpcf.analyses.sql

import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.ifds.IFDSAnalysis
import org.opalj.tac.fpcf.analyses.ifds.JavaIFDSProblem.V
import org.opalj.tac.fpcf.analyses.ifds.taint.ForwardTaintProblem
import org.opalj.tac.{ArrayLoad, ArrayStore, Expr, GetField, GetStatic, PutField, PutStatic, Stmt, StringConst, VirtualFunctionCall}
import org.opalj.tac.fpcf.analyses.ifds.{JavaIFDSProblem, JavaMethod, JavaStatement}
import org.opalj.tac.fpcf.properties._


class SqlTaintAnalysis(project: SomeProject)
  extends IFDSAnalysis()(project, new SqlTaintProblem(project), Taint)

/**
 * Java IFDS analysis that is able to resolve calls of sql statements.
 *
 * @param p project
 */
class SqlTaintProblem(p: SomeProject) extends ForwardTaintProblem(p) {
    /*
    final type TACAICode = TACode[TACMethodParameter, JavaIFDSProblem.V]
    val tacaiKey: Method => AITACode[TACMethodParameter, ValueInformation] = p.get(ComputeTACAIKey)

     */

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

    override def normalFlow(statement:JavaStatement,in:TaintFact,predecessor:Option[JavaStatement]): Set[TaintFact] ={
        in match {
            case sqlTaintFact:SqlTaintFact=>  Set(sqlTaintFact)
            case _ =>  super.normalFlow(statement, in, predecessor)
        }
    }

    override def callFlow(call: JavaStatement, callee: Method, in: TaintFact): Set[TaintFact] = {
            in match {
                case sqlTaintFact: SqlTaintFact => Set(sqlTaintFact)
                case _=>  super.callFlow(call, callee, in)
            }
    }

    override def returnFlow(exit: JavaStatement, in: TaintFact, call: JavaStatement, callFact: TaintFact, successor: JavaStatement): Set[TaintFact] = {
        in match {
            case sqlTaintFact: SqlTaintFact => Set(sqlTaintFact) ++  super.returnFlow(exit, in, call, callFact, successor)
            case _=>     super.returnFlow(exit, in, call, callFact, successor)
        }
    }



/**
     * Returns all possible constant strings. Contains the empty string if at least one was non-constant.
     *
     * @param method method
     * @param defSites def sites of the queried variable
     * @return
     */
    /*private def getPossibleStrings(method: Method, defSites: IntTrieSet): Set[String] = {
        val taCode = tacaiKey(method)

        // TODO: use string analysis here
        defSites.map(site => taCode.stmts.apply(site)).map {
            case a: Assignment[JavaIFDSProblem.V] if a.expr.isStringConst =>
                a.expr.asStringConst.value
            case _ => ""
        }
    } */

    override def callToReturnFlow(call: JavaStatement, in: TaintFact, successor: JavaStatement): Set[TaintFact] = {
        var flow:Set[TaintFact] = Set(in)
        val callStmt = JavaIFDSProblem.asCall(call.stmt)

        callStmt.name match {
            case "valueOf" if callStmt.declaringClass.toJava == "java.lang.Integer" =>
                in match {
                    case Variable(index) if  callStmt.params.find(parm => parm.asVar.definedBy.contains(index)).nonEmpty => flow += Variable(call.index)
                    case _ =>
                }
                flow
            case "intValue" =>
                val leftSideVar = callStmt.receiverOption.get.asVar
                in match {
                    case Variable(index) if leftSideVar.definedBy.contains(index) => flow += Variable(call.index)
                    case _ =>
                }
                flow
            case "append" =>
                val rightSideVar = callStmt.params.head.asVar
                val leftSideVar = callStmt.receiverOption.get.asVar
                in match {
                    case Variable(index) if leftSideVar.definedBy.contains(index)||rightSideVar.definedBy.contains(index) =>
                        flow += Variable(call.index)
                    case _=>
                }
                flow
            case "toString" =>
                val leftSideVar = callStmt.receiverOption.get.asVar
                in match {
                    case Variable(index) if leftSideVar.definedBy.contains(index) => flow += Variable(call.index)
                    case _ =>
                }
                flow
            case "executeUpdate" =>
                if(callStmt.params.size >0){
                    val possibleParamStrings = getPossibleStrings2(callStmt.params(0),call.code,in)
                    possibleParamStrings.foreach(input =>
                        if (
                            input.contains("TAINTED_VALUE") &&
                              SqlStringAnalyzer.checkSQLStringSyntax3(input)
                              &&  SqlStringAnalyzer.doAnalyze(input, new SqlTaintMemory(Set("TAINTED_VALUE","'TAINTED_VALUE'"))) ) {
                            flow += SqlTaintFact(SqlStringAnalyzer.taintMemory)
                        })
                }
                flow
            case "executeQuery" =>
                in match {
                    case sqlTaintFact: SqlTaintFact =>
                        val possibleParamStrings  =  getPossibleStrings2(callStmt.params(0),call.code,in)
                        possibleParamStrings.foreach(string => {
                            if(SqlStringAnalyzer.checkSQLStringSyntax3(string)
                              && SqlStringAnalyzer.doAnalyze(string,sqlTaintFact.sqlTaintMemory)
                              && call.stmt.isAssignment){
                                flow += Variable(call.index)
                            }
                        })
                    case _ =>
                }
                flow
            case "source" =>
                in match {
                    case TaintNullFact => flow += Variable(call.index)
                    case _ =>
                }
                flow
            case "sink" =>
                in match {
                    case Variable(index) if callStmt.params.find(parm => parm.asVar.definedBy.contains(index)).nonEmpty =>
                        flow += FlowFact(Seq(JavaMethod(call.method)))
                    case _=>
                }
                flow
            case _ =>
                icfg.getCalleesIfCallStatement(call) match {
                    case Some(callee) if callee.isEmpty => flow
                    case _ => super.callToReturnFlow(call, in, successor)
                }

        }
    }

    override def isTainted(expression: Expr[V], in: TaintFact): Boolean = {
        super.isTainted(expression, in)
    }


    /**
     * Returns all possible constant strings. Concatenations of constants using the "append" function are reconstructed.
     * tainted variables are replaced by taint identifiers.
     *
     *
     * @param param
     * @param stmts
     * @param in
     * @return
     */
    def getPossibleStrings2(param:Expr[V], stmts:  Array[Stmt[V]], in:TaintFact):Set[String] = {
        // Initialize the set of possible strings and taint status
        var possibleStrings: Set[String] = Set()
        val defSites = param.asVar.definedBy

        // Go through all defsites to find out more possible strings
        for (defSiteIndex <- defSites) {
            var currentSetOfString: Set[String] = Set()

            in match {
                // If the expression for the index is maintained, we replace it with the TAINTED_VALUE
                case Variable(index) if index == defSiteIndex =>
                    currentSetOfString = Set("TAINTED_VALUE")
                case ArrayElement(index: Int,_) if index == defSiteIndex =>
                    currentSetOfString = Set("TAINTED_VALUE")
                case InstanceField(index: Int, _, _) if index == defSiteIndex=>
                    currentSetOfString = Set("TAINTED_VALUE")
                case _ if defSiteIndex >= 0 =>{
                    val expr = stmts(defSiteIndex).asAssignment.expr

                    //Weitere Untersuchung im Ausdruck
                    expr match {
                        // Den Wert aus der String Konstante nehmen
                        case StringConst(_, value) => currentSetOfString = Set(value)

                        case GetField(_,_,_,_,_) =>
                            // Um ein Wert zu kriegen suchen wir nach dem letzten PutField
                            val lastPut = stmts.lastIndexWhere(stmt => stmt.astID == PutField.ASTID)
                            if(lastPut >0){
                                val putFieldExpression = stmts(lastPut).asPutField.value
                                 currentSetOfString = getPossibleStrings2(putFieldExpression,stmts, in)
                            }

                        case gstc:GetStatic =>
                            val lastPut = stmts.lastIndexWhere(stmt => stmt.astID == PutStatic.ASTID)
                            if(lastPut > 0){
                                val putExpression = stmts(lastPut).asPutStatic.value
                                currentSetOfString = getPossibleStrings2(putExpression,stmts, in)
                            }

                        case ArrayLoad(pc,index,arrayRef) =>
                            val elementIndex = index.asVar.value
                            val lastArrayStoreIndex = stmts.lastIndexWhere(stmt => stmt.astID == ArrayStore.ASTID &&
                              stmt.asArrayStore.index.asVar.value == elementIndex)
                            if(lastArrayStoreIndex > 0){
                                val arrayElementExpression = stmts(lastArrayStoreIndex).asArrayStore.value
                                currentSetOfString = getPossibleStrings2(arrayElementExpression,stmts,in)
                            }

                        case VirtualFunctionCall(_, _, _, name, _, receiver, _) if name == "toString" =>
                             currentSetOfString = getPossibleStrings2(receiver.asVar, stmts, in)

                        case VirtualFunctionCall(_, _, _, name, _, receiver, params) if name == "append" =>

                            //Alle möglichen Strings die aus dem receiver gewonnen der funktion append werden können
                            val leftSideString = getPossibleStrings2(receiver.asVar, stmts, in)

                            //Alle möglichen Strings die aus dem parameter gewonnen der funktion append werden können
                            val rightSideString = getPossibleStrings2(params.head.asVar, stmts, in)

                            //Alle Strings die auf der linken seite entstehen können mit allen auf der rechten kombinieren
                            for {
                                leftString <- leftSideString
                                rightString <- rightSideString
                            } {
                                currentSetOfString += leftString + rightString
                            }
                        case _ => currentSetOfString = Set("")
                    }
                }
                case _ =>  currentSetOfString = Set("PARAM_VALUE")

            }
            possibleStrings = possibleStrings ++ currentSetOfString
        }
        possibleStrings
    }


    // TODO: use string analysis here
    def getPossibleStringsAndTaintStatus(param:Expr[V], stmts:  Array[Stmt[V]], in:TaintFact):(Set[String], Boolean) = {
        val defSites = param.asVar.definedBy
        var result: (Set[String], Boolean) = (Set(""), false)
        for (defSiteIndex <- defSites) {
            if (defSiteIndex >= 0) {
                val expr = stmts(defSiteIndex).asAssignment.expr

                in match {
                    case Variable(index) if(index == defSiteIndex) =>
                        result = (result._1 ++ Set("TAINTED_VALUE"), true)
                    case ArrayElement(index: Int,_) if(index == defSiteIndex) =>
                        result = (result._1 ++ Set("TAINTED_VALUE"), true)
                    case InstanceField(index: Int, _, _) if(index == defSiteIndex)=>
                        result = (result._1 ++ Set("TAINTED_VALUE"), true)
                    case _ =>
                        expr match {

                            case StringConst(_, value) =>
                                result = (result._1 ++ Set(value), result._2 )

                            case GetField(_,_,_,_,_) =>
                                // letzte Putfield suchen von dort weiter
                                val lastPut = stmts.lastIndexWhere(stmt => stmt.astID == PutField.ASTID)
                                if(lastPut >0){
                                    val putFieldExpression = stmts(lastPut).asPutField.value
                                    val tmp = getPossibleStringsAndTaintStatus(putFieldExpression,stmts, in)
                                    result = (result._1 ++ tmp._1, result._2 || tmp._2 )
                                }


                            case gstc:GetStatic =>
                                val lastPut = stmts.lastIndexWhere(stmt => stmt.astID == PutStatic.ASTID)
                                if(lastPut > 0){
                                    val putExpression = stmts(lastPut).asPutStatic.value
                                    val tmp = getPossibleStringsAndTaintStatus(putExpression,stmts, in)
                                    result = (result._1 ++ tmp._1, result._2 || tmp._2 )
                                }

                            case VirtualFunctionCall(_, _, _, name, _, receiver, _) if name == "toString" =>
                                val tmp = getPossibleStringsAndTaintStatus(receiver.asVar, stmts, in)
                                result = (result._1 ++ tmp._1, result._2 || tmp._2 )

                            case VirtualFunctionCall(_, _, _, name, _, receiver, params) if name == "append" =>


                                val leftSideString = getPossibleStringsAndTaintStatus(receiver.asVar, stmts, in)
                                val rightSideString = getPossibleStringsAndTaintStatus(params.head.asVar, stmts, in)
                                var possibleString: Set[String] = Set()

                                for {
                                    leftString <- leftSideString._1
                                    rightString <- rightSideString._1
                                } {
                                    possibleString += leftString + rightString
                                }
                                result = (result._1 ++ possibleString, result._2 || leftSideString._2 || rightSideString._2)

                            case _ =>
                        }
                }
            }else{
                in match {
                    case Variable(index) if(index == defSiteIndex) =>
                        result = (result._1 ++ Set("TAINTED_VALUE"), true)
                    case ArrayElement(index: Int,_) if(index == defSiteIndex) =>
                        result = (result._1 ++ Set("TAINTED_VALUE"), true)
                    case InstanceField(index: Int, _, _) if(index == defSiteIndex)=>
                        result = (result._1 ++ Set("TAINTED_VALUE"), true)
                    case _ =>   result = (result._1 ++ Set("PARAM_VALUE"), false)
                }
            }
        }

        result
    }

}
