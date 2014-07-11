/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package ai
package domain
package l1

import org.junit.runner.RunWith

import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.scalatest.ParallelTestExecution
import org.scalatest.junit.JUnitRunner

import br._

/**
 * Tests the ReflectiveInvoker trait.
 *
 * @author Frederik Buss-Joraschek
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class ReflectiveInvokerTest
        extends FlatSpec
        with Matchers //with ParallelTestExecution 
        {

    behavior of "the RefleciveInvoker trait"

    private[this] val IrrelevantPC = Int.MinValue

    class ReflectiveInvokerTestDomain
            extends Domain
            with DefaultDomainValueBinding
            with ThrowAllPotentialExceptionsConfiguration
            with l0.DefaultTypeLevelLongValues
            with l0.DefaultTypeLevelFloatValues
            with l0.DefaultTypeLevelDoubleValues
            with l0.DefaultPrimitiveTypeConversions
            with l0.TypeLevelFieldAccessInstructions
            with l0.SimpleTypeLevelInvokeInstructions
            //    with DefaultReferenceValuesBinding
            //    with DefaultStringValuesBinding
            with l1.DefaultClassValuesBinding
            with l1.DefaultArrayValuesBinding
            with li.DefaultPreciseIntegerValues
            with PredefinedClassHierarchy
            with DefaultHandlingOfMethodResults
            with RecordLastReturnedValues
            with RecordAllThrownExceptions
            with RecordVoidReturns
            with IgnoreSynchronization
            with ReflectiveInvoker {

        type Id = String

        def id = "ReflectiveInvokerTestDomain"

        override protected def maxUpdatesForIntegerValues = 25

        override def warnOnFailedReflectiveCalls: Boolean = false

        var lastObject: Object = _

        def lastValue(): Object = lastObject

        override def toJavaObject(value: DomainValue): Option[Object] = {
            value match {
                case i: IntegerValue ⇒
                    Some(new java.lang.Integer(i.value))
                case r: ReferenceValue if (r.upperTypeBound.contains(ObjectType("java/lang/StringBuilder"))) ⇒
                    Some(new java.lang.StringBuilder())
                case _ ⇒
                    super.toJavaObject(value)
            }
        }

        override def toDomainValue(pc: PC, value: Object): DomainValue = {
            lastObject = value
            super.toDomainValue(pc, value)
        }
    }

    def createDomain() = new ReflectiveInvokerTestDomain

    it should ("be able to call a static method") in {
        val domain = createDomain()
        import domain._

        val stringValue = StringValue(IrrelevantPC, "A")
        val declaringClass = ObjectType.String
        val descriptor = MethodDescriptor(IndexedSeq(ObjectType.Object), ObjectType.String)
        val operands = List(stringValue)

        //static String String.valueOf(Object)
        val result = domain.invokeReflective(IrrelevantPC, declaringClass, "valueOf", descriptor, operands)
        val javaResult = domain.lastObject.asInstanceOf[java.lang.String]
        javaResult should be("A")
    }

    it should ("be able to call a static method with a primitve parameter") in {
        val domain = createDomain()
        import domain._

        val integerValue = IntegerValue(IrrelevantPC, 1)
        val declaringClass = ObjectType.String
        val descriptor = MethodDescriptor(IndexedSeq(IntegerType), ObjectType.String)
        val operands = List(integerValue)

        //static String String.valueOf(int)
        val result = domain.invokeReflective(IrrelevantPC, declaringClass, "valueOf", descriptor, operands)
        val javaResult = domain.lastObject.asInstanceOf[java.lang.String]
        javaResult should be("1")
    }

    it should ("be able to call a virtual method without parameters") in {
        val domain = createDomain()
        import domain._

        val stringValue = StringValue(IrrelevantPC, "Test")
        val declaringClass = ObjectType.String
        val descriptor = MethodDescriptor(IndexedSeq(), IntegerType)
        val operands = List(stringValue)

        //int String.length()
        val result = domain.invokeReflective(IrrelevantPC, declaringClass, "length", descriptor, operands)
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
        val descriptor = MethodDescriptor(IndexedSeq(IntegerType, IntegerType), ObjectType.String)
        val operands = List(
            /*p2=*/ IntegerValue(IrrelevantPC, 3),
            /*p1=*/ IntegerValue(IrrelevantPC, 1),
            receiver)

        //String <String>.substring(int int)
        val result = domain.invokeReflective(IrrelevantPC, declaringClass, "substring", descriptor, operands)
        val javaResult = domain.lastObject.asInstanceOf[java.lang.String]
        javaResult should equal("es")
    }

    it should ("be able to handle methods that return void") in {
        val domain = createDomain()
        import domain._

        val declaringClass = ObjectType("java/lang/StringBuilder")
        val descriptor = MethodDescriptor(IndexedSeq(IntegerType), VoidType)
        val operands = List(IntegerValue(IrrelevantPC, 1), TypedValue(IrrelevantPC, declaringClass))

        //void StringBuilder.ensureCapacity(int)
        val result = domain.invokeReflective(IrrelevantPC, declaringClass, "ensureCapacity", descriptor, operands)
        result should be(Some(ComputationWithSideEffectOnly))
    }

    it should ("return None when the receiver can't be transformed to a Java object ") in {
        val domain = createDomain()
        import domain._

        val instanceValue = TypedValue(IrrelevantPC, ObjectType.String)
        val declaringClass = ObjectType.String
        val descriptor = MethodDescriptor(IndexedSeq(), IntegerType)
        val operands = List(instanceValue)

        //int String.length()
        val result = domain.invokeReflective(IrrelevantPC, declaringClass, "length", descriptor, operands)
        result should be(None)
    }

    it should ("return None when a parameter can't be transformed to a Java object ") in {
        val domain = createDomain()
        import domain._

        val declaringClass = ObjectType.String
        val descriptor = MethodDescriptor(IndexedSeq(IntegerType), ObjectType.String)
        val operands = List(TypedValue(1, ObjectType.Object))

        //String String.valueOf(int)
        val result = domain.invokeReflective(IrrelevantPC, declaringClass, "valueOf", descriptor, operands)
        result should be(None)
    }

    it should ("return None when the class is not in the classpath") in {
        val domain = createDomain()
        import domain._

        val declaringClass = ObjectType("ANonExistingClass")
        val descriptor = MethodDescriptor(IndexedSeq(IntegerType), ObjectType.String)
        val operands = List(StringValue(IrrelevantPC, "A"))

        val result = domain.invokeReflective(IrrelevantPC, declaringClass, "someMethod", descriptor, operands)
        result should be(None)
    }

    it should ("return None when the method is not declared") in {
        val domain = createDomain()
        import domain._

        val receiver = StringValue(IrrelevantPC, "A")
        val declaringClass = ObjectType.String
        val descriptor = MethodDescriptor(IndexedSeq(ObjectType.Object), ObjectType.String)
        val operands = List(receiver)
        val result = domain.invokeReflective(IrrelevantPC, declaringClass, "someMethod", descriptor, operands)
        result should be(None)
    }

    it should ("return Some(NullPointerException) when the receiver is null and we want to invoke an instance method") in {
        val domain = createDomain()
        import domain._

        val receiver = NullValue(IrrelevantPC)
        val declaringClass = ObjectType.String
        val descriptor = MethodDescriptor(IndexedSeq.empty, IntegerType)
        val operands = List(receiver)
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
        val descriptor = MethodDescriptor(IndexedSeq(IntegerType, IntegerType), ObjectType.String)
        val operands = List(
            /*p2=*/ IntegerValue(IrrelevantPC, 1),
            /*p1=*/ IntegerValue(IrrelevantPC, 3),
            receiver)

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
        import domain._

        val result = domain.toDomainValue(1, new Integer(42))
        result.computationalType should be(ComputationalTypeReference)
    }

}
