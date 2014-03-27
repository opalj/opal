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
package de.tud.cs.st
package bat
package resolved
package ai
package domain
package l1

import de.tud.cs.st.bat.TestSupport
import reader.Java7Framework.ClassFiles

import org.scalatest.ParallelTestExecution
import org.scalatest.Matchers
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.junit.Ignore

import de.tud.cs.st.util._

/**
 * Simple test case for ClassValues.
 *
 * @author Arne Lottmann
 */
@RunWith(classOf[JUnitRunner])
class ClassValuesTest
        extends FlatSpec
        with Matchers
        with ParallelTestExecution {

    import PlainClassesTest._

    behavior of "ClassValues"

    it should ("be able to trace static class values") in {
        val domain = new RecordingDomain("Test static class values"); import domain._
        val method = classFile.methods.find(m ⇒ m.name == "staticClassValue").get
        BaseAI(classFile, method, domain)
        domain.returnedValue should be(Some(domain.ClassValue(0, ObjectType("java/lang/String"))))
    }

    it should ("be able to handle the case that we are not able to resolve the class") in {
        val method = classFile.methods.find(m ⇒ m.name == "noLiteralStringInClassForName").get
        val domain = new RecordingDomain(method.toJava); import domain._
        BaseAI(classFile, method, domain)
        domain.returnedValue should be(Some(ObjectValue(9, Unknown, false, ObjectType.Class)))
    }

    it should ("be able to trace literal strings in Class.forName(String) calls") in {
        val domain = new RecordingDomain("Test literal strings in Class.forName class"); import domain._
        val method = classFile.methods.find(m ⇒ m.name == "literalStringInClassForName").get
        BaseAI(classFile, method, domain)
        domain.returnedValue should be(Some(ClassValue(2, ObjectType("java/lang/Integer"))))
    }

    it should ("be able to trace literal strings in Class.forName(String,boolean,ClassLoader) calls") in {
        val method = classFile.methods.find(m ⇒ m.name == "literalStringInLongClassForName").get
        val domain = new RecordingDomain(method.toJava);
        BaseAI(classFile, method, domain)
        val classType = domain.returnedValue
        classType should be(Some(domain.ClassValue(10, ObjectType("java/lang/Integer"))))
    }

    it should ("be able to trace known string variables in Class.forName calls") in {
        val domain = new RecordingDomain("Test literal strings in Class.forName class");
        val method = classFile.methods.find(m ⇒ m.name == "stringVariableInClassForName").get
        BaseAI(classFile, method, domain)
        val classType = domain.returnedValue
        classType should be(Some(domain.ClassValue(4, ObjectType("java/lang/Integer"))))
    }

    it should ("be able to correctly join multiple class values") in {
        val domain = new DefaultConfigurableDomain("test");
        val c1 = domain.ClassValue(1, ObjectType.Serializable)
        val c2 = domain.ClassValue(1, ObjectType.Cloneable)
        c1.join(-1, c2) should be(StructuralUpdate(domain.InitializedObjectValue(1, ObjectType.Class)))
        c1.join(-1, c2) should be(c2.join(-1, c1))
    }

    // these following test cases require a more precise domain
    // the functionality to trace string values across method calls exists in principle,
    // but not in the domain set up for these tests.
    ignore should ("be able to trace literal strings in method parameters in Class.forName calls") in {
        val domain = new RecordingDomain("Test literal strings in Class.forName class");
        val method = classFile.methods.find(m ⇒ m.name == "literalStringAsParameterInClassForName").get
        BaseAI(classFile, method, domain)
        domain.returnedValue.map(_.asInstanceOf[ClassValue].value) should be(Some(ObjectType("java/lang/Integer")))
    }

    ignore should ("be able to trace known string variables in method parameters in Class.forName calls") in {
        val domain = new RecordingDomain("Test literal strings in Class.forName class");
        val method = classFile.methods.find(m ⇒ m.name == "stringVariableAsParameterInClassForName").get
        BaseAI(classFile, method, domain)
        domain.returnedValue.map(_.asInstanceOf[ClassValue].value) should be(Some(ObjectType("java/lang/Integer")))
    }
}

object PlainClassesTest {

    class RecordingDomain[+I](identifier: I) extends DefaultConfigurableDomain[I](identifier)
            with DefaultClassValuesBinding[I]
            with IgnoreSynchronization
            with IgnoreThrownExceptions {
        var returnedValue: Option[DomainValue] = _
        override def areturn(pc: Int, value: DomainValue) { returnedValue = Some(value) }
        override def dreturn(pc: Int, value: DomainValue) { returnedValue = Some(value) }
        override def freturn(pc: Int, value: DomainValue) { returnedValue = Some(value) }
        override def ireturn(pc: Int, value: DomainValue) { returnedValue = Some(value) }
        override def lreturn(pc: Int, value: DomainValue) { returnedValue = Some(value) }
        override def returnVoid(pc: Int) { returnedValue = None }
    }

    val testClassFileName = "classfiles/ai.jar"
    val testClassFile = TestSupport.locateTestResources(testClassFileName, "ext/ai")
    val classFile = ClassFiles(testClassFile).map(_._1).find(_.thisType.fqn == "ai/domain/PlainClassesJava").get
}