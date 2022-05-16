/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.opalj.bi.TestResources.locateTestResources
import org.opalj.ai.common.XHTML.dumpOnFailureDuringValidation
import org.opalj.br._
import org.opalj.br.reader.Java8Framework.ClassFiles
import org.opalj.ai.domain.l0._

/**
 * Basic tests of the abstract interpreter in the presence of simple control flow
 * instructions (if).
 *
 * @author Michael Eichberg
 * @author Dennis Siebert
 */
@RunWith(classOf[JUnitRunner])
class MethodsWithBranchesTest extends AnyFlatSpec with Matchers {

    import MethodsWithBranchesTest._

    import domain.RecordConstraints

    class TestDomain(val name: String)
        extends Domain
        with DefaultSpecialDomainValuesBinding
        with DefaultReferenceValuesBinding
        with DefaultTypeLevelIntegerValues
        with DefaultTypeLevelLongValues
        with DefaultTypeLevelFloatValues
        with DefaultTypeLevelDoubleValues
        with TypeLevelPrimitiveValuesConversions
        with TypeLevelLongValuesShiftOperators
        with TypeLevelFieldAccessInstructions
        with SimpleTypeLevelInvokeInstructions
        with TypeLevelDynamicLoads
        with ThrowAllPotentialExceptionsConfiguration
        with PredefinedClassHierarchy
        with IgnoreSynchronization
        with DefaultHandlingOfMethodResults
        with RecordLastReturnedValues
        with RecordConstraints {

        type Id = String
        def id = "MethodsWithBranchesTestDomain: "+name
    }

    private def evaluateMethod(name: String)(f: TestDomain => Unit): Unit = {
        val domain = new TestDomain(name)
        val method = classFile.methods.find(_.name == name).get
        val result = BaseAI(method, domain)

        dumpOnFailureDuringValidation(
            Some(classFile),
            Some(method),
            method.body.get,
            result
        ) { f(domain) }
    }

    behavior of "the abstract interpreter"

    //
    // RETURNS
    it should "be able to analyze a method that performs a comparison with \"nonnull\"" in {
        evaluateMethod("nullComp") { domain =>
            //    0  aload_0 [o]
            //    1  ifnonnull 6
            //    4  iconst_1
            //    5  ireturn
            //    6  iconst_0
            //    7  ireturn
            import domain._
            domain.allReturnedValues should be(
                Map((5 -> AnIntegerValue), (7 -> AnIntegerValue))
            )

            domain.allConstraints exists { constraint =>
                val ReifiedSingleValueConstraint(pc, value, kind) = constraint
                pc == 4 &&
                    domain.isValueASubtypeOf(value, ObjectType.Object).isYes &&
                    kind == "is null"
            } should be(true)

            domain.allConstraints exists { constraint =>
                val ReifiedSingleValueConstraint(pc, value, kind) = constraint
                pc == 6 &&
                    domain.isValueASubtypeOf(value, ObjectType.Object).isYes &&
                    kind == "is not null"
            } should be(true)
        }
    }

    it should "be able to analyze a method that performs a comparison with \"null\"" in {
        evaluateMethod("nonnullComp") { domain =>
            //    0  aload_0 [o]
            //    1  ifnull 6
            //    4  iconst_1
            //    5  ireturn
            //    6  iconst_0
            //    7  ireturn
            import domain._
            domain.allReturnedValues should be(
                Map((5 -> AnIntegerValue), (7 -> AnIntegerValue))
            )

            domain.allConstraints exists { constraint =>
                val ReifiedSingleValueConstraint(pc, value, kind) = constraint
                pc == 6 &&
                    domain.isValueASubtypeOf(value, ObjectType.Object).isYes &&
                    kind == "is null"
            } should be(true)

            domain.allConstraints exists { constraint =>
                val ReifiedSingleValueConstraint(pc, value, kind) = constraint
                pc == 4 &&
                    domain.isValueASubtypeOf(value, ObjectType.Object).isYes &&
                    kind == "is not null"
            } should be(true)
        }
    }

    it should "be able to analyze methods that perform multiple comparisons" in {
        evaluateMethod("multipleComp") { domain =>
            //     0  aload_0 [a]
            //     1  ifnull 17
            //     4  aload_1 [b]
            //     5  ifnull 17
            //     8  aload_0 [a]
            //     9  aload_1 [b]
            //    10  if_acmpne 15
            //    13  iconst_1
            //    14  ireturn
            //    15  iconst_0
            //    16  ireturn
            //    17  iconst_0
            //    18  ireturn
            import domain._
            allReturnedValues should be(Map(
                (14 -> AnIntegerValue),
                (16 -> AnIntegerValue),
                (18 -> AnIntegerValue)
            ))
        }
    }
}
private object MethodsWithBranchesTest {
    val classFiles = ClassFiles(locateTestResources("ai.jar", "bi"))
    val classFile = classFiles.map(_._1).find(_.thisType.fqn == "ai/MethodsWithBranches").get
}
