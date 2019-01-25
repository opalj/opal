/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.support.info

import scala.annotation.switch

import java.net.URL

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.opalj.log.GlobalLogContext
import org.opalj.log.OPALLogger
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ReportableAnalysisResult
import org.opalj.tac.Assignment
import org.opalj.tac.Call
import org.opalj.tac.ExprStmt
import org.opalj.tac.SimpleTACAIKey
import org.opalj.tac.VirtualFunctionCall
import org.opalj.tac.fpcf.analyses.cg.V

/**
 * Analyzes a project for how a particular class is used within that project. This means that this
 * analysis collect information on which methods are called on objects of that class as well as how
 * often.
 *
 * The analysis can be configured by passing the following optional parameters: `class` (the class
 * to analyze), `granularity` (fine- or coarse-grained; defines which information will be gathered
 * by an analysis run). For further information see
 * [[ClassUsageAnalysis.analysisSpecificParametersDescription]].
 *
 * @author Patrick Mell
 */
object ClassUsageAnalysis extends DefaultOneStepAnalysis {

    implicit val logContext: GlobalLogContext.type = GlobalLogContext

    override def title: String = "Class Usage Analysis"

    override def description: String = {
        "Analysis a project for how a particular class is used, i.e., which methods are called "+
            "on it"
    }

    /**
     * The fully-qualified name of the class that is to be analyzed in a Java format, i.e., dots as
     * package / class separators.
     */
    private var className = "java.lang.StringBuilder"

    /**
     * The analysis can run in two modes: Fine-grained or coarse-grained. Fine-grained means that
     * two method are considered as the same only if their method descriptor is the same, i.e., this
     * mode enables a differentiation between overloaded methods.
     * The coarse-grained method, however, regards two method calls as the same if the class of the
     * base object as well as the method name are equal, i.e., overloaded methods are not
     * distinguished.
     */
    private var isFineGrainedAnalysis = false

    /**
     * Takes a [[Call]] and assembles the method descriptor for this call. The granularity is
     * determined by [[isFineGrainedAnalysis]]: For a fine-grained analysis, the returned string has
     * the format "[fully-qualified classname]#[method name]: [stringified method descriptor]" and
     * for a coarse-grained analysis: [fully-qualified classname]#[method name].
     */
    private def assembleMethodDescriptor(call: Call[V]): String = {
        val fqMethodName = s"${call.declaringClass.toJava}#${call.name}"
        if (isFineGrainedAnalysis) {
            val methodDescriptor = call.descriptor.toString
            s"$fqMethodName: $methodDescriptor"
        } else {
            fqMethodName
        }
    }

    /**
     * Takes any function [[Call]], checks whether the base object is of type [[className]] and if
     * so, updates the passed map by adding the count of the corresponding method. The granularity
     * for counting is determined by [[isFineGrainedAnalysis]].
     */
    private def processFunctionCall(call: Call[V], map: mutable.Map[String, Int]): Unit = {
        val declaringClassName = call.declaringClass.toJava
        if (declaringClassName == className) {
            val methodDescriptor = assembleMethodDescriptor(call, isFineGrainedAnalysis)
            if (map.putIfAbsent(methodDescriptor, new AtomicInteger(1)) != null) {
                map.get(methodDescriptor).addAndGet(1)
            }
        }
    }

    override def analysisSpecificParametersDescription: String = {
        "-class=<fully-qualified class name> \n"+
            "[-granularity=<fine|coarse> (Default: coarse)]"
    }

    /**
     * The fully-qualified name of the class that is to be analyzed in a Java format, i.e., dots as
     * package / class separators.
     */
    private final val parameterNameForClass = "-class="

    /**
     * The analysis can run in two modes: Fine-grained or coarse-grained. Fine-grained means that
     * two methods are considered equal iff their method descriptor is the same, i.e., this mode
     * enables a differentiation between overloaded methods.
     * The coarse-grained method, however, regards two method calls as the same if the class of the
     * base object as well as the method name are equal, i.e., overloaded methods are not
     * distinguished.
     */
    private final val parameterNameForGranularity = "-granularity="

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Iterable[String] = {
        val remainingParameters =
            parameters.filter { p =>
                !p.contains(parameterNameForClass) && !p.contains(parameterNameForGranularity)
            }
        super.checkAnalysisSpecificParameters(remainingParameters)
    }

    /**
     * Takes the parameters passed as program arguments, i.e., in the format
     * "-[param name]=[value]", extracts the values and sets the corresponding object variables.
     */
    private def getAnalysisParameters(parameters: Seq[String]): (String, Boolean) = {
        val classParam = parameters.find(_.startsWith(parameterNameForClass))
        val className = if (classParam.isDefined) {
            classParam.get.substring(classParam.get.indexOf("=") + 1)
        } else {
            throw new IllegalArgumentException("missing argument: -class")
        }

        val granularityParam = parameters.find(_.startsWith(parameterNameForGranularity))
        val isFineGrainedAnalysis =
            if (granularityParam.isDefined) {
                granularityParam.get.substring(granularityParam.get.indexOf("=") + 1) match {
                    case "fine"   => true
                    case "coarse" => false
                    case _ =>
                        val msg = "incorrect argument: -granularity must be one of fine|coarse"
                        throw new IllegalArgumentException(msg)
                }
            } else {
                false // default is coarse grained
            }
        (className, isFineGrainedAnalysis)

    }

    override def doAnalyze(
        project: Project[URL], parameters: Seq[String], isInterrupted: () => Boolean
    ): ReportableAnalysisResult = {
        getAnalysisParameters(parameters)
        val resultMap = mutable.Map[String, Int]()
        val tacProvider = project.get(SimpleTACAIKey)

        project.allMethodsWithBody.foreach { m ⇒
            tacProvider(m).stmts.foreach { stmt ⇒
                (stmt.astID: @switch) match {
                    case Assignment.ASTID ⇒ stmt match {
                        case Assignment(_, _, c: VirtualFunctionCall[V]) ⇒
                            processFunctionCall(c, resultMap)
                        case _ ⇒
                    }
                    case ExprStmt.ASTID ⇒ stmt match {
                        case ExprStmt(_, c: VirtualFunctionCall[V]) ⇒
                            processFunctionCall(c, resultMap)
                        case _ ⇒
                    }
                    case _ ⇒
                }
            }
        }

        val report = ListBuffer[String]("Results")
        // Transform to a list, sort in ascending order of occurrences and format the information
        report.appendAll(resultMap.toList.sortWith(_._2 < _._2).map {
            case (descriptor: String, count: Int) ⇒ s"$descriptor: $count"
        })
        BasicReport(report)
    }

}
