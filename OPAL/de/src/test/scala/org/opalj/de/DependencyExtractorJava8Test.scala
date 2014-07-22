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
package de

import java.net.URL

import org.scalatest.FunSuite

import br.reader.Java8Framework.ClassFiles
import DependencyType._

/**
 * Tests that the dependency extractor does not miss some dependencies and
 * that it does not extract "unexpected" dependencies.
 *
 * @author Thomas Schlosser
 * @author Michael Eichberg
 * @author Marco Jacobasch
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class DependencyExtractorJava8Test extends FunSuite {

    import DependencyType._

    //
    // THE TEST CODE
    //

    test("Dependency extraction") {

        var dependencies: Map[(String, String, DependencyType), Int] =
            DependencyExtractorFixture.extractDependencies(
                "de",
                "classfiles/Dependencies-1.8.jar",
                (dp: DependencyProcessor) ⇒ new DependencyExtractor(dp))

        def assertDependency(src: String, trgt: String, dType: DependencyType): Unit = {
            val key = (src, trgt, dType)

            dependencies.get(key) match {
                case Some(0) ⇒
                    fail("The dependency "+key+" was not extracted the expected number of times.")
                case Some(x) ⇒
                    dependencies = dependencies.updated(key, x - 1)
                case None ⇒
                    val remainigDependencies =
                        dependencies.toList.sorted.
                            mkString("Remaining dependencies:\n\t", "\n\t", "\n")
                    fail("The dependency "+key+" was not extracted.\n"+remainigDependencies)
            }
        }

        def assertImplicitDefaultConstructor(className: String, superClassName: String = "java.lang.Object") {
            // //implicit constructor:
            val constructorName = className+".<init>()"
            assertDependency(constructorName, className, INSTANCE_MEMBER)
            assertDependency(constructorName, superClassName, DECLARING_CLASS_OF_CALLED_METHOD)
            assertDependency(constructorName, superClassName+".<init>()", CALLS_METHOD)
            assertImplicitThisLocalVariable(constructorName)
        }

        def assertImplicitThisLocalVariable(methodName: String) {
            // //implicit local variable 'this'
            assertDependency(
                methodName,
                methodName.substring(0, methodName.substring(0, methodName.lastIndexOf('(')).lastIndexOf('.')),
                LOCAL_VARIABLE_TYPE)
        }

        assert(dependencies.size > 0, "no dependencies extracted")

        //    package dependencies;
        //
        //    import java.util.ArrayList;
        //    import java.util.List;
        //
        //    public class TestClass implements TestInterface {
        assertDependency("dependencies.TestClass", "dependencies.TestInterface", IMPLEMENTS)
        assertDependency("dependencies.TestClass", "java.lang.Object", EXTENDS)
        assertImplicitDefaultConstructor("dependencies.TestClass")
        //        public void testMethod() {
        assertDependency("dependencies.TestClass.testMethod()", "dependencies.TestClass", INSTANCE_MEMBER)
        assertImplicitThisLocalVariable("dependencies.TestClass.testMethod()")
        // NOTE: It is not possible to determine dependencies to types that are erased; e.g., the type "String" is erased at compile time and it is not possible to extract this dependency.
        // List<? extends CharSequence> list = new ArrayList<String>();
        assertDependency("dependencies.TestClass.testMethod()", "java.util.List", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.TestClass.testMethod()", "java.lang.CharSequence", TYPE_IN_TYPE_PARAMETERS)
        assertDependency("dependencies.TestClass.testMethod()", "java.util.ArrayList", CREATES)
        assertDependency("dependencies.TestClass.testMethod()", "java.util.ArrayList", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.TestClass.testMethod()", "java.util.ArrayList.<init>()", CALLS_METHOD)
        //    	list.add(null);
        assertDependency("dependencies.TestClass.testMethod()", "java.util.List", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.TestClass.testMethod()", "java.util.List.add(java.lang.Object)", CALLS_METHOD)
        assertDependency("dependencies.TestClass.testMethod()", "java.lang.Object", PARAMETER_TYPE_OF_CALLED_METHOD)
        //        }
        //
        //        public String testMethod(Integer i, int j) {
        assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "dependencies.TestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer", PARAMETER_TYPE)
        assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.String", RETURN_TYPE)
        assertImplicitThisLocalVariable("dependencies.TestClass.testMethod(java.lang.Integer, int)")
        //    	if (i != null && i.intValue() > j) {
        assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer.intValue()", CALLS_METHOD)
        //    	    return i.toString();
        assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer.toString()", CALLS_METHOD)
        assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.String", RETURN_TYPE_OF_CALLED_METHOD)
        //    	}
        //    	return String.valueOf(j);
        assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.String", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.String.valueOf(int)", CALLS_METHOD)
        assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.String", RETURN_TYPE_OF_CALLED_METHOD)
        //        }
        //    }

        //    package dependencies;
        //
        //    public interface TestInterface {
        assertDependency("dependencies.TestInterface", "java.lang.Object", EXTENDS)
        //        void testMethod();
        assertDependency("dependencies.TestInterface.testMethod()", "dependencies.TestInterface", INSTANCE_MEMBER)
        //
        //        String testMethod(Integer i, int j);
        assertDependency("dependencies.TestInterface.testMethod(java.lang.Integer, int)", "dependencies.TestInterface", INSTANCE_MEMBER)
        assertDependency("dependencies.TestInterface.testMethod(java.lang.Integer, int)", "java.lang.Integer", PARAMETER_TYPE)
        assertDependency("dependencies.TestInterface.testMethod(java.lang.Integer, int)", "java.lang.String", RETURN_TYPE)
        //    }

        //    package dependencies.sub;
        //
        //    public interface MarkerInterface {
        assertDependency("dependencies.sub.MarkerInterface", "java.lang.Object", EXTENDS)
        //
        //    }

        //    package dependencies.sub;
        //
        //    import dependencies.TestInterface;
        //
        //    @Deprecated
        //    public interface DeprecatedInterface extends TestInterface, MarkerInterface {
        assertDependency("dependencies.sub.DeprecatedInterface", "java.lang.Object", EXTENDS)
        assertDependency("dependencies.sub.DeprecatedInterface", "dependencies.TestInterface", IMPLEMENTS)
        assertDependency("dependencies.sub.DeprecatedInterface", "dependencies.sub.MarkerInterface", IMPLEMENTS)
        assertDependency("dependencies.sub.DeprecatedInterface", "java.lang.Deprecated", ANNOTATED_WITH)
        //
        //        @Deprecated
        //        public void deprecatedMethod();
        assertDependency("dependencies.sub.DeprecatedInterface.deprecatedMethod()", "dependencies.sub.DeprecatedInterface", INSTANCE_MEMBER)
        assertDependency("dependencies.sub.DeprecatedInterface.deprecatedMethod()", "java.lang.Deprecated", ANNOTATED_WITH)
        //
        //        public void methodDeprParam(@Deprecated int i);
        assertDependency("dependencies.sub.DeprecatedInterface.methodDeprParam(int)", "dependencies.sub.DeprecatedInterface", INSTANCE_MEMBER)
        assertDependency("dependencies.sub.DeprecatedInterface.methodDeprParam(int)", "java.lang.Deprecated", PARAMETER_ANNOTATED_WITH)
        //    }

        //    package dependencies;
        //
        //    public class FieldsClass {
        assertDependency("dependencies.FieldsClass", "java.lang.Object", EXTENDS)
        assertImplicitDefaultConstructor("dependencies.FieldsClass")
        //        public final static String CONSTANT = "constant";
        assertDependency("dependencies.FieldsClass.CONSTANT", "dependencies.FieldsClass", CLASS_MEMBER)
        assertDependency("dependencies.FieldsClass.CONSTANT", "java.lang.String", FIELD_TYPE)
        assertDependency("dependencies.FieldsClass.CONSTANT", "java.lang.String", CONSTANT_VALUE)
        //        private Integer i;
        assertDependency("dependencies.FieldsClass.i", "dependencies.FieldsClass", INSTANCE_MEMBER)
        assertDependency("dependencies.FieldsClass.i", "java.lang.Integer", FIELD_TYPE)
        //
        //        @Deprecated
        //        protected int deprecatedField;
        assertDependency("dependencies.FieldsClass.deprecatedField", "dependencies.FieldsClass", INSTANCE_MEMBER)
        assertDependency("dependencies.FieldsClass.deprecatedField", "java.lang.Deprecated", ANNOTATED_WITH)
        //
        //        private Integer readField() {
        assertDependency("dependencies.FieldsClass.readField()", "dependencies.FieldsClass", INSTANCE_MEMBER)
        assertDependency("dependencies.FieldsClass.readField()", "java.lang.Integer", RETURN_TYPE)
        assertImplicitThisLocalVariable("dependencies.FieldsClass.readField()")
        //    	return i;
        assertDependency("dependencies.FieldsClass.readField()", "dependencies.FieldsClass", DECLARING_CLASS_OF_ACCESSED_FIELD)
        assertDependency("dependencies.FieldsClass.readField()", "dependencies.FieldsClass.i", READS_FIELD)
        assertDependency("dependencies.FieldsClass.readField()", "java.lang.Integer", TYPE_OF_ACCESSED_FIELD)
        //        }
        //
        //        private void writeField(Integer j) {
        assertDependency("dependencies.FieldsClass.writeField(java.lang.Integer)", "dependencies.FieldsClass", INSTANCE_MEMBER)
        assertDependency("dependencies.FieldsClass.writeField(java.lang.Integer)", "java.lang.Integer", PARAMETER_TYPE)
        assertDependency("dependencies.FieldsClass.writeField(java.lang.Integer)", "java.lang.Integer", LOCAL_VARIABLE_TYPE)
        assertImplicitThisLocalVariable("dependencies.FieldsClass.writeField(java.lang.Integer)")
        //    	i = j;
        assertDependency("dependencies.FieldsClass.writeField(java.lang.Integer)", "dependencies.FieldsClass", DECLARING_CLASS_OF_ACCESSED_FIELD)
        assertDependency("dependencies.FieldsClass.writeField(java.lang.Integer)", "dependencies.FieldsClass.i", WRITES_FIELD)
        assertDependency("dependencies.FieldsClass.writeField(java.lang.Integer)", "java.lang.Integer", TYPE_OF_ACCESSED_FIELD)
        //        }
        //
        //        public Integer readWrite(Integer j) {
        assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "dependencies.FieldsClass", INSTANCE_MEMBER)
        assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", PARAMETER_TYPE)
        assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", RETURN_TYPE)
        assertImplicitThisLocalVariable("dependencies.FieldsClass.readWrite(java.lang.Integer)")
        //    	Integer result = readField();
        assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "dependencies.FieldsClass", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "dependencies.FieldsClass.readField()", CALLS_METHOD)
        assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", RETURN_TYPE_OF_CALLED_METHOD)
        //    	writeField(j);
        assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "dependencies.FieldsClass", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "dependencies.FieldsClass.writeField(java.lang.Integer)", CALLS_METHOD)
        assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", PARAMETER_TYPE_OF_CALLED_METHOD)
        //    	return result;
        //        }
        //    }

        //    package dependencies;
        //
        //    public class OuterClass {
        assertDependency("dependencies.OuterClass", "java.lang.Object", EXTENDS)
        assertImplicitDefaultConstructor("dependencies.OuterClass")
        //        class InnerClass {
        assertDependency("dependencies.OuterClass$InnerClass", "java.lang.Object", EXTENDS)
        assertDependency("dependencies.OuterClass", "dependencies.OuterClass$InnerClass", OUTER_CLASS)
        assertDependency("dependencies.OuterClass$InnerClass", "dependencies.OuterClass", INNER_CLASS)
        //            //implicit field:
        assertDependency("dependencies.OuterClass$InnerClass.this$0", "dependencies.OuterClass$InnerClass", INSTANCE_MEMBER)
        assertDependency("dependencies.OuterClass$InnerClass.this$0", "dependencies.OuterClass", FIELD_TYPE)
        //            public InnerClass(Integer i) {
        assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "dependencies.OuterClass$InnerClass", INSTANCE_MEMBER)
        //            //implicit constructor parameter:
        assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "dependencies.OuterClass", PARAMETER_TYPE)
        assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "java.lang.Integer", PARAMETER_TYPE)
        assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "java.lang.Integer", LOCAL_VARIABLE_TYPE)
        assertImplicitThisLocalVariable("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)")

        assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "java.lang.Object", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "java.lang.Object.<init>()", CALLS_METHOD)
        //            // write to implicit field:
        assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "dependencies.OuterClass$InnerClass", DECLARING_CLASS_OF_ACCESSED_FIELD)
        assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "dependencies.OuterClass$InnerClass.this$0", WRITES_FIELD)
        assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "dependencies.OuterClass", TYPE_OF_ACCESSED_FIELD)
        //            }
        //        }
        //    }

        //    package dependencies;
        //
        //    public class EnclosingMethodClass {
        assertDependency("dependencies.EnclosingMethodClass", "java.lang.Object", EXTENDS)
        assertImplicitDefaultConstructor("dependencies.EnclosingMethodClass")
        //  //implicit field definition in the default constructor
        assertDependency("dependencies.EnclosingMethodClass.<init>()", "dependencies.EnclosingMethodClass$1", CREATES)
        assertDependency("dependencies.EnclosingMethodClass.<init>()", "dependencies.EnclosingMethodClass$1", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.EnclosingMethodClass.<init>()", "dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)", CALLS_METHOD)
        assertDependency("dependencies.EnclosingMethodClass.<init>()", "dependencies.EnclosingMethodClass", PARAMETER_TYPE_OF_CALLED_METHOD)
        assertDependency("dependencies.EnclosingMethodClass.<init>()", "dependencies.EnclosingMethodClass", DECLARING_CLASS_OF_ACCESSED_FIELD)
        assertDependency("dependencies.EnclosingMethodClass.<init>()", "dependencies.EnclosingMethodClass.enclosingField", WRITES_FIELD)
        assertDependency("dependencies.EnclosingMethodClass.<init>()", "java.lang.Object", TYPE_OF_ACCESSED_FIELD)
        //  //implicit field definition in the class initialization method
        assertDependency("dependencies.EnclosingMethodClass.<clinit>()", "dependencies.EnclosingMethodClass", CLASS_MEMBER)
        assertDependency("dependencies.EnclosingMethodClass.<clinit>()", "dependencies.EnclosingMethodClass$2", CREATES)
        assertDependency("dependencies.EnclosingMethodClass.<clinit>()", "dependencies.EnclosingMethodClass$2", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.EnclosingMethodClass.<clinit>()", "dependencies.EnclosingMethodClass$2.<init>()", CALLS_METHOD)
        assertDependency("dependencies.EnclosingMethodClass.<clinit>()", "dependencies.EnclosingMethodClass", DECLARING_CLASS_OF_ACCESSED_FIELD)
        assertDependency("dependencies.EnclosingMethodClass.<clinit>()", "dependencies.EnclosingMethodClass.staticEnclosingField", WRITES_FIELD)
        assertDependency("dependencies.EnclosingMethodClass.<clinit>()", "java.lang.Object", TYPE_OF_ACCESSED_FIELD)
        //
        //        public Object enclosingField = new Object() {
        assertDependency("dependencies.EnclosingMethodClass.enclosingField", "dependencies.EnclosingMethodClass", INSTANCE_MEMBER)
        assertDependency("dependencies.EnclosingMethodClass.enclosingField", "java.lang.Object", FIELD_TYPE)
        //        };
        assertDependency("dependencies.EnclosingMethodClass$1", "java.lang.Object", EXTENDS)
        assertDependency("dependencies.EnclosingMethodClass$1", "dependencies.EnclosingMethodClass", ENCLOSED)
        //	//implicit field:
        assertDependency("dependencies.EnclosingMethodClass$1.this$0", "dependencies.EnclosingMethodClass$1", INSTANCE_MEMBER)
        assertDependency("dependencies.EnclosingMethodClass$1.this$0", "dependencies.EnclosingMethodClass", FIELD_TYPE)
        //	//implicit constructor:
        assertDependency("dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass$1", INSTANCE_MEMBER)
        assertDependency("dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass", PARAMETER_TYPE)
        assertImplicitThisLocalVariable("dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)")
        assertDependency("dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)", "java.lang.Object", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)", "java.lang.Object.<init>()", CALLS_METHOD)
        //	// write to implicit field:
        assertDependency("dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass$1", DECLARING_CLASS_OF_ACCESSED_FIELD)
        assertDependency("dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass$1.this$0", WRITES_FIELD)
        assertDependency("dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass", TYPE_OF_ACCESSED_FIELD)
        //
        //        public static Object staticEnclosingField = new Object() {
        assertDependency("dependencies.EnclosingMethodClass.staticEnclosingField", "dependencies.EnclosingMethodClass", CLASS_MEMBER)
        assertDependency("dependencies.EnclosingMethodClass.staticEnclosingField", "java.lang.Object", FIELD_TYPE)
        //        };
        assertDependency("dependencies.EnclosingMethodClass$2", "java.lang.Object", EXTENDS)
        assertDependency("dependencies.EnclosingMethodClass$2", "dependencies.EnclosingMethodClass", ENCLOSED)
        //	//implicit constructor:
        assertDependency("dependencies.EnclosingMethodClass$2.<init>()", "dependencies.EnclosingMethodClass$2", INSTANCE_MEMBER)
        assertImplicitThisLocalVariable("dependencies.EnclosingMethodClass$2.<init>()")
        assertDependency("dependencies.EnclosingMethodClass$2.<init>()", "java.lang.Object", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.EnclosingMethodClass$2.<init>()", "java.lang.Object.<init>()", CALLS_METHOD)
        //
        //        public void enclosingMethod() {
        assertDependency("dependencies.EnclosingMethodClass.enclosingMethod()", "dependencies.EnclosingMethodClass", INSTANCE_MEMBER)
        assertImplicitThisLocalVariable("dependencies.EnclosingMethodClass.enclosingMethod()")
        //    	new Object() {
        assertDependency("dependencies.EnclosingMethodClass$3", "java.lang.Object", EXTENDS)
        assertDependency("dependencies.EnclosingMethodClass$3", "dependencies.EnclosingMethodClass.enclosingMethod()", ENCLOSED)
        //	//implicit field:
        assertDependency("dependencies.EnclosingMethodClass$3.this$0", "dependencies.EnclosingMethodClass$3", INSTANCE_MEMBER)
        assertDependency("dependencies.EnclosingMethodClass$3.this$0", "dependencies.EnclosingMethodClass", FIELD_TYPE)
        //	//implicit constructor:
        assertDependency("dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass$3", INSTANCE_MEMBER)
        assertDependency("dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass", PARAMETER_TYPE)
        assertImplicitThisLocalVariable("dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)")
        assertDependency("dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)", "java.lang.Object", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)", "java.lang.Object.<init>()", CALLS_METHOD)
        //	// write to implicit field:
        assertDependency("dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass$3", DECLARING_CLASS_OF_ACCESSED_FIELD)
        assertDependency("dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass$3.this$0", WRITES_FIELD)
        assertDependency("dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass", TYPE_OF_ACCESSED_FIELD)
        //    	    public void innerMethod() {
        assertDependency("dependencies.EnclosingMethodClass$3.innerMethod()", "dependencies.EnclosingMethodClass$3", INSTANCE_MEMBER)
        assertImplicitThisLocalVariable("dependencies.EnclosingMethodClass$3.innerMethod()")
        //    	    }
        //    	}.innerMethod();
        assertDependency("dependencies.EnclosingMethodClass.enclosingMethod()", "dependencies.EnclosingMethodClass$3", CREATES)
        assertDependency("dependencies.EnclosingMethodClass.enclosingMethod()", "dependencies.EnclosingMethodClass$3", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.EnclosingMethodClass.enclosingMethod()", "dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)", CALLS_METHOD)
        assertDependency("dependencies.EnclosingMethodClass.enclosingMethod()", "dependencies.EnclosingMethodClass", PARAMETER_TYPE_OF_CALLED_METHOD) // method parameter

        assertDependency("dependencies.EnclosingMethodClass.enclosingMethod()", "dependencies.EnclosingMethodClass$3", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.EnclosingMethodClass.enclosingMethod()", "dependencies.EnclosingMethodClass$3.innerMethod()", CALLS_METHOD)
        //        }
        //    }

        //    package dependencies;
        //
        //    import java.util.FormatterClosedException;
        //
        //    import javax.naming.OperationNotSupportedException;
        //
        //    public class ExceptionTestClass {
        assertDependency("dependencies.ExceptionTestClass", "java.lang.Object", EXTENDS)
        assertImplicitDefaultConstructor("dependencies.ExceptionTestClass")
        //
        //        public void testMethod() throws IllegalStateException,
        //    	    OperationNotSupportedException {
        assertDependency("dependencies.ExceptionTestClass.testMethod()", "dependencies.ExceptionTestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.ExceptionTestClass.testMethod()", "java.lang.IllegalStateException", THROWN_EXCEPTION)
        assertDependency("dependencies.ExceptionTestClass.testMethod()", "javax.naming.OperationNotSupportedException", THROWN_EXCEPTION)
        assertImplicitThisLocalVariable("dependencies.ExceptionTestClass.testMethod()")
        //    	throw new FormatterClosedException();
        assertDependency("dependencies.ExceptionTestClass.testMethod()", "java.util.FormatterClosedException", CREATES)
        assertDependency("dependencies.ExceptionTestClass.testMethod()", "java.util.FormatterClosedException", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.ExceptionTestClass.testMethod()", "java.util.FormatterClosedException.<init>()", CALLS_METHOD)
        //        }
        //
        //        public void catchMethod() {
        assertDependency("dependencies.ExceptionTestClass.catchMethod()", "dependencies.ExceptionTestClass", INSTANCE_MEMBER)
        assertImplicitThisLocalVariable("dependencies.ExceptionTestClass.catchMethod()")
        //    	try {
        //    	    try {
        //    		testMethod();
        assertDependency("dependencies.ExceptionTestClass.catchMethod()", "dependencies.ExceptionTestClass", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.ExceptionTestClass.catchMethod()", "dependencies.ExceptionTestClass.testMethod()", CALLS_METHOD)
        //    	    } catch (IllegalStateException e) {
        assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.IllegalStateException", CATCHES)
        ///        assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.IllegalStateException", LOCAL_VARIABLE_TYPE)
        //    	    }
        //    	} catch (Exception e) {
        assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Exception", CATCHES)
        ////        assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Exception", LOCAL_VARIABLE_TYPE)
        //    	} finally{
        //    	    Integer.valueOf(42);
        assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer.valueOf(int)", CALLS_METHOD)
        assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer", RETURN_TYPE_OF_CALLED_METHOD)
        // // The next six dependencies result from required special handling of the finally block.
        // // Depending on the way the finally block were reached it has to throw an Exception or return normally.
        // // Hence, the bytecode contains the three versions of the finally block which results in multiple
        // // dependencies to types/methods/fields used in the finally block.
        assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer.valueOf(int)", CALLS_METHOD)
        assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer", RETURN_TYPE_OF_CALLED_METHOD)
        assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer.valueOf(int)", CALLS_METHOD)
        assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer", RETURN_TYPE_OF_CALLED_METHOD)
        //            }
        //        }
        //    }

        //    package dependencies;
        //
        //    import java.lang.annotation.ElementType;
        //
        //    public @interface TestAnnotation {
        assertDependency("dependencies.TestAnnotation", "java.lang.Object", EXTENDS)
        assertDependency("dependencies.TestAnnotation", "java.lang.annotation.Annotation", IMPLEMENTS)
        //        public abstract String stringValue() default "default";
        assertDependency("dependencies.TestAnnotation.stringValue()", "dependencies.TestAnnotation", INSTANCE_MEMBER)
        assertDependency("dependencies.TestAnnotation.stringValue()", "java.lang.String", RETURN_TYPE)
        assertDependency("dependencies.TestAnnotation.stringValue()", "java.lang.String", ANNOTATION_DEFAULT_VALUE_TYPE)
        //
        //        public abstract Class<?> classValue() default String.class;
        assertDependency("dependencies.TestAnnotation.classValue()", "dependencies.TestAnnotation", INSTANCE_MEMBER)
        assertDependency("dependencies.TestAnnotation.classValue()", "java.lang.Class", RETURN_TYPE)
        assertDependency("dependencies.TestAnnotation.classValue()", "java.lang.String", ANNOTATION_DEFAULT_VALUE_TYPE)
        //
        //        public abstract ElementType enumValue() default ElementType.TYPE;
        assertDependency("dependencies.TestAnnotation.enumValue()", "dependencies.TestAnnotation", INSTANCE_MEMBER)
        assertDependency("dependencies.TestAnnotation.enumValue()", "java.lang.annotation.ElementType", RETURN_TYPE)
        assertDependency("dependencies.TestAnnotation.enumValue()", "java.lang.annotation.ElementType", ANNOTATION_DEFAULT_VALUE_TYPE)
        assertDependency("dependencies.TestAnnotation.enumValue()", "java.lang.annotation.ElementType.TYPE", USES_ENUM_VALUE)
        //
        //        public abstract SuppressWarnings annotationValue() default @SuppressWarnings("default");
        assertDependency("dependencies.TestAnnotation.annotationValue()", "dependencies.TestAnnotation", INSTANCE_MEMBER)
        assertDependency("dependencies.TestAnnotation.annotationValue()", "java.lang.SuppressWarnings", RETURN_TYPE)
        assertDependency("dependencies.TestAnnotation.annotationValue()", "java.lang.SuppressWarnings", ANNOTATION_DEFAULT_VALUE_TYPE)
        assertDependency("dependencies.TestAnnotation.annotationValue()", "java.lang.String", ANNOTATION_DEFAULT_VALUE_TYPE)
        //
        //        public abstract Class<?>[] arrayClassValue() default { String.class,
        //    	    Integer.class };
        assertDependency("dependencies.TestAnnotation.arrayClassValue()", "dependencies.TestAnnotation", INSTANCE_MEMBER)
        assertDependency("dependencies.TestAnnotation.arrayClassValue()", "java.lang.Class", RETURN_TYPE)
        assertDependency("dependencies.TestAnnotation.arrayClassValue()", "java.lang.String", ANNOTATION_DEFAULT_VALUE_TYPE)
        assertDependency("dependencies.TestAnnotation.arrayClassValue()", "java.lang.Integer", ANNOTATION_DEFAULT_VALUE_TYPE)
        //    }

        //    package dependencies;
        //
        //    import java.lang.annotation.ElementType;
        //
        //    @TestAnnotation
        //    public class AnnotationDefaultAttributeTestClass {
        assertDependency("dependencies.AnnotationDefaultAttributeTestClass", "java.lang.Object", EXTENDS)
        assertDependency("dependencies.AnnotationDefaultAttributeTestClass", "dependencies.TestAnnotation", ANNOTATED_WITH)
        assertImplicitDefaultConstructor("dependencies.AnnotationDefaultAttributeTestClass")
        //
        //        @TestAnnotation(stringValue = "noDefault", classValue = Integer.class, enumValue = ElementType.METHOD, annotationValue = @SuppressWarnings("noDefault"), arrayClassValue = {
        //    	    Long.class, Boolean.class })
        //        public void testMethod() {
        assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "dependencies.AnnotationDefaultAttributeTestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "dependencies.TestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.Integer", ANNOTATION_ELEMENT_TYPE)
        assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.annotation.ElementType", ANNOTATION_ELEMENT_TYPE)
        assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.annotation.ElementType.METHOD", USES_ENUM_VALUE)
        assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.SuppressWarnings", ANNOTATION_ELEMENT_TYPE)
        assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.Long", ANNOTATION_ELEMENT_TYPE)
        assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.Boolean", ANNOTATION_ELEMENT_TYPE)
        assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.String", ANNOTATION_ELEMENT_TYPE)
        assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.String", ANNOTATION_ELEMENT_TYPE)
        assertImplicitThisLocalVariable("dependencies.AnnotationDefaultAttributeTestClass.testMethod()")
        //        }
        //    }

        //    package dependencies;
        //
        //    import java.io.FilterInputStream;
        //    import java.io.InputStream;
        //    import java.util.zip.InflaterInputStream;
        //    import java.util.zip.ZipInputStream;
        //
        //    public class InstructionsTestClass {
        assertDependency("dependencies.InstructionsTestClass", "java.lang.Object", EXTENDS)
        assertImplicitDefaultConstructor("dependencies.InstructionsTestClass")
        //        public Object field;
        assertDependency("dependencies.InstructionsTestClass.field", "dependencies.InstructionsTestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.InstructionsTestClass.field", "java.lang.Object", FIELD_TYPE)
        //        public static InputStream staticField;
        assertDependency("dependencies.InstructionsTestClass.staticField", "dependencies.InstructionsTestClass", CLASS_MEMBER)
        assertDependency("dependencies.InstructionsTestClass.staticField", "java.io.InputStream", FIELD_TYPE)
        //
        //        public void method() {
        assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass", INSTANCE_MEMBER)
        assertImplicitThisLocalVariable("dependencies.InstructionsTestClass.method()")
        //    	// NEW and INVOKESPECIAL (constructor call)
        //    	Object obj = new Object();
        assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object", CREATES)
        assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object.<init>()", CALLS_METHOD)
        assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object", DECLARING_CLASS_OF_CALLED_METHOD)
        //    	FilterInputStream stream = null;
        assertDependency("dependencies.InstructionsTestClass.method()", "java.io.FilterInputStream", LOCAL_VARIABLE_TYPE)
        //    	// ANEWARRAY
        //    	obj = new Long[1];
        assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Long", CREATES_ARRAY)
        //    	// MULTIANEWARRAY
        //    	obj = new Integer[1][];
        assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Integer", CREATES_ARRAY)
        //
        //    	// PUTFIELD
        //    	field = obj;
        assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass", DECLARING_CLASS_OF_ACCESSED_FIELD)
        assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass.field", WRITES_FIELD)
        assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object", TYPE_OF_ACCESSED_FIELD)
        //    	// GETFIELD
        //    	obj = field;
        assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass", DECLARING_CLASS_OF_ACCESSED_FIELD)
        assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass.field", READS_FIELD)
        assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object", TYPE_OF_ACCESSED_FIELD)
        //    	// INSTANCEOF
        //    	if (obj instanceof ZipInputStream) {
        assertDependency("dependencies.InstructionsTestClass.method()", "java.util.zip.ZipInputStream", TYPECHECK)
        //    	    // CHECKCAST
        //    	    stream = (InflaterInputStream) obj;
        assertDependency("dependencies.InstructionsTestClass.method()", "java.util.zip.InflaterInputStream", TYPECAST)
        //    	    // PUTSTATIC
        //    	    staticField = stream;
        assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass", DECLARING_CLASS_OF_ACCESSED_FIELD)
        assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass.staticField", WRITES_FIELD)
        assertDependency("dependencies.InstructionsTestClass.method()", "java.io.InputStream", TYPE_OF_ACCESSED_FIELD)
        //    	    // GETSTATIC
        //    	    obj = staticField;
        assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass", DECLARING_CLASS_OF_ACCESSED_FIELD)
        assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass.staticField", READS_FIELD)
        assertDependency("dependencies.InstructionsTestClass.method()", "java.io.InputStream", TYPE_OF_ACCESSED_FIELD)
        //    	}
        //
        //    	// INVOKESTATIC
        //    	System.currentTimeMillis();
        assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.System", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.System.currentTimeMillis()", CALLS_METHOD)
        //
        //    	TestInterface ti = new TestClass();
        assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.TestInterface", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.TestClass", CREATES)
        assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.TestClass", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.TestClass.<init>()", CALLS_METHOD)
        //    	// INVOKEINTERFACE
        //    	ti.testMethod();
        assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.TestInterface", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.TestInterface.testMethod()", CALLS_METHOD)
        //
        //    	// INVOKEVIRTUAL
        //    	obj.equals(stream);
        assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object.equals(java.lang.Object)", CALLS_METHOD)
        assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object", PARAMETER_TYPE_OF_CALLED_METHOD)
        //        }
        //    }

        //    package dependencies;
        //
        //    import java.io.InputStream;
        //    import java.io.OutputStream;
        //
        //    public interface SignatureTestInterface<T extends InputStream, Z> {
        assertDependency("dependencies.SignatureTestInterface", "java.lang.Object", EXTENDS)
        assertDependency("dependencies.SignatureTestInterface", "java.io.InputStream", TYPE_IN_TYPE_PARAMETERS)
        assertDependency("dependencies.SignatureTestInterface", "java.lang.Object", TYPE_IN_TYPE_PARAMETERS)
        //
        //        public T m1();
        assertDependency("dependencies.SignatureTestInterface.m1()", "dependencies.SignatureTestInterface", INSTANCE_MEMBER)
        assertDependency("dependencies.SignatureTestInterface.m1()", "java.io.InputStream", RETURN_TYPE)
        //
        //        public void m2(T t, Z z);
        assertDependency("dependencies.SignatureTestInterface.m2(java.io.InputStream, java.lang.Object)", "dependencies.SignatureTestInterface", INSTANCE_MEMBER)
        assertDependency("dependencies.SignatureTestInterface.m2(java.io.InputStream, java.lang.Object)", "java.io.InputStream", PARAMETER_TYPE)
        assertDependency("dependencies.SignatureTestInterface.m2(java.io.InputStream, java.lang.Object)", "java.lang.Object", PARAMETER_TYPE)
        //
        //        public <W> W m3();
        assertDependency("dependencies.SignatureTestInterface.m3()", "dependencies.SignatureTestInterface", INSTANCE_MEMBER)
        assertDependency("dependencies.SignatureTestInterface.m3()", "java.lang.Object", TYPE_IN_TYPE_PARAMETERS)
        assertDependency("dependencies.SignatureTestInterface.m3()", "java.lang.Object", RETURN_TYPE)
        //
        //        public <W extends T> W m4();
        assertDependency("dependencies.SignatureTestInterface.m4()", "dependencies.SignatureTestInterface", INSTANCE_MEMBER)
        assertDependency("dependencies.SignatureTestInterface.m4()", "java.io.InputStream", RETURN_TYPE)
        //
        //        public <W extends OutputStream> W m5();
        assertDependency("dependencies.SignatureTestInterface.m5()", "dependencies.SignatureTestInterface", INSTANCE_MEMBER)
        assertDependency("dependencies.SignatureTestInterface.m5()", "java.io.OutputStream", TYPE_IN_TYPE_PARAMETERS)
        assertDependency("dependencies.SignatureTestInterface.m5()", "java.io.OutputStream", RETURN_TYPE)
        //    }

        //    package dependencies;
        //
        //    import java.io.FileOutputStream;
        //    import java.io.FilterInputStream;
        //    import java.util.ArrayList;
        //    import java.util.List;
        //
        //    public abstract class SignatureTestClass<Q extends FilterInputStream>
        //    	implements SignatureTestInterface<Q, String> {
        assertDependency("dependencies.SignatureTestClass", "java.lang.Object", EXTENDS)
        assertDependency("dependencies.SignatureTestClass", "java.io.FilterInputStream", TYPE_IN_TYPE_PARAMETERS)
        assertDependency("dependencies.SignatureTestClass", "java.lang.String", TYPE_IN_TYPE_PARAMETERS)
        assertDependency("dependencies.SignatureTestClass", "dependencies.SignatureTestInterface", IMPLEMENTS)
        assertImplicitDefaultConstructor("dependencies.SignatureTestClass")
        //
        //        protected Q f1;
        assertDependency("dependencies.SignatureTestClass.f1", "dependencies.SignatureTestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.SignatureTestClass.f1", "java.io.FilterInputStream", FIELD_TYPE)
        //
        //        protected List<Long> f2;
        assertDependency("dependencies.SignatureTestClass.f2", "dependencies.SignatureTestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.SignatureTestClass.f2", "java.util.List", FIELD_TYPE)
        assertDependency("dependencies.SignatureTestClass.f2", "java.lang.Long", TYPE_IN_TYPE_PARAMETERS)
        //
        //        public abstract Q m1();
        assertDependency("dependencies.SignatureTestClass.m1()", "dependencies.SignatureTestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.SignatureTestClass.m1()", "java.io.FilterInputStream", RETURN_TYPE)
        // // implicit method:
        // // public InputStream m1(){
        // //     return m1(); //Method m1:()Ljava/io/FilterInputStream;
        // // }
        assertDependency("dependencies.SignatureTestClass.m1()", "dependencies.SignatureTestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.SignatureTestClass.m1()", "java.io.InputStream", RETURN_TYPE)
        ////        assertImplicitThisLocalVariable("dependencies.SignatureTestClass.m1()")
        assertDependency("dependencies.SignatureTestClass.m1()", "dependencies.SignatureTestClass", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.SignatureTestClass.m1()", "dependencies.SignatureTestClass.m1()", CALLS_METHOD)
        assertDependency("dependencies.SignatureTestClass.m1()", "java.io.FilterInputStream", RETURN_TYPE_OF_CALLED_METHOD)
        //
        //        public abstract void m2(Q t, String z);
        assertDependency("dependencies.SignatureTestClass.m2(java.io.FilterInputStream, java.lang.String)", "dependencies.SignatureTestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.SignatureTestClass.m2(java.io.FilterInputStream, java.lang.String)", "java.io.FilterInputStream", PARAMETER_TYPE)
        assertDependency("dependencies.SignatureTestClass.m2(java.io.FilterInputStream, java.lang.String)", "java.lang.String", PARAMETER_TYPE)
        // // implicit method:
        // // public void m2(java.io.InputStream t, java.lang.Object z){
        // //     return m2((java.io.FileInputStream)t, (java.lang.String) z);
        // // }
        assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "dependencies.SignatureTestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.io.InputStream", PARAMETER_TYPE)
        ////        assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.io.InputStream", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.lang.Object", PARAMETER_TYPE)
        ////        assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.lang.Object", LOCAL_VARIABLE_TYPE)
        ////        assertImplicitThisLocalVariable("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)")
        assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.io.FilterInputStream", TYPECAST)
        assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.lang.String", TYPECAST)
        assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "dependencies.SignatureTestClass", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "dependencies.SignatureTestClass.m2(java.io.FilterInputStream, java.lang.String)", CALLS_METHOD)
        assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.io.FilterInputStream", PARAMETER_TYPE_OF_CALLED_METHOD)
        assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.lang.String", PARAMETER_TYPE_OF_CALLED_METHOD)
        //
        //        @SuppressWarnings("unchecked")
        //        public abstract Integer m3();
        assertDependency("dependencies.SignatureTestClass.m3()", "dependencies.SignatureTestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.SignatureTestClass.m3()", "java.lang.Integer", RETURN_TYPE)
        // // implicit method:
        // // public Object m3(){
        // //     return m3(); //Method m3:()Ljava/lang/Integer;
        // // }
        assertDependency("dependencies.SignatureTestClass.m3()", "dependencies.SignatureTestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.SignatureTestClass.m3()", "java.lang.Object", RETURN_TYPE)
        ////        assertImplicitThisLocalVariable("dependencies.SignatureTestClass.m3()")
        assertDependency("dependencies.SignatureTestClass.m3()", "dependencies.SignatureTestClass", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.SignatureTestClass.m3()", "dependencies.SignatureTestClass.m3()", CALLS_METHOD)
        assertDependency("dependencies.SignatureTestClass.m3()", "java.lang.Integer", RETURN_TYPE_OF_CALLED_METHOD)
        //
        //        public abstract Q m4();
        assertDependency("dependencies.SignatureTestClass.m4()", "dependencies.SignatureTestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.SignatureTestClass.m4()", "java.io.FilterInputStream", RETURN_TYPE)
        // // implicit method:
        // // public InputStream m4(){
        // //     return m4(); //Method m4:()Ljava/io/FilterInputStream;
        // // }
        assertDependency("dependencies.SignatureTestClass.m4()", "dependencies.SignatureTestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.SignatureTestClass.m4()", "java.io.InputStream", RETURN_TYPE)
        ////        assertImplicitThisLocalVariable("dependencies.SignatureTestClass.m4()")
        assertDependency("dependencies.SignatureTestClass.m4()", "dependencies.SignatureTestClass", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.SignatureTestClass.m4()", "dependencies.SignatureTestClass.m4()", CALLS_METHOD)
        assertDependency("dependencies.SignatureTestClass.m4()", "java.io.FilterInputStream", RETURN_TYPE_OF_CALLED_METHOD)
        //
        //        @SuppressWarnings("unchecked")
        //        public abstract FileOutputStream m5();
        assertDependency("dependencies.SignatureTestClass.m5()", "dependencies.SignatureTestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.SignatureTestClass.m5()", "java.io.FileOutputStream", RETURN_TYPE)
        // // implicit method:
        // // public OutputStream m5(){
        // //     return m5(); //Method m5:()Ljava/io/FileOutputStream;
        // // }
        assertDependency("dependencies.SignatureTestClass.m5()", "dependencies.SignatureTestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.SignatureTestClass.m5()", "java.io.OutputStream", RETURN_TYPE)
        ////        assertImplicitThisLocalVariable("dependencies.SignatureTestClass.m5()")
        assertDependency("dependencies.SignatureTestClass.m5()", "dependencies.SignatureTestClass", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.SignatureTestClass.m5()", "dependencies.SignatureTestClass.m5()", CALLS_METHOD)
        assertDependency("dependencies.SignatureTestClass.m5()", "java.io.FileOutputStream", RETURN_TYPE_OF_CALLED_METHOD)
        //
        //        public abstract List<String> m6(ArrayList<Integer> p);
        assertDependency("dependencies.SignatureTestClass.m6(java.util.ArrayList)", "dependencies.SignatureTestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.SignatureTestClass.m6(java.util.ArrayList)", "java.util.ArrayList", PARAMETER_TYPE)
        assertDependency("dependencies.SignatureTestClass.m6(java.util.ArrayList)", "java.lang.Integer", TYPE_IN_TYPE_PARAMETERS)
        assertDependency("dependencies.SignatureTestClass.m6(java.util.ArrayList)", "java.util.List", RETURN_TYPE)
        assertDependency("dependencies.SignatureTestClass.m6(java.util.ArrayList)", "java.lang.String", TYPE_IN_TYPE_PARAMETERS)
        //    }

        //    package dependencies;
        //
        //    import java.io.FileOutputStream;
        //    import java.util.jar.JarInputStream;
        //    import java.util.zip.ZipInputStream;
        //
        //    public abstract class SignatureTestSubClass extends
        //    	SignatureTestClass<ZipInputStream> {
        assertDependency("dependencies.SignatureTestSubClass", "dependencies.SignatureTestClass", EXTENDS)
        assertDependency("dependencies.SignatureTestSubClass", "java.util.zip.ZipInputStream", TYPE_IN_TYPE_PARAMETERS)
        assertImplicitDefaultConstructor("dependencies.SignatureTestSubClass", "dependencies.SignatureTestClass")
        //        protected JarInputStream f1;
        assertDependency("dependencies.SignatureTestSubClass.f1", "dependencies.SignatureTestSubClass", INSTANCE_MEMBER)
        assertDependency("dependencies.SignatureTestSubClass.f1", "java.util.jar.JarInputStream", FIELD_TYPE)
        //
        //        @SuppressWarnings("unchecked")
        //        public abstract Integer m3();
        ////        assertDependency("dependencies.SignatureTestSubClass.m3()", "dependencies.SignatureTestSubClass", INSTANCE_MEMBER)
        assertDependency("dependencies.SignatureTestSubClass.m3()", "java.lang.Integer", RETURN_TYPE)
        // // implicit method:
        // // public Object m3(){
        // //     return m3(); //Method m3:()Ljava/lang/Integer;
        // // }
        assertDependency("dependencies.SignatureTestSubClass.m3()", "dependencies.SignatureTestSubClass", INSTANCE_MEMBER)
        ////        assertDependency("dependencies.SignatureTestSubClass.m3()", "java.lang.Object", RETURN_TYPE)
        ////        assertImplicitThisLocalVariable("dependencies.SignatureTestSubClass.m3()")
        ////        assertDependency("dependencies.SignatureTestSubClass.m3()", "dependencies.SignatureTestSubClass", DECLARING_CLASS_OF_CALLED_METHOD)
        ////        assertDependency("dependencies.SignatureTestSubClass.m3()", "dependencies.SignatureTestSubClass.m3()", CALLS_METHOD)
        ////        assertDependency("dependencies.SignatureTestSubClass.m3()", "java.lang.Integer", RETURN_TYPE_OF_CALLED_METHOD)
        //
        //        @SuppressWarnings("unchecked")
        //        public abstract FileOutputStream m5();
        assertDependency("dependencies.SignatureTestSubClass.m5()", "dependencies.SignatureTestSubClass", INSTANCE_MEMBER)
        assertDependency("dependencies.SignatureTestSubClass.m5()", "java.io.FileOutputStream", RETURN_TYPE)
        // // implicit method:
        // // public OutputStream m5(){
        // //     return m5(); //Method m5:()Ljava/io/FileOutputStream;
        // // }
        ////        assertDependency("dependencies.SignatureTestSubClass.m5()", "dependencies.SignatureTestSubClass", INSTANCE_MEMBER)
        ////        assertDependency("dependencies.SignatureTestSubClass.m5()", "java.io.OutputStream", RETURN_TYPE)
        ////        assertImplicitThisLocalVariable("dependencies.SignatureTestSubClass.m5()")
        ////        assertDependency("dependencies.SignatureTestSubClass.m5()", "dependencies.SignatureTestSubClass", DECLARING_CLASS_OF_CALLED_METHOD)
        ////        assertDependency("dependencies.SignatureTestSubClass.m5()", "dependencies.SignatureTestSubClass.m5()", CALLS_METHOD)
        ////        assertDependency("dependencies.SignatureTestSubClass.m5()", "java.io.FileOutputStream", RETURN_TYPE_OF_CALLED_METHOD)
        //    }

        //    @TypeTestAnnotation
        assertDependency("dependencies.package-info", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        //    package dependencies;
        assertDependency("dependencies.package-info", "java.lang.Object", EXTENDS)

        //    package dependencies;
        //
        //    public @interface TypeTestAnnotations {
        assertDependency("dependencies.TypeTestAnnotations", "java.lang.annotation.Annotation", IMPLEMENTS)
        assertDependency("dependencies.TypeTestAnnotations", "java.lang.Object", EXTENDS)
        //        TypeTestAnnotation[] value();
        assertDependency("dependencies.TypeTestAnnotations.value()", "dependencies.TypeTestAnnotation", RETURN_TYPE)
        assertDependency("dependencies.TypeTestAnnotations.value()", "dependencies.TypeTestAnnotations", INSTANCE_MEMBER)
        //    }

        //    package dependencies;
        //    
        //    import java.lang.annotation.ElementType;
        //    import java.lang.annotation.Repeatable;
        //    import java.lang.annotation.Target;
        //    
        //    @Repeatable(TypeTestAnnotations.class)
        assertDependency("dependencies.TypeTestAnnotation", "java.lang.annotation.Repeatable", ANNOTATED_WITH)
        assertDependency("dependencies.TypeTestAnnotation", "dependencies.TypeTestAnnotations", ANNOTATION_ELEMENT_TYPE)
        //    @Target({ ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR, ElementType.FIELD,
        //            ElementType.LOCAL_VARIABLE, ElementType.METHOD, ElementType.PACKAGE,
        //            ElementType.PARAMETER, ElementType.TYPE, ElementType.TYPE_PARAMETER,
        //            ElementType.TYPE_USE })
        assertDependency("dependencies.TypeTestAnnotation", "java.lang.annotation.Target", ANNOTATED_WITH)
        assertDependency("dependencies.TypeTestAnnotation", "java.lang.annotation.ElementType", ANNOTATION_ELEMENT_TYPE)
        assertDependency("dependencies.TypeTestAnnotation", "java.lang.annotation.ElementType", ANNOTATION_ELEMENT_TYPE)
        assertDependency("dependencies.TypeTestAnnotation", "java.lang.annotation.ElementType", ANNOTATION_ELEMENT_TYPE)
        assertDependency("dependencies.TypeTestAnnotation", "java.lang.annotation.ElementType", ANNOTATION_ELEMENT_TYPE)
        assertDependency("dependencies.TypeTestAnnotation", "java.lang.annotation.ElementType", ANNOTATION_ELEMENT_TYPE)
        assertDependency("dependencies.TypeTestAnnotation", "java.lang.annotation.ElementType", ANNOTATION_ELEMENT_TYPE)
        assertDependency("dependencies.TypeTestAnnotation", "java.lang.annotation.ElementType", ANNOTATION_ELEMENT_TYPE)
        assertDependency("dependencies.TypeTestAnnotation", "java.lang.annotation.ElementType", ANNOTATION_ELEMENT_TYPE)
        assertDependency("dependencies.TypeTestAnnotation", "java.lang.annotation.ElementType", ANNOTATION_ELEMENT_TYPE)
        assertDependency("dependencies.TypeTestAnnotation", "java.lang.annotation.ElementType", ANNOTATION_ELEMENT_TYPE)
        assertDependency("dependencies.TypeTestAnnotation", "java.lang.annotation.ElementType.CONSTRUCTOR", USES_ENUM_VALUE)
        assertDependency("dependencies.TypeTestAnnotation", "java.lang.annotation.ElementType.TYPE_USE", USES_ENUM_VALUE)
        assertDependency("dependencies.TypeTestAnnotation", "java.lang.annotation.ElementType.PACKAGE", USES_ENUM_VALUE)
        assertDependency("dependencies.TypeTestAnnotation", "java.lang.annotation.ElementType.ANNOTATION_TYPE", USES_ENUM_VALUE)
        assertDependency("dependencies.TypeTestAnnotation", "java.lang.annotation.ElementType.TYPE_PARAMETER", USES_ENUM_VALUE)
        assertDependency("dependencies.TypeTestAnnotation", "java.lang.annotation.ElementType.FIELD", USES_ENUM_VALUE)
        assertDependency("dependencies.TypeTestAnnotation", "java.lang.annotation.ElementType.LOCAL_VARIABLE", USES_ENUM_VALUE)
        assertDependency("dependencies.TypeTestAnnotation", "java.lang.annotation.ElementType.METHOD", USES_ENUM_VALUE)
        assertDependency("dependencies.TypeTestAnnotation", "java.lang.annotation.ElementType.TYPE", USES_ENUM_VALUE)
        assertDependency("dependencies.TypeTestAnnotation", "java.lang.annotation.ElementType.PARAMETER", USES_ENUM_VALUE)
        //    public @interface TypeTestAnnotation {
        assertDependency("dependencies.TypeTestAnnotation", "java.lang.annotation.Annotation", IMPLEMENTS)
        assertDependency("dependencies.TypeTestAnnotation", "java.lang.Object", EXTENDS)
        //    }

        //    package dependencies;
        //    
        //    import java.io.FileReader;
        //    import java.util.function.Function;
        //    import java.util.function.Supplier;
        //    
        //    import dependencies.OuterClass.InnerClass;
        //    
        //    @TypeTestAnnotation
        //    @SuppressWarnings("unused")
        //    public class AnnotationTypeTestClass {
        assertDependency("dependencies.AnnotationTypeTestClass", "dependencies.AnnotationTypeTestClass$Inheritance", OUTER_CLASS)
        assertDependency("dependencies.AnnotationTypeTestClass", "dependencies.AnnotationTypeTestClass$GenericUpperIntersectionTest", OUTER_CLASS)
        assertDependency("dependencies.AnnotationTypeTestClass", "dependencies.AnnotationTypeTestClass$NestedGeneric", OUTER_CLASS)
        assertDependency("dependencies.AnnotationTypeTestClass", "dependencies.AnnotationTypeTestClass$GenericTest", OUTER_CLASS)
        assertDependency("dependencies.AnnotationTypeTestClass", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass", "java.lang.Object", EXTENDS)

        assertDependency("dependencies.AnnotationTypeTestClass.<init>()", "java.lang.Object", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.<init>()", "dependencies.AnnotationTypeTestClass", DECLARING_CLASS_OF_ACCESSED_FIELD)
        assertDependency("dependencies.AnnotationTypeTestClass.<init>()", "dependencies.AnnotationTypeTestClass", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass.<init>()", "java.lang.Object.<init>()", CALLS_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.<init>()", "dependencies.AnnotationTypeTestClass.number", WRITES_FIELD)
        assertDependency("dependencies.AnnotationTypeTestClass.<init>()", "dependencies.AnnotationTypeTestClass", INSTANCE_MEMBER)

        //    
        //        @TypeTestAnnotation
        //        int number = 0;
        //// One type annotation and one declaration
        assertDependency("dependencies.AnnotationTypeTestClass.number", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.number", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.number", "dependencies.AnnotationTypeTestClass", INSTANCE_MEMBER)

        //    
        //        @TypeTestAnnotation
        //        public void innerClass() {
        assertDependency("dependencies.AnnotationTypeTestClass.innerClass()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.innerClass()", "dependencies.AnnotationTypeTestClass", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass.innerClass()", "dependencies.AnnotationTypeTestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.AnnotationTypeTestClass.innerClass()", "dependencies.OuterClass$InnerClass", LOCAL_VARIABLE_TYPE)

        //            OuterClass.@TypeTestAnnotation InnerClass inner = null;
        assertDependency("dependencies.AnnotationTypeTestClass.innerClass()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)

        //        }
        //    
        //        @TypeTestAnnotation
        //        @TypeTestAnnotation
        //        public void repeatableAnnotation() {
        assertDependency("dependencies.AnnotationTypeTestClass.repeatableAnnotation()", "dependencies.TypeTestAnnotations", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.repeatableAnnotation()", "dependencies.TypeTestAnnotations", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.repeatableAnnotation()", "dependencies.TypeTestAnnotation", ANNOTATION_ELEMENT_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass.repeatableAnnotation()", "dependencies.TypeTestAnnotation", ANNOTATION_ELEMENT_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass.repeatableAnnotation()", "dependencies.AnnotationTypeTestClass", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass.repeatableAnnotation()", "dependencies.AnnotationTypeTestClass", INSTANCE_MEMBER)

        //            @TypeTestAnnotation
        //            @TypeTestAnnotation
        assertDependency("dependencies.AnnotationTypeTestClass.repeatableAnnotation()", "dependencies.TypeTestAnnotation", ANNOTATION_ELEMENT_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass.repeatableAnnotation()", "dependencies.TypeTestAnnotation", ANNOTATION_ELEMENT_TYPE)
        //            int number = 0;
        //        }
        //    
        //        public void array() {
        assertDependency("dependencies.AnnotationTypeTestClass.array()", "dependencies.AnnotationTypeTestClass", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass.array()", "dependencies.AnnotationTypeTestClass", INSTANCE_MEMBER)

        //            @TypeTestAnnotation
        assertDependency("dependencies.AnnotationTypeTestClass.array()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        //            int @TypeTestAnnotation [] array = new @TypeTestAnnotation int @TypeTestAnnotation [] {};
        assertDependency("dependencies.AnnotationTypeTestClass.array()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.array()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.array()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        //        }
        //    
        //        public void twoDimArray() {
        assertDependency("dependencies.AnnotationTypeTestClass.twoDimArray()", "dependencies.AnnotationTypeTestClass", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass.twoDimArray()", "dependencies.AnnotationTypeTestClass", INSTANCE_MEMBER)
        //            @TypeTestAnnotation
        assertDependency("dependencies.AnnotationTypeTestClass.twoDimArray()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        //            int @TypeTestAnnotation [][] array = new @TypeTestAnnotation int @TypeTestAnnotation [][] {};
        assertDependency("dependencies.AnnotationTypeTestClass.twoDimArray()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.twoDimArray()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.twoDimArray()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        //    
        //            @TypeTestAnnotation
        assertDependency("dependencies.AnnotationTypeTestClass.twoDimArray()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        //            int @TypeTestAnnotation [] @TypeTestAnnotation [] array2 = new @TypeTestAnnotation int @TypeTestAnnotation [] @TypeTestAnnotation [] {};
        assertDependency("dependencies.AnnotationTypeTestClass.twoDimArray()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.twoDimArray()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.twoDimArray()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.twoDimArray()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.twoDimArray()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)

        //        }
        //    
        //        public void constructors() {
        assertDependency("dependencies.AnnotationTypeTestClass.constructors()", "dependencies.AnnotationTypeTestClass", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass.constructors()", "dependencies.AnnotationTypeTestClass", INSTANCE_MEMBER)

        //            String string = new @TypeTestAnnotation String();
        assertDependency("dependencies.AnnotationTypeTestClass.constructors()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.constructors()", "java.lang.String", CREATES)
        assertDependency("dependencies.AnnotationTypeTestClass.constructors()", "java.lang.String", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.constructors()", "java.lang.String", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass.constructors()", "java.lang.String.<init>()", CALLS_METHOD)

        //            OuterClass outerClass = new OuterClass();
        assertDependency("dependencies.AnnotationTypeTestClass.constructors()", "dependencies.OuterClass.<init>()", CALLS_METHOD)

        //            InnerClass innerClass = outerClass.new @TypeTestAnnotation InnerClass(1);
        assertDependency("dependencies.AnnotationTypeTestClass.constructors()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.constructors()", "dependencies.OuterClass$InnerClass", CREATES)
        assertDependency("dependencies.AnnotationTypeTestClass.constructors()", "dependencies.OuterClass$InnerClass", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.constructors()", "dependencies.OuterClass$InnerClass", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass.constructors()", "dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", CALLS_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.constructors()", "dependencies.OuterClass", CREATES)
        assertDependency("dependencies.AnnotationTypeTestClass.constructors()", "dependencies.OuterClass", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.constructors()", "dependencies.OuterClass", PARAMETER_TYPE_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.constructors()", "dependencies.OuterClass", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass.constructors()", "java.lang.Integer", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.constructors()", "java.lang.Integer", RETURN_TYPE_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.constructors()", "java.lang.Integer", PARAMETER_TYPE_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.constructors()", "java.lang.Integer.valueOf(int)", CALLS_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.constructors()", "java.lang.Object", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.constructors()", "java.lang.Object.getClass()", CALLS_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.constructors()", "java.lang.Class", RETURN_TYPE_OF_CALLED_METHOD)

        //        }
        //    
        //        public void cast() {
        assertDependency("dependencies.AnnotationTypeTestClass.cast()", "dependencies.AnnotationTypeTestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.AnnotationTypeTestClass.cast()", "dependencies.AnnotationTypeTestClass", LOCAL_VARIABLE_TYPE)

        //            TestInterface inter = (@TypeTestAnnotation TestInterface) new TestClass();
        assertDependency("dependencies.AnnotationTypeTestClass.cast()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.cast()", "dependencies.TestInterface", TYPECAST)
        assertDependency("dependencies.AnnotationTypeTestClass.cast()", "dependencies.TestClass", CREATES)
        assertDependency("dependencies.AnnotationTypeTestClass.cast()", "dependencies.TestClass", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.cast()", "dependencies.TestClass.<init>()", CALLS_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.cast()", "dependencies.TestInterface", LOCAL_VARIABLE_TYPE)

        //            inter.toString();
        assertDependency("dependencies.AnnotationTypeTestClass.cast()", "java.lang.Object", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.cast()", "java.lang.Object.toString()", CALLS_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.cast()", "java.lang.String", RETURN_TYPE_OF_CALLED_METHOD)

        //        }
        //    
        //        public void instance() {
        assertDependency("dependencies.AnnotationTypeTestClass.instance()", "dependencies.AnnotationTypeTestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.AnnotationTypeTestClass.instance()", "dependencies.AnnotationTypeTestClass", LOCAL_VARIABLE_TYPE)

        //            String string = new String();
        assertDependency("dependencies.AnnotationTypeTestClass.instance()", "java.lang.String", CREATES)
        assertDependency("dependencies.AnnotationTypeTestClass.instance()", "java.lang.String.<init>()", CALLS_METHOD)

        //            boolean check = string instanceof @TypeTestAnnotation String;
        assertDependency("dependencies.AnnotationTypeTestClass.instance()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.instance()", "java.lang.String", TYPECHECK)
        assertDependency("dependencies.AnnotationTypeTestClass.instance()", "java.lang.String", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.instance()", "java.lang.String", LOCAL_VARIABLE_TYPE)
        //        }
        //    
        //        public void genericClass() {
        assertDependency("dependencies.AnnotationTypeTestClass.genericClass()", "dependencies.AnnotationTypeTestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.AnnotationTypeTestClass.genericClass()", "dependencies.AnnotationTypeTestClass", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass.genericClass()", "dependencies.AnnotationTypeTestClass$GenericTest", LOCAL_VARIABLE_TYPE)

        //            GenericTest<@TypeTestAnnotation Integer> generic = new GenericTest<>();
        assertDependency("dependencies.AnnotationTypeTestClass.genericClass()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.genericClass()", "dependencies.AnnotationTypeTestClass$GenericTest", CREATES)
        assertDependency("dependencies.AnnotationTypeTestClass.genericClass()", "dependencies.AnnotationTypeTestClass$GenericTest", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.genericClass()", "dependencies.AnnotationTypeTestClass", PARAMETER_TYPE_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.genericClass()", "dependencies.AnnotationTypeTestClass$GenericTest.<init>(dependencies.AnnotationTypeTestClass)", CALLS_METHOD)

        //            generic = new GenericTest<@TypeTestAnnotation Integer>();
        assertDependency("dependencies.AnnotationTypeTestClass.genericClass()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.genericClass()", "dependencies.AnnotationTypeTestClass$GenericTest", CREATES)
        assertDependency("dependencies.AnnotationTypeTestClass.genericClass()", "dependencies.AnnotationTypeTestClass$GenericTest", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.genericClass()", "dependencies.AnnotationTypeTestClass$GenericTest.<init>(dependencies.AnnotationTypeTestClass)", CALLS_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.genericClass()", "dependencies.AnnotationTypeTestClass", PARAMETER_TYPE_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.genericClass()", "java.lang.Integer", TYPE_IN_TYPE_PARAMETERS)

        //        }
        //    
        //        public void boundedTypes() {
        assertDependency("dependencies.AnnotationTypeTestClass.boundedTypes()", "dependencies.AnnotationTypeTestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.AnnotationTypeTestClass.boundedTypes()", "dependencies.AnnotationTypeTestClass", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass.boundedTypes()", "dependencies.AnnotationTypeTestClass", PARAMETER_TYPE_OF_CALLED_METHOD)

        //            GenericTest<@TypeTestAnnotation ? super @TypeTestAnnotation String> instance = new GenericTest<>();
        assertDependency("dependencies.AnnotationTypeTestClass.boundedTypes()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.boundedTypes()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.boundedTypes()", "dependencies.AnnotationTypeTestClass$GenericTest", CREATES)
        assertDependency("dependencies.AnnotationTypeTestClass.boundedTypes()", "dependencies.AnnotationTypeTestClass$GenericTest", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.boundedTypes()", "dependencies.AnnotationTypeTestClass$GenericTest", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass.boundedTypes()", "dependencies.AnnotationTypeTestClass$GenericTest.<init>(dependencies.AnnotationTypeTestClass)", CALLS_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.boundedTypes()", "java.lang.String", TYPE_IN_TYPE_PARAMETERS)

        //        }
        //    
        //        public void throwException() throws @TypeTestAnnotation IllegalArgumentException {
        assertDependency("dependencies.AnnotationTypeTestClass.throwException()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.throwException()", "dependencies.AnnotationTypeTestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.AnnotationTypeTestClass.throwException()", "dependencies.AnnotationTypeTestClass", LOCAL_VARIABLE_TYPE)

        //            throw new IllegalArgumentException();
        assertDependency("dependencies.AnnotationTypeTestClass.throwException()", "java.lang.IllegalArgumentException", CREATES)
        assertDependency("dependencies.AnnotationTypeTestClass.throwException()", "java.lang.IllegalArgumentException", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.throwException()", "java.lang.IllegalArgumentException", THROWN_EXCEPTION)
        assertDependency("dependencies.AnnotationTypeTestClass.throwException()", "java.lang.IllegalArgumentException.<init>()", CALLS_METHOD)

        //        }
        //    
        //        public void tryWithResource(String path) {
        assertDependency("dependencies.AnnotationTypeTestClass.tryWithResource(java.lang.String)", "dependencies.AnnotationTypeTestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.AnnotationTypeTestClass.tryWithResource(java.lang.String)", "dependencies.AnnotationTypeTestClass", LOCAL_VARIABLE_TYPE)
        //            try (@TypeTestAnnotation
        assertDependency("dependencies.AnnotationTypeTestClass.tryWithResource(java.lang.String)", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        //            FileReader br = new @TypeTestAnnotation FileReader(path)) {
        assertDependency("dependencies.AnnotationTypeTestClass.tryWithResource(java.lang.String)", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.tryWithResource(java.lang.String)", "java.io.FileReader", CREATES)
        assertDependency("dependencies.AnnotationTypeTestClass.tryWithResource(java.lang.String)", "java.io.FileReader", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.tryWithResource(java.lang.String)", "java.io.FileReader", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass.tryWithResource(java.lang.String)", "java.io.FileReader.<init>(java.lang.String)", CALLS_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.tryWithResource(java.lang.String)", "java.io.FileReader.close()", CALLS_METHOD)

        assertDependency("dependencies.AnnotationTypeTestClass.tryWithResource(java.lang.String)", "java.lang.String", PARAMETER_TYPE_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.tryWithResource(java.lang.String)", "java.lang.String", PARAMETER_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass.tryWithResource(java.lang.String)", "java.lang.String", LOCAL_VARIABLE_TYPE)

        assertDependency("dependencies.AnnotationTypeTestClass.tryWithResource(java.lang.String)", "java.lang.Throwable", PARAMETER_TYPE_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.tryWithResource(java.lang.String)", "java.lang.Throwable", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.tryWithResource(java.lang.String)", "java.lang.Throwable.addSuppressed(java.lang.Throwable)", CALLS_METHOD)

        assertDependency("dependencies.AnnotationTypeTestClass.tryWithResource(java.lang.String)", "java.io.FileReader", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.tryWithResource(java.lang.String)", "java.io.FileReader", DECLARING_CLASS_OF_CALLED_METHOD)

        //                // EMPTY
        //            } catch (Exception e) {
        //                // EMTPY
        assertDependency("dependencies.AnnotationTypeTestClass.tryWithResource(java.lang.String)", "java.lang.Exception", CATCHES)
        assertDependency("dependencies.AnnotationTypeTestClass.tryWithResource(java.lang.String)", "java.io.FileReader.close()", CALLS_METHOD)

        //            }
        //        }
        //    
        //        public void tryCatch() {
        assertDependency("dependencies.AnnotationTypeTestClass.tryCatch()", "dependencies.AnnotationTypeTestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.AnnotationTypeTestClass.tryCatch()", "dependencies.AnnotationTypeTestClass", LOCAL_VARIABLE_TYPE)

        //            try {
        //                // EMPTY
        //            } catch (@TypeTestAnnotation Exception e) {
        //            }
        //        }
        //    
        //        public void java8() {
        assertDependency("dependencies.AnnotationTypeTestClass.java8()", "dependencies.AnnotationTypeTestClass", INSTANCE_MEMBER)
        assertDependency("dependencies.AnnotationTypeTestClass.java8()", "dependencies.AnnotationTypeTestClass", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass.java8()", "dependencies.AnnotationTypeTestClass", DECLARING_CLASS_OF_CALLED_METHOD)

        //            lambdaFunction("value", @TypeTestAnnotation String::toString);
        assertDependency("dependencies.AnnotationTypeTestClass.java8()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.java8()", "dependencies.AnnotationTypeTestClass.lambdaFunction(java.lang.String, java.util.function.Function)", CALLS_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.java8()", "java.lang.String", RETURN_TYPE_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.java8()", "java.lang.String", PARAMETER_TYPE_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.java8()", "java.lang.invoke.LambdaMetafactory", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.java8()", "java.lang.invoke.LambdaMetafactory.metafactory(java.lang.invoke.MethodHandles$Lookup, java.lang.String, java.lang.invoke.MethodType, java.lang.invoke.MethodType, java.lang.invoke.MethodHandle, java.lang.invoke.MethodType)", CALLS_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.java8()", "java.util.function.Function", RETURN_TYPE_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.java8()", "java.util.function.Function", PARAMETER_TYPE_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.java8()", "java.util.function.Supplier", RETURN_TYPE_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.java8()", "java.util.function.Supplier", LOCAL_VARIABLE_TYPE)

        //            Supplier<TestClass> instance = @TypeTestAnnotation TestClass::new;
        assertDependency("dependencies.AnnotationTypeTestClass.java8()", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.java8()", "dependencies.TestClass", TYPE_IN_TYPE_PARAMETERS)

        assertDependency("dependencies.AnnotationTypeTestClass.java8()", "java.lang.invoke.LambdaMetafactory", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.java8()", "java.lang.invoke.LambdaMetafactory.metafactory(java.lang.invoke.MethodHandles$Lookup, java.lang.String, java.lang.invoke.MethodType, java.lang.invoke.MethodType, java.lang.invoke.MethodHandle, java.lang.invoke.MethodType)", CALLS_METHOD)

        //        }
        //    
        //        public static @TypeTestAnnotation String lambdaFunction(
        assertDependency("dependencies.AnnotationTypeTestClass.lambdaFunction(java.lang.String, java.util.function.Function)", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.lambdaFunction(java.lang.String, java.util.function.Function)", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.lambdaFunction(java.lang.String, java.util.function.Function)", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)

        //                @TypeTestAnnotation String value,
        assertDependency("dependencies.AnnotationTypeTestClass.lambdaFunction(java.lang.String, java.util.function.Function)", "dependencies.TypeTestAnnotation", PARAMETER_ANNOTATED_WITH)

        //                Function<@TypeTestAnnotation String, @TypeTestAnnotation String> function) {
        assertDependency("dependencies.AnnotationTypeTestClass.lambdaFunction(java.lang.String, java.util.function.Function)", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass.lambdaFunction(java.lang.String, java.util.function.Function)", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)

        //            return function.apply(value);
        assertDependency("dependencies.AnnotationTypeTestClass.lambdaFunction(java.lang.String, java.util.function.Function)", "java.lang.Object", RETURN_TYPE_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.lambdaFunction(java.lang.String, java.util.function.Function)", "java.lang.Object", PARAMETER_TYPE_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.lambdaFunction(java.lang.String, java.util.function.Function)", "java.lang.String", TYPECAST)
        assertDependency("dependencies.AnnotationTypeTestClass.lambdaFunction(java.lang.String, java.util.function.Function)", "java.lang.String", TYPE_IN_TYPE_PARAMETERS)
        assertDependency("dependencies.AnnotationTypeTestClass.lambdaFunction(java.lang.String, java.util.function.Function)", "java.lang.String", PARAMETER_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass.lambdaFunction(java.lang.String, java.util.function.Function)", "java.lang.String", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass.lambdaFunction(java.lang.String, java.util.function.Function)", "java.lang.String", TYPE_IN_TYPE_PARAMETERS)
        assertDependency("dependencies.AnnotationTypeTestClass.lambdaFunction(java.lang.String, java.util.function.Function)", "java.lang.String", RETURN_TYPE)

        assertDependency("dependencies.AnnotationTypeTestClass.lambdaFunction(java.lang.String, java.util.function.Function)", "java.util.function.Function", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass.lambdaFunction(java.lang.String, java.util.function.Function)", "java.util.function.Function", PARAMETER_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass.lambdaFunction(java.lang.String, java.util.function.Function)", "java.util.function.Function", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass.lambdaFunction(java.lang.String, java.util.function.Function)", "java.util.function.Function.apply(java.lang.Object)", CALLS_METHOD)

        assertDependency("dependencies.AnnotationTypeTestClass.lambdaFunction(java.lang.String, java.util.function.Function)", "dependencies.AnnotationTypeTestClass", CLASS_MEMBER)
        assertDependency("dependencies.AnnotationTypeTestClass.lambdaFunction(java.lang.String, java.util.function.Function)", "java.lang.String", TYPE_IN_TYPE_PARAMETERS)
        assertDependency("dependencies.AnnotationTypeTestClass.lambdaFunction(java.lang.String, java.util.function.Function)", "java.lang.String", TYPE_IN_TYPE_PARAMETERS)

        //        }

        //    
        //        public class Inheritance implements @TypeTestAnnotation TestInterface {
        assertDependency("dependencies.AnnotationTypeTestClass$Inheritance", "dependencies.AnnotationTypeTestClass", INNER_CLASS)
        assertDependency("dependencies.AnnotationTypeTestClass$Inheritance", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass$Inheritance", "dependencies.TestInterface", IMPLEMENTS)
        assertDependency("dependencies.AnnotationTypeTestClass$Inheritance", "java.lang.Object", EXTENDS)

        assertDependency("dependencies.AnnotationTypeTestClass$Inheritance.this$0", "dependencies.AnnotationTypeTestClass$Inheritance", INSTANCE_MEMBER)
        assertDependency("dependencies.AnnotationTypeTestClass$Inheritance.this$0", "dependencies.AnnotationTypeTestClass", FIELD_TYPE)

        assertDependency("dependencies.AnnotationTypeTestClass$Inheritance.<init>(dependencies.AnnotationTypeTestClass)", "dependencies.AnnotationTypeTestClass$Inheritance", DECLARING_CLASS_OF_ACCESSED_FIELD)
        assertDependency("dependencies.AnnotationTypeTestClass$Inheritance.<init>(dependencies.AnnotationTypeTestClass)", "java.lang.Object", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass$Inheritance.<init>(dependencies.AnnotationTypeTestClass)", "dependencies.AnnotationTypeTestClass", PARAMETER_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass$Inheritance.<init>(dependencies.AnnotationTypeTestClass)", "java.lang.Object.<init>()", CALLS_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass$Inheritance.<init>(dependencies.AnnotationTypeTestClass)", "dependencies.AnnotationTypeTestClass$Inheritance", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass$Inheritance.<init>(dependencies.AnnotationTypeTestClass)", "dependencies.AnnotationTypeTestClass$Inheritance.this$0", WRITES_FIELD)
        assertDependency("dependencies.AnnotationTypeTestClass$Inheritance.<init>(dependencies.AnnotationTypeTestClass)", "dependencies.AnnotationTypeTestClass", TYPE_OF_ACCESSED_FIELD)
        assertDependency("dependencies.AnnotationTypeTestClass$Inheritance.<init>(dependencies.AnnotationTypeTestClass)", "dependencies.AnnotationTypeTestClass$Inheritance", INSTANCE_MEMBER)

        //            @Override
        //            public void testMethod() {
        assertDependency("dependencies.AnnotationTypeTestClass$Inheritance.testMethod()", "dependencies.AnnotationTypeTestClass$Inheritance", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass$Inheritance.testMethod()", "dependencies.AnnotationTypeTestClass$Inheritance", INSTANCE_MEMBER)
        //            }
        //    
        //            @Override
        //            public String testMethod(Integer i, int j) {
        assertDependency("dependencies.AnnotationTypeTestClass$Inheritance.testMethod(java.lang.Integer, int)", "java.lang.Integer", PARAMETER_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass$Inheritance.testMethod(java.lang.Integer, int)", "java.lang.Integer", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass$Inheritance.testMethod(java.lang.Integer, int)", "java.lang.String", RETURN_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass$Inheritance.testMethod(java.lang.Integer, int)", "dependencies.AnnotationTypeTestClass$Inheritance", INSTANCE_MEMBER)
        assertDependency("dependencies.AnnotationTypeTestClass$Inheritance.testMethod(java.lang.Integer, int)", "dependencies.AnnotationTypeTestClass$Inheritance", LOCAL_VARIABLE_TYPE)
        //                return null;
        //            }
        //        }
        //    
        //        public class GenericTest<@TypeTestAnnotation T extends @TypeTestAnnotation Object> {
        assertDependency("dependencies.AnnotationTypeTestClass$GenericTest", "dependencies.AnnotationTypeTestClass", INNER_CLASS)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericTest", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericTest", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericTest", "java.lang.Object", TYPE_IN_TYPE_PARAMETERS)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericTest", "java.lang.Object", EXTENDS)

        assertDependency("dependencies.AnnotationTypeTestClass$GenericTest.this$0", "dependencies.AnnotationTypeTestClass$GenericTest", INSTANCE_MEMBER)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericTest.this$0", "dependencies.AnnotationTypeTestClass", FIELD_TYPE)

        assertDependency("dependencies.AnnotationTypeTestClass$GenericTest.<init>(dependencies.AnnotationTypeTestClass)", "dependencies.AnnotationTypeTestClass$GenericTest", DECLARING_CLASS_OF_ACCESSED_FIELD)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericTest.<init>(dependencies.AnnotationTypeTestClass)", "java.lang.Object", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericTest.<init>(dependencies.AnnotationTypeTestClass)", "dependencies.AnnotationTypeTestClass", PARAMETER_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericTest.<init>(dependencies.AnnotationTypeTestClass)", "java.lang.Object.<init>()", CALLS_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericTest.<init>(dependencies.AnnotationTypeTestClass)", "dependencies.AnnotationTypeTestClass$GenericTest", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericTest.<init>(dependencies.AnnotationTypeTestClass)", "dependencies.AnnotationTypeTestClass$GenericTest.this$0", WRITES_FIELD)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericTest.<init>(dependencies.AnnotationTypeTestClass)", "dependencies.AnnotationTypeTestClass", TYPE_OF_ACCESSED_FIELD)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericTest.<init>(dependencies.AnnotationTypeTestClass)", "dependencies.AnnotationTypeTestClass$GenericTest", INSTANCE_MEMBER)
        //            
        //            public <@TypeTestAnnotation U> void inspect(U u){
        assertDependency("dependencies.AnnotationTypeTestClass$GenericTest.inspect(java.lang.Object)", "java.lang.Object", TYPE_IN_TYPE_PARAMETERS)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericTest.inspect(java.lang.Object)", "java.lang.Object", PARAMETER_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericTest.inspect(java.lang.Object)", "java.lang.Object", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericTest.inspect(java.lang.Object)", "dependencies.AnnotationTypeTestClass$GenericTest", INSTANCE_MEMBER)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericTest.inspect(java.lang.Object)", "dependencies.AnnotationTypeTestClass$GenericTest", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericTest.inspect(java.lang.Object)", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        //            }
        //    
        //        }
        //    
        //        public class GenericUpperIntersectionTest<@TypeTestAnnotation T extends @TypeTestAnnotation Object & @TypeTestAnnotation TypeTestAnnotation> {
        assertDependency("dependencies.AnnotationTypeTestClass$GenericUpperIntersectionTest", "dependencies.AnnotationTypeTestClass", INNER_CLASS)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericUpperIntersectionTest", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericUpperIntersectionTest", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericUpperIntersectionTest", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericUpperIntersectionTest", "java.lang.Object", TYPE_IN_TYPE_PARAMETERS)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericUpperIntersectionTest", "dependencies.TypeTestAnnotation", TYPE_IN_TYPE_PARAMETERS)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericUpperIntersectionTest", "java.lang.Object", EXTENDS)

        assertDependency("dependencies.AnnotationTypeTestClass$GenericUpperIntersectionTest.this$0", "dependencies.AnnotationTypeTestClass$GenericUpperIntersectionTest", INSTANCE_MEMBER)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericUpperIntersectionTest.this$0", "dependencies.AnnotationTypeTestClass", FIELD_TYPE)

        assertDependency("dependencies.AnnotationTypeTestClass$GenericUpperIntersectionTest.<init>(dependencies.AnnotationTypeTestClass)", "java.lang.Object", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericUpperIntersectionTest.<init>(dependencies.AnnotationTypeTestClass)", "dependencies.AnnotationTypeTestClass$GenericUpperIntersectionTest", DECLARING_CLASS_OF_ACCESSED_FIELD)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericUpperIntersectionTest.<init>(dependencies.AnnotationTypeTestClass)", "java.lang.Object.<init>()", CALLS_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericUpperIntersectionTest.<init>(dependencies.AnnotationTypeTestClass)", "dependencies.AnnotationTypeTestClass$GenericUpperIntersectionTest.this$0", WRITES_FIELD)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericUpperIntersectionTest.<init>(dependencies.AnnotationTypeTestClass)", "dependencies.AnnotationTypeTestClass$GenericUpperIntersectionTest", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericUpperIntersectionTest.<init>(dependencies.AnnotationTypeTestClass)", "dependencies.AnnotationTypeTestClass", PARAMETER_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericUpperIntersectionTest.<init>(dependencies.AnnotationTypeTestClass)", "dependencies.AnnotationTypeTestClass", TYPE_OF_ACCESSED_FIELD)
        assertDependency("dependencies.AnnotationTypeTestClass$GenericUpperIntersectionTest.<init>(dependencies.AnnotationTypeTestClass)", "dependencies.AnnotationTypeTestClass$GenericUpperIntersectionTest", INSTANCE_MEMBER)

        //        }
        //    
        //        public @TypeTestAnnotation class NestedGeneric<@TypeTestAnnotation T extends @TypeTestAnnotation GenericTest<@TypeTestAnnotation U>, @TypeTestAnnotation U> {
        assertDependency("dependencies.AnnotationTypeTestClass$NestedGeneric", "dependencies.AnnotationTypeTestClass", INNER_CLASS)
        assertDependency("dependencies.AnnotationTypeTestClass$NestedGeneric", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass$NestedGeneric", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass$NestedGeneric", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass$NestedGeneric", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass$NestedGeneric", "dependencies.TypeTestAnnotation", ANNOTATED_WITH)
        assertDependency("dependencies.AnnotationTypeTestClass$NestedGeneric", "dependencies.AnnotationTypeTestClass$GenericTest", TYPE_IN_TYPE_PARAMETERS)
        assertDependency("dependencies.AnnotationTypeTestClass$NestedGeneric", "java.lang.Object", TYPE_IN_TYPE_PARAMETERS)
        assertDependency("dependencies.AnnotationTypeTestClass$NestedGeneric", "java.lang.Object", EXTENDS)

        assertDependency("dependencies.AnnotationTypeTestClass$NestedGeneric.this$0", "dependencies.AnnotationTypeTestClass$NestedGeneric", INSTANCE_MEMBER)
        assertDependency("dependencies.AnnotationTypeTestClass$NestedGeneric.this$0", "dependencies.AnnotationTypeTestClass", FIELD_TYPE)

        assertDependency("dependencies.AnnotationTypeTestClass$NestedGeneric.<init>(dependencies.AnnotationTypeTestClass)", "java.lang.Object", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass$NestedGeneric.<init>(dependencies.AnnotationTypeTestClass)", "dependencies.AnnotationTypeTestClass$NestedGeneric", DECLARING_CLASS_OF_ACCESSED_FIELD)
        assertDependency("dependencies.AnnotationTypeTestClass$NestedGeneric.<init>(dependencies.AnnotationTypeTestClass)", "java.lang.Object.<init>()", CALLS_METHOD)
        assertDependency("dependencies.AnnotationTypeTestClass$NestedGeneric.<init>(dependencies.AnnotationTypeTestClass)", "dependencies.AnnotationTypeTestClass$NestedGeneric.this$0", WRITES_FIELD)
        assertDependency("dependencies.AnnotationTypeTestClass$NestedGeneric.<init>(dependencies.AnnotationTypeTestClass)", "dependencies.AnnotationTypeTestClass$NestedGeneric", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass$NestedGeneric.<init>(dependencies.AnnotationTypeTestClass)", "dependencies.AnnotationTypeTestClass", PARAMETER_TYPE)
        assertDependency("dependencies.AnnotationTypeTestClass$NestedGeneric.<init>(dependencies.AnnotationTypeTestClass)", "dependencies.AnnotationTypeTestClass", TYPE_OF_ACCESSED_FIELD)
        assertDependency("dependencies.AnnotationTypeTestClass$NestedGeneric.<init>(dependencies.AnnotationTypeTestClass)", "dependencies.AnnotationTypeTestClass$NestedGeneric", INSTANCE_MEMBER)

        //        }
        //    
        //    }

        val remainingDependencies = dependencies.view.filter(_._2 > 0)
        assert(remainingDependencies.isEmpty,
            "Too many dependencies have been extracted for:\n"+
                remainingDependencies.mkString("\n"))
    }
}

