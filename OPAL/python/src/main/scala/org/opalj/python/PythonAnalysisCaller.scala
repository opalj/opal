/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.python

import org.opalj.br.analyses.SomeProject
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement
import org.opalj.tac.fpcf.analyses.ifds.taint.{ArrayElement, TaintFact}

/**
 * Class which does convert a call from Java to JavaScript into something that a JavaScript IFDS analysis
 * can consume and converts the results back into facts for the Java taint analysis.
 *
 * @param p Project
 */
class PythonAnalysisCaller(p: SomeProject) {

    val sourceFinder = new LocalPythonSourceFinder(p)

    /**
     * Analyze an evaluation of a JavaScript script.
     *
     * @param stmt Java Statement
     * @param in incoming BindingFact
     * @return set of taints
     */
    def analyze(stmt: JavaStatement, in: PythonFact): Set[TaintFact] = {
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
     * Prepare and analyze a JavaScript function call.
     *
     * @param sourceFile JavaScript source
     * @param in incoming fact from a variable-length parameter list
     * @param fName function name
     * @param stmtIndex statement index
     * @return set of facts
     */
    private def analyzeFunction(sourceFile: PythonSource, in: ArrayElement,
                                fName: String, stmtIndex: Int): Set[TaintFact] = {
        System.out.println(sourceFile.asString)

        Set.empty //TODO
    }

    /**
     * Prepare and analyze a script evaluation.
     *
     * @param sourceFile JavaScript source
     * @param in incoming BindingFact
     * @return set of facts
     */
    private def analyzeScript(sourceFile: PythonSource, in: BindingFact): Set[TaintFact] = {
        Set.empty //TODO
    }
}
