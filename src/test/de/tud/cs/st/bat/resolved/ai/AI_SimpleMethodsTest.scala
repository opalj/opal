/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
*
*  - Redistributions of source code must retain the above copyright notice,
*    this list of conditions and the following disclaimer.
*  - Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*  - Neither the name of the Software Technology Group or Technische
*    Universität Darmstadt nor the names of its contributors may be used to
*    endorse or promote products derived from this software without specific
*    prior written permission.
*
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st.bat
package resolved
package ai

import reader.Java6Framework
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.Spec
import org.scalatest.FlatSpec
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.ShouldMatchers

/**
 * Basic tests of the abstract interpreter.
 *
 * @author Michael Eichberg
 * @author Dennis Siebert
 */
@RunWith(classOf[JUnitRunner])
class AI_SimpleMethodsTest extends FlatSpec with ShouldMatchers /*with BeforeAndAfterAll */ {

    class RecordingDomain extends TypeDomain {
        var returnedValue: Option[Value] = _
        override def areturn(value: Value) { returnedValue = Some(value) }
        override def dreturn(value: Value) { returnedValue = Some(value) }
        override def freturn(value: Value) { returnedValue = Some(value) }
        override def ireturn(value: Value) { returnedValue = Some(value) }
        override def lreturn(value: Value) { returnedValue = Some(value) }
        override def returnVoid() { returnedValue = None }
    }

    val classFile = Java6Framework.ClassFiles("test/classfiles/ai.zip").find(_.thisClass.className == "ai/SimpleMethods").get
    assert(classFile ne null)

    behavior of "the basic abstract interpreter"

    //
    // RETURNS
    it should "be able to analyze a method that does nothing" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "nop").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(None)
    }

    it should "be able to analyze a method that returns a fixed integer value" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "iOne").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeIntegerValue))
    }

    it should "be able to analyze a method that returns a fixed long value" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "lOne").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeLongValue))
    }

    it should "be able to analyze a method that returns a fixed double value" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "dOne").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeDoubleValue))
    }

    it should "be able to analyze a method that returns a fixed float value" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "fOne").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeFloatValue))
    }

    //
    // LDC
    it should "be able to analyze a method that returns a fixed string value" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "sLDC").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(AReferenceTypeValue(ObjectType("java/lang/String"))))
    }

    //
    // PARAMETER
    it should "be able to analyze a method that just returns a parameter value" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "identity").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeIntegerValue))
    }
    it should "be able to analyze a method that just returns a parameter string" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "sOne").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(AReferenceTypeValue(ObjectType("java/lang/String"))))
    }

    //
    // BINARY OPERATIONS ON INT
    it should "be able to analyze a method that adds two int values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "iAdd").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeIntegerValue))
    }
    it should "be able to analyze a method that ands two int values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "iAnd").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeIntegerValue))
    }

    it should "be able to analyze a method  that divides two int values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "iDiv").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeIntegerValue))
    }
    it should "be able to analyze a method that multiplies two int values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "iMul").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeIntegerValue))
    }

    it should "be able to analyze a method that ors two int values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "iOr").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeIntegerValue))
    }

    it should "be able to analyze a method that shift left an int values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "iShl").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeIntegerValue))
    }

    it should "be able to analyze a method that shift right an int values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "iShr").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeIntegerValue))
    }
    it should "be able to analyze a method that reminder an int values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "iRem").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeIntegerValue))
    }
    it should "be able to analyze a method that substracts two int values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "iSub").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeIntegerValue))
    }

    it should "be able to analyze a method that logical shift right an int values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "iushr").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeIntegerValue))
    }

    it should "be able to analyze a method that XORs an int values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "iushr").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeIntegerValue))
    }

    //
    // BINARY OPERATIONS ON LONG
    it should "be able to analyze a method that adds two long values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "lAdd").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeLongValue))
    }
    it should "be able to analyze a method that ands two long values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "lAnd").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeLongValue))
    }

    it should "be able to analyze a method  that divides two long values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "lDiv").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeLongValue))
    }
    it should "be able to analyze a method that multiplies two long values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "lMul").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeLongValue))
    }

    it should "be able to analyze a method that ors two long values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "lOr").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeLongValue))
    }

    it should "be able to analyze a method that shift left an long values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "lShl").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeLongValue))
    }

    it should "be able to analyze a method that shift right an long values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "lShr").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeLongValue))
    }
    it should "be able to analyze a method that reminder an long values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "lRem").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeLongValue))
    }
    it should "be able to analyze a method that substracts two long values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "lSub").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeLongValue))
    }

    it should "be able to analyze a method that logical shift right an long values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "lushr").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeLongValue))
    }

    it should "be able to analyze a method that XORs an long values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "lushr").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeLongValue))
    }

    //
    // BINARY OPERATIONS ON DOUBLE
    it should "be able to analyze a method that adds two double values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "dAdd").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeDoubleValue))
    }

    it should "be able to analyze a method  that divides two double values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "dDiv").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeDoubleValue))
    }
    it should "be able to analyze a method that multiplies two double values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "dMul").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeDoubleValue))
    }

    it should "be able to analyze a method that reminder an double values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "dRem").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeDoubleValue))
    }
    it should "be able to analyze a method that substracts two double values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "dSub").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeDoubleValue))
    }

    //
    // BINARY OPERATIONS ON FLOAT
    it should "be able to analyze a method that adds two float values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "fAdd").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeFloatValue))
    }

    it should "be able to analyze a method  that divides two float values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "fDiv").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeFloatValue))
    }
    it should "be able to analyze a method that multiplies two float values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "fMul").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeFloatValue))
    }

    it should "be able to analyze a method that reminder an float values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "fRem").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeFloatValue))
    }
    it should "be able to analyze a method that substracts two float values" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "fSub").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeFloatValue))
    }

    //
    // INTEGER TYPE CONVERSION
    it should "be able to analyze a method that casts an int to a byte" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "iToByte").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeByteValue))
    }
    it should "be able to analyze a method that casts an int to a char" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "iToChar").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeCharValue))
    }
    it should "be able to analyze a method that casts an int to a double" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "iToDouble").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeDoubleValue))
    }
    it should "be able to analyze a method that casts an int to a float" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "iToFloat").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeFloatValue))
    }
    it should "be able to analyze a method that casts an int to a long" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "iToLong").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeLongValue))
    }

    it should "be able to analyze a method that casts an int to a short" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "iToShort").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeShortValue))
    }

    //
    // LONG TYPE CONVERSION
    it should "be able to analyze a method that casts an long to a double" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "lToDouble").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeDoubleValue))
    }
    it should "be able to analyze a method that casts an long to a float" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "lToFloat").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeFloatValue))
    }
    it should "be able to analyze a method that casts an long to a int" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "lToInt").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeIntegerValue))
    }

    //
    // DOUBLE TYPE CONVERSION

    it should "be able to analyze a method that casts an double to a float" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "dToFloat").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeFloatValue))
    }
    it should "be able to analyze a method that casts an double to a int" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "dToInt").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeIntegerValue))
    }
    it should "be able to analyze a method that casts an double to a long" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "dToLong").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeLongValue))
    }
    //
    // FLOAT TYPE CONVERSION
    it should "be able to analyze a method that casts an float to a double" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "fToDouble").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeDoubleValue))
    }
    it should "be able to analyze a method that casts an float to a int" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "fToInt").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeIntegerValue))
    }
    it should "be able to analyze a method that casts an float to a long" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "fToLong").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeLongValue))
    }

    //
    // UNARY EXPRESSIONS
    it should "be able to analyze a method that returns a negativ float value" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "fNeg").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeFloatValue))
    }

    it should "be able to analyze a method that returns a negativ double value" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "dNeg").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeDoubleValue))
    }

    it should "be able to analyze a method that returns a negativ long value" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "lNeg").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeLongValue))
    }

    it should "be able to analyze a method that returns a negativ int value" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "iNeg").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeIntegerValue))
    }

    //
    // TYPE CHECKS
    it should "be able to correctly handle casts" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "asSimpleMethods").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(TypedValue(ObjectType("ai/SimpleMethods"))))
    }

    it should "be able to correctly handle an instance of" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "asSimpleMethodsInstance").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeBooleanValue))
    }

    //
    // OTHER
    it should "be able to analyze a method that multiplies a value by two" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "twice").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeDoubleValue))
    }

    it should "be able to handle simple methods correctly" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "objectToString").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(TypedValue.AString))
    }

    it should "be able to analyze a method that squares a given double value" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "square").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeDoubleValue))
    }

    it should "be able to analyze a classical setter method" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "setValue").get
        val result = AI(classFile, method)(domain)

        result should not be (null)
        domain.returnedValue should be(None)
    }

    it should "be able to analyze a classical getter method" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "getValue").get
        /*val result =*/ AI(classFile, method)(domain)

        domain.returnedValue should be(Some(SomeFloatValue))
    }

    it should "be able to return the correct type of an object if an object that is passed in is directly returned" in {
        implicit val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "asIs").get
        val t = ObjectType("some/Foo")
        val locals = new Array[Value](1)
        locals(0) = TypedValue(t)
        AI(method.body.get.instructions, locals)

        domain.returnedValue should be(Some(TypedValue(t)))
    }

    it should "be able to analyze a method that creates an instance of an object using reflection" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "create").get
        val result = AI(classFile, method)(domain)

        domain.returnedValue should be(Some(TypedValue(ObjectType.Object)))
    }

    it should "be able to analyze a method that creates an object and which calls multiple methods of the new object" in {
        val domain = new RecordingDomain
        val method = classFile.methods.find(_.name == "multipleCalls").get
        val result = AI(classFile, method)(domain)

        domain.returnedValue should be(None)
    }
}
