/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.opalj.log.LogContext
import org.opalj.log.GlobalLogContext
import org.opalj.br.Code

import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable

/**
 * This system test(suite) just loads a very large number of class files and performs
 * an abstract interpretation of all methods using different domain configurations.
 *
 * This test suite has the following goals:
 *  - Test if seemingly independent (partial-) domain implementations are really
 *    independent by using different mixin-composition orders and comparing the
 *    results.
 *  - Test if several different domain configurations are actually working.
 *  - (Test if we can load and process a large number of different classes
 *    without exceptions.)
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class DomainIndependenceTest extends AnyFlatSpec with Matchers {

    private[this] implicit val logContext: LogContext = GlobalLogContext

    // We use this domain for the comparison of the values; it has the same
    // expressive power as the other domains.
    private object ValuesDomain
        extends ValuesCoordinatingDomain
        with l0.DefaultTypeLevelIntegerValues
        with l0.DefaultTypeLevelLongValues
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l0.DefaultReferenceValuesBinding
        with l0.TypeLevelDynamicLoads
        with PredefinedClassHierarchy

    //
    // The following three domains are very basic domains that – given that the
    // same partial domains are used – should compute the same results.
    //

    private class Domain1(val code: Code)
        extends Domain
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
        with l0.TypeLevelLongValuesShiftOperators
        with l0.TypeLevelFieldAccessInstructions
        with l0.TypeLevelInvokeInstructions
        with l0.TypeLevelDynamicLoads
        with PredefinedClassHierarchy
        with TheCode

    private class Domain2(val code: Code)
        extends Domain
        with DefaultHandlingOfMethodResults
        with IgnoreSynchronization
        with ThrowAllPotentialExceptionsConfiguration
        with l0.TypeLevelInvokeInstructions
        with l0.TypeLevelFieldAccessInstructions
        with l0.TypeLevelDynamicLoads
        with PredefinedClassHierarchy
        with DefaultSpecialDomainValuesBinding
        with l0.DefaultTypeLevelDoubleValues
        with l0.DefaultTypeLevelIntegerValues
        with l0.DefaultReferenceValuesBinding
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelLongValues
        with TheCode
        with l0.TypeLevelPrimitiveValuesConversions
        with l0.TypeLevelLongValuesShiftOperators

    private class Domain3(val code: Code)
        extends Domain
        with l0.TypeLevelPrimitiveValuesConversions
        with l0.DefaultReferenceValuesBinding
        with l0.DefaultTypeLevelIntegerValues
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelLongValues
        with l0.DefaultTypeLevelDoubleValues
        with l0.TypeLevelInvokeInstructions
        with l0.TypeLevelFieldAccessInstructions
        with l0.TypeLevelDynamicLoads
        with l0.TypeLevelLongValuesShiftOperators
        with PredefinedClassHierarchy
        with IgnoreSynchronization
        with DefaultHandlingOfMethodResults
        with ThrowAllPotentialExceptionsConfiguration
        with TheCode

    behavior of "a final domain composed of \"independent\" partial domains"

    it should "always calculate the same result" in {

        def corresponds(r1: AIResult, r2: AIResult): Option[String] = {
            r1.operandsArray.corresponds(r2.operandsArray) { (lOperands, rOperands) =>
                (lOperands == null && rOperands == null) ||
                    (lOperands != null && rOperands != null &&
                        lOperands.corresponds(rOperands) { (lValue, rValue) =>
                            val lVD = lValue.adapt(ValuesDomain, -1 /*Irrelevant*/ )
                            val rVD = rValue.adapt(ValuesDomain, -1 /*Irrelevant*/ )
                            if (!(lVD.abstractsOver(rVD) && rVD.abstractsOver(lVD)))
                                return Some(Console.RED_B+"the operand stack value "+lVD+" and "+rVD+" do not correspond ")
                            else
                                true
                        })
            }

            r1.localsArray.corresponds(r2.localsArray) { (lLocals, rLocals) =>
                (lLocals == null && rLocals == null) ||
                    (lLocals != null && rLocals != null &&
                        lLocals.corresponds(rLocals) { (lValue, rValue) =>
                            (lValue == null && rValue == null) || (
                                lValue != null && rValue != null && {
                                    val lVD = lValue.adapt(ValuesDomain, -1 /*Irrelevant*/ )
                                    val rVD = rValue.adapt(ValuesDomain, -1 /*Irrelevant*/ )
                                    if (lVD.isInstanceOf[ValuesDomain.ReturnAddressValue] || rVD.isInstanceOf[ValuesDomain.ReturnAddressValue]) {
                                        if ((lVD.isInstanceOf[ValuesDomain.ReturnAddressValue] && !rVD.isInstanceOf[ValuesDomain.ReturnAddressValue]) ||
                                            (rVD.isInstanceOf[ValuesDomain.ReturnAddressValue] && !lVD.isInstanceOf[ValuesDomain.ReturnAddressValue]))
                                            return Some(Console.BLUE_B+"the register value "+lVD+" does not correspond with "+rVD)
                                        else
                                            true
                                    } else if (!(lVD.abstractsOver(rVD) && rVD.abstractsOver(lVD)))
                                        return Some(Console.BLUE_B+"the register value "+lVD+" does not correspond with "+rVD)
                                    else
                                        true
                                }
                            )
                        })
            }

            None
        }

        val failed = new java.util.concurrent.atomic.AtomicInteger(0)
        val aiCount = new java.util.concurrent.atomic.AtomicInteger(0)
        val comparisonCount = new java.util.concurrent.atomic.AtomicInteger(0)

        for {
            (classFile, source) <- org.opalj.br.reader.readJREClassFiles().par
            method <- classFile.methods
            body <- method.body
        } {
            def TheAI() = new InstructionCountBoundedAI[Domain](body)

            val a1 = TheAI()
            val r1 = a1(method, new Domain1(body))
            aiCount.incrementAndGet()
            val a2 = TheAI()
            val r2 = a2(method, new Domain2(body))
            aiCount.incrementAndGet()
            val a3 = TheAI()
            val r3 = a3(method, new Domain3(body))
            aiCount.incrementAndGet()

            def abort(ai: InstructionCountBoundedAI[_], r: AIResult): Unit = {
                fail(
                    "the abstract interpretation of "+
                        method.toJava(
                            "was aborted after evaluating "+
                                ai.currentEvaluationCount+" instructions;\n"+r.stateToString
                        )
                )
            }

            if (r1.wasAborted) abort(a1, r1)
            if (r2.wasAborted) abort(a2, r1)
            if (r3.wasAborted) abort(a3, r1)

            corresponds(r1, r2).foreach { m =>
                failed.incrementAndGet()
                // let's test if r1 is stable....
                val a1_2 = TheAI()
                val r1_2 = a1_2(method, new Domain1(body))
                if (corresponds(r1, r1_2).nonEmpty) {
                    failed.incrementAndGet()
                    info(
                        classFile.thisType.toJava+"{ "+
                            method.signatureToJava(false)+"(Instructions "+method.body.get.instructions.size+")}\n"+
                            Console.BLUE+"\t// domain r1 is not deterministic (concurrency bug?)\n"+
                            Console.RESET
                    )
                } else
                    info(
                        classFile.thisType.toJava+"{ "+
                            method.signatureToJava(false)+"(Instructions "+method.body.get.instructions.size+")} \n"+
                            "\t// the results of r1 and r2 do not correspond\n"+
                            "\t// "+Console.BOLD + m + Console.RESET+"\n"
                    )
                comparisonCount.incrementAndGet()
            }
            comparisonCount.incrementAndGet()

            corresponds(r2, r3).foreach { m =>
                failed.incrementAndGet()
                info(
                    classFile.thisType.toJava+"{ "+
                        method.signatureToJava(false)+"(Instructions "+method.body.get.instructions.size+")} \n"+
                        "\t// the results of r2 and r3 do not correspond\n"+
                        "\t// "+Console.BOLD + m + Console.RESET+"\n"
                )
            }
            comparisonCount.incrementAndGet()
        }

        if (comparisonCount.get() < 2)
            fail("did not find any class files/method to analyze")
        if (failed.get() > 0) {
            fail("the domains computed different results in "+
                failed.get()+" cases out of "+comparisonCount.get)
        }
        info(
            s"successfully compared (${comparisonCount.get} comparisons) the results of "+
                s" ${aiCount.get} abstract interpretations"
        )
    }
}
