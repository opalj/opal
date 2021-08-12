/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bugpicker
package core
package analyses

import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.Date

import scala.collection.JavaConverters.collectionAsScalaIterableConverter
import scala.collection.JavaConverters._
import scala.util.control.ControlThrowable
import scala.xml.Node
import scala.xml.NodeSeq
import scala.xml.Unparsed
import com.typesafe.config.Config
import com.typesafe.config.ConfigRenderOptions
import net.ceedubs.ficus.Ficus._
import org.opalj.ai.BoundedInterruptableAI
import org.opalj.ai.InterpretationFailedException
import org.opalj.ai.analyses.FieldValuesKey
import org.opalj.ai.analyses.MethodReturnValuesKey
import org.opalj.br.ClassFile
import org.opalj.br.Code
import org.opalj.br.Method
import org.opalj.br.analyses.Analysis
import org.opalj.br.analyses.ProgressManagement
import org.opalj.br.analyses.AnalysisException
import org.opalj.br.analyses.Project
import org.opalj.log.OPALLogger
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.ai.analyses.cg.VTACallGraphKey
import org.opalj.ai.util.XHTML
import org.opalj.util.Nanoseconds
import org.opalj.util.Milliseconds
import org.opalj.br.analyses.PropertyStoreKey
import org.opalj.fpcf.FPCFAnalysesRegistry
import org.opalj.br.analyses.StringConstantsInformationKey
import org.opalj.br.analyses.FieldAccessInformationKey
import org.opalj.util.Milliseconds
import org.opalj.issues.Issue
import org.opalj.issues.PackageLocation
import org.opalj.issues.ProjectLocation
import org.opalj.issues.IssueOrdering
import org.opalj.fpcf.FPCFAnalysesManagerKey
import org.opalj.ai.analyses.cg.CallGraphCache
import org.opalj.br.MethodSignature
import org.opalj.br.analyses.cg.InstantiableClassesKey
import org.opalj.issues.Relevance

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

    override def description: String = "Finds code smells in Java (byte) code."

    /**
     * Executes the analysis of the project's concrete methods.
     *
     * @param parameters A list of (optional) parameters. The parameters that are
     *      matched are defined by:
     *      [[BugPickerAnalysis.MaxEvalFactorPattern]],
     *      [[BugPickerAnalysis.MaxEvalTimePattern]],
     *      [[BugPickerAnalysis.MaxCardinalityOfIntegerRangesPattern]],
     *      [[BugPickerAnalysis.MaxCardinalityOfLongSetsPattern]],
     *      [[BugPickerAnalysis.MaxCallChainLengthPattern]], and "`-debug`".
     *
     */
    override def analyze(
        theProject:             Project[URL],
        parameters:             Seq[String],
        initProgressManagement: (Int) => ProgressManagement
    ): BugPickerResults = {

        import theProject.config

        implicit val project = theProject
        implicit val logContext = theProject.logContext

        // related to managing the analysis progress
        val classFilesCount = theProject.projectClassFilesCount

        val progressManagement = initProgressManagement(PreAnalysesCount + classFilesCount)
        import progressManagement.step

        val analysisParameters = theProject.config.as[Config]("org.opalj.bugpicker.analysisParameter")

        val maxEvalFactor = analysisParameters.as[Double]("maxEvalFactor")
        val maxEvalTime = new Milliseconds(analysisParameters.as[Long]("maxEvalTime"))
        val maxCardinalityOfIntegerRanges = analysisParameters.as[Long]("maxCardinalityOfLongSets")
        val maxCardinalityOfLongSets = analysisParameters.as[Int]("maxCardinalityOfLongSets")
        val configuredAnalyses = analysisParameters.as[List[String]]("fpcfAnalyses")
        val fpcfAnalyses = configuredAnalyses.map { a => FPCFAnalysesRegistry.factory(a) }
        val maxCallChainLength = theProject.config.as[Int](
            "org.opalj.bugpicker.analysis.RootBugPickerAnalysisDomain.maxCallChainLength"
        )

        val debug = parameters.contains("-debug")
        if (debug) {
            val cp = System.getProperty("java.class.path")
            val cpSorted = cp.split(java.io.File.pathSeparatorChar).sorted
            val renderingOptions =
                ConfigRenderOptions.defaults().
                    setOriginComments(false).
                    setComments(true).
                    setJson(false)
            val bugpickerConf = theProject.config.withOnlyPath("org.opalj")
            val settings = bugpickerConf.root().render(renderingOptions)
            OPALLogger.info(
                "configuration",
                cpSorted.mkString("System ClassPath:\n\t", "\n\t", "\n")+"\n"+
                    "Settings:"+"\n"+
                    settings
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

        val fieldAccessInformation = step(2, "[Pre-Analysis] Analyzing field accesses") {
            (theProject.get(FieldAccessInformationKey), None)
        }

        val stringConstantsInformation = step(3, "[Pre-Analysis] Analyzing the usage of string constants") {
            (theProject.get(StringConstantsInformationKey), None)
        }

        step(4, "[Pre-Analysis] Analyzing field declarations to derive more precise field value information") {
            (theProject.get(FieldValuesKey), None)
        }

        step(5, "[Pre-Analysis] Analyzing methods to get more precise return type information") {
            (theProject.get(MethodReturnValuesKey), None)
        }

        val computedCallGraph = step(6, "[Pre-Analysis] Creating the call graph") {
            (theProject.get(VTACallGraphKey), None)
        }
        val callGraph = computedCallGraph.callGraph
        val callGraphEntryPoints = computedCallGraph.entryPoints().toSet

        //
        //
        // Compute Fixpoint properties
        //
        //

        val analysesManager = theProject.get(FPCFAnalysesManagerKey)
        val propertyStore = theProject.get(PropertyStoreKey)
        step(7, "[FPCF-Analysis] executing fixpoint analyses") {
            (
                {
                    fpcfAnalyses.foreach(analysesManager.run(_, false))
                    propertyStore.waitOnPropertyComputationCompletion(true)
                },
                None
            )
        }

        //
        //
        // MAIN ANALYSIS
        //
        //

        val doInterrupt: () => Boolean = progressManagement.isInterrupted _

        val filteredResults = new ConcurrentLinkedQueue[Issue]()
        val issuesPackageFilterString = config.as[String]("org.opalj.bugpicker.issues.packages")
        OPALLogger.debug(
            "project configuration",
            s"only issues in packages matching $issuesPackageFilterString are shown"
        )
        val issuesPackageFilter = issuesPackageFilterString.r
        def addResults(issues: Iterable[Issue]): Unit = {
            if (issues.nonEmpty) {
                val filteredIssues = issues.filter { issue =>
                    issue.locations.head match {
                        case l: PackageLocation =>
                            val packageName = l.thePackage
                            val allMatches = issuesPackageFilter.findFirstIn(packageName)
                            allMatches.isDefined && packageName == allMatches.get
                        case _ =>
                            // the issue is a project level issue and hence kept
                            true
                    }
                }
                filteredResults.addAll(filteredIssues.asJavaCollection)
            }
        }

        val fieldValueInformation = theProject.get(FieldValuesKey)
        val methodReturnValueInformation = theProject.get(MethodReturnValuesKey)

        val cache = new CallGraphCache[MethodSignature, scala.collection.Set[Method]](theProject)

        def analyzeMethod(method: Method, body: Code): Unit = {
            val classFile: ClassFile = method.classFile
            // USED DURING DEVELEOPMENT; e.g., if we see a specific method.
            val debug = false

            // ---------------------------------------------------------------------------
            // Analyses that don't require an abstract interpretation
            // ---------------------------------------------------------------------------

            //
            // CHECK IF THE METHOD IS USED
            //
            addResults(
                UnusedMethodsAnalysis(theProject, computedCallGraph, callGraphEntryPoints, method)
            )

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
                    method,
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
                val result0 = ai0(method, analysisDomain)
                if (result0.wasAborted && maxCallChainLength > 0) {
                    val logMessage =
                        s"analysis of ${method.fullyQualifiedSignature} with method call execution aborted "+
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

                    val result1 = ai1(method, fallbackAnalysisDomain)

                    if (result1.wasAborted)
                        OPALLogger.warn(
                            "configuration",
                            logMessage+": retry without performing invocations also failed"
                        )
                    else
                        OPALLogger.info("configuration", logMessage)

                    result1
                } else
                    result0

            }

            if (!result.wasAborted) {
                if (debug) {
                    import result._
                    val domainName = domain.getClass.getName
                    org.opalj.io.writeAndOpen(
                        org.opalj.ai.common.XHTML.dump(
                            Some(classFile),
                            Some(method),
                            method.body.get,
                            Some(
                                s"Created: ${new Date}<br>Domain: $domainName<br>"+
                                    XHTML.evaluatedInstructionsToXHTML(result.evaluated)
                            ),
                            domain
                        )(cfJoins, result.operandsArray, result.localsArray),
                        "AIResult",
                        ".html"
                    )
                }

                //
                // FIND DEAD CODE
                //
                addResults(DeadEdgesAnalysis(theProject, method, result))

                //
                // FIND SUSPICIOUS CODE
                //
                addResults(GuardedAndUnguardedAccessAnalysis(theProject, method, result))

                //
                // FIND INSTRUCTIONS THAT ALWAYS THROW AN EXCEPTION
                //
                addResults(ThrowsExceptionAnalysis(theProject, method, result).toIterable)

                //
                // FIND USELESS COMPUTATIONS
                //
                addResults(UselessComputationsAnalysis(theProject, method, result))

                //
                // FIND USELESS REEVALUATIONS OF COMPUTATIONS
                //
                addResults(UselessReComputationsAnalysis(theProject, method, result))

                //
                // FIND UNUSED LOCAL VARIABLES
                //
                addResults(
                    UnusedLocalVariables(theProject, propertyStore, callGraph, method, result)
                )

                //
                // FIND STRANGE USES OF THE COLLECTIONS API
                //
                addResults(
                    CollectionsUsage(theProject, propertyStore, callGraph, method, result)
                )

            } else if (!doInterrupt()) {
                OPALLogger.error(
                    "internal error",
                    s"analysis of ${method.fullyQualifiedSignature} aborted "+
                        s"after ${ai0.currentEvaluationCount} steps "+
                        s"(code size: ${method.body.get.instructions.length})"
                )
            } /* else (doInterrupt === true) the analysis as such was interrupted*/
        }

        val exceptions = new ConcurrentLinkedQueue[AnalysisException]
        var analysisTime = Nanoseconds.None
        val identifiedIssues = time {
            val stepIds = new AtomicInteger(PreAnalysesCount + 1)

            theProject.parForeachProjectClassFile(doInterrupt) { classFile =>
                val stepId = stepIds.getAndIncrement()
                try {
                    progressManagement.start(stepId, classFile.thisType.toJava)

                    // ---------------------------------------------------------------------------
                    // Class based analyses
                    // ---------------------------------------------------------------------------

                    addResults(AnonymousInnerClassShouldBeStatic(theProject, classFile))
                    addResults(ManualGarbageCollection(theProject, classFile))
                    addResults(CovariantEquals(classFile))

                    //
                    // FIND UNUSED FIELDS
                    //
                    addResults(
                        UnusedFields(
                            theProject,
                            propertyStore, fieldAccessInformation, stringConstantsInformation,
                            classFile
                        )
                    )

                    // ---------------------------------------------------------------------------
                    // Analyses of the methods
                    // ---------------------------------------------------------------------------

                    for (method <- classFile.methods; body <- method.body) {
                        try {
                            analyzeMethod(method, body)
                        } catch {
                            case afe: InterpretationFailedException =>
                                val ms = method.fullyQualifiedSignature
                                val steps = afe.ai.asInstanceOf[BoundedInterruptableAI[_]].currentEvaluationCount
                                val message =
                                    s"the analysis of $ms failed/was aborted after $steps steps"
                                exceptions add (AnalysisException(message, afe))
                            case ct: ControlThrowable => throw ct
                            case t: Throwable =>
                                val ms = method.fullyQualifiedSignature
                                val message = s"the analysis of ${ms} failed"
                                exceptions add (AnalysisException(message, t))
                        }
                    }
                } catch {
                    case t: Throwable =>
                        OPALLogger.error(
                            "internal error", s"evaluation step $stepId failed", t
                        )
                        throw t
                } finally {
                    progressManagement.end(stepId)
                }
            }
            filteredResults.asScala.toSeq
        } { t => analysisTime = t }

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
    // 2: FieldAccessInformation
    // 3: StringConstantsInformation
    // 4: FieldValues analysis
    // 5: MethodReturnValues analysis
    // 6: Callgraph
    // 7: FPCF properties
    final val PreAnalysesCount = 7

    // We want to match expressions such as:
    // -maxEvalFactor=1
    // -maxEvalFactor=20
    // -maxEvalFactor=1.25
    // -maxEvalFactor=10.5
    // -maxEvalFactor=Infinity
    final val MaxEvalFactorPattern = """-maxEvalFactor=(\d+(?:.\d+)?|Infinity)""".r
    final val DefaultMaxEvalFactor = 1.75d

    final val MaxEvalTimePattern = """-maxEvalTime=(\d+)""".r
    final val DefaultMaxEvalTime: Milliseconds = new Milliseconds(10000L) // in ms => 10secs.

    final val MaxCallChainLengthPattern = """-maxCallChainLength=(\d)""".r
    final val DefaultMaxCallChainLength = 1

    final val MaxCardinalityOfIntegerRangesPattern =
        """-maxCardinalityOfIntegerRanges=(\d+)""".r
    final val DefaultMaxCardinalityOfIntegerRanges = 16L

    final val MaxCardinalityOfLongSetsPattern =
        """-maxCardinalityOfLongSets=(\d+)""".r
    final val DefaultMaxCardinalityOfLongSets = 2

    final val FixpointAnalysesPattern = """-fixpointAnalyses=(.+)""".r
    final val DefaultFixpointAnalyses = Seq.empty[String]

    def resultsAsXHTML(
        config:       Seq[String],
        theIssues:    Iterable[Issue],
        showSearch:   Boolean,
        analysisTime: Nanoseconds
    ): Node = {
        // TODO Make the filtering a configurable property!!!
        val issuesCount = theIssues.size
        val issues =
            if (issuesCount > 1000) {
                theIssues.filter(_.relevance.value >= Relevance.VeryLow.value)
            } else {
                theIssues
            }
        val basicInfoOnly = issuesCount > 15000

        val totalIssues = {
            var is = s"(Total issues: $issuesCount)"
            if (issues.size < theIssues.size)
                is += "(Due to the number of identified issues all those that are most likely completely irrelevant are filtered.)"
            if (basicInfoOnly)
                is += "(Due to the number of remaining issues an abbreviated report is shown.)"

            is
        }

        val issuesNode: Iterable[Node] = {
            import scala.collection.SortedMap
            val groupedMessages =
                SortedMap.empty[String, List[Issue]] ++
                    issues.groupBy { i =>
                        i.locations.head match {
                            case thePackage: PackageLocation => thePackage.thePackage
                            case _: ProjectLocation          => "<project>"
                        }
                    }
            val result =
                (for { (pkg, mdc) <- groupedMessages } yield {
                    <details class="package_summary">
                        <summary class="package_summary">{ pkg.replace('/', '.') }</summary>
                        { mdc.toSeq.sorted(IssueOrdering).map(_.toXHTML(basicInfoOnly)) }
                    </details>
                })
            result.seq
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
                        <span>Number of issues currently displayed:<span id="issues_displayed"> { issuesCount } </span>{ totalIssues }</span>
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
                                config.filterNot(_.contains("debug")).map(p => <li>{ p }</li>)
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
