/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain
package l1

import org.junit.runner.RunWith
import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import br._
import org.opalj.collection.immutable.Chain
import org.opalj.collection.immutable.Naught
import org.opalj.collection.immutable.RefArray

/**
 * Tests the ReflectiveInvoker trait.
 *
 * @author Frederik Buss-Joraschek
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ReflectiveInvokerTest extends FlatSpec with Matchers {

    private[this] val IrrelevantPC = Int.MinValue

    class ReflectiveInvokerTestDomain
        extends CorrelationalDomain
        with GlobalLogContextProvider
        with DefaultSpecialDomainValuesBinding
        with ThrowAllPotentialExceptionsConfiguration
        with l0.DefaultTypeLevelLongValues
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l0.TypeLevelPrimitiveValuesConversions
        with l0.TypeLevelLongValuesShiftOperators
        with l0.TypeLevelFieldAccessInstructions
        with l0.SimpleTypeLevelInvokeInstructions
        //    with DefaultStringValuesBinding
        with l1.DefaultClassValuesBinding
        with l1.DefaultArrayValuesBinding
        with l1.DefaultIntegerRangeValues
        with PredefinedClassHierarchy
        with DefaultHandlingOfMethodResults
        with RecordLastReturnedValues
        with RecordAllThrownExceptions
        with RecordVoidReturns
        with IgnoreSynchronization
        with ReflectiveInvoker {

        override def warnOnFailedReflectiveCalls: Boolean = false

        var lastObject: Object = _

        def lastValue(): Object = lastObject

        override def toJavaObject(pc: PC, value: DomainValue): Option[Object] = {
            value match {
                case i: IntegerRange if i.lowerBound == i.upperBound ⇒
                    Some(Integer.valueOf(i.lowerBound))
                case r: ReferenceValue if r.upperTypeBound.includes(ObjectType.StringBuilder) ⇒
                    Some(new java.lang.StringBuilder())
                case _ ⇒
                    super.toJavaObject(pc, value)
            }
        }

        override def toDomainValue(pc: PC, value: Object): DomainReferenceValue = {
            lastObject = value
            super.toDomainValue(pc, value)
        }
    }

    def createDomain() = new ReflectiveInvokerTestDomain

    behavior of "the RefleciveInvoker trait"

    it should "be able to call a static method" in {
        val domain = createDomain()
        import domain._

        val stringValue = StringValue(IrrelevantPC, "A")
        val declaringClass = ObjectType.String
        val descriptor = MethodDescriptor(FieldTypes(ObjectType.Object), ObjectType.String)
        val operands = Chain(stringValue)

        //static String String.valueOf(Object)
        /*val result =*/ domain.invokeReflective(IrrelevantPC, declaringClass, "valueOf", descriptor, operands)
        val javaResult = domain.lastObject.asInstanceOf[java.lang.String]
        javaResult should be("A")
    }

    it should "be able to call a static method with a primitve parameter" in {
        val domain = createDomain()
        import domain._

        val integerValue = IntegerValue(IrrelevantPC, 1)
        val declaringClass = ObjectType.String
        val descriptor = MethodDescriptor(FieldTypes(IntegerType), ObjectType.String)
        val operands = Chain(integerValue)

        //static String String.valueOf(int)
        /*val result =*/ domain.invokeReflective(IrrelevantPC, declaringClass, "valueOf", descriptor, operands)
        val javaResult = domain.lastObject.asInstanceOf[java.lang.String]
        javaResult should be("1")
    }

    it should "be able to call a virtual method without parameters" in {
        val domain = createDomain()
        import domain._

        val stringValue = StringValue(IrrelevantPC, "Test")
        val declaringClass = ObjectType.String
        val descriptor = MethodDescriptor(FieldTypes.Empty, IntegerType)
        val operands = Chain(stringValue)

        //int String.length()
        /*val result =*/ domain.invokeReflective(IrrelevantPC, declaringClass, "length", descriptor, operands)
        domain.lastObject /* IT IS A PRIMITIVE VALUE*/ should equal(null)
    }

    //    it should ("be able to call a method that returns a primitive value") in {
    //        val domain = createDomain()
    //        import domain._
    //
    //        val stringValue = StringValue(IrrelevantPC, "Test")
    //        val declaringClass = ObjectType.String
    //        val descriptor = MethodDescriptor(IndexedSeq(), IntegerType)
    //        val operands = List(stringValue)
    //
    //        //int String.length()
    //        val result = domain.invokeReflective(IrrelevantPC, declaringClass, "length", descriptor, operands)
    //        result should be(Some(ComputedValue(Some(IntegerRange(4, 4)))))
    //    }

    it should ("be able to call a virtual method with multiple parameters") in {
        val domain = createDomain()
        import domain._

        val receiver = StringValue(IrrelevantPC, "Test")
        val declaringClass = ObjectType.String
        val descriptor = MethodDescriptor(FieldTypes(IntegerType, IntegerType), ObjectType.String)
        val operands =
            /*p2=*/ IntegerValue(IrrelevantPC, 3) :&:
                /*p1=*/ IntegerValue(IrrelevantPC, 1) :&:
                receiver :&:
                Naught

        //String <String>.substring(int int)
        /*val result =*/ domain.invokeReflective(IrrelevantPC, declaringClass, "substring", descriptor, operands)
        val javaResult = domain.lastObject.asInstanceOf[java.lang.String]
        javaResult should equal("es")
    }

    it should ("be able to handle methods that return void") in {
        val domain = createDomain()
        import domain._

        val declaringClass = ObjectType.StringBuilder
        val descriptor = MethodDescriptor(FieldTypes(IntegerType), VoidType)
        val operands =
            IntegerValue(IrrelevantPC, 1) :&:
                TypedValue(IrrelevantPC, declaringClass) :&:
                Naught

        //void StringBuilder.ensureCapacity(int)
        val result = domain.invokeReflective(IrrelevantPC, declaringClass, "ensureCapacity", descriptor, operands)
        result should be(Some(ComputationWithSideEffectOnly))
    }

    it should ("return None when the receiver can't be transformed to a Java object ") in {
        val domain = createDomain()
        import domain._

        val instanceValue = TypedValue(IrrelevantPC, ObjectType.String)
        val declaringClass = ObjectType.String
        val descriptor = MethodDescriptor(FieldTypes.Empty, IntegerType)
        val operands = Chain(instanceValue)

        //int String.length()
        val result = domain.invokeReflective(IrrelevantPC, declaringClass, "length", descriptor, operands)
        result should be(None)
    }

    it should ("return None when a parameter can't be transformed to a Java object ") in {
        val domain = createDomain()
        import domain._

        val declaringClass = ObjectType.String
        val descriptor = MethodDescriptor(FieldTypes(IntegerType), ObjectType.String)
        val operands = Chain(TypedValue(1, ObjectType.Object))

        //String String.valueOf(int)
        val result = domain.invokeReflective(IrrelevantPC, declaringClass, "valueOf", descriptor, operands)
        result should be(None)
    }

    it should ("return None when the class is not in the classpath") in {
        val domain = createDomain()
        import domain._

        val declaringClass = ObjectType("ANonExistingClass")
        val descriptor = MethodDescriptor(FieldTypes(IntegerType), ObjectType.String)
        val operands = Chain(StringValue(IrrelevantPC, "A"))

        val result = domain.invokeReflective(IrrelevantPC, declaringClass, "someMethod", descriptor, operands)
        result should be(None)
    }

    it should ("return None when the method is not declared") in {
        val domain = createDomain()
        import domain._

        val receiver = StringValue(IrrelevantPC, "A")
        val declaringClass = ObjectType.String
        val descriptor = MethodDescriptor(FieldTypes(ObjectType.Object), ObjectType.String)
        val operands = Chain(receiver)
        val result = domain.invokeReflective(IrrelevantPC, declaringClass, "someMethod", descriptor, operands)
        result should be(None)
    }

    it should ("return Some(NullPointerException) when the receiver is null and we want to invoke an instance method") in {
        val domain = createDomain()
        import domain._

        val receiver = NullValue(IrrelevantPC)
        val declaringClass = ObjectType.String
        val descriptor = MethodDescriptor(NoFieldTypes, IntegerType)
        val operands = Chain(receiver)
        val result = domain.invokeReflective(IrrelevantPC, declaringClass, "length", descriptor, operands)
        result should be(Some(ThrowsException(Seq(
            InitializedObjectValue(IrrelevantPC, ObjectType.NullPointerException)
        ))))
    }

    it should ("return the exception that is thrown by the invoked method") in {
        val domain = createDomain()
        import domain._

        val receiver = StringValue(IrrelevantPC, "Test")
        val declaringClass = ObjectType.String
        val descriptor = MethodDescriptor(RefArray(IntegerType, IntegerType), ObjectType.String)
        val operands =
            /*p2=*/ IntegerValue(IrrelevantPC, 1) :&:
                /*p1=*/ IntegerValue(IrrelevantPC, 3) :&:
                receiver :&:
                Naught

        //String <String>.substring(int /*lower*/, int/*upper*/)
        val result = domain.invokeReflective(IrrelevantPC, declaringClass, "substring", descriptor, operands)
        result should be(Some(ThrowsException(List(
            InitializedObjectValue(IrrelevantPC, ObjectType("java/lang/StringIndexOutOfBoundsException"))
        ))))
    }

    // TODO [Refactoring] Move to extra class.
    behavior of "the JavaObjectConversions trait"

    it should ("convert to the correct target type") in {
        val domain = createDomain()
        val result = domain.toDomainValue(1, Integer.valueOf(42))
        result.computationalType should be(ComputationalTypeReference)
    }

}
