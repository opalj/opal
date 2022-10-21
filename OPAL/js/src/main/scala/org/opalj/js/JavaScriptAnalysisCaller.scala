/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.js

import com.ibm.wala.cast.js.ipa.callgraph.{JSCFABuilder, JSCallGraphUtil}
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory
import com.ibm.wala.cast.js.util.JSCallGraphBuilderUtil
import com.ibm.wala.dataflow.IFDS.TabulationDomain
import com.ibm.wala.ipa.callgraph.CallGraph
import com.ibm.wala.ipa.cfg.BasicBlockInContext
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock
import com.ibm.wala.util.collections.Pair
import com.ibm.wala.util.intset.{IntIterator, IntSet, IntSetUtil, MutableIntSet}
import org.opalj.br.analyses.SomeProject
import org.opalj.js.wala_ifds.WalaJavaScriptIFDSTaintAnalysis
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement
import org.opalj.tac.fpcf.properties._
import org.mozilla.javascript.{Parser, Token}
import org.mozilla.javascript.ast.{AstNode, AstRoot, FunctionNode, NodeVisitor, Symbol}

import java.io.File

/**
 * Class which does convert a call from Java to JavaScript into something that a JavaScript IFDS analysis
 * can consume and converts the results back into facts for the Java taint analysis.
 *
 * @param p Project
 */
class JavaScriptAnalysisCaller(p: SomeProject) {
    type Domain = TabulationDomain[Pair[Integer, BasicBlockInContext[IExplodedBasicBlock]], BasicBlockInContext[IExplodedBasicBlock]]

    val sourceFinder = new LocalJSSourceFinder(p)

    /**
     * Analyze an evaluation of a JavaScript script.
     *
     * @param stmt Java Statement
     * @param in incoming BindingFact
     * @return set of taints
     */
    def analyze(stmt: JavaStatement, in: JSFact): Set[TaintFact] = {
        // TODO: pool queries

        val sourceFiles = sourceFinder(stmt)
        in match {
            case b: BindingFact         => sourceFiles.flatMap(s => analyzeScript(s, b))
            /* If we don't know what variable is tainted, we have to give up. */
            case b: WildcardBindingFact => Set(b)
        }
    }

    /**
     * Analyze a call to a JavaScript function.
     *
     * @param stmt Java Statement
     * @param in Variable-length parameter array
     * @param fName function name
     * @return set of taints
     */
    def analyze(stmt: JavaStatement, in: ArrayElement, fName: String): Set[TaintFact] = {
        // TODO: pool queries

        val sourceFiles = sourceFinder(stmt)
        sourceFiles.flatMap(s => analyzeFunction(s, in, fName, stmt.index)) + in
    }

    /**
     * Return all variable names that are in the top level scope.
     *
     * @param root root AST node of the script
     * @return set of variable names
     */
    private def getTopLevelVariableNames(root: AstRoot): Set[String] = {
        var nameSet: Set[String] = Set()
        root.getSymbols.forEach(symbol => {
            if ((symbol.getDeclType == Token.VAR
                || symbol.getDeclType == Token.LET
                || symbol.getDeclType == Token.CONST)
                && symbol.getContainingTable == root)
                nameSet += symbol.getName
        })
        nameSet
    }

    /**
     * Return the corresponding function node for the function symbol.
     *
     * @param func function symbol
     * @return function node
     */
    private def getFunctionNode(func: Symbol): Option[FunctionNode] = {
        /**
         * Visitor to find the node of the function in the AST.
         */
        class FunctionFinder extends NodeVisitor {
            var fnResult: Option[FunctionNode] = None

            override def visit(node: AstNode): Boolean = {
                if (fnResult.isEmpty)
                    node match {
                        case fn: FunctionNode =>
                            fnResult = Some(fn)
                            false
                        case _ => true
                    }
                else
                    false
            }
        }

        val fnFinder = new FunctionFinder()
        func.getContainingTable.visit(fnFinder)
        fnFinder.fnResult
    }

    /**
     * Map a list of variable names to a list of BindingFacts
     *
     * @param vars    variable names
     * @param defSite definition site of the incoming BindingFact
     * @return list of BindingFacts
     */
    private def varNamesToFacts(vars: List[String], defSite: Integer): Set[TaintFact] =
        vars.map(v => if (v == "opal_tainted_return") Variable(defSite) else BindingFact(defSite, v)).toSet

    /**
     * Generate a argument list of length n with one tainted argument.
     *
     * @param n number of arguments
     * @param taintedIdx index which should be tainted
     * @return argument list as string
     */
    private def generateFunctionArgs(n: Int, taintedIdx: Int): String = {
        (0 until n).map(i =>
            if (i == taintedIdx) {
                s"opal_tainted_arg"
            } else {
                s"opal_fill_arg"
            }).mkString(", ")
    }

    /**
     * Prepare and analyze a JavaScript function call.
     *
     * @param sourceFile JavaScript source
     * @param in incoming fact from a variable-length parameter list
     * @param fName function name
     * @param stmtIndex statement index
     * @return set of facts
     */
    private def analyzeFunction(sourceFile: JavaScriptSource, in: ArrayElement,
                                fName: String, stmtIndex: Int): Set[TaintFact] = {
        val parser: Parser = new Parser()

        val root: AstRoot = parser.parse(sourceFile.asString, "fileName", 1)
        val sTable = root.getSymbolTable
        if (!sTable.containsKey(fName))
            return Set()
        val fSymbol = sTable.get(fName)
        if (fSymbol.getDeclType != Token.FUNCTION)
            return Set()
        val fNode = getFunctionNode(fSymbol)
        if (fNode.isEmpty)
            return Set()

        val nameSet: Set[String] = getTopLevelVariableNames(root)+"opal_tainted_return"
        val beforeCode =
            s"""function opal_source() {
               |    return \"secret\";
               |}
               |function opal_last_stmt(${generateParams(nameSet.size)}) { }
               |\n""".stripMargin
        val funcCall = s"var opal_tainted_return = $fName(${generateFunctionArgs(fNode.get.getParamCount, in.element)});"
        val afterCode = s"""
                 |var opal_fill_arg = 42;
                 |var opal_tainted_arg = opal_source();
                 |$funcCall
                 |opal_last_stmt(${nameSet.mkString(", ")});\n""".stripMargin

        val f: File = sourceFile.asFile(beforeCode, afterCode)
        analyzeCommon(f, stmtIndex)
    }

    /**
     * Generate a n-length parameter list.
     *
     * @param n number of parameters
     * @return parameter list as string
     */
    private def generateParams(n: Int): String = {
        (0 until n).map(i => s"p$i").mkString(", ")
    }

    /**
     * Prepare and analyze a script evaluation.
     *
     * @param sourceFile JavaScript source
     * @param in incoming BindingFact
     * @return set of facts
     */
    private def analyzeScript(sourceFile: JavaScriptSource, in: BindingFact): Set[TaintFact] = {
        val parser: Parser = new Parser()
        val taintedVar = s"var ${in.keyName} = opal_source();\n"
        val root: AstRoot = parser.parse(taintedVar + sourceFile.asString, "fileName", 1)
        val nameSet: Set[String] = getTopLevelVariableNames(root)

        val beforeCode =
            s"""function opal_source() {
               |    return \"secret\";
               |}
               |function opal_last_stmt(${generateParams(nameSet.size)}) { }
               |\n
               |$taintedVar""".stripMargin
        val afterCode = s"\nopal_last_stmt(${nameSet.mkString(", ")});\n"
        val f: File = sourceFile.asFile(beforeCode, afterCode)
        analyzeCommon(f, in.index)
    }

    /**
     * Invoke the WALA IFDS taint analysis
     *
     * @param f JavaScript file
     * @param idx Index used in the out facts
     * @return set of facts
     */
    private def analyzeCommon(f: File, idx: Int): Set[TaintFact] = {
        JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory)
        val B: JSCFABuilder = JSCallGraphBuilderUtil.makeScriptCGBuilder(f.getParent, f.getName)
        val CG: CallGraph = B.makeCallGraph(B.getOptions)

        /**
         * Check basic block for source.
         *
         * @param ebb exploded basic block
         * @return true if ebb contains a source
         */
        def sources(ebb: BasicBlockInContext[IExplodedBasicBlock]): Boolean = {
            val inst = ebb.getDelegate.getInstruction
            inst match {
                case i: SSAAbstractInvokeInstruction =>
                    CG.getPossibleTargets(ebb.getNode, i.getCallSite).forEach(target => {
                        if (target.getMethod.getDeclaringClass.getName.toString.endsWith("opal_source"))
                            return true
                    })
                case _ =>
            }
            false
        }

        var varsAliveAfterJS: List[String] = List()

        /**
         * Add all tainted variable names to varsAliveAfterJS after the analysis ran.
         *
         * @param bb basic block
         * @param r result set
         * @param d domain
         */
        def sinks(bb: BasicBlockInContext[IExplodedBasicBlock], r: IntSet, d: Domain): Void = {
            val inst = bb.getDelegate.getInstruction
            inst match {
                case invInst: SSAAbstractInvokeInstruction =>
                    CG.getPossibleTargets(bb.getNode, invInst.getCallSite).forEach(target => {
                        if (target.getMethod.getDeclaringClass.getName.toString.endsWith("opal_last_stmt")) {
                            /* Collect all parameters.  */
                            val paramIdx: MutableIntSet = IntSetUtil.make()
                            for (i <- 1 until invInst.getNumberOfPositionalParameters) {
                                paramIdx.add(inst.getUse(i))
                            }

                            /* Collect all tainted variables reaching the end of the JS script. */
                            val it: IntIterator = r.intIterator()
                            val taints: MutableIntSet = IntSetUtil.make()
                            while (it.hasNext) {
                                val vn = d.getMappedObject(it.next())
                                taints.add(vn.fst)
                            }

                            /* Intersect all tainted variables with the parameters of the last statement.
                               This is a workaround to only reconstruct the variable names for the "last" SSA name. */
                            val taintedParams: IntSet = taints.intersection(paramIdx)
                            val taintedIt: IntIterator = taintedParams.intIterator()
                            while (taintedIt.hasNext) {
                                val idx = taintedIt.next()
                                varsAliveAfterJS ++= bb.getNode.getIR.getLocalNames(1, idx).toList
                            }
                        }
                    })
                case _ =>
            }
            null
        }

        WalaJavaScriptIFDSTaintAnalysis.startJSAnalysis(CG, sources, sinks)
        f.delete()
        varNamesToFacts(varsAliveAfterJS, idx)
    }
}
