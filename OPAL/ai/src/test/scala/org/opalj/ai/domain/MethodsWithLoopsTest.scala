/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br._
import org.opalj.br.reader.Java8Framework.ClassFiles

/**
 * Basic tests of the abstract interpreter in the presence of simple control flow
 * instructions (if).
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class MethodsWithLoopsTest extends AnyFlatSpec with Matchers {

    import MethodsWithLoopsTest._

    def findMethod(name: String): Method = {
        classFile.methods.find(_.name == name).get
    }

    //    import domain.l0.BaseDomain
    //    private def evaluateMethod(name: String, f: BaseDomain => Unit) {
    //        val domain = new BaseDomain()
    //        val method = classFile.methods.find(_.name == name).get
    //        val result = BaseAI(method, domain)
    //
    //        org.opalj.ai.debug.XHTML.dumpOnFailureDuringValidation(
    //            Some(classFile),
    //            Some(method),
    //            method.body.get,
    //            result) {
    //                f(domain)
    //            }
    //    }

    behavior of "the abstract interpreter when analyzing methods with loops"

    //
    // RETURNS
    it should "be able to analyze a method that never terminates" in {

        object MostBasicDomain
            extends Domain
            with DefaultSpecialDomainValuesBinding
            with ThrowAllPotentialExceptionsConfiguration
            with l0.DefaultReferenceValuesBinding
            with l0.DefaultTypeLevelIntegerValues
            with l0.DefaultTypeLevelLongValues
            with l0.TypeLevelPrimitiveValuesConversions
            with l0.TypeLevelLongValuesShiftOperators
            with l0.DefaultTypeLevelFloatValues
            with l0.DefaultTypeLevelDoubleValues
            with l0.TypeLevelFieldAccessInstructions
            with l0.SimpleTypeLevelInvokeInstructions
            with l0.TypeLevelDynamicLoads
            with PredefinedClassHierarchy
            with DefaultHandlingOfMethodResults
            with IgnoreSynchronization

        val method = findMethod("endless")
        /*val result =*/ BaseAI(method, MostBasicDomain)
        // if we reach this point, everything is OK
    }

}
object MethodsWithLoopsTest {

    val classFiles = ClassFiles(locateTestResources("ai.jar", "bi"))

    val classFile = classFiles.map(_._1).find(_.thisType.fqn == "ai/MethodsWithLoops").get
}
