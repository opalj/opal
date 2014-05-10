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
package dependency

import reader.Java8Framework.ClassFiles
import DependencyType._

import org.scalatest.FunSuite

import java.net.URL

/**
 * Tests that the dependency extractor does not miss some dependencies and
 * that it does not extract "unexpected" dependencies.
 *
 * @author Thomas Schlosser
 * @author Michael Eichberg
 */
@org.junit.runner.RunWith(classOf[org.scalatest.junit.JUnitRunner])
class DependencyExtractorTest extends FunSuite {

    import DependencyType._

    //
    // THE TEST CODE
    //

    test("Dependency extraction") {

        var dependencies: Map[(String, String, DependencyType), Int] =
            DependencyExtractorFixture.extractDependencies(
                "ext/dependencies",
                "classfiles/Dependencies.jar",
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
        assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.IllegalStateException", LOCAL_VARIABLE_TYPE)
        //    	    }
        //    	} catch (Exception e) {
        assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Exception", CATCHES)
        assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Exception", LOCAL_VARIABLE_TYPE)
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
        assertImplicitThisLocalVariable("dependencies.SignatureTestClass.m1()")
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
        assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.io.InputStream", LOCAL_VARIABLE_TYPE)
        assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.lang.Object", PARAMETER_TYPE)
        assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.lang.Object", LOCAL_VARIABLE_TYPE)
        assertImplicitThisLocalVariable("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)")
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
        assertImplicitThisLocalVariable("dependencies.SignatureTestClass.m3()")
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
        assertImplicitThisLocalVariable("dependencies.SignatureTestClass.m4()")
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
        assertImplicitThisLocalVariable("dependencies.SignatureTestClass.m5()")
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
        assertDependency("dependencies.SignatureTestSubClass.m3()", "dependencies.SignatureTestSubClass", INSTANCE_MEMBER)
        assertDependency("dependencies.SignatureTestSubClass.m3()", "java.lang.Integer", RETURN_TYPE)
        // // implicit method:
        // // public Object m3(){
        // //     return m3(); //Method m3:()Ljava/lang/Integer;
        // // }
        assertDependency("dependencies.SignatureTestSubClass.m3()", "dependencies.SignatureTestSubClass", INSTANCE_MEMBER)
        assertDependency("dependencies.SignatureTestSubClass.m3()", "java.lang.Object", RETURN_TYPE)
        assertImplicitThisLocalVariable("dependencies.SignatureTestSubClass.m3()")
        assertDependency("dependencies.SignatureTestSubClass.m3()", "dependencies.SignatureTestSubClass", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.SignatureTestSubClass.m3()", "dependencies.SignatureTestSubClass.m3()", CALLS_METHOD)
        assertDependency("dependencies.SignatureTestSubClass.m3()", "java.lang.Integer", RETURN_TYPE_OF_CALLED_METHOD)
        //
        //        @SuppressWarnings("unchecked")
        //        public abstract FileOutputStream m5();
        assertDependency("dependencies.SignatureTestSubClass.m5()", "dependencies.SignatureTestSubClass", INSTANCE_MEMBER)
        assertDependency("dependencies.SignatureTestSubClass.m5()", "java.io.FileOutputStream", RETURN_TYPE)
        // // implicit method:
        // // public OutputStream m5(){
        // //     return m5(); //Method m5:()Ljava/io/FileOutputStream;
        // // }
        assertDependency("dependencies.SignatureTestSubClass.m5()", "dependencies.SignatureTestSubClass", INSTANCE_MEMBER)
        assertDependency("dependencies.SignatureTestSubClass.m5()", "java.io.OutputStream", RETURN_TYPE)
        assertImplicitThisLocalVariable("dependencies.SignatureTestSubClass.m5()")
        assertDependency("dependencies.SignatureTestSubClass.m5()", "dependencies.SignatureTestSubClass", DECLARING_CLASS_OF_CALLED_METHOD)
        assertDependency("dependencies.SignatureTestSubClass.m5()", "dependencies.SignatureTestSubClass.m5()", CALLS_METHOD)
        assertDependency("dependencies.SignatureTestSubClass.m5()", "java.io.FileOutputStream", RETURN_TYPE_OF_CALLED_METHOD)
        //    }

        val remainingDependencies = dependencies.view.filter(_._2 > 0)
        assert(remainingDependencies.isEmpty,
            "Too many dependencies have been extracted for:\n"+
                remainingDependencies.mkString("\n"))
    }
}

