/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package bugpicker
package core
package analysis

import java.net.URL
import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.collection.JavaConversions
import scala.collection.SortedMap
import scala.util.control.ControlThrowable
import scala.xml.Node
import scala.xml.Unparsed
import scala.io.Source
import java.lang.Double.parseDouble
import java.lang.Long.parseLong
import java.lang.Integer.parseInt
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentLinkedQueue
import org.opalj.ai.BoundedInterruptableAI
import org.opalj.ai.InterpretationFailedException
import org.opalj.ai.analyses.FieldValuesKey
import org.opalj.ai.analyses.MethodReturnValuesKey
import org.opalj.ai.analyses.cg.CallGraphCache
import org.opalj.ai.collectPCWithOperands
import org.opalj.br.ClassFile
import org.opalj.br.Code
import org.opalj.br.Method
import org.opalj.br.MethodSignature
import org.opalj.br.MethodWithBody
import org.opalj.br.analyses.InstantiableClassesKey
import org.opalj.br.analyses.Analysis
import org.opalj.br.analyses.ProgressManagement
import org.opalj.br.analyses.AnalysisException
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.IStoreInstruction
import org.opalj.br.instructions.LStoreInstruction
import org.opalj.io.process
import org.opalj.log.OPALLogger
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.ai.analyses.cg.VTACallGraphKey
import org.opalj.ai.util.XHTML
import org.opalj.util.Nanoseconds
import org.opalj.util.Milliseconds
import scala.xml.NodeSeq
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.fpcf.FPCFAnalysisRegistry
import org.opalj.fpcf.analysis.FPCFAnalysisRunner
import org.opalj.fpcf.analysis.FPCFAnalysesManagerKey

/**
 * Wrapper around several analyses that analyze the control- and data-flow to identify
 * various issues in the source code of projects.
 *
 * ==Precision==
 * The analyses are designed such that they try to avoid to report false positives to facilitate
 * usage of the BugPicker. However, given
 * that we analyze Java bytecode, some findings may be the result of the compilation
 * scheme employed by the compiler and, hence, cannot be resolved at the
 * source code level. This is in particular true for finally blocks in Java programs. In
 * this case compilers typically include the same block two (or more) times in the code.
 * Furthermore, Java reflection and reflection-like mechanisms are also a source of
 * false positives.
 *
 * @author Michael Eichberg
 */
class BugPickerAnalysis extends Analysis[URL, BugPickerResults] {

    import BugPickerAnalysis._

    override def title: String = "BugPicker"

    override def description: String = "Finds bugs in Java (byte) code."

    /**
     * Executes the analysis of the project's concrete methods.
     *
     * @param parameters A list of (optional) parameters. The parameters that are
     *      matched are defined by:
     *      [[BugPickerAnalysis$.MaxEvalFactorPattern]],
     *      [[BugPickerAnalysis$.MaxEvalTimePattern]],
     *      [[BugPickerAnalysis$.MaxCardinalityOfIntegerRangesPattern]],
     *      [[BugPickerAnalysis$.MaxCardinalityOfLongSetsPattern]],
     *      [[BugPickerAnalysis$.MaxCallChainLengthPattern]], and "`-debug`".
     *
     */
    override def analyze(
        theProject:             Project[URL],
        parameters:             Seq[String],
        initProgressManagement: (Int) ⇒ ProgressManagement
    ): BugPickerResults = {

        implicit val logContext = theProject.logContext

        // related to managing the analysis progress
        val classFilesCount = theProject.projectClassFilesCount

        val progressManagement = initProgressManagement(PreAnalysesCount + classFilesCount)
        import progressManagement.step

        val maxEvalFactor: Double =
            parameters.
                collectFirst { case MaxEvalFactorPattern(d) ⇒ parseDouble(d).toDouble }.
                getOrElse(DefaultMaxEvalFactor)

        val maxEvalTime: Milliseconds =
            parameters.
                collectFirst { case MaxEvalTimePattern(l) ⇒ new Milliseconds(parseLong(l).toLong) }.
                getOrElse(DefaultMaxEvalTime)

        val maxCardinalityOfIntegerRanges: Long =
            parameters.
                collectFirst { case MaxCardinalityOfIntegerRangesPattern(i) ⇒ parseLong(i) }.
                getOrElse(DefaultMaxCardinalityOfIntegerRanges)

        val maxCardinalityOfLongSets: Int =
            parameters.
                collectFirst { case MaxCardinalityOfLongSetsPattern(i) ⇒ parseInt(i) }.
                getOrElse(DefaultMaxCardinalityOfLongSets)

        val maxCallChainLength: Int =
            parameters.
                collectFirst { case MaxCallChainLengthPattern(i) ⇒ parseInt(i) }.
                getOrElse(DefaultMaxCallChainLength)

        val fixpointAnalyses = {
            parameters.collectFirst {
                case FixpointAnalysesPattern(i) ⇒
                    var fpas = Seq.empty[FPCFAnalysisRunner]
                    for (fpa ← i.split(";"))
                        fpas = fpas :+ FPCFAnalysisRegistry.getFixpointAnalysisFactory(fpa)
                    fpas
            }.getOrElse(Seq.empty[FPCFAnalysisRunner])
        }

        val debug = parameters.contains("-debug")
        if (debug) {
            val cp = System.getProperty("java.class.path")
            val cpSorted = cp.split(java.io.File.pathSeparatorChar).sorted
            OPALLogger.info(
                "configuration",
                cpSorted.mkString("System ClassPath:\n\t", "\n\t", "\n")+"\n"+
                    "Settings:"+"\n"+
                    s"\tmaxEvalFactor=$maxEvalFactor"+"\n"+
                    s"\tmaxEvalTime=${maxEvalTime}ms"+"\n"+
                    s"\tmaxCardinalityOfIntegerRanges=$maxCardinalityOfIntegerRanges"+"\n"+
                    s"\tmaxCardinalityOfLongSets=$maxCardinalityOfLongSets"+"\n"+
                    s"\tmaxCallChainLength=$maxCallChainLength\n"+
                    s"\fixpointAnalyses=$fixpointAnalyses"
            )
        }

        //
        //
        // PREANALYSES
        //
        //

        step(1, "[Pre-Analysis] Identifying non-instantiable classes") {
            (theProject.get(InstantiableClassesKey), None)
        }

        step(2, "[Pre-Analysis] Analyzing field declarations to derive more precise field value information") {
            (theProject.get(FieldValuesKey), None)
        }

        step(3, "[Pre-Analysis] Analyzing methods to get more precise return type information") {
            (theProject.get(MethodReturnValuesKey), None)
        }

        val callGraph = step(4, "[Pre-Analysis] Creating the call graph") {
            (theProject.get(VTACallGraphKey), None)
        }
        val callGraphEntryPoints = callGraph.entryPoints().toSet

        //
        //
        // Compute Fixpoint properties
        //
        //

        val analysesManager = theProject.get(FPCFAnalysesManagerKey)
        step(5, "[FPCF-Analysis] executing fixpoint analyses") {
            (
                {
                    fixpointAnalyses.foreach(analysesManager.runWithRecommended(_)(false))
                    theProject.get(SourceElementsPropertyStoreKey).waitOnPropertyComputationCompletion(true)
                },
                None
            )
        }

        //
        //
        // MAIN ANALYSIS
        //
        //

        val doInterrupt: () ⇒ Boolean = progressManagement.isInterrupted

        val results = new ConcurrentLinkedQueue[StandardIssue]()
        val fieldValueInformation = theProject.get(FieldValuesKey)
        val methodReturnValueInformation = theProject.get(MethodReturnValuesKey)
        val cache = new CallGraphCache[MethodSignature, scala.collection.Set[Method]](theProject)

        def analyzeMethod(classFile: ClassFile, method: Method, body: Code): Unit = {
            // USED DURING DEVELEOPMENT; e.g., if we see a specific method.
            val debug = false

            // ---------------------------------------------------------------------------
            // Analyses that don't require an abstract interpretation
            // ---------------------------------------------------------------------------

            //
            // CHECK IF THE METHOD IS USED
            //
            UnusedMethodsAnalysis.analyze(
                theProject, callGraph, callGraphEntryPoints,
                classFile, method
            ) foreach { issue ⇒ results.add(issue) }

            // ---------------------------------------------------------------------------
            // Analyses that are dependent on the result of the abstract interpretation
            // ---------------------------------------------------------------------------

            val analysisDomain =
                new RootBugPickerAnalysisDomain(
                    theProject,
                    // Map.empty, Map.empty,
                    fieldValueInformation, methodReturnValueInformation,
                    cache,
                    maxCardinalityOfIntegerRanges,
                    maxCardinalityOfLongSets, maxCallChainLength,
                    classFile, method,
                    debug
                )
            val ai0 =
                new BoundedInterruptableAI[analysisDomain.type](
                    body,
                    maxEvalFactor,
                    maxEvalTime,
                    doInterrupt
                )
            val result = {
                val result0 = ai0(classFile, method, analysisDomain)
                if (result0.wasAborted && maxCallChainLength > 0) {
                    val logMessage =
                        s"analysis of ${method.fullyQualifiedSignature(classFile.thisType)} with method call execution aborted "+
                            s"after ${ai0.currentEvaluationCount} steps "+
                            s"(code size: ${method.body.get.instructions.length})"
                    // let's try it again, but without performing method calls;
                    // let's reuse the current state
                    val fallbackAnalysisDomain =
                        new FallbackBugPickerAnalysisDomain(
                            theProject,
                            fieldValueInformation, methodReturnValueInformation,
                            cache,
                            maxCardinalityOfIntegerRanges, maxCardinalityOfLongSets,
                            method
                        )

                    val ai1 =
                        new BoundedInterruptableAI[fallbackAnalysisDomain.type](
                            body,
                            maxEvalFactor,
                            maxEvalTime,
                            doInterrupt
                        )

                    val result1 = ai1(classFile, method, fallbackAnalysisDomain)

                    if (result1.wasAborted)
                        OPALLogger.warn(
                            "configuration",
                            logMessage+
                                ": retry without performing invocations also failed"
                        )
                    else
                        OPALLogger.info("configuration", logMessage)

                    result1
                } else
                    result0

            }

            if (!result.wasAborted) {

                if (debug) {
                    org.opalj.io.writeAndOpen(
                        org.opalj.ai.common.XHTML.dump(
                            Some(classFile),
                            Some(method),
                            method.body.get,
                            Some(
                                "Created: "+(new java.util.Date).toString+"<br>"+
                                    "Domain: "+result.domain.getClass.getName+"<br>"+
                                    XHTML.evaluatedInstructionsToXHTML(result.evaluated)
                            ),
                            result.domain
                        )(
                                result.operandsArray,
                                result.localsArray
                            ),
                        "AIResult",
                        ".html"
                    )
                }

                //
                // FIND DEAD CODE
                //
                results.addAll(
                    JavaConversions.asJavaCollection(
                        DeadEdgesAnalysis.analyze(theProject, classFile, method, result)
                    )
                )

                //
                // FIND SUSPICIOUS CODE
                //
                results.addAll(
                    scala.collection.JavaConversions.asJavaCollection(
                        GuardedAndUnguardedAccessAnalysis.analyze(theProject, classFile, method, result)
                    )
                )

                //
                // FIND INSTRUCTIONS THAT ALWAYS THROW AN EXCEPTION
                //
                results.addAll(
                    scala.collection.JavaConversions.asJavaCollection(
                        ThrowsExceptionAnalysis.analyze(theProject, classFile, method, result)
                    )
                )

                //
                // FIND USELESS COMPUTATIONS
                //
                results.addAll(
                    scala.collection.JavaConversions.asJavaCollection(
                        UselessComputationsAnalysis.analyze(theProject, classFile, method, result)
                    )
                )

                //
                // FIND USELESS EXPRESSION EVALUATIONS
                //

                import result.domain.ConcreteIntegerValue
                import result.domain.ConcreteLongValue
                import result.domain

                if (domain.code.localVariableTable.isDefined) {
                    // This analysis requires debug information to increase the likelihood
                    // the we identify the correct local variable re-assignments. Otherwise
                    // we are not able to distinguish the reuse of a "register variable"/
                    // local variable for a new/different purpose or the situation where
                    // the same variable is updated the second time using the same
                    // value.

                    val operandsArray = result.operandsArray
                    val localsArray = result.localsArray
                    val code = domain.code

                    val methodsWithValueReassignment =
                        collectPCWithOperands(domain)(body, operandsArray) {
                            case (
                                pc,
                                IStoreInstruction(index),
                                Seq(ConcreteIntegerValue(a), _*)
                                ) if localsArray(pc) != null &&
                                domain.intValueOption(localsArray(pc)(index)).map(_ == a).getOrElse(false) &&
                                code.localVariable(pc, index).map(lv ⇒ lv.startPC < pc).getOrElse(false) ⇒

                                val lv = code.localVariable(pc, index).get

                                StandardIssue(
                                    theProject, classFile, Some(method), Some(pc),
                                    Some(operandsArray(pc)),
                                    Some(localsArray(pc)),
                                    "useless (re-)assignment",
                                    Some("(Re-)Assigned the same value ("+a+") to the same variable ("+lv.name+")."),
                                    Set(IssueCategory.Flawed, IssueCategory.Comprehensibility),
                                    Set(IssueKind.ConstantComputation),
                                    Seq.empty,
                                    new Relevance(20)
                                )

                            case (
                                pc,
                                LStoreInstruction(index),
                                Seq(ConcreteLongValue(a), _*)
                                ) if localsArray(pc) != null &&
                                domain.longValueOption(localsArray(pc)(index)).map(_ == a).getOrElse(false) &&
                                code.localVariable(pc, index).map(lv ⇒ lv.startPC < pc).getOrElse(false) ⇒

                                val lv = code.localVariable(pc, index).get

                                StandardIssue(
                                    theProject, classFile, Some(method), Some(pc),
                                    Some(operandsArray(pc)),
                                    Some(localsArray(pc)),
                                    "useless (re-)assignment",
                                    Some("(Re-)Assigned the same value ("+a+") to the same variable ("+lv.name+")."),
                                    Set(IssueCategory.Flawed, IssueCategory.Comprehensibility),
                                    Set(IssueKind.ConstantComputation),
                                    Seq.empty,
                                    new Relevance(20)
                                )
                        }

                    results.addAll(
                        scala.collection.JavaConversions.asJavaCollection(methodsWithValueReassignment)
                    )
                }

            } else if (!doInterrupt()) {
                OPALLogger.error(
                    "internal error",
                    s"analysis of ${method.fullyQualifiedSignature(classFile.thisType)} aborted "+
                        s"after ${ai0.currentEvaluationCount} steps "+
                        s"(code size: ${method.body.get.instructions.length})"
                )
            } /* else (doInterrupt === true) the analysis as such was interrupted*/
        }

        val exceptions = new ConcurrentLinkedQueue[AnalysisException]
        var analysisTime = Nanoseconds.None
        val identifiedIssues = time {
            val stepIds = new AtomicInteger(PreAnalysesCount + 1)

            theProject.parForeachProjectClassFile(doInterrupt) { classFile ⇒
                val stepId = stepIds.getAndIncrement()
                try {
                    progressManagement.start(stepId, classFile.thisType.toJava)
                    for (method @ MethodWithBody(body) ← classFile.methods) {
                        try {
                            analyzeMethod(classFile, method, body)
                        } catch {
                            case afe: InterpretationFailedException ⇒
                                val ms = method.fullyQualifiedSignature(classFile.thisType)
                                val steps = afe.ai.asInstanceOf[BoundedInterruptableAI[_]].currentEvaluationCount
                                val message =
                                    s"the analysis of $ms "+
                                        s"failed/was aborted after $steps steps"
                                exceptions add (AnalysisException(message, afe))
                            case ct: ControlThrowable ⇒ throw ct
                            case t: Throwable ⇒
                                val ms = method.fullyQualifiedSignature(classFile.thisType)
                                val message = s"the analysis of ${ms} failed"
                                exceptions add (AnalysisException(message, t))
                        }
                    }
                } catch {
                    case t: Throwable ⇒
                        OPALLogger.error(
                            "internal error", s"evaluation step $stepId failed", t
                        )
                        throw t
                } finally {
                    progressManagement.end(stepId)
                }
            }
            val rawIssues = JavaConversions.collectionAsScalaIterable(results).toSeq
            OPALLogger.info("analysis progress", s"post processing ${rawIssues.size} issues")
            StandardIssue.fold(rawIssues)
        } { t ⇒ analysisTime = t }

        OPALLogger.info(
            "analysis progress",
            s"the analysis took ${analysisTime.toSeconds} "+
                s"and found ${identifiedIssues.size} unique issues"
        )
        import scala.collection.JavaConverters._
        (analysisTime, identifiedIssues, exceptions.asScala)
    }
}
/**
 * Common constants and helper methods related to the configuration of the BugPicker and
 * generating reports.
 *
 * @author Michael Eichberg
 */
object BugPickerAnalysis {

    // 1: InstantiableClasses analysis
    // 2: FieldValues analysis
    // 3: MethodReturnValues analysis
    // 4: Callgraph
    // 5: FPCF properties
    final val PreAnalysesCount = 5

    // We want to match expressions such as:
    // -maxEvalFactor=1
    // -maxEvalFactor=20
    // -maxEvalFactor=1.25
    // -maxEvalFactor=10.5
    // -maxEvalFactor=Infinity
    final val MaxEvalFactorPattern = """-maxEvalFactor=(\d+(?:.\d+)?|Infinity)""".r
    final val DefaultMaxEvalFactor = 1.75d

    final val MaxEvalTimePattern = """-maxEvalTime=(\d+)""".r
    final val DefaultMaxEvalTime: Milliseconds = new Milliseconds(10000) // in ms => 10secs.

    final val MaxCallChainLengthPattern = """-maxCallChainLength=(\d)""".r
    final val DefaultMaxCallChainLength = 1

    final val MaxCardinalityOfIntegerRangesPattern =
        """-maxCardinalityOfIntegerRanges=(\d+)""".r
    final val DefaultMaxCardinalityOfIntegerRanges = 16l

    final val MaxCardinalityOfLongSetsPattern =
        """-maxCardinalityOfLongSets=(\d+)""".r
    final val DefaultMaxCardinalityOfLongSets = 2

    final val FixpointAnalysesPattern = """-fixpointAnalyses=(.+)""".r
    final val DefaultFixpointAnalyses = Seq.empty[String]

    def resultsAsXHTML(
        parameters:        Seq[String],
        methodsWithIssues: Iterable[Issue],
        showSearch:        Boolean,
        analysisTime:      Nanoseconds
    ): Node = {
        val methodsWithIssuesCount = methodsWithIssues.size
        val basicInfoOnly = methodsWithIssuesCount > 10000

        val issuesNode: Iterable[Node] = {
            import scala.collection.SortedMap
            val groupedMessages =
                SortedMap.empty[String, List[Issue]] ++
                    methodsWithIssues.groupBy(dc ⇒ dc.classFile.thisType.packageName)
            val result =
                (for { (pkg, mdc) ← groupedMessages } yield {
                    <details class="package_summary">
                        <summary class="package_summary">{ pkg.replace('/', '.') }</summary>
                        { mdc.toSeq.sorted(IssueOrdering).map(_.asXHTML(basicInfoOnly)) }
                    </details>
                })
            result.seq
        }

        val totalIssues = {
            val is = s"(Total issues: $methodsWithIssuesCount)"
            if (basicInfoOnly)
                is+"(Due to the number of issues an abbreviated report is shown.)"
            else
                is
        }

        val (searchJS: NodeSeq, searchBox: NodeSeq) =
            if (showSearch) {
                (
                    <script type="text/javascript">{ Unparsed(SearchJS) }</script>,
                    <span id="search_box"><label for="search_field">Search:</label><input type="search" id="search_field" name="search" disabled="true"/></span>
                )
            } else {
                (NodeSeq.Empty, NodeSeq.Empty)
            }
        <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
                <meta http-equiv='Content-Type' content='application/xhtml+xml; charset=utf-8'/>
                <script type="text/javascript">{ Unparsed(HTMLJS) }</script>
                <script type="text/javascript">{ Unparsed(ReportJS) }</script>
                { searchJS }
                <style>{ Unparsed(HTMLCSS) }</style>
                <style>{ Unparsed(ReportCSS) }</style>
            </head>
            <body>
                <div id="analysis_controls">
                    <div>
                        <span>Number of issues currently displayed:<span id="issues_displayed"> { methodsWithIssuesCount } </span>{ totalIssues }</span>
                        { searchBox }
                    </div>
                    <div>
                        Suppress issues with an estimated
                        <abbr title='The importance is calculated using the available context information. E.g., a dead "default case" in a switch statement is often the result of defensive programming and, hence, not important.'>importance</abbr>
                        less than:
                        <abbr title="The identified issue is probably not important or is just a technical artifact.">1</abbr>
                        <input type="range" name="relevance" id="relevance" min="1" max="100"/>
                        <abbr title="The identified issue is probably very important.">100</abbr>
                    </div>
                    <div class="issue_filter">
                        <span>Manifestation in the Code:</span><br/>
                        <span id="filter_data-kind"> </span>
                    </div>
                    <div class="issue_filter">
                        <span>Software Quality Attributes:</span><br/>
                        <span id="filter_data-category"> </span>
                    </div>
                    <div>
                        Show all Packages:<a class="onclick" onclick="openAllPackages()">+</a><a class="onclick" onclick="closeAllPackages()">-</a>
                    </div>
                </div>
                <div id="analysis_parameters">
                    <details id="analysis_parameters_summary">
                        <summary>Parameters</summary>
                        <ul>
                            {
                                parameters.filterNot(p ⇒
                                    p.startsWith("-debug") ||
                                        p.startsWith("-html") || p.startsWith("-eclipse")).map(p ⇒ <li>{ p }</li>)
                            }
                        </ul>
                    </details>
                </div>
                <div id="analysis_results">
                    { issuesNode }
                </div>
            </body>
        </html>
    }
    //<div id="debug"><span id="debug_info"></span></div> <-- add if you want to debug
}
