/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.python

import org.opalj.br.analyses.SomeProject
import org.opalj.tac.fpcf.analyses.ifds.JavaStatement
import org.opalj.tac.fpcf.analyses.ifds.taint.{ArrayElement, TaintFact, Variable}

import java.io.File
import javax.script.ScriptEngineManager
import scala.io.Source

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
     * Return all variable names that are in the top level scope.
     *
     * @param srcCode code of the script
     * @return set of variable names
     */
    private def getTopLevelVariableNames(srcCode: String): Set[String] = {
        var nameSet: Set[String] = Set()
        val sem = new ScriptEngineManager
        val se = sem.getEngineByName("python")
        val analysisCode =
            s"""
               |import ast
               |
               |def get_top_level_variables(src_code):
               |    a = ast.parse(src_code)
               |    variables = []
               |    for node in ast.iter_child_nodes(a):
               |        if isinstance(node, ast.Assign):
               |            for arg in node.targets:
               |                for a in ast.walk(arg):
               |                    if isinstance(a, ast.Name):
               |                        variables.append(a.id)
               |    return ','.join(variables)
               |
               |""".stripMargin
        se.eval(analysisCode)
        se.eval(s"get_top_level_variables('${srcCode.replace("\n", "\\n")}')").toString
            .split(",").filter(s => s.nonEmpty).foreach(s => nameSet += s)
        nameSet
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
     * @param n          number of arguments
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
    private def analyzeFunction(sourceFile: PythonSource, in: ArrayElement,
                                fName: String, stmtIndex: Int): Set[TaintFact] = {
        val sem = new ScriptEngineManager
        val se = sem.getEngineByName("python")
        val parametersCode =
            s"""
               |import ast
               |
               |def get_parameters(src_code, function_name):
               |    a = ast.parse(src_code)
               |    variables = []
               |    for function in ast.iter_child_nodes(a):
               |        if isinstance(function, ast.FunctionDef) and function.name == function_name:
               |            for node in ast.walk(function):
               |                if isinstance(node, ast.arguments):
               |                    for arg in node.args:
               |                        for a in ast.walk(arg):
               |                            if isinstance(a, ast.Name):
               |                                variables.append(a.id)
               |    return ",".join(variables)
               |""".stripMargin
        se.eval(parametersCode)
        val parameters = se.eval(s"get_parameters('${sourceFile.asString.replace("\n", "\\n")}', '$fName')").toString.split(",")

        val nameSet: Set[String] = getTopLevelVariableNames(sourceFile.asString)+"opal_tainted_return"
        val beforeCode =
            s"""
               |import functools
               |class tstr:
               |    def __init__(self, value):
               |        self.value = value
               |    def __radd__(self, other):
               |        t = tstr(str.__add__(other, self.value))
               |        t._taint = self._taint
               |        return t
               |
               |    def __repr__(self):
               |        return self.__class__.__name__ + str.__repr__(self.value) + " " + str(self.tainted())
               |
               |
               |    def taint(self):
               |        self._taint = True
               |        return self
               |
               |    def tainted(self):
               |        return hasattr(self, '_taint')
               |
               |
               |def opal_source():
               |    s = tstr("secret")
               |    s.taint()
               |    return s
               |
               |tainted_arguments = []
               |
               |def opal_last_stmt(args):
               |    for a in args.split(', '):
               |        variable = globals()[a]
               |        if hasattr(variable, '_taint'):
               |            tainted_arguments.append(a)
               |    print(tainted_arguments)
               |\n""".stripMargin
        val funcCall = s"opal_tainted_return = $fName(${generateFunctionArgs(parameters.size, in.element)})"
        val afterCode =
            s"""
               |opal_fill_arg = 42
               |opal_tainted_arg = opal_source()
               |$funcCall
               |opal_last_stmt('${nameSet.mkString(", ")}')\n""".stripMargin

        val f: File = sourceFile.asFile(beforeCode, afterCode)

        analyzeCommon(f, stmtIndex)
    }

    /**
     * Prepare and analyze a script evaluation.
     *
     * @param sourceFile Python source
     * @param in incoming BindingFact
     * @return set of facts
     */
    private def analyzeScript(sourceFile: PythonSource, in: BindingFact): Set[TaintFact] = {
        val taintedVar = s"${in.keyName} = opal_source()\n"
        val nameSet: Set[String] = getTopLevelVariableNames(taintedVar + sourceFile.asString)

        val beforeCode =
            s"""
               |import functools
               |
               |class tstr:
               |    def __init__(self, value):
               |        self.value = value
               |    def __radd__(self, other):
               |        t = tstr(str.__add__(other, self.value))
               |        t._taint = self._taint
               |        return t
               |
               |    def __repr__(self):
               |        return self.__class__.__name__ + str.__repr__(self.value) + " " + str(self.tainted())
               |
               |
               |    def taint(self):
               |        self._taint = True
               |        return self
               |
               |    def tainted(self):
               |        return hasattr(self, '_taint')
               |
               |
               |def opal_source():
               |    s = tstr("secret")
               |    s.taint()
               |    return s
               |
               |tainted_arguments = []
               |
               |def opal_last_stmt(args):
               |    for a in args.split(', '):
               |        variable = globals()[a]
               |        if hasattr(variable, '_taint'):
               |            tainted_arguments.append(a)
               |    print(tainted_arguments)
               |
               |\n
               |$taintedVar""".stripMargin
        val afterCode = s"\nopal_last_stmt('${nameSet.mkString(", ")}')\n"
        val f: File = sourceFile.asFile(beforeCode, afterCode)
        analyzeCommon(f, in.index)
    }

    /**
     * Invoke Python taint analysis
     *
     * @param f   Python file
     * @param idx Index used in the out facts
     * @return set of facts
     */
    private def analyzeCommon(f: File, idx: Int): Set[TaintFact] = {
        var code = ""
        val source = Source.fromFile(f)
        for (line <- source.getLines()) {
            code += (line+"\n")
        }
        source.close()
        val sem = new ScriptEngineManager
        val se = sem.getEngineByName("python")
        //        val reader = new FileReader(f)
        println(code)
        se.eval(code)
        val varsAliveAfterPy: List[String] = se.eval("','.join(tainted_arguments)").toString.split(",").filter(a => a.nonEmpty).toList
        //        System.out.println(varsAliveAfterPy)
        varNamesToFacts(varsAliveAfterPy, idx)
    }
}
