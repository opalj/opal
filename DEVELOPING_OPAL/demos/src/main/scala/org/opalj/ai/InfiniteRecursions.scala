/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai

import java.net.URL
import scala.Console.BLUE
import scala.Console.BOLD
import scala.Console.RESET
import scala.language.existentials
import org.opalj.br.Method
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.SomeProject
import org.opalj.br.instructions.INVOKEINTERFACE
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.NEW

import scala.collection.parallel.CollectionConverters.IterableIsParallelizable

/**
 * An analysis that finds self-recursive calls with unchanged parameters.
 *
 * @author Marco Jacobasch
 * @author Michael Eichberg
 */
object InfiniteRecursions extends ProjectAnalysisApplication {

    override def title: String = "infinite recursions analysis"

    override def description: String = {
        "identifies method which calls themselves using infinite recursion"
    }

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String]   = List.empty,
        isInterrupted: () => Boolean
    ): BasicReport = {

        // In a real application we should take this from a parameter
        val maxRecursionDepth = 3

        val result =
            // for every method that calls itself ...
            for {
                classFile <- project.allClassFiles.par
                method <- classFile.methods
                body <- method.body.toSeq
                descriptor = method.descriptor
                if descriptor.parameterTypes.forall { t =>
                    // we don't have (as of Jan 1st 2015) a domain that enables a meaningful
                    // tracking of Float and Double values
                    t.isReferenceType || t.isLongType || t.isIntegerType
                }
                classType = classFile.thisType
                name = method.name
                pcs = body.foldLeft(List.empty[Int /*PC*/ ]) { (invokes, pc, instruction) =>
                    instruction match {
                        case INVOKEVIRTUAL(`classType`, `name`, `descriptor`)    => pc :: invokes
                        case INVOKESTATIC(`classType`, _, `name`, `descriptor`)  => pc :: invokes
                        case INVOKESPECIAL(`classType`, _, `name`, `descriptor`) => pc :: invokes
                        case INVOKEINTERFACE(`classType`, `name`, `descriptor`)  => pc :: invokes
                        case _                                                   => invokes
                    }
                }
                if pcs.nonEmpty
                result <- inifiniteRecursions(maxRecursionDepth, project, method, pcs)
            } yield { result }

        BasicReport(result.map(_.toString).mkString("\n"))
    }

    /**
     * Perform an abstract interpretation and check if (after some stabilization time)
     * the parameters to the recursive call are unchanged.
     *
     * `maxRecursionDepth` determines after how many non-recursive calls the analysis
     * is aborted.
     */
    def inifiniteRecursions(
        maxRecursionDepth: Int,
        project:           SomeProject,
        method:            Method,
        pcs:               List[Int /*PC*/ ]
    ): Option[InfiniteRecursion] = {

        assert(maxRecursionDepth > 1)
        assert(pcs.toSet.size == pcs.size, s"the seq $pcs contains duplicates")

        val body = method.body.get
        val parametersCount =
            method.descriptor.parametersCount + (if (method.isStatic) 0 else 1)

        // we are always analyzing the same method, hence, we can reuse the same domain
        // for all "Abstract Interpretations"
        val domain = new InfiniteRecursionsDomain(project, method)
        import domain.Operands

        var previousCallOperandsList: Seq[Operands] = Seq.empty

        def reduceCallOperands(operandsArray: domain.OperandsArray): Seq[Operands] = {
            var callOperandsList: List[Operands] = List.empty
            for {
                pc <- pcs
                if operandsArray(pc) ne null
                nextCallOperands: domain.Operands = operandsArray(pc).take(parametersCount)
            } {
                // IntegerRangeValues and ReferenceValues have useable equals semantics
                if (!callOperandsList.exists { _ == nextCallOperands })
                    callOperandsList = nextCallOperands :: callOperandsList
            }
            callOperandsList
        }

        // initialize callOperandsList by doing a first abstract interpretation
        val initialOperandsArray = BaseAI(method, domain).operandsArray
        previousCallOperandsList = reduceCallOperands(initialOperandsArray)

        def analyze(depth: Int, callOperands: Operands): Option[InfiniteRecursion] = {
            if (depth > maxRecursionDepth)
                return None;

            val parameters = mapOperandsToParameters(callOperands, method, domain)
            val aiResult = BaseAI.performInterpretation(body, domain)(List.empty, parameters)
            val operandsArray = aiResult.operandsArray
            val localsArray = aiResult.localsArray
            val callOperandsList =
                reduceCallOperands(operandsArray) filter { callOperands =>
                    if (previousCallOperandsList.contains(callOperands)) {

                        // let's check if we have a potential recursive call...
                        // i.e., if we can track back the operands to parameters
                        // concrete (fixed) values or values that are always created
                        // in the same manner; the idea is to reduce false positives
                        // due to non-infinite recursions due to side effects
                        if (callOperands.forall {
                            case domain.DomainSingleOriginReferenceValueTag(v) =>
                                if (v.origin < 0 /* === the value is a parameter*/ ||
                                    // the value is always created anew (no sideeffect)
                                    body.instructions(v.origin).opcode == NEW.opcode)
                                    true
                                else
                                    false
                            case v: domain.AnIntegerValue =>
                                if (localsArray(0).exists(_ eq v))
                                    true // the value is parameter
                                else
                                    false
                            case v: domain.ALongValue =>
                                if (localsArray(0).exists(_ eq v))
                                    true // the value is parameter
                                else
                                    false
                            case _: domain.LongSet      => true
                            case _: domain.IntegerRange => true
                            case _                      => false
                        })
                            return Some(InfiniteRecursion(method, callOperands));

                        // these operands are not relevant...
                        false
                    } else {
                        true
                    }
                }

            callOperandsList foreach { callOperands =>
                val result = analyze(depth + 1, callOperands)
                if (result.nonEmpty)
                    return result;
            }
            None
        }

        previousCallOperandsList foreach { callOperands =>
            val result = analyze(0, callOperands)
            if (result.nonEmpty)
                return result;
        }
        // no recursion...
        None
    }

}

class InfiniteRecursionsDomain(val project: SomeProject, val method: Method)
    extends Domain
    with domain.DefaultSpecialDomainValuesBinding
    with domain.ThrowAllPotentialExceptionsConfiguration
    with domain.l0.DefaultTypeLevelFloatValues
    with domain.l0.DefaultTypeLevelDoubleValues
    with domain.l0.TypeLevelFieldAccessInstructions
    with domain.l0.TypeLevelInvokeInstructions
    with domain.l0.TypeLevelDynamicLoads
    with domain.l1.DefaultReferenceValuesBinding
    with domain.l1.DefaultIntegerRangeValues
    with domain.l1.MaxArrayLengthRefinement
    // [CURRENTLY ONLY A WASTE OF RESOURCES] with domain.l1.ConstraintsBetweenIntegerValues
    // with domain.l1.DefaultIntegerSetValues
    with domain.l1.DefaultLongSetValues
    with domain.l1.LongSetValuesShiftOperators
    with domain.l1.ConcretePrimitiveValuesConversions
    with domain.DefaultHandlingOfMethodResults
    with domain.IgnoreSynchronization
    with domain.TheProject
    with domain.TheMethod

case class InfiniteRecursion(method: Method, operands: List[_ <: AnyRef]) {

    override def toString: String = {
        val declaringClassOfMethod = method.classFile.thisType.toJava

        "infinite recursion in "+BOLD + BLUE +
            declaringClassOfMethod + RESET +
            operands.mkString(s"{ ${method.signatureToJava()}{ ", ", ", " }}")
    }
}
