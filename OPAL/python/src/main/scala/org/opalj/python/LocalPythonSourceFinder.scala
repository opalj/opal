/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.python

import org.opalj.br.{Method, ObjectType}
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.IntTrieSet
import org.opalj.tac.{AITACode, ASTNode, Assignment, Call, ComputeTACAIKey, Expr, ExprStmt, MethodCall, Stmt, TACMethodParameter, TACode}
import org.opalj.tac.fpcf.analyses.ifds.JavaIFDSProblem.V
import org.opalj.tac.fpcf.analyses.ifds.{JavaIFDSProblem, JavaStatement}
import org.opalj.value.ValueInformation

import scala.collection.mutable

/**
 * Uses the def-use information of TACAI to find the javascript sources passed
 * to an ScriptEngine within method boundaries.
 *
 * @param p Project
 */
class LocalPythonSourceFinder(val p: SomeProject) extends (JavaStatement => Set[PythonSource]) {
    final type TACAICode = TACode[TACMethodParameter, JavaIFDSProblem.V]
    val tacaiKey: Method => AITACode[TACMethodParameter, ValueInformation] = p.get(ComputeTACAIKey)

    /* Maps a statement defining a Python source to the instance.  */
    val statementToPythonSource: mutable.Map[Stmt[V], PythonSource] = mutable.Map()
    /* Maps the statement to search from to the resulting set of PythonSources.  */
    val pythonSourceCache: mutable.Map[(JavaStatement, Expr[V]), Set[PythonSource]] = mutable.Map()

    /**
     * @see [[LocalPythonSourceFinder.findPythonSourceOnInvokeFunction()]]
     */
    override def apply(stmt: JavaStatement): Set[PythonSource] = findPythonSourceOnInvokeFunction(stmt, JavaIFDSProblem.asCall(stmt.stmt).allParams.head.asVar)

    /**
     * In case of a snippet like this:
     * 1 ScriptEngine se = ...;
     * 2 se.eval("Python Code");
     * 3 ((Invocable) se).invokeFunction("myFunction", args...);
     * This functions finds "Python Code" given "se" at se.invokeFunction().
     *
     * @param javaStmt statement to start with
     * @param arg    ScriptEngine variable
     * @return javascript source code
     */
    private def findPythonSourceOnInvokeFunction(javaStmt: JavaStatement, arg: Expr[V]): Set[PythonSource] = {
        val decls = findCallOnObject(javaStmt.method, arg.asVar.definedBy, "getEngineByName")

        val maybeCached = pythonSourceCache.get((javaStmt, arg))
        if (maybeCached.isDefined)
            maybeCached.get
        else
            decls.flatMap(decl => {
                val evals = findCallOnObject(
                    javaStmt.method,
                    decl.asAssignment.targetVar.usedBy,
                    "eval"
                )
                val pythonSources = evals.flatMap(eval => {
                    val evalCall = JavaIFDSProblem.asCall(eval)
                    varToPythonSource(javaStmt.method, evalCall.params.head.asVar)
                })
                pythonSourceCache += (javaStmt, arg) -> pythonSources
                pythonSources
            })
    }

    /**
     * Finds all definiton/use sites inside the method.
     *
     * @param method method to be searched in
     * @param sites  definition or use sites
     * @return sites as JavaStatement
     */
    private def searchStmts(method: Method, sites: IntTrieSet): Set[Stmt[JavaIFDSProblem.V]] = {
        val taCode = tacaiKey(method)
        sites.map(site => taCode.stmts.apply(site))
    }

    /**
     * If stmt is a call, return it as a FunctionCall
     *
     * @param stmt Statement
     * @return maybe a function call
     */
    private def maybeCall(stmt: Stmt[JavaIFDSProblem.V]): Option[Call[JavaIFDSProblem.V]] = {
        def isCall(node: ASTNode[JavaIFDSProblem.V]) = node match {
            case expr: Expr[JavaIFDSProblem.V] => expr.isVirtualFunctionCall || expr.isStaticFunctionCall
            case stmt: Stmt[JavaIFDSProblem.V] => (stmt.isNonVirtualMethodCall
                || stmt.isVirtualMethodCall
                || stmt.isStaticMethodCall)
        }

        stmt match {
            case exprStmt: ExprStmt[JavaIFDSProblem.V] if isCall(exprStmt.expr) =>
                Some(exprStmt.expr.asFunctionCall)
            case assignStmt: Assignment[JavaIFDSProblem.V] if isCall(assignStmt.expr) =>
                Some(assignStmt.expr.asFunctionCall)
            case call: MethodCall[JavaIFDSProblem.V] if isCall(stmt) =>
                Some(call)
            case _ => None
        }
    }

    /**
     * Finds instance method calls.
     *
     * @param method Method to search in.
     * @param sites def/use sites
     * @param methodName searched method name as string
     * @return
     */
    private def findCallOnObject(method: Method, sites: IntTrieSet, methodName: String): Set[Stmt[V]] = {
        val stmts = searchStmts(method, sites)
        stmts.map(stmt => maybeCall(stmt) match {
            case Some(call) if call.name.equals(methodName) => Some(stmt)
            case _                                          => None
        }).filter(_.isDefined).map(_.get)
    }

    /**
     * Tries to resolve a variable either to a string constant or a file path containing the variable's value
     *
     * @param method   method to be searched in
     * @param variable variable of interest
     * @return PythonSource
     */
    private def varToPythonSource(method: Method, variable: JavaIFDSProblem.V): Set[PythonSource] = {
        val resultSet: mutable.Set[PythonSource] = mutable.Set()

        // TODO: use a string analysis instead of 'Assignment with a.expr.isStringConst' here

        def findFileArg(sites: IntTrieSet): Unit = {
            val calls = findCallOnObject(method, sites, "<init>")
            calls.foreach(init => {
                val defSitesOfFileSrc = init.asInstanceMethodCall.params.head.asVar.definedBy
                val defs = searchStmts(method, defSitesOfFileSrc)
                defs.foreach {
                    /* new File("path/to/src"); */
                    case a: Assignment[JavaIFDSProblem.V] if a.expr.isStringConst =>
                    //                        resultSet.add(statementToPythonSource.getOrElseUpdate(a,
                    //                            PythonFileSource(a.expr.asStringConst.value)))
                    /* File constructor argument is no string constant */
                    case _ =>
                }
            })
        }

        def findFileReaderArg(sites: IntTrieSet): Unit = {
            val calls = findCallOnObject(method, sites, "<init>")
            calls.foreach(init => {
                val defSitesOfFileReaderSrc = init.asInstanceMethodCall.params.head.asVar.definedBy
                val defs = searchStmts(method, defSitesOfFileReaderSrc)
                defs.foreach {
                    /* FileReader fr = new FileReader(new File("path/to/src")); */
                    case a: Assignment[JavaIFDSProblem.V] if a.expr.isStringConst =>
                    //                        resultSet.add(statementToPythonSource.getOrElseUpdate(a,
                    //                            PythonFileSource(a.expr.asStringConst.value)))
                    /* new FileReader(new File(...)); */
                    case a: Assignment[JavaIFDSProblem.V] if a.expr.isNew =>
                        if (a.expr.asNew.tpe.isSubtypeOf(ObjectType("java/io/File"))(p.classHierarchy))
                            findFileArg(a.targetVar.usedBy)
                    // Unknown argument
                    case _ =>
                }
            })
        }

        val nextPythonTmts = searchStmts(method, variable.definedBy)
        nextPythonTmts.foreach {
            /* se.eval("function() ..."); */
            case a: Assignment[JavaIFDSProblem.V] if a.expr.isStringConst =>
                resultSet.add(statementToPythonSource.getOrElseUpdate(a, PythonStringSource(a.expr.asStringConst.value)))
            case a: Assignment[JavaIFDSProblem.V] if a.expr.isNew =>
                val tpe: ObjectType = a.expr.asNew.tpe
                /* se.eval(new FileReader(...)); */
                if (tpe.isSubtypeOf(ObjectType("java/io/FileReader"))(p.classHierarchy))
                    findFileReaderArg(a.targetVar.usedBy)
                /* se.eval(new File(...)); */
                else if (tpe.isSubtypeOf(ObjectType("java/io/File"))(p.classHierarchy))
                    findFileArg(a.targetVar.usedBy)
            case _ =>
        }

        resultSet.toSet
    }
}
