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

        /*
        val callObject = JavaIFDSProblem.asCall(call.stmt)
        val allParams = callObject.allParams
        val allParamsWithIndices = allParams.zipWithIndex

        var facts:Set[TaintFact] =  Set()

        for( (param,paramINdex) <- allParamsWithIndices){
           if(callee.descriptor.parameterTypes.head == ObjectType.String){
               val x = getPossibleString(param,call.code,in)
              /* println(call.index)
               println(in)
               println(x)
               */
               if(x._1.nonEmpty ){
                   facts += StringValue(JavaIFDSProblem.switchParamAndVariableIndex(paramINdex,callee.isStatic),x._1,x._2)
               }
           }
        }


        def getPossibleString(param:Expr[V], stmts:  Array[Stmt[V]],in:TaintFact):Tuple2[Set[String],Boolean] = {

            val defsites = param.asVar.definedBy
            var result:Tuple2[Set[String],Boolean] = (Set.empty[String],false)
            for (index <- defsites) {

                if(index < 0 )return result
                val expr = call.code(index).asAssignment.expr

                val taintStatuts = in match {
                    case Variable(indexF) => indexF == index
                    case StringValue(indexF,values,taintStatus) => indexF == index
                    case _ => false
                }

                expr match {
                    case StaticFunctionCall(pc, declaringClass, isInterface, name, descriptor, params) if name == "source" =>
                        result =  (Set("TAINTED"), true)

                    case StringConst(_, v) =>  result =  (Set(v), false)

                    case VirtualFunctionCall(pc,declaringClass,isInterface,name,descriptor,receiver,params) if name == "toString" =>
                        val tmp = getPossibleString(receiver.asVar, stmts, in)

                        result =  (tmp._1,tmp._2 || taintStatuts )

                    case VirtualFunctionCall(pc,declaringClass,isInterface,name,descriptor,receiver,params) if name == "append" =>
                        val leftSideString = getPossibleString(receiver.asVar, stmts, in)
                        val rightSideString = getPossibleString(params.head.asVar, stmts, in)
                        var possibleString: Set[String] = Set()

                        for {
                            l <- leftSideString._1
                            r <- rightSideString._1
                        } {
                            possibleString += l + r
                        }
                        result =  (possibleString, leftSideString._2 || rightSideString._2 || taintStatuts)

                    case _ =>  result =  (Set(""), false)
                }

            }
            result
        }




       /*
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

        */
        if(callee.name == "sink")Set.empty
        else super.callFlow(call, callee, in)

         */

            in match {
                case sqlTaintFact: SqlTaintFact => Set(sqlTaintFact)
                case _=>  super.callFlow(call, callee, in)
            }



    }

    override def returnFlow(exit: JavaStatement, in: TaintFact, call: JavaStatement,
                            callFact: TaintFact, successor: JavaStatement): Set[TaintFact] = {

        in match {
            case sqlTaintFact: SqlTaintFact => Set(sqlTaintFact) ++  super.returnFlow(exit, in, call, callFact, successor)
            case _=>     super.returnFlow(exit, in, call, callFact, successor)
        }


        /*
        if (!isPossibleReturnFlow(exit, successor)) return Set.empty
        val callee = exit.callable
        if (sanitizesReturnValue(callee)) return Set.empty
        val callStatement = JavaIFDSProblem.asCall(call.stmt)
        val allParams = callStatement.allParams

        in match {
            case StringValue(index, values, taintStatus) =>
                var flows: Set[TaintFact] = Set.empty
                if (index < 0 && index > -100) {
                    val param = allParams(
                        JavaIFDSProblem.switchParamAndVariableIndex(index, callee.isStatic)
                    )
                    flows ++= param.asVar.definedBy.map(i => StringValue(i, values, taintStatus))
                }
                if (exit.stmt.astID == ReturnValue.ASTID && call.stmt.astID == Assignment.ASTID) {
                    val returnValueDefinedBy = exit.stmt.asReturnValue.expr.asVar.definedBy
                    if (returnValueDefinedBy.contains(index))
                        flows += StringValue(call.index, values, taintStatus)
                }
                flows

            case _ => super.returnFlow(exit, in, call, callFact, successor)
        }

         */
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
                              SqlStringTaintAnalyzer.checkSQLStringSyntax3(input)
                              &&  SqlStringTaintAnalyzer.doAnalyze(input, new SqlTaintMemory(Set("TAINTED_VALUE","'TAINTED_VALUE'"))) ) {
                            flow += SqlTaintFact(SqlStringTaintAnalyzer.taintMemory)
                        })
                }
                flow

            case "executeQuery" =>
                in match {
                    case sqlTaintFact: SqlTaintFact =>
                        val possibleParamStrings  =  getPossibleStrings2(callStmt.params(0),call.code,in)
                        possibleParamStrings.foreach(string => {
                            if(SqlStringTaintAnalyzer.checkSQLStringSyntax3(string)
                              && SqlStringTaintAnalyzer.doAnalyze(string,sqlTaintFact.sqlTaintMemory)
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


            //TODO sink case ?
            // TODO filter for Analye, statt in der Analyse filtern.
                 //  +1 für Calltiefe => weniger speicher auf heap
                // +1 weniger calls
                // +1 performance

        }


  /*
        val callStmt = JavaIFDSProblem.asCall(call.stmt)
        // val allParams = callStmt.allParams
        val allParamsWithIndex = callStmt.allParams.zipWithIndex

        // if (!invokesScriptFunction(callStmt)) {
        in match {
            case BindingFact(index, _) =>
                if (JavaIFDSProblem.getParameterIndex(allParamsWithIndex, index) == NO_MATCH)
                    Set(in)
                else
                    Set()
            case _ => super.callToReturnFlow(call, in, successor)
        }

   */

        /* } else {
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
                            {}//Some(jsAnalysis.analyze(call, arrIn, fName))).filter(_.isDefined).flatMap(_.get) ++ Set(in)
                /* Call to eval. */
                case f: BindingFact if callStmt.name == "eval"
                    && (JavaIFDSProblem.getParameterIndex(allParamsWithIndex, f.index) == -1
                        || JavaIFDSProblem.getParameterIndex(allParamsWithIndex, f.index) == -3) =>
                {}//jsAnalysis.analyze(call, f)
                case f: WildcardBindingFact if callStmt.name == "eval"
                    && (JavaIFDSProblem.getParameterIndex(allParamsWithIndex, f.index) == -1
                        || JavaIFDSProblem.getParameterIndex(allParamsWithIndex, f.index) == -3) =>
                {}//jsAnalysis.analyze(call, f)
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
        } */
    }

    override def isTainted(expression: Expr[V], in: TaintFact): Boolean = {
        super.isTainted(expression, in)
       /*
        val definedBy = expression.asVar.definedBy
        expression.isVar && (in match {
            case BindingFact(index, _) => definedBy.contains(index)
            case _                     => super.isTainted(expression, in)
        })

        */
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

    def getPossibleStrings2(param:Expr[V], stmts:  Array[Stmt[V]], in:TaintFact):Set[String] = {
        // Initialize the set of possible strings and taint status
        var possibleStrings: Set[String] = Set()
        val defSites = param.asVar.definedBy

        // Durche alle Defsites durchgehen um weitere möglichen Strings zu ermitteln
        for (defSiteIndex <- defSites) {
            var currentSetOfString: Set[String] = Set()

            in match {
                // Wenn der ausdruck für den Intext getaintet ist, ersetzen wir in durch den TAINTIDENTIFIER
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

    //TODO
    // AppendMethode
    //Get Strings used by foo bar.
    //Handle Concatenation.
    //put the Strings in the Prototype
}
