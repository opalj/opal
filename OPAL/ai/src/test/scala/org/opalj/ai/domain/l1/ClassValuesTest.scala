/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import org.junit.runner.RunWith
import org.scalatest.matchers.should.Matchers
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatestplus.junit.JUnitRunner
import org.opalj.bi.TestResources.locateTestResources
import org.opalj.br._
import org.opalj.br.reader.Java8Framework.ClassFiles

/**
 * Simple test case for ClassValues.
 *
 * @author Arne Lottmann
 */
@RunWith(classOf[JUnitRunner])
class ClassValuesTest extends AnyFlatSpec with Matchers {

    import PlainClassesTest._

    behavior of "ClassValues"

    it should ("be able to create the right representation for Arrays of primitive values") in {
        val domain = new RecordingDomain
        domain.simpleClassForNameCall(-1, "[B") should be(
            ComputedValue(domain.ClassValue(-1, ArrayType(ByteType)))
        )
        domain.simpleClassForNameCall(-1, "[[J") should be(
            ComputedValue(domain.ClassValue(-1, ArrayType(ArrayType(LongType))))
        )
    }

    it should ("be able to create the right representation for Arrays of object values") in {
        val domain = new RecordingDomain
        domain.simpleClassForNameCall(-1, "[Ljava/lang/Object;") should be(
            ComputedValue(domain.ClassValue(-1, ArrayType(ObjectType.Object)))
        )
    }

    it should ("be able to trace static class values") in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(m => m.name == "staticClassValue").get
        BaseAI(method, domain)
        domain.returnedValue should be(Some(domain.ClassValue(0, ObjectType("java/lang/String"))))
    }

    it should ("be able to handle the case that we are not able to resolve the class") in {
        val method = classFile.methods.find(m => m.name == "noLiteralStringInClassForName").get
        val domain = new RecordingDomain
        BaseAI(method, domain)
        domain.returnedValue should be(Some(domain.ObjectValue(9, Unknown, false, ObjectType.Class)))
    }

    it should ("be able to trace literal strings in Class.forName(String) calls") in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(m => m.name == "literalStringInClassForName").get
        BaseAI(method, domain)
        domain.returnedValue should be(Some(domain.ClassValue(2, ObjectType("java/lang/Integer"))))
    }

    it should ("be able to trace literal strings in Class.forName(String,boolean,ClassLoader) calls") in {
        val method = classFile.methods.find(m => m.name == "literalStringInLongClassForName").get
        val domain = new RecordingDomain
        BaseAI(method, domain)
        val classType = domain.returnedValue
        classType should be(Some(domain.ClassValue(10, ObjectType("java/lang/Integer"))))
    }

    it should ("be able to trace known string variables in Class.forName calls") in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(m => m.name == "stringVariableInClassForName").get
        BaseAI(method, domain)
        val classType = domain.returnedValue
        classType should be(Some(domain.ClassValue(4, ObjectType("java/lang/Integer"))))
    }

    it should ("be able to correctly join multiple class values") in {
        val domain = new RecordingDomain
        val c1 = domain.ClassValue(1, ObjectType.Serializable)
        val c2 = domain.ClassValue(1, ObjectType.Cloneable)
        c1.join(-1, c2) should be(StructuralUpdate(domain.InitializedObjectValue(1, ObjectType.Class)))
        c1.join(-1, c2) should be(c2.join(-1, c1))
    }

    it should ("be able to trace static class values of primitves") in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(m => m.name == "staticPrimitveClassValue").get
        BaseAI(method, domain)
        domain.returnedValue.map(_.asInstanceOf[domain.DomainClassValue].value) should be(Some(IntegerType))
    }
}

object PlainClassesTest {

    class RecordingDomain
        extends CorrelationalDomain
        with DefaultSpecialDomainValuesBinding
        with DefaultHandlingForReturnInstructions
        with DefaultHandlingOfVoidReturns
        with ThrowAllPotentialExceptionsConfiguration
        with PredefinedClassHierarchy
        with DefaultHandlingOfMethodResults
        with IgnoreSynchronization
        with l0.DefaultTypeLevelIntegerValues
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l0.DefaultTypeLevelLongValues
        with l0.TypeLevelPrimitiveValuesConversions
        with l0.TypeLevelLongValuesShiftOperators
        with l0.TypeLevelFieldAccessInstructions
        with l0.SimpleTypeLevelInvokeInstructions
        with l0.TypeLevelDynamicLoads
        with l1.DefaultClassValuesBinding {

        var returnedValue: Option[DomainValue] = _
        override def areturn(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue] = {
            returnedValue = Some(value)
            super.areturn(pc, value)
        }
        override def dreturn(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue] = {
            returnedValue = Some(value)
            super.dreturn(pc, value)
        }
        override def freturn(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue] = {
            returnedValue = Some(value)
            super.freturn(pc, value)
        }
        override def ireturn(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue] = {
            returnedValue = Some(value)
            super.ireturn(pc, value)
        }
        override def lreturn(pc: Int, value: DomainValue): Computation[Nothing, ExceptionValue] = {
            returnedValue = Some(value)
            super.lreturn(pc, value)
        }
        override def returnVoid(pc: Int): Computation[Nothing, ExceptionValue] = {
            returnedValue = None
            super.returnVoid(pc)
        }
    }

    val testClassFile = locateTestResources("ai.jar", "bi")
    val classFile = ClassFiles(testClassFile).map(_._1).find(_.thisType.fqn == "ai/domain/PlainClassesJava").get
}
