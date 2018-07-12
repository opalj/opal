/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses.cg

import scala.collection.Map
import org.opalj.br._
import org.opalj.br.analyses.SomeProject

/**
 * Basic representation of a (calculated) call graph.
 *
 * ==Terminology==
 * A method that calls another method is referred to as the `caller`. The method
 * that is called is called the `callee`. Hence, a caller calls a callee.
 *
 * ==Thread Safety==
 * The call graph is effectively immutable and can be accessed by multiple
 * threads concurrently.
 * Calls will never block.
 *
 * ==Call Graph Construction==
 * The call graph is constructed by the [[CallGraphFactory]].
 *
 * @param calledByMap The map of all methods that are called by at least one method.
 *      I.e., the value is never the empty map.
 * @param callsMap The map of all methods that call at least one method.
 *      I.e., the value is never an empty map.
 * @author Michael Eichberg
 */
class CallGraph private[opalj] (
        val project:                   SomeProject,
        private[this] val calledByMap: Map[Method, Map[Method, PCs]],
        private[this] val callsMap:    Map[Method, Map[PC, Iterable[Method]]]
) {

    /**
     * Returns the invoke instructions (by means of (`Method`,`PC`) pairs) that
     * call the given method. If this method is not called by any other method an
     * empty map is returned.
     */
    def calledBy(method: Method): Map[Method, PCs] = {
        calledByMap.getOrElse(method, Map.empty)
    }

    /**
     * Returns the methods that are potentially invoked by the invoke instruction
     * identified by the (`method`,`pc`) pair.
     *
     * If the project is incomplete the iterable may be empty!
     */
    def calls(method: Method, pc: PC): Iterable[Method] = {
        callsMap.get(method).map { callees ⇒
            callees.get(pc).getOrElse(Iterable.empty)
        }.getOrElse(Iterable.empty)
    }

    /**
     * Returns the methods that are called by the invoke instructions of the given method.
     *
     * If this method does not call any methods an empty map is returned.
     */
    // In case of the CHA Call Graph this could also be easily calculated on-demand,
    // since we do not use any information that is not readily available.
    // However, we collect/store that information for the time being to make the
    // implementation more uniform.
    def calls(method: Method): Map[PC, Iterable[Method]] = {
        callsMap.getOrElse(method, Map.empty)
    }

    /**
     * Calls the function `f` for each method that calls some other method.
     */
    def foreachCallingMethod[U](f: (Method, Map[PC, Iterable[Method]]) ⇒ U): Unit = {
        callsMap foreach { entry ⇒
            val (method, callees) = entry
            f(method, callees)
        }
    }

    /**
     * Calls the function `f` for each method that is called by some other method.
     */
    def foreachCalledByMethod[U](f: (Method, Map[Method, PCs]) ⇒ U): Unit = {
        calledByMap foreach { entry ⇒
            val (method, callees) = entry
            f(method, callees)
        }
    }

    /** Number of methods that call at least one other method. */
    def callsCount: Int = callsMap.size

    def callEdgesCount: Int = {
        val perMethodCallTargetsCount =
            callsMap.map { e ⇒
                val (_, perMethodCallTargets) = e
                perMethodCallTargets.values.map(_.size).sum
            }
        perMethodCallTargetsCount.sum
    }

    def calledByEdgesCount: Int = {
        // calledByMap = Map[Method, Map[Method, PCs]]
        val perMethodCalledByCount =
            calledByMap.map { e ⇒
                val (_, perMethodCallers) = e
                perMethodCallers.values.map(_.size).sum
            }

        perMethodCalledByCount.sum
    }

    /** Number of methods that are called by at least one other method. */
    def calledByCount: Int = calledByMap.size

    def callSites: Int = callsMap.values.map(_.size).sum

    /**
     * Statistics about the number of potential targets per call site.
     * (TSV format (tab-separated file) - can easily be read by most spreadsheet
     * applications).
     */
    def callsStatistics(
        maxNumberOfResults: Int = 65534 /*65534+1 for the header === max size for common spread sheet applications*/
    ): String = {

        var result: List[(String, String, String, Int, Int)] = List.empty
        project.allMethods foreach { (method: Method) ⇒
            val callSites = calls(method)
            callSites foreach { callSite ⇒
                val (pc, targets) = callSite
                result ::= ((
                    method.classFile.fqn,
                    "\""+method.signatureToJava(withVisibility = true)+"\"",
                    "\""+method.body.get.instructions(pc).toString(pc).replace('\n', ' ')+"\"",
                    pc,
                    targets.size
                ))
            }
        }
        result = result.sortWith((a, b) ⇒ a._5 > b._5 || (a._5 == b._5 && a._4 < b._4))
        result = result.take(maxNumberOfResults)
        val resultsAsString: List[List[String]] =
            // add(prepend) the line with the column titles
            List("\"Class\"", "\"Method\"", "\"Callsite (PC)\"", "\"Invoke\"", "\"Targets\"") ::
                result.map { e ⇒ e.productIterator.toList.map(_.toString()) }
        resultsAsString.view.map(_.mkString("\t")).mkString("\n")
    }

    /**
     * Statistics about the number of methods that potentially call a specific method.
     * (TSV format (tab-separated file) - can easily be read by most spreadsheet
     * applications).
     */
    def calledByStatistics(maxNumberOfResults: Int = 65536): String = {
        assume(maxNumberOfResults > 0)

        var result: List[List[String]] = List.empty
        var resultCount = 0
        project.allMethods forall { (method: Method) ⇒
            val callingSites = calledBy(method)
            callingSites forall { callingSite ⇒
                val (callerMethod, callingInstructions) = callingSite
                result =
                    List(
                        method.classFile.fqn,
                        method.signatureToJava(withVisibility = false),
                        callerMethod.classFile.fqn,
                        callerMethod.signatureToJava(withVisibility = false),
                        callingInstructions.size.toString
                    ) :: result
                resultCount += 1
                resultCount < maxNumberOfResults
            }
            resultCount < maxNumberOfResults
        }
        // add(prepend) the line with the column titles
        result ::=
            List(
                "\"Class\"",
                "\"Method\"",
                "\"Class of calling Method\"",
                "\"Calling Method\"",
                "\"Calling Sites\""
            )
        result.view.map(_.mkString("\t")).mkString("\n")
    }
}
