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
     * Definition of rules for implicit data flows for 'executeUpdate', 'executeQuery' function.
     * Definition of rules for 'append','valueOf', 'intValue', 'toString', 'sink' and 'source' function
     * (not optimized and only usable for specific cases)
     *
     * @param call The statement, which invoked the call.
     * @param in The facts, which hold before the `call`.
     * @param successor
     *  @return The facts, which hold after the call independently of what happens in the callee
     *         under the assumption that `in` held before `call`.
     */
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
                    val possibleParamStrings = getPossibleStrings(callStmt.params(0),call.code,in)
                    possibleParamStrings.foreach(input =>
                        if (
                            input.contains("TAINTED_VALUE") &&
                              SqlStringAnalyzer.hasValidSqlSyntax(input)
                              &&  SqlStringAnalyzer.doAnalyze(input, new SqlTaintMemory(Set("TAINTED_VALUE","'TAINTED_VALUE'"))) ) {
                            flow += SqlTaintFact(SqlStringAnalyzer.getTaintMemory())
                        })
                }
                flow
            case "executeQuery" =>
                in match {
                    case sqlTaintFact: SqlTaintFact =>
                        val possibleParamStrings  =  getPossibleStrings(callStmt.params(0),call.code,in)
                        possibleParamStrings.foreach(string => {
                            if(SqlStringAnalyzer.hasValidSqlSyntax(string)
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

    /**
     * Returns all possible constant strings. Concatenations of constants using the "append" function are reconstructed.
     * tainted variables are replaced by taint identifiers.
     *(not optimized and only usable for specific cases)
     *
     * @param param Expression of a parameter for which a string is to be obtained.
     * @param stmts Statements used for reconstruction
     * @param in The fact which holds before the call.
     * @return a set of possible and modified strings
     */
    def getPossibleStrings(param:Expr[V], stmts:  Array[Stmt[V]], in:TaintFact):Set[String] = {
        // Initialize the set of possible strings and taint status
        var possibleStrings: Set[String] = Set()

        //def sites of the queried variable
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

                    //Further expression investigation
                    expr match {
                        // Take the value from the string constant
                        case StringConst(_, value) => currentSetOfString = Set(value)

                        case GetField(_,_,_,_,_) =>
                            //To get a value we look for the last PutField
                            val lastPut = stmts.lastIndexWhere(stmt => stmt.astID == PutField.ASTID)
                            if(lastPut >0){
                                val putFieldExpression = stmts(lastPut).asPutField.value
                                 currentSetOfString = getPossibleStrings(putFieldExpression,stmts, in)
                            }

                        case gstc:GetStatic =>
                            //To get a value we look for the last PutStatic
                            val lastPut = stmts.lastIndexWhere(stmt => stmt.astID == PutStatic.ASTID)
                            if(lastPut > 0){
                                val putExpression = stmts(lastPut).asPutStatic.value
                                currentSetOfString = getPossibleStrings(putExpression,stmts, in)
                            }

                        case ArrayLoad(pc,index,arrayRef) =>
                            val elementIndex = index.asVar.value
                            val lastArrayStoreIndex = stmts.lastIndexWhere(stmt => stmt.astID == ArrayStore.ASTID &&
                              stmt.asArrayStore.index.asVar.value == elementIndex)
                            if(lastArrayStoreIndex > 0){
                                val arrayElementExpression = stmts(lastArrayStoreIndex).asArrayStore.value
                                currentSetOfString = getPossibleStrings(arrayElementExpression,stmts,in)
                            }

                        case VirtualFunctionCall(_, _, _, name, _, receiver, _) if name == "toString" =>
                             currentSetOfString = getPossibleStrings(receiver.asVar, stmts, in)

                        case VirtualFunctionCall(_, _, _, name, _, receiver, params) if name == "append" =>

                            //All possible strings that can be obtained from the receiver of the function 'append'
                            val leftSideString = getPossibleStrings(receiver.asVar, stmts, in)

                            //All possible strings that can be obtained from the parameter of the function 'append'
                            val rightSideString = getPossibleStrings(params.head.asVar, stmts, in)

                            //All strings generated on the left side in combination with all strings on the right side.
                            for {
                                leftString <- leftSideString
                                rightString <- rightSideString
                            } {
                                currentSetOfString += leftString + rightString
                            }
                        case _ => currentSetOfString = Set("")
                    }
                }

                //We assume that an index < 0 is a parameter.
                case _ =>  currentSetOfString = Set("PARAM_VALUE")

            }
            possibleStrings = possibleStrings ++ currentSetOfString
        }
        possibleStrings
    }
}
