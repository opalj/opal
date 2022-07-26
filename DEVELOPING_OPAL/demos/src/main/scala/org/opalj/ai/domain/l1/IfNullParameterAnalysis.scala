/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import java.net.URL
import scala.Console.BLUE
import scala.Console.RESET
import scala.collection.Set
import scala.collection.immutable.LongMap
import org.opalj.ai.Domain
import org.opalj.ai.InterruptableAI
import org.opalj.ai.domain
import org.opalj.br.ClassFile
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.Project
import org.opalj.util.PerformanceEvaluation.time

import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable

/**
 * A very basic analysis that determines the behavior of a method if a parameter
 * is potentially `null` compared to a call of the method where the parameter is guaranteed
 * to be non-null.
 *
 * Note that the difference may not just manifest in the number of thrown exceptions.
 * E.g., consider the following code from `javax.imageio.ImageIO`:
 * {{{
 * public static Iterator<ImageReader> getImageReadersBySuffix(String fileSuffix) {
 *  if (fileSuffix == null) {
 *      throw new IllegalArgumentException("fileSuffix == null!");
 *  }
 *  // Ensure category is present
 *  ...
 * }}}
 * Here, the difference is that an `IllegalArgumentException` is thrown. The reported
 * result is:
 * `Map(13 -> Set(java.lang.IllegalArgumentException(origin=4)))`
 * and states that the instruction with the program counter 13 (the `throw` instruction)
 * throws the exception created by program counter 4 (the `new Illegal...` expression).
 *
 * @author Michael Eichberg
 */
object IfNullParameterAnalysis extends ProjectAnalysisApplication {

    override def title: String =
        "Identifies methods that are sensitive to parameters that are \"null\""

    override def description: String =
        "Identifies methods that exhibit different behavior w.r.t. "+
            "the number and kind of thrown exceptions if a parameter is \"null\"."

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {

        // Explicitly specifies that all reference values are not null.
        def setToNonNull(domain: DefaultDomain[URL])(locals: domain.Locals): domain.Locals = {
            locals.map { value =>
                if (value == null)
                    // not all local values are used right from the beginning
                    null
                else if (!value.isInstanceOf[domain.SingleOriginReferenceValue]) {
                    // we are not concerned about primitive values
                    value
                } else {
                    val initialReferenceValue = value.asInstanceOf[domain.SingleOriginReferenceValue]
                    // create a new reference value which is not null
                    initialReferenceValue.update(isNull = No)
                }
            }
        }

        val methodsWithDifferentExceptions = time {
            for {
                classFile <- theProject.allProjectClassFiles.par
                method <- classFile.methods
                if method.body.isDefined
                if method.descriptor.parameterTypes.exists { _.isReferenceType }
                originalType = method.returnType
            } yield {
                val ai = new InterruptableAI[Domain]

                // 1. Default interpretation
                val domain1 =
                    new DefaultDomain(theProject, method) with domain.RecordAllThrownExceptions
                ai.performInterpretation(method.body.get, domain1)(
                    ai.initialOperands(method, domain1), ai.initialLocals(method, domain1)(None)
                )

                // 2. Interpretation under the assumption that all values are non-null
                val domain2 =
                    new DefaultDomain(theProject, method) with domain.RecordAllThrownExceptions
                val nonNullLocals = setToNonNull(domain2)(ai.initialLocals(method, domain2)(None))
                ai.performInterpretation(method.body.get, domain2)(
                    ai.initialOperands(method, domain2), nonNullLocals
                )

                // Let's calculate the diff. The basic idea is to iterate over
                // all thrown exceptions and to throw away those that are
                // thrown in both cases, the remaining ones constitute
                // the difference.
                var result = LongMap.empty[Set[_ <: AnyRef]]
                var d2ThrownExceptions = domain2.allThrownExceptions
                domain1.allThrownExceptions.foreach { e =>
                    val (pc, d1thrownException) = e
                    val d2ThrownException = d2ThrownExceptions.get(pc)
                    if (d2ThrownException.isDefined) {
                        val adaptedD2ThrownException =
                            d2ThrownException.get.map(ex =>
                                ex.adapt(
                                    domain1,
                                    // We need to keep the original location, otherwise
                                    // the correlation analysis would miserably fail!
                                    ex.asInstanceOf[domain2.DomainSingleOriginReferenceValue].origin
                                ).asInstanceOf[domain1.ExceptionValue]).toSet[domain1.DomainReferenceValue]
                        val diff =
                            d1thrownException.diff(adaptedD2ThrownException) ++
                                adaptedD2ThrownException.diff(d1thrownException)
                        if (diff.nonEmpty)
                            result = result.updated(pc, diff)

                        d2ThrownExceptions = d2ThrownExceptions - (pc)
                    } else {
                        result += e
                    }
                }

                (
                    classFile, //<= only used for sorting purposes
                    BLUE + method.toJava + RESET,
                    result ++ d2ThrownExceptions
                )
            }
        } { t => println("Analysis time "+t.toSeconds) }

        val methodsWithDifferences = methodsWithDifferentExceptions.filter(_._3.nonEmpty).seq.toSeq
        BasicReport(
            methodsWithDifferences.sortWith { (l, r) =>
                val (cf1: ClassFile, _, _) = l
                val (cf2: ClassFile, _, _) = r
                cf1.thisType.toString < cf2.thisType.toString
            }.map(e => (e._2, e._3)).mkString("\n\n")+
                "Number of findings: "+methodsWithDifferences.size
        )
    }

}
