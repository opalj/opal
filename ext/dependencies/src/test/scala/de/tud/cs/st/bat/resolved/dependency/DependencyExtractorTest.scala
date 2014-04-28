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

import reader.Java7Framework.ClassFiles
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

    test("Dependency extraction") {

        var dependencies: List[(String, String, DependencyType)] = Nil

        var nodes = new scala.collection.mutable.ArrayBuffer[String](1000)

        object SourceElementIDsProvider extends SourceElementIDs {

            val FIELD_AND_METHOD_SEPARATOR = "."

            def sourceElementID(identifier: String): Int = {
                var index = nodes.indexOf(identifier)
                if (index == -1) {
                    nodes += identifier
                    index = nodes.length - 1
                }
                index
            }

            def sourceElementID(t: Type): Int =
                sourceElementID(getNameOfUnderlyingType(t))

            def sourceElementID(definingObjectType: ObjectType, fieldName: String): Int =
                sourceElementID(getNameOfUnderlyingType(definingObjectType) + FIELD_AND_METHOD_SEPARATOR + fieldName)

            def sourceElementID(definingReferenceType: ReferenceType, methodName: String, methodDescriptor: MethodDescriptor): Int =
                sourceElementID(getNameOfUnderlyingType(definingReferenceType) + FIELD_AND_METHOD_SEPARATOR + getMethodAsName(methodName, methodDescriptor))

            private def getMethodAsName(methodName: String, methodDescriptor: MethodDescriptor): String = {
                methodName+"("+methodDescriptor.parameterTypes.map(pT ⇒ getNameOfUnderlyingType(pT)).mkString(", ")+")"
            }

            private def getNameOfUnderlyingType(obj: Type): String =
                if (obj.isArrayType)
                    obj.asInstanceOf[ArrayType].elementType.toJava
                else
                    obj.toJava
        }

        val dependencyExtractor = new DependencyExtractor(SourceElementIDsProvider) with NoSourceElementsVisitor {
            def processDependency(src: Int, trgt: Int, dType: DependencyType) {
                val srcNode = nodes(src)
                val trgtNode = nodes(trgt)
                dependencies = (srcNode, trgtNode, dType) :: dependencies
            }
        }

        def assertDependency(src: String, trgt: String, dType: DependencyType) {
            val dependency = (src, trgt, dType)
            if (dependencies.contains(dependency)) {
                dependencies = dependencies diff List(dependency)
            } else {
                throw new AssertionError("Dependency "+dependency+" was not extracted successfully!\nRemaining dependencies:\n"+dependencies.mkString("\n"))
            }
        }

        // extract dependencies
        for (cs @ (classFile, _) ← ClassFiles(TestSupport.locateTestResources("classfiles/Dependencies.jar", "ext/dependencies"))) {
            dependencyExtractor.process(classFile)
        }

        def assertTestClass() {
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
            assertDependency("dependencies.TestClass.testMethod()", "dependencies.TestClass", IS_INSTANCE_MEMBER)
            assertImplicitThisLocalVariable("dependencies.TestClass.testMethod()")
            // NOTE: It is not possible to determine dependencies to types that are erased; e.g., the type "String" is erased at compile time and it is not possible to extract this dependency.
            // List<? extends CharSequence> list = new ArrayList<String>();
            assertDependency("dependencies.TestClass.testMethod()", "java.util.List", HAS_LOCAL_VARIABLE_OF_TYPE)
            assertDependency("dependencies.TestClass.testMethod()", "java.lang.CharSequence", USES_TYPE_IN_TYPE_PARAMETERS)
            assertDependency("dependencies.TestClass.testMethod()", "java.util.ArrayList", CREATES)
            assertDependency("dependencies.TestClass.testMethod()", "java.util.ArrayList", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.TestClass.testMethod()", "java.util.ArrayList.<init>()", CALLS_METHOD)
            //    	list.add(null);
            assertDependency("dependencies.TestClass.testMethod()", "java.util.List", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.TestClass.testMethod()", "java.util.List.add(java.lang.Object)", CALLS_INTERFACE_METHOD)
            assertDependency("dependencies.TestClass.testMethod()", "java.lang.Object", USES_PARAMETER_TYPE)
            //        }
            //
            //        public String testMethod(Integer i, int j) {
            assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "dependencies.TestClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer", HAS_PARAMETER_OF_TYPE)
            assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer", HAS_LOCAL_VARIABLE_OF_TYPE)
            assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.String", RETURNS)
            assertImplicitThisLocalVariable("dependencies.TestClass.testMethod(java.lang.Integer, int)")
            //    	if (i != null && i.intValue() > j) {
            assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer.intValue()", CALLS_METHOD)
            //    	    return i.toString();
            assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer.toString()", CALLS_METHOD)
            assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.String", USES_RETURN_TYPE)
            //    	}
            //    	return String.valueOf(j);
            assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.String", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.String.valueOf(int)", CALLS_METHOD)
            assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.String", USES_RETURN_TYPE)
            //        }
            //    }
        }

        def assertTestInterface() {
            //    package dependencies;
            //
            //    public interface TestInterface {
            assertDependency("dependencies.TestInterface", "java.lang.Object", EXTENDS)
            //        void testMethod();
            assertDependency("dependencies.TestInterface.testMethod()", "dependencies.TestInterface", IS_INSTANCE_MEMBER)
            //
            //        String testMethod(Integer i, int j);
            assertDependency("dependencies.TestInterface.testMethod(java.lang.Integer, int)", "dependencies.TestInterface", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.TestInterface.testMethod(java.lang.Integer, int)", "java.lang.Integer", HAS_PARAMETER_OF_TYPE)
            assertDependency("dependencies.TestInterface.testMethod(java.lang.Integer, int)", "java.lang.String", RETURNS)
            //    }
        }

        def assertMarkerInterface() {
            //    package dependencies.sub;
            //
            //    public interface MarkerInterface {
            assertDependency("dependencies.sub.MarkerInterface", "java.lang.Object", EXTENDS)
            //
            //    }
        }

        def assertDeprecatedInterface() {
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
            assertDependency("dependencies.sub.DeprecatedInterface.deprecatedMethod()", "dependencies.sub.DeprecatedInterface", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.sub.DeprecatedInterface.deprecatedMethod()", "java.lang.Deprecated", ANNOTATED_WITH)
            //
            //        public void methodDeprParam(@Deprecated int i);
            assertDependency("dependencies.sub.DeprecatedInterface.methodDeprParam(int)", "dependencies.sub.DeprecatedInterface", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.sub.DeprecatedInterface.methodDeprParam(int)", "java.lang.Deprecated", PARAMETER_ANNOTATED_WITH)
            //    }
        }

        def assertFieldsClass() {
            //    package dependencies;
            //
            //    public class FieldsClass {
            assertDependency("dependencies.FieldsClass", "java.lang.Object", EXTENDS)
            assertImplicitDefaultConstructor("dependencies.FieldsClass")
            //        public final static String CONSTANT = "constant";
            assertDependency("dependencies.FieldsClass.CONSTANT", "dependencies.FieldsClass", IS_CLASS_MEMBER)
            assertDependency("dependencies.FieldsClass.CONSTANT", "java.lang.String", IS_OF_TYPE)
            assertDependency("dependencies.FieldsClass.CONSTANT", "java.lang.String", USES_CONSTANT_VALUE_OF_TYPE)
            //        private Integer i;
            assertDependency("dependencies.FieldsClass.i", "dependencies.FieldsClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.FieldsClass.i", "java.lang.Integer", IS_OF_TYPE)
            //
            //        @Deprecated
            //        protected int deprecatedField;
            assertDependency("dependencies.FieldsClass.deprecatedField", "dependencies.FieldsClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.FieldsClass.deprecatedField", "java.lang.Deprecated", ANNOTATED_WITH)
            //
            //        private Integer readField() {
            assertDependency("dependencies.FieldsClass.readField()", "dependencies.FieldsClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.FieldsClass.readField()", "java.lang.Integer", RETURNS)
            assertImplicitThisLocalVariable("dependencies.FieldsClass.readField()")
            //    	return i;
            assertDependency("dependencies.FieldsClass.readField()", "dependencies.FieldsClass", USES_FIELD_DECLARING_TYPE)
            assertDependency("dependencies.FieldsClass.readField()", "dependencies.FieldsClass.i", READS_FIELD)
            assertDependency("dependencies.FieldsClass.readField()", "java.lang.Integer", USES_FIELD_READ_TYPE)
            //        }
            //
            //        private void writeField(Integer j) {
            assertDependency("dependencies.FieldsClass.writeField(java.lang.Integer)", "dependencies.FieldsClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.FieldsClass.writeField(java.lang.Integer)", "java.lang.Integer", HAS_PARAMETER_OF_TYPE)
            assertDependency("dependencies.FieldsClass.writeField(java.lang.Integer)", "java.lang.Integer", HAS_LOCAL_VARIABLE_OF_TYPE)
            assertImplicitThisLocalVariable("dependencies.FieldsClass.writeField(java.lang.Integer)")
            //    	i = j;
            assertDependency("dependencies.FieldsClass.writeField(java.lang.Integer)", "dependencies.FieldsClass", USES_FIELD_DECLARING_TYPE)
            assertDependency("dependencies.FieldsClass.writeField(java.lang.Integer)", "dependencies.FieldsClass.i", WRITES_FIELD)
            assertDependency("dependencies.FieldsClass.writeField(java.lang.Integer)", "java.lang.Integer", USES_FIELD_WRITE_TYPE)
            //        }
            //
            //        public Integer readWrite(Integer j) {
            assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "dependencies.FieldsClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", HAS_PARAMETER_OF_TYPE)
            assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", HAS_LOCAL_VARIABLE_OF_TYPE)
            assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", RETURNS)
            assertImplicitThisLocalVariable("dependencies.FieldsClass.readWrite(java.lang.Integer)")
            //    	Integer result = readField();
            assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", HAS_LOCAL_VARIABLE_OF_TYPE)
            assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "dependencies.FieldsClass", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "dependencies.FieldsClass.readField()", CALLS_METHOD)
            assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", USES_RETURN_TYPE)
            //    	writeField(j);
            assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "dependencies.FieldsClass", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "dependencies.FieldsClass.writeField(java.lang.Integer)", CALLS_METHOD)
            assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", USES_PARAMETER_TYPE)
            //    	return result;
            //        }
            //    }
        }

        def assertOuterAndInnerClass() {
            //    package dependencies;
            //
            //    public class OuterClass {
            assertDependency("dependencies.OuterClass", "java.lang.Object", EXTENDS)
            assertImplicitDefaultConstructor("dependencies.OuterClass")
            //        class InnerClass {
            assertDependency("dependencies.OuterClass$InnerClass", "java.lang.Object", EXTENDS)
            assertDependency("dependencies.OuterClass$InnerClass", "dependencies.OuterClass", IS_ENCLOSED)
            //            //implicit field:
            assertDependency("dependencies.OuterClass$InnerClass.this$0", "dependencies.OuterClass$InnerClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.OuterClass$InnerClass.this$0", "dependencies.OuterClass", IS_OF_TYPE)
            //            public InnerClass(Integer i) {
            assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "dependencies.OuterClass$InnerClass", IS_INSTANCE_MEMBER)
            //            //implicit constructor parameter:
            assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "dependencies.OuterClass", HAS_PARAMETER_OF_TYPE)
            assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "java.lang.Integer", HAS_PARAMETER_OF_TYPE)
            assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "java.lang.Integer", HAS_LOCAL_VARIABLE_OF_TYPE)
            assertImplicitThisLocalVariable("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)")

            assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "java.lang.Object", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "java.lang.Object.<init>()", CALLS_METHOD)
            //            // write to implicit field:
            assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "dependencies.OuterClass$InnerClass", USES_FIELD_DECLARING_TYPE)
            assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "dependencies.OuterClass$InnerClass.this$0", WRITES_FIELD)
            assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "dependencies.OuterClass", USES_FIELD_WRITE_TYPE)
            //            }
            //        }
            //    }
        }

        def assertEnclosingMethodAndInnerClass() {
            //    package dependencies;
            //
            //    public class EnclosingMethodClass {
            assertDependency("dependencies.EnclosingMethodClass", "java.lang.Object", EXTENDS)
            assertImplicitDefaultConstructor("dependencies.EnclosingMethodClass")
            //  //implicit field definition in the default constructor
            assertDependency("dependencies.EnclosingMethodClass.<init>()", "dependencies.EnclosingMethodClass$1", CREATES)
            assertDependency("dependencies.EnclosingMethodClass.<init>()", "dependencies.EnclosingMethodClass$1", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.EnclosingMethodClass.<init>()", "dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)", CALLS_METHOD)
            assertDependency("dependencies.EnclosingMethodClass.<init>()", "dependencies.EnclosingMethodClass", USES_PARAMETER_TYPE)
            assertDependency("dependencies.EnclosingMethodClass.<init>()", "dependencies.EnclosingMethodClass", USES_FIELD_DECLARING_TYPE)
            assertDependency("dependencies.EnclosingMethodClass.<init>()", "dependencies.EnclosingMethodClass.enclosingField", WRITES_FIELD)
            assertDependency("dependencies.EnclosingMethodClass.<init>()", "java.lang.Object", USES_FIELD_WRITE_TYPE)
            //  //implicit field definition in the class initialization method
            assertDependency("dependencies.EnclosingMethodClass.<clinit>()", "dependencies.EnclosingMethodClass", IS_CLASS_MEMBER)
            assertDependency("dependencies.EnclosingMethodClass.<clinit>()", "dependencies.EnclosingMethodClass$2", CREATES)
            assertDependency("dependencies.EnclosingMethodClass.<clinit>()", "dependencies.EnclosingMethodClass$2", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.EnclosingMethodClass.<clinit>()", "dependencies.EnclosingMethodClass$2.<init>()", CALLS_METHOD)
            assertDependency("dependencies.EnclosingMethodClass.<clinit>()", "dependencies.EnclosingMethodClass", USES_FIELD_DECLARING_TYPE)
            assertDependency("dependencies.EnclosingMethodClass.<clinit>()", "dependencies.EnclosingMethodClass.staticEnclosingField", WRITES_FIELD)
            assertDependency("dependencies.EnclosingMethodClass.<clinit>()", "java.lang.Object", USES_FIELD_WRITE_TYPE)
            //
            //        public Object enclosingField = new Object() {
            assertDependency("dependencies.EnclosingMethodClass.enclosingField", "dependencies.EnclosingMethodClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.EnclosingMethodClass.enclosingField", "java.lang.Object", IS_OF_TYPE)
            //        };
            assertDependency("dependencies.EnclosingMethodClass$1", "java.lang.Object", EXTENDS)
            assertDependency("dependencies.EnclosingMethodClass$1", "dependencies.EnclosingMethodClass", IS_ENCLOSED)
            //	//implicit field:
            assertDependency("dependencies.EnclosingMethodClass$1.this$0", "dependencies.EnclosingMethodClass$1", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.EnclosingMethodClass$1.this$0", "dependencies.EnclosingMethodClass", IS_OF_TYPE)
            //	//implicit constructor:
            assertDependency("dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass$1", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass", HAS_PARAMETER_OF_TYPE)
            assertImplicitThisLocalVariable("dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)")
            assertDependency("dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)", "java.lang.Object", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)", "java.lang.Object.<init>()", CALLS_METHOD)
            //	// write to implicit field:
            assertDependency("dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass$1", USES_FIELD_DECLARING_TYPE)
            assertDependency("dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass$1.this$0", WRITES_FIELD)
            assertDependency("dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass", USES_FIELD_WRITE_TYPE)
            //
            //        public static Object staticEnclosingField = new Object() {
            assertDependency("dependencies.EnclosingMethodClass.staticEnclosingField", "dependencies.EnclosingMethodClass", IS_CLASS_MEMBER)
            assertDependency("dependencies.EnclosingMethodClass.staticEnclosingField", "java.lang.Object", IS_OF_TYPE)
            //        };
            assertDependency("dependencies.EnclosingMethodClass$2", "java.lang.Object", EXTENDS)
            assertDependency("dependencies.EnclosingMethodClass$2", "dependencies.EnclosingMethodClass", IS_ENCLOSED)
            //	//implicit constructor:
            assertDependency("dependencies.EnclosingMethodClass$2.<init>()", "dependencies.EnclosingMethodClass$2", IS_INSTANCE_MEMBER)
            assertImplicitThisLocalVariable("dependencies.EnclosingMethodClass$2.<init>()")
            assertDependency("dependencies.EnclosingMethodClass$2.<init>()", "java.lang.Object", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.EnclosingMethodClass$2.<init>()", "java.lang.Object.<init>()", CALLS_METHOD)
            //
            //        public void enclosingMethod() {
            assertDependency("dependencies.EnclosingMethodClass.enclosingMethod()", "dependencies.EnclosingMethodClass", IS_INSTANCE_MEMBER)
            assertImplicitThisLocalVariable("dependencies.EnclosingMethodClass.enclosingMethod()")
            //    	new Object() {
            assertDependency("dependencies.EnclosingMethodClass$3", "java.lang.Object", EXTENDS)
            assertDependency("dependencies.EnclosingMethodClass$3", "dependencies.EnclosingMethodClass.enclosingMethod()", IS_ENCLOSED)
            //	//implicit field:
            assertDependency("dependencies.EnclosingMethodClass$3.this$0", "dependencies.EnclosingMethodClass$3", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.EnclosingMethodClass$3.this$0", "dependencies.EnclosingMethodClass", IS_OF_TYPE)
            //	//implicit constructor:
            assertDependency("dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass$3", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass", HAS_PARAMETER_OF_TYPE)
            assertImplicitThisLocalVariable("dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)")
            assertDependency("dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)", "java.lang.Object", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)", "java.lang.Object.<init>()", CALLS_METHOD)
            //	// write to implicit field:
            assertDependency("dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass$3", USES_FIELD_DECLARING_TYPE)
            assertDependency("dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass$3.this$0", WRITES_FIELD)
            assertDependency("dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass", USES_FIELD_WRITE_TYPE)
            //    	    public void innerMethod() {
            assertDependency("dependencies.EnclosingMethodClass$3.innerMethod()", "dependencies.EnclosingMethodClass$3", IS_INSTANCE_MEMBER)
            assertImplicitThisLocalVariable("dependencies.EnclosingMethodClass$3.innerMethod()")
            //    	    }
            //    	}.innerMethod();
            assertDependency("dependencies.EnclosingMethodClass.enclosingMethod()", "dependencies.EnclosingMethodClass$3", CREATES)
            assertDependency("dependencies.EnclosingMethodClass.enclosingMethod()", "dependencies.EnclosingMethodClass$3", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.EnclosingMethodClass.enclosingMethod()", "dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)", CALLS_METHOD)
            assertDependency("dependencies.EnclosingMethodClass.enclosingMethod()", "dependencies.EnclosingMethodClass", USES_PARAMETER_TYPE) // method parameter

            assertDependency("dependencies.EnclosingMethodClass.enclosingMethod()", "dependencies.EnclosingMethodClass$3", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.EnclosingMethodClass.enclosingMethod()", "dependencies.EnclosingMethodClass$3.innerMethod()", CALLS_METHOD)
            //        }
            //    }
        }

        def assertExceptionTestClass() {
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
            assertDependency("dependencies.ExceptionTestClass.testMethod()", "dependencies.ExceptionTestClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.ExceptionTestClass.testMethod()", "java.lang.IllegalStateException", THROWS)
            assertDependency("dependencies.ExceptionTestClass.testMethod()", "javax.naming.OperationNotSupportedException", THROWS)
            assertImplicitThisLocalVariable("dependencies.ExceptionTestClass.testMethod()")
            //    	throw new FormatterClosedException();
            assertDependency("dependencies.ExceptionTestClass.testMethod()", "java.util.FormatterClosedException", CREATES)
            assertDependency("dependencies.ExceptionTestClass.testMethod()", "java.util.FormatterClosedException", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.ExceptionTestClass.testMethod()", "java.util.FormatterClosedException.<init>()", CALLS_METHOD)
            //        }
            //
            //        public void catchMethod() {
            assertDependency("dependencies.ExceptionTestClass.catchMethod()", "dependencies.ExceptionTestClass", IS_INSTANCE_MEMBER)
            assertImplicitThisLocalVariable("dependencies.ExceptionTestClass.catchMethod()")
            //    	try {
            //    	    try {
            //    		testMethod();
            assertDependency("dependencies.ExceptionTestClass.catchMethod()", "dependencies.ExceptionTestClass", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.ExceptionTestClass.catchMethod()", "dependencies.ExceptionTestClass.testMethod()", CALLS_METHOD)
            //    	    } catch (IllegalStateException e) {
            assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.IllegalStateException", CATCHES)
            assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.IllegalStateException", HAS_LOCAL_VARIABLE_OF_TYPE)
            //    	    }
            //    	} catch (Exception e) {
            assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Exception", CATCHES)
            assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Exception", HAS_LOCAL_VARIABLE_OF_TYPE)
            //    	} finally{
            //    	    Integer.valueOf(42);
            assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer.valueOf(int)", CALLS_METHOD)
            assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer", USES_RETURN_TYPE)
            // // The next six dependencies result from required special handling of the finally block.
            // // Depending on the way the finally block were reached it has to throw an Exception or return normally.
            // // Hence, the bytecode contains the three versions of the finally block which results in multiple
            // // dependencies to types/methods/fields used in the finally block.
            assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer.valueOf(int)", CALLS_METHOD)
            assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer", USES_RETURN_TYPE)
            assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer.valueOf(int)", CALLS_METHOD)
            assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer", USES_RETURN_TYPE)
            //            }
            //        }
            //    }
        }

        def assertTestAnnotation() {
            //    package dependencies;
            //
            //    import java.lang.annotation.ElementType;
            //
            //    public @interface TestAnnotation {
            assertDependency("dependencies.TestAnnotation", "java.lang.Object", EXTENDS)
            assertDependency("dependencies.TestAnnotation", "java.lang.annotation.Annotation", IMPLEMENTS)
            //        public abstract String stringValue() default "default";
            assertDependency("dependencies.TestAnnotation.stringValue()", "dependencies.TestAnnotation", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.TestAnnotation.stringValue()", "java.lang.String", RETURNS)
            //
            //        public abstract Class<?> classValue() default String.class;
            assertDependency("dependencies.TestAnnotation.classValue()", "dependencies.TestAnnotation", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.TestAnnotation.classValue()", "java.lang.Class", RETURNS)
            assertDependency("dependencies.TestAnnotation.classValue()", "java.lang.String", USES_DEFAULT_CLASS_VALUE_TYPE)
            //
            //        public abstract ElementType enumValue() default ElementType.TYPE;
            assertDependency("dependencies.TestAnnotation.enumValue()", "dependencies.TestAnnotation", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.TestAnnotation.enumValue()", "java.lang.annotation.ElementType", RETURNS)
            assertDependency("dependencies.TestAnnotation.enumValue()", "java.lang.annotation.ElementType", USES_DEFAULT_ENUM_VALUE_TYPE)
            assertDependency("dependencies.TestAnnotation.enumValue()", "java.lang.annotation.ElementType.TYPE", USES_ENUM_VALUE)
            //
            //        public abstract SuppressWarnings annotationValue() default @SuppressWarnings("default");
            assertDependency("dependencies.TestAnnotation.annotationValue()", "dependencies.TestAnnotation", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.TestAnnotation.annotationValue()", "java.lang.SuppressWarnings", RETURNS)
            assertDependency("dependencies.TestAnnotation.annotationValue()", "java.lang.SuppressWarnings", USES_DEFAULT_ANNOTATION_VALUE_TYPE)
            //
            //        public abstract Class<?>[] arrayClassValue() default { String.class,
            //    	    Integer.class };
            assertDependency("dependencies.TestAnnotation.arrayClassValue()", "dependencies.TestAnnotation", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.TestAnnotation.arrayClassValue()", "java.lang.Class", RETURNS)
            assertDependency("dependencies.TestAnnotation.arrayClassValue()", "java.lang.String", USES_DEFAULT_CLASS_VALUE_TYPE)
            assertDependency("dependencies.TestAnnotation.arrayClassValue()", "java.lang.Integer", USES_DEFAULT_CLASS_VALUE_TYPE)
            //    }
        }

        def assertAnnotationDefaultAttributeTestClass() {
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
            assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "dependencies.AnnotationDefaultAttributeTestClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "dependencies.TestAnnotation", ANNOTATED_WITH)
            assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.Integer", USES_DEFAULT_CLASS_VALUE_TYPE)
            assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.annotation.ElementType", USES_DEFAULT_ENUM_VALUE_TYPE)
            assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.annotation.ElementType.METHOD", USES_ENUM_VALUE)
            assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.SuppressWarnings", USES_DEFAULT_ANNOTATION_VALUE_TYPE)
            assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.Long", USES_DEFAULT_CLASS_VALUE_TYPE)
            assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.Boolean", USES_DEFAULT_CLASS_VALUE_TYPE)
            assertImplicitThisLocalVariable("dependencies.AnnotationDefaultAttributeTestClass.testMethod()")
            //        }
            //    }
        }

        def assertInstructionsTestClass() {
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
            assertDependency("dependencies.InstructionsTestClass.field", "dependencies.InstructionsTestClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.InstructionsTestClass.field", "java.lang.Object", IS_OF_TYPE)
            //        public static InputStream staticField;
            assertDependency("dependencies.InstructionsTestClass.staticField", "dependencies.InstructionsTestClass", IS_CLASS_MEMBER)
            assertDependency("dependencies.InstructionsTestClass.staticField", "java.io.InputStream", IS_OF_TYPE)
            //
            //        public void method() {
            assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass", IS_INSTANCE_MEMBER)
            assertImplicitThisLocalVariable("dependencies.InstructionsTestClass.method()")
            //    	// NEW and INVOKESPECIAL (constructor call)
            //    	Object obj = new Object();
            assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object", HAS_LOCAL_VARIABLE_OF_TYPE)
            assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object", CREATES)
            assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object.<init>()", CALLS_METHOD)
            assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object", USES_METHOD_DECLARING_TYPE)
            //    	FilterInputStream stream = null;
            assertDependency("dependencies.InstructionsTestClass.method()", "java.io.FilterInputStream", HAS_LOCAL_VARIABLE_OF_TYPE)
            //    	// ANEWARRAY
            //    	obj = new Long[1];
            assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Long", CREATES_ARRAY_OF_TYPE)
            //    	// MULTIANEWARRAY
            //    	obj = new Integer[1][];
            assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Integer", CREATES_ARRAY_OF_TYPE)
            //
            //    	// PUTFIELD
            //    	field = obj;
            assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass", USES_FIELD_DECLARING_TYPE)
            assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass.field", WRITES_FIELD)
            assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object", USES_FIELD_WRITE_TYPE)
            //    	// GETFIELD
            //    	obj = field;
            assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass", USES_FIELD_DECLARING_TYPE)
            assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass.field", READS_FIELD)
            assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object", USES_FIELD_READ_TYPE)
            //    	// INSTANCEOF
            //    	if (obj instanceof ZipInputStream) {
            assertDependency("dependencies.InstructionsTestClass.method()", "java.util.zip.ZipInputStream", CHECKS_INSTANCEOF)
            //    	    // CHECKCAST
            //    	    stream = (InflaterInputStream) obj;
            assertDependency("dependencies.InstructionsTestClass.method()", "java.util.zip.InflaterInputStream", CASTS_INTO)
            //    	    // PUTSTATIC
            //    	    staticField = stream;
            assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass", USES_FIELD_DECLARING_TYPE)
            assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass.staticField", WRITES_FIELD)
            assertDependency("dependencies.InstructionsTestClass.method()", "java.io.InputStream", USES_FIELD_WRITE_TYPE)
            //    	    // GETSTATIC
            //    	    obj = staticField;
            assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass", USES_FIELD_DECLARING_TYPE)
            assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass.staticField", READS_FIELD)
            assertDependency("dependencies.InstructionsTestClass.method()", "java.io.InputStream", USES_FIELD_READ_TYPE)
            //    	}
            //
            //    	// INVOKESTATIC
            //    	System.currentTimeMillis();
            assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.System", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.System.currentTimeMillis()", CALLS_METHOD)
            //
            //    	TestInterface ti = new TestClass();
            assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.TestInterface", HAS_LOCAL_VARIABLE_OF_TYPE)
            assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.TestClass", CREATES)
            assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.TestClass", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.TestClass.<init>()", CALLS_METHOD)
            //    	// INVOKEINTERFACE
            //    	ti.testMethod();
            assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.TestInterface", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.TestInterface.testMethod()", CALLS_INTERFACE_METHOD)
            //
            //    	// INVOKEVIRTUAL
            //    	obj.equals(stream);
            assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object.equals(java.lang.Object)", CALLS_METHOD)
            assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object", USES_PARAMETER_TYPE)
            //        }
            //    }
        }

        def assertSignatureTestInterface() {
            //    package dependencies;
            //
            //    import java.io.InputStream;
            //    import java.io.OutputStream;
            //
            //    public interface SignatureTestInterface<T extends InputStream, Z> {
            assertDependency("dependencies.SignatureTestInterface", "java.lang.Object", EXTENDS)
            assertDependency("dependencies.SignatureTestInterface", "java.io.InputStream", USES_TYPE_IN_TYPE_PARAMETERS)
            assertDependency("dependencies.SignatureTestInterface", "java.lang.Object", USES_TYPE_IN_TYPE_PARAMETERS)
            //
            //        public T m1();
            assertDependency("dependencies.SignatureTestInterface.m1()", "dependencies.SignatureTestInterface", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.SignatureTestInterface.m1()", "java.io.InputStream", RETURNS)
            //
            //        public void m2(T t, Z z);
            assertDependency("dependencies.SignatureTestInterface.m2(java.io.InputStream, java.lang.Object)", "dependencies.SignatureTestInterface", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.SignatureTestInterface.m2(java.io.InputStream, java.lang.Object)", "java.io.InputStream", HAS_PARAMETER_OF_TYPE)
            assertDependency("dependencies.SignatureTestInterface.m2(java.io.InputStream, java.lang.Object)", "java.lang.Object", HAS_PARAMETER_OF_TYPE)
            //
            //        public <W> W m3();
            assertDependency("dependencies.SignatureTestInterface.m3()", "dependencies.SignatureTestInterface", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.SignatureTestInterface.m3()", "java.lang.Object", USES_TYPE_IN_TYPE_PARAMETERS)
            assertDependency("dependencies.SignatureTestInterface.m3()", "java.lang.Object", RETURNS)
            //
            //        public <W extends T> W m4();
            assertDependency("dependencies.SignatureTestInterface.m4()", "dependencies.SignatureTestInterface", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.SignatureTestInterface.m4()", "java.io.InputStream", RETURNS)
            //
            //        public <W extends OutputStream> W m5();
            assertDependency("dependencies.SignatureTestInterface.m5()", "dependencies.SignatureTestInterface", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.SignatureTestInterface.m5()", "java.io.OutputStream", USES_TYPE_IN_TYPE_PARAMETERS)
            assertDependency("dependencies.SignatureTestInterface.m5()", "java.io.OutputStream", RETURNS)
            //    }
        }

        def assertSignatureTestClass() {
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
            assertDependency("dependencies.SignatureTestClass", "java.io.FilterInputStream", USES_TYPE_IN_TYPE_PARAMETERS)
            assertDependency("dependencies.SignatureTestClass", "java.lang.String", USES_TYPE_IN_TYPE_PARAMETERS)
            assertDependency("dependencies.SignatureTestClass", "dependencies.SignatureTestInterface", IMPLEMENTS)
            assertImplicitDefaultConstructor("dependencies.SignatureTestClass")
            //
            //        protected Q f1;
            assertDependency("dependencies.SignatureTestClass.f1", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.SignatureTestClass.f1", "java.io.FilterInputStream", IS_OF_TYPE)
            //
            //        protected List<Long> f2;
            assertDependency("dependencies.SignatureTestClass.f2", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.SignatureTestClass.f2", "java.util.List", IS_OF_TYPE)
            assertDependency("dependencies.SignatureTestClass.f2", "java.lang.Long", USES_TYPE_IN_TYPE_PARAMETERS)
            //
            //        public abstract Q m1();
            assertDependency("dependencies.SignatureTestClass.m1()", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.SignatureTestClass.m1()", "java.io.FilterInputStream", RETURNS)
            // // implicit method:
            // // public InputStream m1(){
            // //     return m1(); //Method m1:()Ljava/io/FilterInputStream;
            // // }
            assertDependency("dependencies.SignatureTestClass.m1()", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.SignatureTestClass.m1()", "java.io.InputStream", RETURNS)
            assertImplicitThisLocalVariable("dependencies.SignatureTestClass.m1()")
            assertDependency("dependencies.SignatureTestClass.m1()", "dependencies.SignatureTestClass", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.SignatureTestClass.m1()", "dependencies.SignatureTestClass.m1()", CALLS_METHOD)
            assertDependency("dependencies.SignatureTestClass.m1()", "java.io.FilterInputStream", USES_RETURN_TYPE)
            //
            //        public abstract void m2(Q t, String z);
            assertDependency("dependencies.SignatureTestClass.m2(java.io.FilterInputStream, java.lang.String)", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.SignatureTestClass.m2(java.io.FilterInputStream, java.lang.String)", "java.io.FilterInputStream", HAS_PARAMETER_OF_TYPE)
            assertDependency("dependencies.SignatureTestClass.m2(java.io.FilterInputStream, java.lang.String)", "java.lang.String", HAS_PARAMETER_OF_TYPE)
            // // implicit method:
            // // public void m2(java.io.InputStream t, java.lang.Object z){
            // //     return m2((java.io.FileInputStream)t, (java.lang.String) z);
            // // }
            assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.io.InputStream", HAS_PARAMETER_OF_TYPE)
            assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.io.InputStream", HAS_LOCAL_VARIABLE_OF_TYPE)
            assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.lang.Object", HAS_PARAMETER_OF_TYPE)
            assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.lang.Object", HAS_LOCAL_VARIABLE_OF_TYPE)
            assertImplicitThisLocalVariable("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)")
            assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.io.FilterInputStream", CASTS_INTO)
            assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.lang.String", CASTS_INTO)
            assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "dependencies.SignatureTestClass", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "dependencies.SignatureTestClass.m2(java.io.FilterInputStream, java.lang.String)", CALLS_METHOD)
            assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.io.FilterInputStream", USES_PARAMETER_TYPE)
            assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.lang.String", USES_PARAMETER_TYPE)
            //
            //        @SuppressWarnings("unchecked")
            //        public abstract Integer m3();
            assertDependency("dependencies.SignatureTestClass.m3()", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.SignatureTestClass.m3()", "java.lang.Integer", RETURNS)
            // // implicit method:
            // // public Object m3(){
            // //     return m3(); //Method m3:()Ljava/lang/Integer;
            // // }
            assertDependency("dependencies.SignatureTestClass.m3()", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.SignatureTestClass.m3()", "java.lang.Object", RETURNS)
            assertImplicitThisLocalVariable("dependencies.SignatureTestClass.m3()")
            assertDependency("dependencies.SignatureTestClass.m3()", "dependencies.SignatureTestClass", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.SignatureTestClass.m3()", "dependencies.SignatureTestClass.m3()", CALLS_METHOD)
            assertDependency("dependencies.SignatureTestClass.m3()", "java.lang.Integer", USES_RETURN_TYPE)
            //
            //        public abstract Q m4();
            assertDependency("dependencies.SignatureTestClass.m4()", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.SignatureTestClass.m4()", "java.io.FilterInputStream", RETURNS)
            // // implicit method:
            // // public InputStream m4(){
            // //     return m4(); //Method m4:()Ljava/io/FilterInputStream;
            // // }
            assertDependency("dependencies.SignatureTestClass.m4()", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.SignatureTestClass.m4()", "java.io.InputStream", RETURNS)
            assertImplicitThisLocalVariable("dependencies.SignatureTestClass.m4()")
            assertDependency("dependencies.SignatureTestClass.m4()", "dependencies.SignatureTestClass", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.SignatureTestClass.m4()", "dependencies.SignatureTestClass.m4()", CALLS_METHOD)
            assertDependency("dependencies.SignatureTestClass.m4()", "java.io.FilterInputStream", USES_RETURN_TYPE)
            //
            //        @SuppressWarnings("unchecked")
            //        public abstract FileOutputStream m5();
            assertDependency("dependencies.SignatureTestClass.m5()", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.SignatureTestClass.m5()", "java.io.FileOutputStream", RETURNS)
            // // implicit method:
            // // public OutputStream m5(){
            // //     return m5(); //Method m5:()Ljava/io/FileOutputStream;
            // // }
            assertDependency("dependencies.SignatureTestClass.m5()", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.SignatureTestClass.m5()", "java.io.OutputStream", RETURNS)
            assertImplicitThisLocalVariable("dependencies.SignatureTestClass.m5()")
            assertDependency("dependencies.SignatureTestClass.m5()", "dependencies.SignatureTestClass", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.SignatureTestClass.m5()", "dependencies.SignatureTestClass.m5()", CALLS_METHOD)
            assertDependency("dependencies.SignatureTestClass.m5()", "java.io.FileOutputStream", USES_RETURN_TYPE)
            //
            //        public abstract List<String> m6(ArrayList<Integer> p);
            assertDependency("dependencies.SignatureTestClass.m6(java.util.ArrayList)", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.SignatureTestClass.m6(java.util.ArrayList)", "java.util.ArrayList", HAS_PARAMETER_OF_TYPE)
            assertDependency("dependencies.SignatureTestClass.m6(java.util.ArrayList)", "java.lang.Integer", USES_TYPE_IN_TYPE_PARAMETERS)
            assertDependency("dependencies.SignatureTestClass.m6(java.util.ArrayList)", "java.util.List", RETURNS)
            assertDependency("dependencies.SignatureTestClass.m6(java.util.ArrayList)", "java.lang.String", USES_TYPE_IN_TYPE_PARAMETERS)
            //    }
        }

        def assertSignatureTestSubClass() {
            //    package dependencies;
            //
            //    import java.io.FileOutputStream;
            //    import java.util.jar.JarInputStream;
            //    import java.util.zip.ZipInputStream;
            //
            //    public abstract class SignatureTestSubClass extends
            //    	SignatureTestClass<ZipInputStream> {
            assertDependency("dependencies.SignatureTestSubClass", "dependencies.SignatureTestClass", EXTENDS)
            assertDependency("dependencies.SignatureTestSubClass", "java.util.zip.ZipInputStream", USES_TYPE_IN_TYPE_PARAMETERS)
            assertImplicitDefaultConstructor("dependencies.SignatureTestSubClass", "dependencies.SignatureTestClass")
            //        protected JarInputStream f1;
            assertDependency("dependencies.SignatureTestSubClass.f1", "dependencies.SignatureTestSubClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.SignatureTestSubClass.f1", "java.util.jar.JarInputStream", IS_OF_TYPE)
            //
            //        @SuppressWarnings("unchecked")
            //        public abstract Integer m3();
            assertDependency("dependencies.SignatureTestSubClass.m3()", "dependencies.SignatureTestSubClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.SignatureTestSubClass.m3()", "java.lang.Integer", RETURNS)
            // // implicit method:
            // // public Object m3(){
            // //     return m3(); //Method m3:()Ljava/lang/Integer;
            // // }
            assertDependency("dependencies.SignatureTestSubClass.m3()", "dependencies.SignatureTestSubClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.SignatureTestSubClass.m3()", "java.lang.Object", RETURNS)
            assertImplicitThisLocalVariable("dependencies.SignatureTestSubClass.m3()")
            assertDependency("dependencies.SignatureTestSubClass.m3()", "dependencies.SignatureTestSubClass", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.SignatureTestSubClass.m3()", "dependencies.SignatureTestSubClass.m3()", CALLS_METHOD)
            assertDependency("dependencies.SignatureTestSubClass.m3()", "java.lang.Integer", USES_RETURN_TYPE)
            //
            //        @SuppressWarnings("unchecked")
            //        public abstract FileOutputStream m5();
            assertDependency("dependencies.SignatureTestSubClass.m5()", "dependencies.SignatureTestSubClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.SignatureTestSubClass.m5()", "java.io.FileOutputStream", RETURNS)
            // // implicit method:
            // // public OutputStream m5(){
            // //     return m5(); //Method m5:()Ljava/io/FileOutputStream;
            // // }
            assertDependency("dependencies.SignatureTestSubClass.m5()", "dependencies.SignatureTestSubClass", IS_INSTANCE_MEMBER)
            assertDependency("dependencies.SignatureTestSubClass.m5()", "java.io.OutputStream", RETURNS)
            assertImplicitThisLocalVariable("dependencies.SignatureTestSubClass.m5()")
            assertDependency("dependencies.SignatureTestSubClass.m5()", "dependencies.SignatureTestSubClass", USES_METHOD_DECLARING_TYPE)
            assertDependency("dependencies.SignatureTestSubClass.m5()", "dependencies.SignatureTestSubClass.m5()", CALLS_METHOD)
            assertDependency("dependencies.SignatureTestSubClass.m5()", "java.io.FileOutputStream", USES_RETURN_TYPE)
            //    }
        }

        def assertImplicitDefaultConstructor(className: String, superClassName: String = "java.lang.Object") {
            //	//implicit constructor:
            val constructorName = className+".<init>()"
            assertDependency(constructorName, className, IS_INSTANCE_MEMBER)
            assertDependency(constructorName, superClassName, USES_METHOD_DECLARING_TYPE)
            assertDependency(constructorName, superClassName+".<init>()", CALLS_METHOD)
            assertImplicitThisLocalVariable(constructorName)
        }

        def assertImplicitThisLocalVariable(methodName: String) {
            // //implicit local variable 'this'
            assertDependency(methodName, methodName.substring(0, methodName.substring(0, methodName.lastIndexOf('(')).lastIndexOf('.')), HAS_LOCAL_VARIABLE_OF_TYPE)
        }

        // test that the extracted dependencies are as expected
        assertTestClass()
        assertTestInterface()
        assertMarkerInterface()
        assertDeprecatedInterface()
        assertFieldsClass()
        assertOuterAndInnerClass()
        assertEnclosingMethodAndInnerClass()
        assertExceptionTestClass()
        assertTestAnnotation()
        assertAnnotationDefaultAttributeTestClass()
        assertInstructionsTestClass()
        assertSignatureTestInterface()
        assertSignatureTestClass()
        assertSignatureTestSubClass()

        assert(dependencies.isEmpty,
            "Too many ["+dependencies.size+"] dependencies have been extracted:\n"+
                dependencies.mkString("\n"))
    }
}

