/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import java.net.URL

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

import org.opalj.concurrent.ConcurrentExceptions
import org.opalj.br.Method
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.MethodInfo
import org.opalj.br.TestSupport.createJREProject

/**
 * This integration test(suite) just loads a very large number of class files and performs
 * an abstract interpretation of all methods to test if a domain that uses more
 * precise partial domains leads to a more precise result.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class PrecisionOfDomainsTest extends AnyFunSpec with Matchers {

    describe("a more precise domain") {

        type TheAIResult = AIResult { val domain: Domain with TheMethod }

        it("should return a more precise result") {
            val theProject = createJREProject()
            // The following three domains are very basic domains that – given that the
            // same partial domains are used – should compute the same results.

            // We use this domain for the comparison of the values; it has a comparable
            // expressive power as the other domains.
            object ValuesDomain extends ValuesCoordinatingDomain
                with l1.DefaultLongValues
                with l0.DefaultTypeLevelFloatValues
                with l0.DefaultTypeLevelDoubleValues
                with l1.DefaultReferenceValuesBinding
                with l1.DefaultIntegerRangeValues
                with l0.TypeLevelDynamicLoads
                with TheProject {
                override val project: Project[URL] = theProject
            }

            class TypeLevelDomain(val method: Method, val project: Project[URL])
                extends Domain
                with TheProject
                with TheMethod
                with DefaultHandlingOfMethodResults
                with IgnoreSynchronization
                with DefaultSpecialDomainValuesBinding
                with ThrowAllPotentialExceptionsConfiguration
                with l0.DefaultReferenceValuesBinding
                with l0.DefaultTypeLevelIntegerValues
                with l0.DefaultTypeLevelLongValues
                with l0.DefaultTypeLevelFloatValues
                with l0.DefaultTypeLevelDoubleValues
                with l0.TypeLevelPrimitiveValuesConversions
                with l0.TypeLevelFieldAccessInstructions
                with l0.TypeLevelInvokeInstructions
                with l0.TypeLevelDynamicLoads
                with l0.TypeLevelLongValuesShiftOperators

            class L1RangesDomain[I](val method: Method, val project: Project[URL])
                extends CorrelationalDomain
                with TheProject
                with TheMethod
                with ThrowAllPotentialExceptionsConfiguration
                with DefaultHandlingOfMethodResults
                with IgnoreSynchronization
                with l1.DefaultReferenceValuesBinding
                with l1.NullPropertyRefinement
                with l1.DefaultIntegerRangeValues
                with l1.MaxArrayLengthRefinement
                with l1.DefaultLongValues
                with l1.LongValuesShiftOperators
                with l0.DefaultTypeLevelFloatValues
                with l0.DefaultTypeLevelDoubleValues
                with l0.TypeLevelPrimitiveValuesConversions
                with l0.TypeLevelInvokeInstructions
                with l0.TypeLevelFieldAccessInstructions
                with l0.TypeLevelDynamicLoads

            class L1SetsDomain[I](val method: Method, val project: Project[URL])
                extends CorrelationalDomain
                with TheProject
                with TheMethod
                with ThrowAllPotentialExceptionsConfiguration
                with DefaultHandlingOfMethodResults
                with IgnoreSynchronization
                with l1.DefaultReferenceValuesBinding
                with l1.NullPropertyRefinement
                with l1.DefaultIntegerSetValues // SET
                with l1.DefaultLongSetValues // SET
                with l1.LongValuesShiftOperators
                with l0.DefaultTypeLevelFloatValues
                with l0.DefaultTypeLevelDoubleValues
                with l0.TypeLevelPrimitiveValuesConversions
                with l0.TypeLevelInvokeInstructions
                with l0.TypeLevelFieldAccessInstructions
                with l0.TypeLevelDynamicLoads

            def checkAbstractsOver(r1: TheAIResult, r2: TheAIResult): Option[String] = {
                var pc = -1
                r1.operandsArray.corresponds(r2.operandsArray) { (lOperands, rOperands) =>
                    pc += 1
                    def compareOperands(): Option[String] = {
                        var op = -1
                        lOperands.corresponds(rOperands) { (lValue, rValue) =>
                            op += 1
                            val lVD = lValue.adapt(ValuesDomain, -1 /*Irrelevant*/ )
                            val rVD = rValue.adapt(ValuesDomain, -1 /*Irrelevant*/ )
                            if (!lVD.abstractsOver(rVD)) {
                                val line =
                                    r1.domain.code.lineNumber(pc).map(_.toString).getOrElse("N/A")
                                return Some(
                                    s"$pc[line=$line]: the operand stack $op "+
                                        s"value $lVD (${lVD.getClass.getName})"+
                                        s" does not abstract over $rVD (${rVD.getClass.getName})"+
                                        s" (original: $lValue join $rValue )"
                                );
                            }
                            // this primarily tests the "isMorePreciseThan" method
                            if (lVD.isMorePreciseThan(rVD)) {
                                val line =
                                    r1.domain.code.lineNumber(pc).map(_.toString).getOrElse("N/A")
                                return Some(
                                    s"$pc[line=$line]: the operand stack value $op "+
                                        s"$lVD#${System.identityHashCode(lVD)} (${lVD.getClass.getName})"+
                                        s" is more precise than "+
                                        s"$rVD#${System.identityHashCode(rVD)} (${rVD.getClass.getName})"+
                                        s" (original: $lValue join $rValue )"
                                );
                            }
                            true
                        }
                        None
                    }

                    (lOperands == null && rOperands == null) ||
                        (lOperands != null && (rOperands == null || {
                            val result = compareOperands()
                            if (result.isDefined)
                                return result;
                            true
                        }))
                }
                None
            }

            val failed = new java.util.concurrent.atomic.AtomicBoolean(false)
            val comparisonCount = new java.util.concurrent.atomic.AtomicInteger(0)

            try {
                theProject.parForeachMethodWithBody() { methodInfo =>
                    val MethodInfo(_, method) = methodInfo
                    val r1 = BaseAI(method, new TypeLevelDomain(method, theProject))
                    val r2_ranges = BaseAI(method, new L1RangesDomain(method, theProject))
                    val r2_sets = BaseAI(method, new L1SetsDomain(method, theProject))

                    def handleAbstractsOverFailure(
                        lpDomain: String,
                        mpDomain: String
                    )(
                        m: String
                    ): Unit = {
                        failed.set(true)
                        val bodyMessage =
                            "\" /*Instructions "+method.body.get.instructions.size+"*/\n"+
                                s"\tthe less precise domain ($lpDomain) did not abstract "+
                                s"over the state of the more precise domain ($mpDomain)\n"+
                                "\t"+Console.BOLD + m + Console.RESET+"\n"
                        println(method.toJava(bodyMessage))
                    }

                    checkAbstractsOver(r1, r2_ranges).foreach(
                        handleAbstractsOverFailure("TypeLevelDomain", "L1RangesDomain")
                    )
                    comparisonCount.incrementAndGet()

                    checkAbstractsOver(r1, r2_sets).foreach(
                        handleAbstractsOverFailure("TypeLevelDomain", "L1SetsDomain")
                    )
                    comparisonCount.incrementAndGet()

                }
            } catch {
                case ce: ConcurrentExceptions =>
                    ce.getSuppressed()(0).printStackTrace()
                    fail(ce.getSuppressed.mkString("underlying exceptions:\n", "\n", "\n\n"))
            }

            if (comparisonCount.get() < 2) {
                fail("didn't find any class files/methods to analyze")
            }

            if (failed.get()) {
                fail("the less precise domain did not abstract over the more precise domain")
            }

            info(s"compared the results of ${comparisonCount.get} abstract interpretations")
        }
    }
}
