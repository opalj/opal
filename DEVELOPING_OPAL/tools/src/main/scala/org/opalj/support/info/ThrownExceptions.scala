/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package support
package info

import java.net.URL

import org.opalj.util.Nanoseconds
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.fpcf.PropertyKind
import org.opalj.fpcf.PropertyStore
import org.opalj.fpcf.SomeEPS
import org.opalj.br.Method
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.Project
import org.opalj.br.collection.TypesSet
import org.opalj.br.fpcf.properties.{ThrownExceptions => ThrownExceptionsProperty}
import org.opalj.br.fpcf.FPCFAnalysesManagerKey
import org.opalj.br.fpcf.PropertyStoreKey
import org.opalj.br.fpcf.analyses.EagerL1ThrownExceptionsAnalysis
import org.opalj.br.fpcf.analyses.LazyVirtualMethodThrownExceptionsAnalysis

/**
 * Prints out the information about the exceptions thrown by methods.
 *
 * @author Michael Eichberg
 * @author Andreas Muttschelller
 */
object ThrownExceptions extends ProjectAnalysisApplication {

    override def title: String = "Thrown Exceptions"

    override def description: String = {
        "Computes the set of the exceptions (in)directly thrown by methods"
    }

    final val AnalysisLevelL0 = "-analysisLevel=L0"
    final val AnalysisLevelL1 = "-analysisLevel=L1"
    final val SuppressPerMethodReports = "-suppressPerMethodReports"

    override def analysisSpecificParametersDescription: String = {
        "[-analysisLevel=<L0|L1>  (Default: L1)]\n"+
            "[-suppressPerMethodReports]"
    }

    override def checkAnalysisSpecificParameters(parameters: Seq[String]): Iterable[String] = {
        val remainingParameters =
            parameters.filter { p =>
                p != AnalysisLevelL0 && p != AnalysisLevelL1 && p != SuppressPerMethodReports
            }
        super.checkAnalysisSpecificParameters(remainingParameters)
    }

    def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {
        var executionTime: Nanoseconds = Nanoseconds.None
        val ps: PropertyStore = time {

            if (parameters.contains(AnalysisLevelL0)) {
                // We are relying on/using the "FallbackAnalysis":
                val ps = project.get(PropertyStoreKey)
                ps.setupPhase(Set.empty[PropertyKind]) // <= ALWAYS REQUIRED.
                // We have to query the properties...
                project.allMethods foreach { m => ps.force(m, ThrownExceptionsProperty.key) }
                ps.waitOnPhaseCompletion()
                ps
            } else /* if no analysis level is specified or L1 */ {
                val (ps, _) = project.get(FPCFAnalysesManagerKey).runAll(
                    LazyVirtualMethodThrownExceptionsAnalysis,
                    EagerL1ThrownExceptionsAnalysis
                )
                ps
            }
        } { t => executionTime = t }

        val allMethods = ps.entities(ThrownExceptionsProperty.key).iterator.to(Iterable)
        val (epsNotThrowingExceptions, otherEPS) =
            allMethods.partition(_.ub.throwsNoExceptions)
        val epsThrowingExceptions = otherEPS.filter(eps => eps.lb.types != TypesSet.SomeException)

        val methodsThrowingExceptions = epsThrowingExceptions.map(_.e.asInstanceOf[Method])
        val privateMethodsThrowingExceptionsCount = methodsThrowingExceptions.count(_.isPrivate)

        val privateMethodsNotThrowingExceptions =
            epsNotThrowingExceptions.map(_.e.asInstanceOf[Method]).filter(_.isPrivate)

        val perMethodsReport =
            if (parameters.contains(SuppressPerMethodReports))
                ""
            else {
                val epsThrowingExceptionsByClassFile = epsThrowingExceptions groupBy (_.e.asInstanceOf[Method].classFile)
                epsThrowingExceptionsByClassFile.map { e =>
                    val (cf, epsThrowingExceptionsPerMethod) = e
                    cf.thisType.toJava+"{"+
                        epsThrowingExceptionsPerMethod.map { eps: SomeEPS =>
                            val m: Method = eps.e.asInstanceOf[Method]
                            val ThrownExceptionsProperty(types) = eps.ub
                            m.descriptor.toJava(m.name)+" throws "+types.toString
                        }.toList.sorted.mkString("\n\t\t", "\n\t\t", "\n")+
                        "}"

                }.mkString("\n", "\n", "\n")
            }

        val psStatistics = ps.statistics.map(e => e._1+": "+e._2).mkString("Property Store Statistics:\n\t", "\n\t", "\n")

        val analysisStatistics: String =
            "\nStatistics:\n"+
                "#methods with a thrown exceptions property: "+
                s"${allMethods.size} (${project.methodsCount})\n"+
                "#methods with exceptions information more precise than _ <: Throwable: "+
                s"${methodsThrowingExceptions.size + epsNotThrowingExceptions.size}\n"+
                s" ... #exceptions == 0: ${epsNotThrowingExceptions.size}\n"+
                s" ... #exceptions == 0 and private: ${privateMethodsNotThrowingExceptions.size}\n"+
                s" ... #exceptions >  0 and private: $privateMethodsThrowingExceptionsCount\n"+
                s"execution time: ${executionTime.toSeconds}\n"

        BasicReport(
            psStatistics+
                "\nThrown Exceptions Information:\n"+
                perMethodsReport+"\n"+
                ps.toString(printProperties = false) +
                analysisStatistics
        )
    }
}
