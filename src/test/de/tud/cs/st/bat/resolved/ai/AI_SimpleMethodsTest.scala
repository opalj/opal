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
  */
@RunWith(classOf[JUnitRunner])
class AI_SimpleMethodsTest extends FlatSpec with ShouldMatchers /*with BeforeAndAfterAll */ {

   class RecordingDomain extends TypeDomain {
      var returnedValue : Option[Value] = _
      override def areturn(value : Value) { returnedValue = Some(value) }
      override def dreturn(value : Value) { returnedValue = Some(value) }
      override def freturn(value : Value) { returnedValue = Some(value) }
      override def ireturn(value : Value) { returnedValue = Some(value) }
      override def lreturn(value : Value) { returnedValue = Some(value) }
      override def returnVoid() { returnedValue = None }
   }

   val classFile = Java6Framework.ClassFiles("test/classfiles/ai.zip").find(_.thisClass.className == "ai/SimpleMethods").get

   behavior of "the basic abstract interpreter"

   it should "be able to analyze a method that does nothing" in {
      val method = classFile.methods.find(_.name == "nop").get
      val domain = new RecordingDomain
      /*val result =*/ AI(classFile, method)(domain)

      domain.returnedValue should be(None)
   }

   it should "be able to analyze a method that returns a fixed value" in {
      val method = classFile.methods.find(_.name == "one").get
      val domain = new RecordingDomain
      /*val result =*/ AI(classFile, method)(domain)

      domain.returnedValue should be(Some(TypedValue.IntegerValue))
   }

   it should "be able to analyze a method that just returns a given value" in {
      val method = classFile.methods.find(_.name == "identity").get
      val domain = new RecordingDomain
      /*val result =*/ AI(classFile, method)(domain)

      domain.returnedValue should be(Some(TypedValue.IntegerValue))
   }

   it should "be able to analyze that adds two values" in {
      val method = classFile.methods.find(_.name == "add").get
      val domain = new RecordingDomain
      /*val result =*/ AI(classFile, method)(domain)

      domain.returnedValue should be(Some(TypedValue.IntegerValue))
   }

   it should "be able to analyze a method that casts an int to a byte" in {
      val method = classFile.methods.find(_.name == "toByte").get
      val domain = new RecordingDomain
      /*val result =*/ AI(classFile, method)(domain)

      domain.returnedValue should be(Some(TypedValue.ByteValue))
   }

   it should "be able to analyze a method that casts an int to a short" in {
      val method = classFile.methods.find(_.name == "toShort").get
      val domain = new RecordingDomain
      /*val result =*/ AI(classFile, method)(domain)

      domain.returnedValue should be(Some(TypedValue.ShortValue))
   }

   it should "be able to analyze a method that multiplies a value by two" in {
      val method = classFile.methods.find(_.name == "twice").get
      val domain = new RecordingDomain
      /*val result =*/ AI(classFile, method)(domain)

      domain.returnedValue should be(Some(TypedValue.DoubleValue))
   }

   it should "be able to analyze a method that squares a given double value" in {
      val method = classFile.methods.find(_.name == "square").get
      val domain = new RecordingDomain
      /*val result =*/ AI(classFile, method)(domain)

      domain.returnedValue should be(Some(TypedValue.DoubleValue))
   }

   it should "be able to analyze a method that creates an instance of an object using reflection" in {
      val method = classFile.methods.find(_.name == "create").get
      val domain = new RecordingDomain
      val result = AI(classFile, method)(domain)

      domain.returnedValue should be(Some(TypedValue(ObjectType.Object)))
   }

   it should "be able to analyze a classical setter method" in {
      val method = classFile.methods.find(_.name == "setValue").get
      val domain = new RecordingDomain
      val result = AI(classFile, method)(domain)

      result should not be (null)
      domain.returnedValue should be(None)
   }

   it should "be able to analyze a classical getter method" in {
      val method = classFile.methods.find(_.name == "getValue").get
      val domain = new RecordingDomain
      /*val result =*/ AI(classFile, method)(domain)

      domain.returnedValue should be(Some(TypedValue.FloatValue))
   }
}
