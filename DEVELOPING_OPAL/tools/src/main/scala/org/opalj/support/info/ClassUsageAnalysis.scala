/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import scala.annotation.switch
import scala.language.postfixOps

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable.ListBuffer

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.MultiProjectAnalysisApplication
import org.opalj.br.analyses.SomeProject
import org.opalj.br.cli.ClassNameArg
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.cli.GranularityArg
import org.opalj.tac.Assignment
import org.opalj.tac.Call
import org.opalj.tac.DUVar
import org.opalj.tac.ExprStmt
import org.opalj.tac.LazyDetachedTACAIKey
import org.opalj.tac.NonVirtualMethodCall
import org.opalj.tac.StaticMethodCall
import org.opalj.tac.VirtualMethodCall
import org.opalj.value.ValueInformation

import org.rogach.scallop.ScallopConf

/**
 * Analyzes a project for how a particular class is used within that project. Collects information
 * on which methods are called on objects of that class as well as how often.
 *
 * The analysis can be configured by passing the following parameters: `class` (the class to
 * analyze) and `granularity` (fine or coarse; defines which information will be gathered by an
 * analysis run; this parameter is optional).
 *
 * The analysis can run in two modes: Fine-grained or coarse-grained. Fine-grained means that
 * two methods are considered equal iff their method descriptor is the same, i.e., this mode
 * enables a differentiation between overloaded methods.
 * The coarse-grained method, however, regards two method calls as the same if the class of the
 * base object as well as the method name are equal, i.e., overloaded methods are not
 * distinguished.
 *
 * @author Patrick Mell
 * @author Dominik Helm
 */
object ClassUsageAnalysis extends MultiProjectAnalysisApplication {

    protected class ClassUsageConfig(args: Array[String]) extends ScallopConf(args)
        with MultiProjectAnalysisConfig[ClassUsageConfig] {

        banner("Analyzes a project for how a particular class is used within it, i.e., which methods of instances of that class are called\n")

        args(
            ClassNameArg !,
            GranularityArg
        )

    }

    protected type ConfigType = ClassUsageConfig

    protected def createConfig(args: Array[String]): ClassUsageConfig = new ClassUsageConfig(args)

    private type V = DUVar[ValueInformation]

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

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: ClassUsageConfig,
        execution:      Int
    ): (SomeProject, BasicReport) = {
        val className = analysisConfig(ClassNameArg)
        val isFineGrainedAnalysis = analysisConfig.get(GranularityArg, false)

        val resultMap: ConcurrentHashMap[String, AtomicInteger] = new ConcurrentHashMap()

        val (project, _) = analysisConfig.setupProject(cp)

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

        (project, BasicReport(report))
    }
}
