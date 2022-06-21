/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.support.info

import scala.annotation.switch

import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

import scala.collection.mutable.ListBuffer

import org.opalj.log.GlobalLogContext
import org.opalj.value.ValueInformation
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.ReportableAnalysisResult
import org.opalj.tac.Assignment
import org.opalj.tac.Call
import org.opalj.tac.DUVar
import org.opalj.tac.ExprStmt
import org.opalj.tac.LazyDetachedTACAIKey
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.VirtualMethodCall

/**
 * Analyzes a project for how a particular class is used within that project. Collects information
 * on which methods are called on objects of that class as well as how often.
 *
 * The analysis can be configured by passing the following parameters: `class` (the class to
 * analyze) and `granularity` (fine or coarse; defines which information will be gathered by an
 * analysis run; this parameter is optional). For further information see
 * [[ClassUsageAnalysis.analysisSpecificParametersDescription]].
 *
 * @author Patrick Mell
 * @author Dominik Helm
 */
object ClassUsageAnalysis extends ProjectAnalysisApplication {

    private type V = DUVar[ValueInformation]

    implicit val logContext: GlobalLogContext.type = GlobalLogContext

    override def title: String = "Class Usage Analysis"

    override def description: String = {
        "Analyzes a project for how a particular class is used within it, i.e., which methods "+
            "of instances of that class are called"
    }

    /**
     * Takes a [[Call]] and assembles the method descriptor for this call. The granularity is
     * determined by [[isFineGrainedAnalysis]]: For a fine-grained analysis, the returned string has
     * the format "[fully-qualified classname]#[method name]: [stringified method descriptor]" and
     * for a coarse-grained analysis: [fully-qualified classname]#[method name].
     */
    private def assembleMethodDescriptor(call: Call[V], isFineGrainedAnalysis: Boolean): String = {
        val fqMethodName = s"${call.declaringClass.toJava}#${call.name}"
        if (isFineGrainedAnalysis) {
            val methodDescriptor = call.descriptor.toString
            s"$fqMethodName: $methodDescriptor"
        } else {
            fqMethodName
        }
    }

    /**
     * Takes any [[Call]], checks whether the base object is of type [[className]] and if so,
     * updates the passed map by adding the count of the corresponding method. The granularity for
     * counting is determined by [[isFineGrainedAnalysis]].
     */
    private def processCall(
        call:                  Call[V],
        map:                   ConcurrentHashMap[String, AtomicInteger],
        className:             String,
        isFineGrainedAnalysis: Boolean
    ): Unit = {
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
        val (className, isFineGrainedAnalysis) = getAnalysisParameters(parameters)
        val resultMap: ConcurrentHashMap[String, AtomicInteger] = new ConcurrentHashMap()
        val tacProvider = project.get(LazyDetachedTACAIKey)

        project.parForeachMethodWithBody() { methodInfo =>
            tacProvider(methodInfo.method).stmts.foreach { stmt =>
                (stmt.astID: @switch) match {
                    case Assignment.ASTID | ExprStmt.ASTID =>
                        stmt.asAssignmentLike.expr match {
                            case c: Call[V] @unchecked =>
                                processCall(c, resultMap, className, isFineGrainedAnalysis)
                            case _ =>
                        }
                    case NonVirtualMethodCall.ASTID | VirtualMethodCall.ASTID |
                        StaticMethodCall.ASTID =>
                        processCall(stmt.asMethodCall, resultMap, className, isFineGrainedAnalysis)
                    case _ =>
                }
            }
        }

        val report = ListBuffer[String]("Result:")
        // Transform to a list, sort in ascending order of occurrences, and format the information
        resultMap.entrySet().stream().sorted { (value1, value2) =>
            value1.getValue.get().compareTo(value2.getValue.get())
        }.forEach(next => report.append(s"${next.getKey}: ${next.getValue}"))
        BasicReport(report)
    }

}
