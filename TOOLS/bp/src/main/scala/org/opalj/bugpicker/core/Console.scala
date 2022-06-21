/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bugpicker
package core

import java.lang.Integer.parseInt
import java.net.URL
import java.nio.file.Files
import java.nio.charset.StandardCharsets
import java.io.File

import scala.xml.Node

import com.typesafe.config.Config

import org.opalj.io.writeAndOpen
import org.opalj.io.process
import org.opalj.log.OPALLogger
import org.opalj.log.OPALLogger
import org.opalj.log.GlobalLogContext
import org.opalj.br.analyses.Analysis
import org.opalj.br.analyses.AnalysisApplication
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProgressManagement
import org.opalj.ai.util.XHTML
import org.opalj.bugpicker.core.analyses.BugPickerAnalysis
import org.opalj.bugpicker.core.analyses.BugPickerAnalysis.resultsAsXHTML
import org.opalj.issues.IssueKind
import org.opalj.log.LogContext

/**
 * The command line interface of the bug picker.
 *
 * @author Michael Eichberg
 */
object Console extends Analysis[URL, BasicReport] with AnalysisApplication {

    val analysis = this

    final val IDLFileOutputNameMatcher = """-idl=([\w-_\.\:/\\]+)""".r

    final val HTMLFileOutputNameMatcher = """-html=([\w-_\.\:/\\]+)""".r

    final val DebugFileOutputNameMatcher = """-debug=([\w-_\.\:/\\]+)""".r

    final val MinRelevancePattern = """-minRelevance=(\d\d?)""".r
    final val MinRelevance = 0

    final val IssueKindsPattern = """-kinds=([\w_,]+)""".r

    override def main(unparsedArgs: Array[String]): Unit = {
        try {
            super.main(unparsedArgs)
        } catch {
            case t: Throwable => t.printStackTrace()
        }
    }

    final override val analysisSpecificParametersDescription: String =
        """[-maxEvalFactor=<DoubleValue {[0.1,100),Infinity}=1.75> determines the maximum effort that
            |               the analysis will spend when analyzing a specific method. The effort is
            |               always relative to the size of the method. For the vast majority of methods
            |               a value between 0.5 and 1.5 is sufficient to completely analyze a single
            |               method using the default settings.
            |               A value greater than 1.5 can already lead to very long evaluation times.
            |               If the threshold is exceeded the analysis of the method is aborted and no
            |               result can be drawn.]
            |[-maxEvalTime=<IntValue [10,1000000]=10000> determines the time (in ms) that the analysis
            |               is allowed to take for one method before the analysis is terminated.]
            |[-maxCardinalityOfIntegerRanges=<LongValue [1,4294967295]=16> basically determines for each integer
            |               value how long the value is "precisely" tracked. Internally the analysis
            |               computes the range of values that an integer value may have at runtime. The
            |               maximum size/cardinality of this range is controlled by this setting. If
            |               the range is exceeded the precise tracking of the respective value is
            |               terminated.
            |               Increasing this value may significantly increase the analysis time and
            |               may require the increase of maxEvalFactor.]
            |[-maxCardinalityOfLongSets=<IntValue [1,1024]=2> basically determines for each long
            |               value how long the value is "precisely" tracked.
            |               The maximum size/cardinality of this set is controlled by this setting. If
            |               the set's size is tool large the precise tracking of the respective value is
            |               terminated.
            |               Increasing this value may significantly increase the analysis time and
            |               may require the increase of maxEvalFactor.]            |
            |[-maxCallChainLength=<IntValue [0..9]=1> determines the maximum length of the call chain
            |               that is analyzed.
            |               If you increase this value by one, it is typically also necessary
            |               to increase the maxEvalFactor by a factor of 2 to 3. Otherwise it
            |               may happen that many analyses are aborted because the evaluation time
            |               is exhausted and – overall – the analysis reports less issues!]
            |[-minRelevance=<IntValue [0..99]=0> the minimum relevance of the shown issues.]
            |[-kinds=<Issue Kinds="constant_computation,dead_path,throws_exception,
            |                unguarded_use,..."> a comma seperated list of issue kinds
            |                that should be reported.]
            |[-eclipse      creates an eclipse console compatible output.]
            |[-idl          creates an idl report.]
            |[-html[=<FileName>] generates an HTML report which is written to the optionally
            |               specified location.]
            |[-debug[=<FileName>] turns on the debug mode (more information are logged and
            |               internal, recoverable exceptions are logged) the report is optionally
            |               written to the specified location.]""".stripMargin('|')

    private final val bugPickerAnalysis = new BugPickerAnalysis

    override def title: String = bugPickerAnalysis.title

    override def description: String = bugPickerAnalysis.description

    private[this] var cpFiles: Iterable[File] = null
    private[this] var libcpFiles: Iterable[File] = null

    override def setupProject(
        cpFiles:                 Iterable[File],
        libcpFiles:              Iterable[File],
        completelyLoadLibraries: Boolean,
        analysisMode:            AnalysisMode,
        fallbackConfiguration:   Config
    )(
        implicit
        initialLogContext: LogContext
    ): Project[URL] = {
        this.cpFiles = cpFiles
        this.libcpFiles = libcpFiles
        super.setupProject(
            cpFiles, libcpFiles, completelyLoadLibraries,
            analysisMode,
            fallbackConfiguration
        )
    }

    override def analyze(
        theProject:             Project[URL],
        parameters:             Seq[String],
        initProgressManagement: (Int) => ProgressManagement
    ): BasicReport = {

        import theProject.logContext

        OPALLogger.info("analysis progress", "starting analysis")

        val (analysisTime, issues0, exceptions) =
            bugPickerAnalysis.analyze(theProject, parameters, initProgressManagement)

        //
        // PREPARE THE GENERATION OF THE REPORT OF THE FOUND ISSUES
        // (HTML/Eclipse)
        //

        // Filter the report
        val minRelevance: Int =
            parameters.
                collectFirst { case MinRelevancePattern(i) => parseInt(i) }.
                getOrElse(MinRelevance)
        val issues1 = issues0.filter { i => i.relevance.value >= minRelevance }

        val issues = parameters.collectFirst { case IssueKindsPattern(ks) => ks } match {
            case Some(ks) =>
                val relevantKinds = ks.split(',').map(_.replace('_', ' ')).toSet
                issues1.filter(issue => (issue.kinds intersect (relevantKinds)).nonEmpty)
            case None =>
                issues1
        }

        // Generate the report well suited for the eclipse console
        //
        if (parameters.contains("-eclipse")) {
            val formattedIssues = issues.map { issue => issue.toEclipseConsoleString }
            println(formattedIssues.toSeq.sorted.mkString("\n"))
        }

        // Generate a report using the bug description language
        //
        if (parameters.contains("-idl")) {
            val formattedIssues = issues.map { issue => issue.toIDL.toString }
            println(s"Analysis of "+cpFiles.mkString(", "))
            println("Parameters")
            println(parameters.mkString("\n"))
            println("Issues")
            val idlReport = "["+formattedIssues.toSeq.mkString(",\n")+"]"
            println(idlReport)

            writeAndOpen(idlReport, "BugPickerAnalysisResults", ".json")
        }

        // Generate the HTML report
        //

        lazy val htmlReport = resultsAsXHTML(parameters, issues, showSearch = false, analysisTime).toString
        parameters.collectFirst { case HTMLFileOutputNameMatcher(name) => name } match {
            case Some(fileName) =>
                val file = new File(fileName).toPath
                process { Files.newBufferedWriter(file, StandardCharsets.UTF_8) } { fos =>
                    fos.write(htmlReport, 0, htmlReport.length)
                }
            case _ => // Nothing to do
        }
        if (parameters.contains("-html")) {
            writeAndOpen(htmlReport, "BugPickerAnalysisResults", ".html")
        }

        //
        // PREPARE THE GENERATION OF THE REPORT OF THE OCCURED EXCEPTIONS
        //
        if (exceptions.nonEmpty) {
            OPALLogger.error(
                "internal error",
                s"the analysis threw ${exceptions.size} exceptions"
            )
            exceptions.foreach { e =>
                OPALLogger.error(
                    "internal error", "the analysis failed", e
                )
            }

            var exceptionsReport: Node = null
            def getExceptionsReport = {
                if (exceptionsReport eq null) {
                    val exceptionNodes =
                        exceptions.take(10).map { e =>
                            <p>{ XHTML.throwableToXHTML(e) }</p>
                        }
                    exceptionsReport =
                        XHTML.createXHTML(
                            Some(s"${exceptions.size}/${exceptionNodes.size} Thrown Exceptions"),
                            <div>{ exceptionNodes }</div>
                        )
                }
                exceptionsReport
            }
            parameters.collectFirst { case DebugFileOutputNameMatcher(name) => name } match {
                case Some(fileName) =>
                    process { new java.io.FileOutputStream(fileName) } { fos =>
                        fos.write(getExceptionsReport.toString.getBytes("UTF-8"))
                    }
                case _ => // Nothing to do
            }
            if (parameters.contains("-debug")) {
                org.opalj.io.writeAndOpen(getExceptionsReport, "Exceptions", ".html")
            }
        }

        //
        // Print some statistics and "return"
        //
        val groupedIssues =
            issues.groupBy(_.relevance).toList.
                sortWith((e1, e2) => e1._1.value < e2._1.value)
        val groupedAndCountedIssues = groupedIssues.map(e => e._1+": "+e._2.size)

        BasicReport(
            groupedAndCountedIssues.mkString(
                s"Issues (∑${issues.size}):\n\t",
                "\n\t",
                s"\nIdentified in: ${analysisTime.toSeconds}.\n"
            )
        )
    }

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Seq[String] = {

        OPALLogger.info("analysis progress", "checking parameters")(GlobalLogContext)

        var outputFormatGiven = false

        import org.opalj.bugpicker.core.analyses.BugPickerAnalysis._

        val issues =
            parameters.filterNot(parameter => parameter match {
                case MaxEvalFactorPattern(d) =>
                    try {
                        val factor = java.lang.Double.parseDouble(d).toDouble
                        (factor >= 0.1d && factor < 100.0d) ||
                            factor == Double.PositiveInfinity
                    } catch {
                        case _: NumberFormatException => false
                    }
                case MaxEvalTimePattern(l) =>
                    try {
                        val maxTime = java.lang.Long.parseLong(l).toLong
                        maxTime >= 10L && maxTime <= 1000000L
                    } catch {
                        case _: NumberFormatException => false
                    }
                case MaxCardinalityOfIntegerRangesPattern(i) =>
                    try {
                        val cardinality = java.lang.Long.parseLong(i).toLong
                        cardinality >= 1L && cardinality <= 4294967295L
                    } catch {
                        case _: NumberFormatException => false
                    }
                case MaxCardinalityOfLongSetsPattern(i) =>
                    try {
                        val cardinality = java.lang.Integer.parseInt(i).toInt
                        cardinality >= 1 && cardinality <= 1024
                    } catch {
                        case _: NumberFormatException => false
                    }
                case MaxCallChainLengthPattern(_) =>
                    // the pattern ensures that the value is legal...
                    true

                case IssueKindsPattern(ks) =>
                    val kinds = ks.split(',').map(_.replace('_', ' '))
                    kinds.nonEmpty && kinds.forall { IssueKind.AllKinds.contains(_) }

                case MinRelevancePattern(_) =>
                    // the pattern ensures that the value is legal...
                    true

                case HTMLFileOutputNameMatcher(_) =>
                    outputFormatGiven = true; true
                case "-html" =>
                    outputFormatGiven = true; true
                case "-eclipse" =>
                    outputFormatGiven = true; true
                case "-idl" =>
                    outputFormatGiven = true; true
                case "-debug"                      => true
                case DebugFileOutputNameMatcher(_) => true
                case _                             => false
            })

        if (!outputFormatGiven)
            OPALLogger.warn("analysis configuration", "no output format specified")(GlobalLogContext)

        issues.map("unknown or illegal parameter: "+_)
    }
}
