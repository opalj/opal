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
package de.tud.cs.st.bat.resolved
package dependency

import org.junit.runner.RunWith
import scala.collection.mutable.ArrayBuffer
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import de.tud.cs.st.bat.resolved.reader.Java6Framework
import DependencyType._

/**
 * Tests whether the DependencyExtractor extracts all dependencies correctly.
 * The class files in the "test/classfiles/Dependencies.zip" file
 * provide a basis for the verifications done in this tests.
 *
 * @author Thomas Schlosser
 */
@RunWith(classOf[JUnitRunner])
class DependencyExtractorTest extends FunSuite with de.tud.cs.st.util.perf.BasicPerformanceEvaluation {

    type Dependency = (String, String, DependencyType)
    type Dependencies = List[Dependency]

    test("Dependency extraction") {
        val testClasses = Java6Framework.ClassFiles("test/classfiles/Dependencies.zip")

        // create dependency builder that collects all added dependencies
        val dependencyBuilder = new CollectorDependencyBuilder
        val dependencyExtractor = new DependencyExtractor(dependencyBuilder)

        for (classFile ← testClasses) {
            // process classFile using dependency extractor
            dependencyExtractor.process(classFile)
        }

        //verification...
        implicit val aDeps = new AssertableDependencies(dependencyBuilder.dependencies)

        assertTestClass
        assertTestInterface
        assertMarkerInterface
        assertDeprecatedInterface
        assertFieldsClass
        assertOuterAndInnerClass
        assertEnclosingMethodAndInnerClass
        assertExceptionTestClass
        assertTestAnnotation
        assertAnnotationDefaultAttributeTestClass
        assertInstructionsTestClass
        assertSignatureTestInterface
        assertSignatureTestClass
        assertSignatureTestSubClass

        assert(aDeps.deps.isEmpty, "Too many ["+aDeps.deps.size+"] dependencies have been extracted:\n"+aDeps.deps.mkString("\n"))
    }

    private def assertTestClass(implicit aDeps: AssertableDependencies) {
        //    package dependencies;
        //
        //    import java.util.ArrayList;
        //    import java.util.List;
        //    
        //    public class TestClass implements TestInterface {
        aDeps.assertDependency("dependencies.TestClass", "dependencies.TestInterface", IMPLEMENTS)
        aDeps.assertDependency("dependencies.TestClass", "java.lang.Object", EXTENDS)
        assertImplicitDefaultConstructor("dependencies.TestClass")
        //        public void testMethod() {
        aDeps.assertDependency("dependencies.TestClass.testMethod()", "dependencies.TestClass", IS_INSTANCE_MEMBER_OF)
        assertImplicitThisLocalVariable("dependencies.TestClass.testMethod()")
        // // NOTE: It is not possible to determine a dependency to 'java.lang.String' which is used in type parameters of ArrayList.
        //    	List<? extends CharSequence> list = new ArrayList<String>();
        aDeps.assertDependency("dependencies.TestClass.testMethod()", "java.util.List", HAS_LOCAL_VARIABLE_OF_TYPE)
        aDeps.assertDependency("dependencies.TestClass.testMethod()", "java.lang.CharSequence", USES_TYPE_IN_TYPE_PARAMETERS)
        aDeps.assertDependency("dependencies.TestClass.testMethod()", "java.util.ArrayList", CREATES)
        aDeps.assertDependency("dependencies.TestClass.testMethod()", "java.util.ArrayList", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.TestClass.testMethod()", "java.util.ArrayList.<init>()", CALLS_METHOD)
        //    	list.add(null);
        aDeps.assertDependency("dependencies.TestClass.testMethod()", "java.util.List", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.TestClass.testMethod()", "java.util.List.add(java.lang.Object)", CALLS_INTERFACE_METHOD)
        aDeps.assertDependency("dependencies.TestClass.testMethod()", "java.lang.Object", USES_PARAMETER_TYPE)
        //        }
        //    
        //        public String testMethod(Integer i, int j) {
        aDeps.assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "dependencies.TestClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer", HAS_PARAMETER_OF_TYPE)
        aDeps.assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer", HAS_LOCAL_VARIABLE_OF_TYPE)
        aDeps.assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.String", RETURNS)
        assertImplicitThisLocalVariable("dependencies.TestClass.testMethod(java.lang.Integer, int)")
        //    	if (i != null && i.intValue() > j) {
        aDeps.assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer.intValue()", CALLS_METHOD)
        //    	    return i.toString();
        aDeps.assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer.toString()", CALLS_METHOD)
        aDeps.assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.String", USES_RETURN_TYPE)
        //    	}
        //    	return String.valueOf(j);
        aDeps.assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.String", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.String.valueOf(int)", CALLS_METHOD)
        aDeps.assertDependency("dependencies.TestClass.testMethod(java.lang.Integer, int)", "java.lang.String", USES_RETURN_TYPE)
        //        }
        //    }
    }

    private def assertTestInterface(implicit aDeps: AssertableDependencies) {
        //    package dependencies;
        //
        //    public interface TestInterface {
        aDeps.assertDependency("dependencies.TestInterface", "java.lang.Object", EXTENDS)
        //        void testMethod();
        aDeps.assertDependency("dependencies.TestInterface.testMethod()", "dependencies.TestInterface", IS_INSTANCE_MEMBER_OF)
        //    
        //        String testMethod(Integer i, int j);
        aDeps.assertDependency("dependencies.TestInterface.testMethod(java.lang.Integer, int)", "dependencies.TestInterface", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.TestInterface.testMethod(java.lang.Integer, int)", "java.lang.Integer", HAS_PARAMETER_OF_TYPE)
        aDeps.assertDependency("dependencies.TestInterface.testMethod(java.lang.Integer, int)", "java.lang.String", RETURNS)
        //    }
    }

    private def assertMarkerInterface(implicit aDeps: AssertableDependencies) {
        //    package dependencies.sub;
        //
        //    public interface MarkerInterface {
        aDeps.assertDependency("dependencies.sub.MarkerInterface", "java.lang.Object", EXTENDS)
        //    
        //    }
    }

    private def assertDeprecatedInterface(implicit aDeps: AssertableDependencies) {
        //    package dependencies.sub;
        //
        //    import dependencies.TestInterface;
        //    
        //    @Deprecated
        //    public interface DeprecatedInterface extends TestInterface, MarkerInterface {
        aDeps.assertDependency("dependencies.sub.DeprecatedInterface", "java.lang.Object", EXTENDS)
        aDeps.assertDependency("dependencies.sub.DeprecatedInterface", "dependencies.TestInterface", IMPLEMENTS)
        aDeps.assertDependency("dependencies.sub.DeprecatedInterface", "dependencies.sub.MarkerInterface", IMPLEMENTS)
        aDeps.assertDependency("dependencies.sub.DeprecatedInterface", "java.lang.Deprecated", ANNOTATED_WITH)
        //    
        //        @Deprecated
        //        public void deprecatedMethod();
        aDeps.assertDependency("dependencies.sub.DeprecatedInterface.deprecatedMethod()", "dependencies.sub.DeprecatedInterface", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.sub.DeprecatedInterface.deprecatedMethod()", "java.lang.Deprecated", ANNOTATED_WITH)
        //    
        //        public void methodDeprParam(@Deprecated int i);
        aDeps.assertDependency("dependencies.sub.DeprecatedInterface.methodDeprParam(int)", "dependencies.sub.DeprecatedInterface", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.sub.DeprecatedInterface.methodDeprParam(int)", "java.lang.Deprecated", PARAMETER_ANNOTATED_WITH)
        //    }
    }

    private def assertFieldsClass(implicit aDeps: AssertableDependencies) {
        //    package dependencies;
        //
        //    public class FieldsClass {
        aDeps.assertDependency("dependencies.FieldsClass", "java.lang.Object", EXTENDS)
        assertImplicitDefaultConstructor("dependencies.FieldsClass")
        //        public final static String CONSTANT = "constant";
        aDeps.assertDependency("dependencies.FieldsClass.CONSTANT", "dependencies.FieldsClass", IS_CLASS_MEMBER_OF)
        aDeps.assertDependency("dependencies.FieldsClass.CONSTANT", "java.lang.String", IS_OF_TYPE)
        aDeps.assertDependency("dependencies.FieldsClass.CONSTANT", "java.lang.String", USES_CONSTANT_VALUE_OF_TYPE)
        //        private Integer i;
        aDeps.assertDependency("dependencies.FieldsClass.i", "dependencies.FieldsClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.FieldsClass.i", "java.lang.Integer", IS_OF_TYPE)
        //    
        //        @Deprecated
        //        protected int deprecatedField;
        aDeps.assertDependency("dependencies.FieldsClass.deprecatedField", "dependencies.FieldsClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.FieldsClass.deprecatedField", "java.lang.Deprecated", ANNOTATED_WITH)
        //    
        //        private Integer readField() {
        aDeps.assertDependency("dependencies.FieldsClass.readField()", "dependencies.FieldsClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.FieldsClass.readField()", "java.lang.Integer", RETURNS)
        assertImplicitThisLocalVariable("dependencies.FieldsClass.readField()")
        //    	return i;
        aDeps.assertDependency("dependencies.FieldsClass.readField()", "dependencies.FieldsClass", USES_FIELD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.FieldsClass.readField()", "dependencies.FieldsClass.i", READS_FIELD)
        aDeps.assertDependency("dependencies.FieldsClass.readField()", "java.lang.Integer", USES_FIELD_READ_TYPE)
        //        }
        //    
        //        private void writeField(Integer j) {
        aDeps.assertDependency("dependencies.FieldsClass.writeField(java.lang.Integer)", "dependencies.FieldsClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.FieldsClass.writeField(java.lang.Integer)", "java.lang.Integer", HAS_PARAMETER_OF_TYPE)
        aDeps.assertDependency("dependencies.FieldsClass.writeField(java.lang.Integer)", "java.lang.Integer", HAS_LOCAL_VARIABLE_OF_TYPE)
        assertImplicitThisLocalVariable("dependencies.FieldsClass.writeField(java.lang.Integer)")
        //    	i = j;
        aDeps.assertDependency("dependencies.FieldsClass.writeField(java.lang.Integer)", "dependencies.FieldsClass", USES_FIELD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.FieldsClass.writeField(java.lang.Integer)", "dependencies.FieldsClass.i", WRITES_FIELD)
        aDeps.assertDependency("dependencies.FieldsClass.writeField(java.lang.Integer)", "java.lang.Integer", USES_FIELD_WRITE_TYPE)
        //        }
        //    
        //        public Integer readWrite(Integer j) {
        aDeps.assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "dependencies.FieldsClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", HAS_PARAMETER_OF_TYPE)
        aDeps.assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", HAS_LOCAL_VARIABLE_OF_TYPE)
        aDeps.assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", RETURNS)
        assertImplicitThisLocalVariable("dependencies.FieldsClass.readWrite(java.lang.Integer)")
        //    	Integer result = readField();
        aDeps.assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", HAS_LOCAL_VARIABLE_OF_TYPE)
        aDeps.assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "dependencies.FieldsClass", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "dependencies.FieldsClass.readField()", CALLS_METHOD)
        aDeps.assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", USES_RETURN_TYPE)
        //    	writeField(j);
        aDeps.assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "dependencies.FieldsClass", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "dependencies.FieldsClass.writeField(java.lang.Integer)", CALLS_METHOD)
        aDeps.assertDependency("dependencies.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", USES_PARAMETER_TYPE)
        //    	return result;
        //        }
        //    }
    }

    private def assertOuterAndInnerClass(implicit aDeps: AssertableDependencies) {
        //    package dependencies;
        //
        //    public class OuterClass {
        aDeps.assertDependency("dependencies.OuterClass", "java.lang.Object", EXTENDS)
        assertImplicitDefaultConstructor("dependencies.OuterClass")
        //        class InnerClass {
        aDeps.assertDependency("dependencies.OuterClass$InnerClass", "java.lang.Object", EXTENDS)
        aDeps.assertDependency("dependencies.OuterClass$InnerClass", "dependencies.OuterClass", IS_INNER_CLASS_OF)
        //	//implicit field:
        aDeps.assertDependency("dependencies.OuterClass$InnerClass.this$0", "dependencies.OuterClass$InnerClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.OuterClass$InnerClass.this$0", "dependencies.OuterClass", IS_OF_TYPE)
        //		public InnerClass(Integer i) {
        aDeps.assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "dependencies.OuterClass$InnerClass", IS_INSTANCE_MEMBER_OF)
        //	//implicit constructor parameter:
        aDeps.assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "dependencies.OuterClass", HAS_PARAMETER_OF_TYPE)
        aDeps.assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "java.lang.Integer", HAS_PARAMETER_OF_TYPE)
        aDeps.assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "java.lang.Integer", HAS_LOCAL_VARIABLE_OF_TYPE)
        assertImplicitThisLocalVariable("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)")

        aDeps.assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "java.lang.Object", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "java.lang.Object.<init>()", CALLS_METHOD)
        //	// write to implicit field:
        aDeps.assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "dependencies.OuterClass$InnerClass", USES_FIELD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "dependencies.OuterClass$InnerClass.this$0", WRITES_FIELD)
        aDeps.assertDependency("dependencies.OuterClass$InnerClass.<init>(dependencies.OuterClass, java.lang.Integer)", "dependencies.OuterClass", USES_FIELD_WRITE_TYPE)
        //		}
        //        }
        //    }
    }

    private def assertEnclosingMethodAndInnerClass(implicit aDeps: AssertableDependencies) {
        //    package dependencies;
        //    
        //    public class EnclosingMethodClass {
        aDeps.assertDependency("dependencies.EnclosingMethodClass", "java.lang.Object", EXTENDS)
        assertImplicitDefaultConstructor("dependencies.EnclosingMethodClass")
        //  //implicit field definition in the default constructor
        aDeps.assertDependency("dependencies.EnclosingMethodClass.<init>()", "dependencies.EnclosingMethodClass$1", CREATES)
        aDeps.assertDependency("dependencies.EnclosingMethodClass.<init>()", "dependencies.EnclosingMethodClass$1", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.EnclosingMethodClass.<init>()", "dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)", CALLS_METHOD)
        aDeps.assertDependency("dependencies.EnclosingMethodClass.<init>()", "dependencies.EnclosingMethodClass", USES_PARAMETER_TYPE)
        aDeps.assertDependency("dependencies.EnclosingMethodClass.<init>()", "dependencies.EnclosingMethodClass", USES_FIELD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.EnclosingMethodClass.<init>()", "dependencies.EnclosingMethodClass.enclosingField", WRITES_FIELD)
        aDeps.assertDependency("dependencies.EnclosingMethodClass.<init>()", "java.lang.Object", USES_FIELD_WRITE_TYPE)
        //  //implicit field definition in the class initialization method
        aDeps.assertDependency("dependencies.EnclosingMethodClass.<clinit>()", "dependencies.EnclosingMethodClass", IS_CLASS_MEMBER_OF)
        aDeps.assertDependency("dependencies.EnclosingMethodClass.<clinit>()", "dependencies.EnclosingMethodClass$2", CREATES)
        aDeps.assertDependency("dependencies.EnclosingMethodClass.<clinit>()", "dependencies.EnclosingMethodClass$2", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.EnclosingMethodClass.<clinit>()", "dependencies.EnclosingMethodClass$2.<init>()", CALLS_METHOD)
        aDeps.assertDependency("dependencies.EnclosingMethodClass.<clinit>()", "dependencies.EnclosingMethodClass", USES_FIELD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.EnclosingMethodClass.<clinit>()", "dependencies.EnclosingMethodClass.staticEnclosingField", WRITES_FIELD)
        aDeps.assertDependency("dependencies.EnclosingMethodClass.<clinit>()", "java.lang.Object", USES_FIELD_WRITE_TYPE)
        //    
        //        public Object enclosingField = new Object() {
        aDeps.assertDependency("dependencies.EnclosingMethodClass.enclosingField", "dependencies.EnclosingMethodClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.EnclosingMethodClass.enclosingField", "java.lang.Object", IS_OF_TYPE)
        //        };
        aDeps.assertDependency("dependencies.EnclosingMethodClass$1", "java.lang.Object", EXTENDS)
        aDeps.assertDependency("dependencies.EnclosingMethodClass$1", "dependencies.EnclosingMethodClass", IS_INNER_CLASS_OF)
        //	//implicit field:
        aDeps.assertDependency("dependencies.EnclosingMethodClass$1.this$0", "dependencies.EnclosingMethodClass$1", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.EnclosingMethodClass$1.this$0", "dependencies.EnclosingMethodClass", IS_OF_TYPE)
        //	//implicit constructor:
        aDeps.assertDependency("dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass$1", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass", HAS_PARAMETER_OF_TYPE)
        assertImplicitThisLocalVariable("dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)")
        aDeps.assertDependency("dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)", "java.lang.Object", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)", "java.lang.Object.<init>()", CALLS_METHOD)
        //	// write to implicit field:
        aDeps.assertDependency("dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass$1", USES_FIELD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass$1.this$0", WRITES_FIELD)
        aDeps.assertDependency("dependencies.EnclosingMethodClass$1.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass", USES_FIELD_WRITE_TYPE)
        //
        //        public static Object staticEnclosingField = new Object() {
        aDeps.assertDependency("dependencies.EnclosingMethodClass.staticEnclosingField", "dependencies.EnclosingMethodClass", IS_CLASS_MEMBER_OF)
        aDeps.assertDependency("dependencies.EnclosingMethodClass.staticEnclosingField", "java.lang.Object", IS_OF_TYPE)
        //        };
        aDeps.assertDependency("dependencies.EnclosingMethodClass$2", "java.lang.Object", EXTENDS)
        aDeps.assertDependency("dependencies.EnclosingMethodClass$2", "dependencies.EnclosingMethodClass", IS_INNER_CLASS_OF)
        //	//implicit constructor:
        aDeps.assertDependency("dependencies.EnclosingMethodClass$2.<init>()", "dependencies.EnclosingMethodClass$2", IS_INSTANCE_MEMBER_OF)
        assertImplicitThisLocalVariable("dependencies.EnclosingMethodClass$2.<init>()")
        aDeps.assertDependency("dependencies.EnclosingMethodClass$2.<init>()", "java.lang.Object", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.EnclosingMethodClass$2.<init>()", "java.lang.Object.<init>()", CALLS_METHOD)
        //
        //        public void enclosingMethod() {
        aDeps.assertDependency("dependencies.EnclosingMethodClass.enclosingMethod()", "dependencies.EnclosingMethodClass", IS_INSTANCE_MEMBER_OF)
        assertImplicitThisLocalVariable("dependencies.EnclosingMethodClass.enclosingMethod()")
        //    	new Object() {
        aDeps.assertDependency("dependencies.EnclosingMethodClass$3", "java.lang.Object", EXTENDS)
        aDeps.assertDependency("dependencies.EnclosingMethodClass$3", "dependencies.EnclosingMethodClass.enclosingMethod()", IS_INNER_CLASS_OF)
        //	//implicit field:
        aDeps.assertDependency("dependencies.EnclosingMethodClass$3.this$0", "dependencies.EnclosingMethodClass$3", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.EnclosingMethodClass$3.this$0", "dependencies.EnclosingMethodClass", IS_OF_TYPE)
        //	//implicit constructor:
        aDeps.assertDependency("dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass$3", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass", HAS_PARAMETER_OF_TYPE)
        assertImplicitThisLocalVariable("dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)")
        aDeps.assertDependency("dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)", "java.lang.Object", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)", "java.lang.Object.<init>()", CALLS_METHOD)
        //	// write to implicit field:
        aDeps.assertDependency("dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass$3", USES_FIELD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass$3.this$0", WRITES_FIELD)
        aDeps.assertDependency("dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)", "dependencies.EnclosingMethodClass", USES_FIELD_WRITE_TYPE)
        //    	    public void innerMethod() {
        aDeps.assertDependency("dependencies.EnclosingMethodClass$3.innerMethod()", "dependencies.EnclosingMethodClass$3", IS_INSTANCE_MEMBER_OF)
        assertImplicitThisLocalVariable("dependencies.EnclosingMethodClass$3.innerMethod()")
        //    	    }
        //    	}.innerMethod();
        aDeps.assertDependency("dependencies.EnclosingMethodClass.enclosingMethod()", "dependencies.EnclosingMethodClass$3", CREATES)
        aDeps.assertDependency("dependencies.EnclosingMethodClass.enclosingMethod()", "dependencies.EnclosingMethodClass$3", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.EnclosingMethodClass.enclosingMethod()", "dependencies.EnclosingMethodClass$3.<init>(dependencies.EnclosingMethodClass)", CALLS_METHOD)
        aDeps.assertDependency("dependencies.EnclosingMethodClass.enclosingMethod()", "dependencies.EnclosingMethodClass", USES_PARAMETER_TYPE) // method parameter

        aDeps.assertDependency("dependencies.EnclosingMethodClass.enclosingMethod()", "dependencies.EnclosingMethodClass$3", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.EnclosingMethodClass.enclosingMethod()", "dependencies.EnclosingMethodClass$3.innerMethod()", CALLS_METHOD)
        //        }
        //    }
    }

    private def assertExceptionTestClass(implicit aDeps: AssertableDependencies) {
        //    package dependencies;
        //    
        //    import java.util.FormatterClosedException;
        //    
        //    import javax.naming.OperationNotSupportedException;
        //    
        //    public class ExceptionTestClass {
        aDeps.assertDependency("dependencies.ExceptionTestClass", "java.lang.Object", EXTENDS)
        assertImplicitDefaultConstructor("dependencies.ExceptionTestClass")
        //    
        //        public void testMethod() throws IllegalStateException,
        //    	    OperationNotSupportedException {
        aDeps.assertDependency("dependencies.ExceptionTestClass.testMethod()", "dependencies.ExceptionTestClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.ExceptionTestClass.testMethod()", "java.lang.IllegalStateException", THROWS)
        aDeps.assertDependency("dependencies.ExceptionTestClass.testMethod()", "javax.naming.OperationNotSupportedException", THROWS)
        assertImplicitThisLocalVariable("dependencies.ExceptionTestClass.testMethod()")
        //    	throw new FormatterClosedException();
        aDeps.assertDependency("dependencies.ExceptionTestClass.testMethod()", "java.util.FormatterClosedException", CREATES)
        aDeps.assertDependency("dependencies.ExceptionTestClass.testMethod()", "java.util.FormatterClosedException", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.ExceptionTestClass.testMethod()", "java.util.FormatterClosedException.<init>()", CALLS_METHOD)
        //        }
        //
        //        public void catchMethod() {
        aDeps.assertDependency("dependencies.ExceptionTestClass.catchMethod()", "dependencies.ExceptionTestClass", IS_INSTANCE_MEMBER_OF)
        assertImplicitThisLocalVariable("dependencies.ExceptionTestClass.catchMethod()")
        //    	try {
        //    	    try {
        //    		testMethod();
        aDeps.assertDependency("dependencies.ExceptionTestClass.catchMethod()", "dependencies.ExceptionTestClass", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.ExceptionTestClass.catchMethod()", "dependencies.ExceptionTestClass.testMethod()", CALLS_METHOD)
        //    	    } catch (IllegalStateException e) {
        aDeps.assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.IllegalStateException", CATCHES)
        aDeps.assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.IllegalStateException", HAS_LOCAL_VARIABLE_OF_TYPE)
        //    	    }
        //    	} catch (Exception e) {
        aDeps.assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Exception", CATCHES)
        aDeps.assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Exception", HAS_LOCAL_VARIABLE_OF_TYPE)
        //    	} finally{
        //    	    Integer.valueOf(42);
        aDeps.assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer.valueOf(int)", CALLS_METHOD)
        aDeps.assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer", USES_RETURN_TYPE)
        // // The next six dependencies result from required special handling of the finally block.
        // // Depending on the way the finally block were reached it has to throw an Exception or return normally.
        // // Hence, the bytecode contains the three versions of the finally block which results in multiple
        // // dependencies to types/methods/fields used in the finally block.
        aDeps.assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer.valueOf(int)", CALLS_METHOD)
        aDeps.assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer", USES_RETURN_TYPE)
        aDeps.assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer.valueOf(int)", CALLS_METHOD)
        aDeps.assertDependency("dependencies.ExceptionTestClass.catchMethod()", "java.lang.Integer", USES_RETURN_TYPE)
        //    	}
        //        }
        //    }
    }

    private def assertTestAnnotation(implicit aDeps: AssertableDependencies) {
        //    package dependencies;
        //    
        //    import java.lang.annotation.ElementType;
        //    
        //    public @interface TestAnnotation {
        aDeps.assertDependency("dependencies.TestAnnotation", "java.lang.Object", EXTENDS)
        aDeps.assertDependency("dependencies.TestAnnotation", "java.lang.annotation.Annotation", IMPLEMENTS)
        //        public abstract String stringValue() default "default";
        aDeps.assertDependency("dependencies.TestAnnotation.stringValue()", "dependencies.TestAnnotation", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.TestAnnotation.stringValue()", "java.lang.String", RETURNS)
        //    
        //        public abstract Class<?> classValue() default String.class;
        aDeps.assertDependency("dependencies.TestAnnotation.classValue()", "dependencies.TestAnnotation", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.TestAnnotation.classValue()", "java.lang.Class", RETURNS)
        aDeps.assertDependency("dependencies.TestAnnotation.classValue()", "java.lang.String", USES_DEFAULT_CLASS_VALUE_TYPE)
        //    
        //        public abstract ElementType enumValue() default ElementType.TYPE;
        aDeps.assertDependency("dependencies.TestAnnotation.enumValue()", "dependencies.TestAnnotation", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.TestAnnotation.enumValue()", "java.lang.annotation.ElementType", RETURNS)
        aDeps.assertDependency("dependencies.TestAnnotation.enumValue()", "java.lang.annotation.ElementType", USES_DEFAULT_ENUM_VALUE_TYPE)
        aDeps.assertDependency("dependencies.TestAnnotation.enumValue()", "java.lang.annotation.ElementType.TYPE", USES_ENUM_VALUE)
        //    
        //        public abstract SuppressWarnings annotationValue() default @SuppressWarnings("default");
        aDeps.assertDependency("dependencies.TestAnnotation.annotationValue()", "dependencies.TestAnnotation", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.TestAnnotation.annotationValue()", "java.lang.SuppressWarnings", RETURNS)
        aDeps.assertDependency("dependencies.TestAnnotation.annotationValue()", "java.lang.SuppressWarnings", USES_DEFAULT_ANNOTATION_VALUE_TYPE)
        //    
        //        public abstract Class<?>[] arrayClassValue() default { String.class,
        //    	    Integer.class };
        aDeps.assertDependency("dependencies.TestAnnotation.arrayClassValue()", "dependencies.TestAnnotation", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.TestAnnotation.arrayClassValue()", "java.lang.Class", RETURNS)
        aDeps.assertDependency("dependencies.TestAnnotation.arrayClassValue()", "java.lang.String", USES_DEFAULT_CLASS_VALUE_TYPE)
        aDeps.assertDependency("dependencies.TestAnnotation.arrayClassValue()", "java.lang.Integer", USES_DEFAULT_CLASS_VALUE_TYPE)
        //    }
    }

    private def assertAnnotationDefaultAttributeTestClass(implicit aDeps: AssertableDependencies) {
        //    package dependencies;
        //    
        //    import java.lang.annotation.ElementType;
        //    
        //    @TestAnnotation
        //    public class AnnotationDefaultAttributeTestClass {
        aDeps.assertDependency("dependencies.AnnotationDefaultAttributeTestClass", "java.lang.Object", EXTENDS)
        aDeps.assertDependency("dependencies.AnnotationDefaultAttributeTestClass", "dependencies.TestAnnotation", ANNOTATED_WITH)
        assertImplicitDefaultConstructor("dependencies.AnnotationDefaultAttributeTestClass")
        //    
        //        @TestAnnotation(stringValue = "noDefault", classValue = Integer.class, enumValue = ElementType.METHOD, annotationValue = @SuppressWarnings("noDefault"), arrayClassValue = {
        //    	    Long.class, Boolean.class })
        //        public void testMethod() {
        aDeps.assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "dependencies.AnnotationDefaultAttributeTestClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "dependencies.TestAnnotation", ANNOTATED_WITH)
        aDeps.assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.Integer", USES_DEFAULT_CLASS_VALUE_TYPE)
        aDeps.assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.annotation.ElementType", USES_DEFAULT_ENUM_VALUE_TYPE)
        aDeps.assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.annotation.ElementType.METHOD", USES_ENUM_VALUE)
        aDeps.assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.SuppressWarnings", USES_DEFAULT_ANNOTATION_VALUE_TYPE)
        aDeps.assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.Long", USES_DEFAULT_CLASS_VALUE_TYPE)
        aDeps.assertDependency("dependencies.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.Boolean", USES_DEFAULT_CLASS_VALUE_TYPE)
        assertImplicitThisLocalVariable("dependencies.AnnotationDefaultAttributeTestClass.testMethod()")
        //        }
        //    }
    }

    private def assertInstructionsTestClass(implicit aDeps: AssertableDependencies) {
        //    package dependencies;
        //    
        //    import java.io.FilterInputStream;
        //    import java.io.InputStream;
        //    import java.util.zip.InflaterInputStream;
        //    import java.util.zip.ZipInputStream;
        //    
        //    public class InstructionsTestClass {
        aDeps.assertDependency("dependencies.InstructionsTestClass", "java.lang.Object", EXTENDS)
        assertImplicitDefaultConstructor("dependencies.InstructionsTestClass")
        //        public Object field;
        aDeps.assertDependency("dependencies.InstructionsTestClass.field", "dependencies.InstructionsTestClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.InstructionsTestClass.field", "java.lang.Object", IS_OF_TYPE)
        //        public static InputStream staticField;
        aDeps.assertDependency("dependencies.InstructionsTestClass.staticField", "dependencies.InstructionsTestClass", IS_CLASS_MEMBER_OF)
        aDeps.assertDependency("dependencies.InstructionsTestClass.staticField", "java.io.InputStream", IS_OF_TYPE)
        //    
        //        public void method() {
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass", IS_INSTANCE_MEMBER_OF)
        assertImplicitThisLocalVariable("dependencies.InstructionsTestClass.method()")
        //    	// NEW and INVOKESPECIAL (constructor call)
        //    	Object obj = new Object();
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object", HAS_LOCAL_VARIABLE_OF_TYPE)
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object", CREATES)
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object.<init>()", CALLS_METHOD)
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object", USES_METHOD_DECLARING_TYPE)
        //    	FilterInputStream stream = null;
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "java.io.FilterInputStream", HAS_LOCAL_VARIABLE_OF_TYPE)
        //    	// ANEWARRAY
        //    	obj = new Long[1];
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Long", CREATES_ARRAY_OF_TYPE)
        //    	// MULTIANEWARRAY
        //    	obj = new Integer[1][];
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Integer", CREATES_ARRAY_OF_TYPE)
        //    
        //    	// PUTFIELD
        //    	field = obj;
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass", USES_FIELD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass.field", WRITES_FIELD)
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object", USES_FIELD_WRITE_TYPE)
        //    	// GETFIELD
        //    	obj = field;
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass", USES_FIELD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass.field", READS_FIELD)
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object", USES_FIELD_READ_TYPE)
        //    	// INSTANCEOF
        //    	if (obj instanceof ZipInputStream) {
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "java.util.zip.ZipInputStream", CHECKS_INSTANCEOF)
        //    	    // CHECKCAST
        //    	    stream = (InflaterInputStream) obj;
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "java.util.zip.InflaterInputStream", CASTS_INTO)
        //    	    // PUTSTATIC
        //    	    staticField = stream;
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass", USES_FIELD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass.staticField", WRITES_FIELD)
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "java.io.InputStream", USES_FIELD_WRITE_TYPE)
        //    	    // GETSTATIC
        //    	    obj = staticField;
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass", USES_FIELD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.InstructionsTestClass.staticField", READS_FIELD)
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "java.io.InputStream", USES_FIELD_READ_TYPE)
        //    	}
        //    
        //    	// INVOKESTATIC
        //    	System.currentTimeMillis();
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.System", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.System.currentTimeMillis()", CALLS_METHOD)
        //    
        //    	TestInterface ti = new TestClass();
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.TestInterface", HAS_LOCAL_VARIABLE_OF_TYPE)
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.TestClass", CREATES)
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.TestClass", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.TestClass.<init>()", CALLS_METHOD)
        //    	// INVOKEINTERFACE
        //    	ti.testMethod();
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.TestInterface", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "dependencies.TestInterface.testMethod()", CALLS_INTERFACE_METHOD)
        //    
        //    	// INVOKEVIRTUAL
        //    	obj.equals(stream);
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object.equals(java.lang.Object)", CALLS_METHOD)
        aDeps.assertDependency("dependencies.InstructionsTestClass.method()", "java.lang.Object", USES_PARAMETER_TYPE)
        //    
        //    	// TODO [Java 7]: add test for INVOKEDYNAMIC
        //        }
        //    }
    }

    private def assertSignatureTestInterface(implicit aDeps: AssertableDependencies) {
        //    package dependencies;
        //    
        //    import java.io.InputStream;
        //    import java.io.OutputStream;
        //    
        //    public interface SignatureTestInterface<T extends InputStream, Z> {
        aDeps.assertDependency("dependencies.SignatureTestInterface", "java.lang.Object", EXTENDS)
        aDeps.assertDependency("dependencies.SignatureTestInterface", "java.io.InputStream", USES_TYPE_IN_TYPE_PARAMETERS)
        aDeps.assertDependency("dependencies.SignatureTestInterface", "java.lang.Object", USES_TYPE_IN_TYPE_PARAMETERS)
        //
        //        public T m1();
        aDeps.assertDependency("dependencies.SignatureTestInterface.m1()", "dependencies.SignatureTestInterface", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.SignatureTestInterface.m1()", "java.io.InputStream", RETURNS)
        //    
        //        public void m2(T t, Z z);
        aDeps.assertDependency("dependencies.SignatureTestInterface.m2(java.io.InputStream, java.lang.Object)", "dependencies.SignatureTestInterface", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.SignatureTestInterface.m2(java.io.InputStream, java.lang.Object)", "java.io.InputStream", HAS_PARAMETER_OF_TYPE)
        aDeps.assertDependency("dependencies.SignatureTestInterface.m2(java.io.InputStream, java.lang.Object)", "java.lang.Object", HAS_PARAMETER_OF_TYPE)
        //    
        //        public <W> W m3();
        aDeps.assertDependency("dependencies.SignatureTestInterface.m3()", "dependencies.SignatureTestInterface", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.SignatureTestInterface.m3()", "java.lang.Object", USES_TYPE_IN_TYPE_PARAMETERS)
        aDeps.assertDependency("dependencies.SignatureTestInterface.m3()", "java.lang.Object", RETURNS)
        //    
        //        public <W extends T> W m4();
        aDeps.assertDependency("dependencies.SignatureTestInterface.m4()", "dependencies.SignatureTestInterface", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.SignatureTestInterface.m4()", "java.io.InputStream", RETURNS)
        //    
        //        public <W extends OutputStream> W m5();
        aDeps.assertDependency("dependencies.SignatureTestInterface.m5()", "dependencies.SignatureTestInterface", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.SignatureTestInterface.m5()", "java.io.OutputStream", USES_TYPE_IN_TYPE_PARAMETERS)
        aDeps.assertDependency("dependencies.SignatureTestInterface.m5()", "java.io.OutputStream", RETURNS)
        //    }
    }

    private def assertSignatureTestClass(implicit aDeps: AssertableDependencies) {
        //    package dependencies;
        //    
        //    import java.io.FileOutputStream;
        //    import java.io.FilterInputStream;
        //    import java.util.ArrayList;
        //    import java.util.List;
        //    
        //    public abstract class SignatureTestClass<Q extends FilterInputStream>
        //    	implements SignatureTestInterface<Q, String> {
        aDeps.assertDependency("dependencies.SignatureTestClass", "java.lang.Object", EXTENDS)
        aDeps.assertDependency("dependencies.SignatureTestClass", "java.io.FilterInputStream", USES_TYPE_IN_TYPE_PARAMETERS)
        aDeps.assertDependency("dependencies.SignatureTestClass", "java.lang.String", USES_TYPE_IN_TYPE_PARAMETERS)
        aDeps.assertDependency("dependencies.SignatureTestClass", "dependencies.SignatureTestInterface", IMPLEMENTS)
        assertImplicitDefaultConstructor("dependencies.SignatureTestClass")
        //    
        //        protected Q f1;
        aDeps.assertDependency("dependencies.SignatureTestClass.f1", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.SignatureTestClass.f1", "java.io.FilterInputStream", IS_OF_TYPE)
        //    
        //        protected List<Long> f2;
        aDeps.assertDependency("dependencies.SignatureTestClass.f2", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.SignatureTestClass.f2", "java.util.List", IS_OF_TYPE)
        aDeps.assertDependency("dependencies.SignatureTestClass.f2", "java.lang.Long", USES_TYPE_IN_TYPE_PARAMETERS)
        //    
        //        public abstract Q m1();
        aDeps.assertDependency("dependencies.SignatureTestClass.m1()", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.SignatureTestClass.m1()", "java.io.FilterInputStream", RETURNS)
        // // implicit method:
        // // public InputStream m1(){
        // //     return m1(); //Method m1:()Ljava/io/FilterInputStream;
        // // }
        aDeps.assertDependency("dependencies.SignatureTestClass.m1()", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.SignatureTestClass.m1()", "java.io.InputStream", RETURNS)
        assertImplicitThisLocalVariable("dependencies.SignatureTestClass.m1()")
        aDeps.assertDependency("dependencies.SignatureTestClass.m1()", "dependencies.SignatureTestClass", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.SignatureTestClass.m1()", "dependencies.SignatureTestClass.m1()", CALLS_METHOD)
        aDeps.assertDependency("dependencies.SignatureTestClass.m1()", "java.io.FilterInputStream", USES_RETURN_TYPE)
        //    
        //        public abstract void m2(Q t, String z);
        aDeps.assertDependency("dependencies.SignatureTestClass.m2(java.io.FilterInputStream, java.lang.String)", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.SignatureTestClass.m2(java.io.FilterInputStream, java.lang.String)", "java.io.FilterInputStream", HAS_PARAMETER_OF_TYPE)
        aDeps.assertDependency("dependencies.SignatureTestClass.m2(java.io.FilterInputStream, java.lang.String)", "java.lang.String", HAS_PARAMETER_OF_TYPE)
        // // implicit method:
        // // public void m2(java.io.InputStream t, java.lang.Object z){
        // //     return m2((java.io.FileInputStream)t, (java.lang.String) z);
        // // }
        aDeps.assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.io.InputStream", HAS_PARAMETER_OF_TYPE)
        aDeps.assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.io.InputStream", HAS_LOCAL_VARIABLE_OF_TYPE)
        aDeps.assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.lang.Object", HAS_PARAMETER_OF_TYPE)
        aDeps.assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.lang.Object", HAS_LOCAL_VARIABLE_OF_TYPE)
        assertImplicitThisLocalVariable("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)")
        aDeps.assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.io.FilterInputStream", CASTS_INTO)
        aDeps.assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.lang.String", CASTS_INTO)
        aDeps.assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "dependencies.SignatureTestClass", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "dependencies.SignatureTestClass.m2(java.io.FilterInputStream, java.lang.String)", CALLS_METHOD)
        aDeps.assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.io.FilterInputStream", USES_PARAMETER_TYPE)
        aDeps.assertDependency("dependencies.SignatureTestClass.m2(java.io.InputStream, java.lang.Object)", "java.lang.String", USES_PARAMETER_TYPE)
        //    
        //        @SuppressWarnings("unchecked")
        //        public abstract Integer m3();
        aDeps.assertDependency("dependencies.SignatureTestClass.m3()", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.SignatureTestClass.m3()", "java.lang.Integer", RETURNS)
        // // implicit method:
        // // public Object m3(){
        // //     return m3(); //Method m3:()Ljava/lang/Integer;
        // // }
        aDeps.assertDependency("dependencies.SignatureTestClass.m3()", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.SignatureTestClass.m3()", "java.lang.Object", RETURNS)
        assertImplicitThisLocalVariable("dependencies.SignatureTestClass.m3()")
        aDeps.assertDependency("dependencies.SignatureTestClass.m3()", "dependencies.SignatureTestClass", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.SignatureTestClass.m3()", "dependencies.SignatureTestClass.m3()", CALLS_METHOD)
        aDeps.assertDependency("dependencies.SignatureTestClass.m3()", "java.lang.Integer", USES_RETURN_TYPE)
        //    
        //        public abstract Q m4();
        aDeps.assertDependency("dependencies.SignatureTestClass.m4()", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.SignatureTestClass.m4()", "java.io.FilterInputStream", RETURNS)
        // // implicit method:
        // // public InputStream m4(){
        // //     return m4(); //Method m4:()Ljava/io/FilterInputStream;
        // // }
        aDeps.assertDependency("dependencies.SignatureTestClass.m4()", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.SignatureTestClass.m4()", "java.io.InputStream", RETURNS)
        assertImplicitThisLocalVariable("dependencies.SignatureTestClass.m4()")
        aDeps.assertDependency("dependencies.SignatureTestClass.m4()", "dependencies.SignatureTestClass", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.SignatureTestClass.m4()", "dependencies.SignatureTestClass.m4()", CALLS_METHOD)
        aDeps.assertDependency("dependencies.SignatureTestClass.m4()", "java.io.FilterInputStream", USES_RETURN_TYPE)
        //    
        //        @SuppressWarnings("unchecked")
        //        public abstract FileOutputStream m5();
        aDeps.assertDependency("dependencies.SignatureTestClass.m5()", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.SignatureTestClass.m5()", "java.io.FileOutputStream", RETURNS)
        // // implicit method:
        // // public OutputStream m5(){
        // //     return m5(); //Method m5:()Ljava/io/FileOutputStream;
        // // }
        aDeps.assertDependency("dependencies.SignatureTestClass.m5()", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.SignatureTestClass.m5()", "java.io.OutputStream", RETURNS)
        assertImplicitThisLocalVariable("dependencies.SignatureTestClass.m5()")
        aDeps.assertDependency("dependencies.SignatureTestClass.m5()", "dependencies.SignatureTestClass", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.SignatureTestClass.m5()", "dependencies.SignatureTestClass.m5()", CALLS_METHOD)
        aDeps.assertDependency("dependencies.SignatureTestClass.m5()", "java.io.FileOutputStream", USES_RETURN_TYPE)
        //    
        //        public abstract List<String> m6(ArrayList<Integer> p);
        aDeps.assertDependency("dependencies.SignatureTestClass.m6(java.util.ArrayList)", "dependencies.SignatureTestClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.SignatureTestClass.m6(java.util.ArrayList)", "java.util.ArrayList", HAS_PARAMETER_OF_TYPE)
        aDeps.assertDependency("dependencies.SignatureTestClass.m6(java.util.ArrayList)", "java.lang.Integer", USES_TYPE_IN_TYPE_PARAMETERS)
        aDeps.assertDependency("dependencies.SignatureTestClass.m6(java.util.ArrayList)", "java.util.List", RETURNS)
        aDeps.assertDependency("dependencies.SignatureTestClass.m6(java.util.ArrayList)", "java.lang.String", USES_TYPE_IN_TYPE_PARAMETERS)
        //    }
    }

    private def assertSignatureTestSubClass(implicit aDeps: AssertableDependencies) {
        //    package dependencies;
        //    
        //    import java.io.FileOutputStream;
        //    import java.util.jar.JarInputStream;
        //    import java.util.zip.ZipInputStream;
        //    
        //    public abstract class SignatureTestSubClass extends
        //    	SignatureTestClass<ZipInputStream> {
        aDeps.assertDependency("dependencies.SignatureTestSubClass", "dependencies.SignatureTestClass", EXTENDS)
        aDeps.assertDependency("dependencies.SignatureTestSubClass", "java.util.zip.ZipInputStream", USES_TYPE_IN_TYPE_PARAMETERS)
        assertImplicitDefaultConstructor("dependencies.SignatureTestSubClass", "dependencies.SignatureTestClass")
        //        protected JarInputStream f1;
        aDeps.assertDependency("dependencies.SignatureTestSubClass.f1", "dependencies.SignatureTestSubClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.SignatureTestSubClass.f1", "java.util.jar.JarInputStream", IS_OF_TYPE)
        //    
        //        @SuppressWarnings("unchecked")
        //        public abstract Integer m3();
        aDeps.assertDependency("dependencies.SignatureTestSubClass.m3()", "dependencies.SignatureTestSubClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.SignatureTestSubClass.m3()", "java.lang.Integer", RETURNS)
        // // implicit method:
        // // public Object m3(){
        // //     return m3(); //Method m3:()Ljava/lang/Integer;
        // // }
        aDeps.assertDependency("dependencies.SignatureTestSubClass.m3()", "dependencies.SignatureTestSubClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.SignatureTestSubClass.m3()", "java.lang.Object", RETURNS)
        assertImplicitThisLocalVariable("dependencies.SignatureTestSubClass.m3()")
        aDeps.assertDependency("dependencies.SignatureTestSubClass.m3()", "dependencies.SignatureTestSubClass", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.SignatureTestSubClass.m3()", "dependencies.SignatureTestSubClass.m3()", CALLS_METHOD)
        aDeps.assertDependency("dependencies.SignatureTestSubClass.m3()", "java.lang.Integer", USES_RETURN_TYPE)
        //            
        //        @SuppressWarnings("unchecked")
        //        public abstract FileOutputStream m5();
        aDeps.assertDependency("dependencies.SignatureTestSubClass.m5()", "dependencies.SignatureTestSubClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.SignatureTestSubClass.m5()", "java.io.FileOutputStream", RETURNS)
        // // implicit method:
        // // public OutputStream m5(){
        // //     return m5(); //Method m5:()Ljava/io/FileOutputStream;
        // // }
        aDeps.assertDependency("dependencies.SignatureTestSubClass.m5()", "dependencies.SignatureTestSubClass", IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency("dependencies.SignatureTestSubClass.m5()", "java.io.OutputStream", RETURNS)
        assertImplicitThisLocalVariable("dependencies.SignatureTestSubClass.m5()")
        aDeps.assertDependency("dependencies.SignatureTestSubClass.m5()", "dependencies.SignatureTestSubClass", USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency("dependencies.SignatureTestSubClass.m5()", "dependencies.SignatureTestSubClass.m5()", CALLS_METHOD)
        aDeps.assertDependency("dependencies.SignatureTestSubClass.m5()", "java.io.FileOutputStream", USES_RETURN_TYPE)
        //    }
    }

    private def assertImplicitDefaultConstructor(className: String, superClassName: String = "java.lang.Object")(implicit aDeps: AssertableDependencies) {
        //	//implicit constructor:
        val constructorName = className+".<init>()"
        aDeps.assertDependency(constructorName, className, IS_INSTANCE_MEMBER_OF)
        aDeps.assertDependency(constructorName, superClassName, USES_METHOD_DECLARING_TYPE)
        aDeps.assertDependency(constructorName, superClassName+".<init>()", CALLS_METHOD)
        assertImplicitThisLocalVariable(constructorName)
    }

    private def assertImplicitThisLocalVariable(methodName: String)(implicit aDeps: AssertableDependencies) {
        // //implicit local variable 'this'
        aDeps.assertDependency(methodName, methodName.substring(0, methodName.substring(0, methodName.lastIndexOf('(')).lastIndexOf('.')), HAS_LOCAL_VARIABLE_OF_TYPE)
    }

    class AssertableDependencies(var deps: Dependencies) {
        def assertDependency(src: String, trgt: String, dType: DependencyType) {
            val dep = (src, trgt, dType)
            if (deps.contains(dep)) {
                deps = deps diff List(dep)
                //        println("verified dependency: " + src + "--[" + dType + "]-->" + trgt)
            }
            else {
                throw new AssertionError("Dependency "+dep+" was not extracted successfully!\nRemaining dependencies:\n"+deps.mkString("\n"))
            }
        }
    }

    class CollectorDependencyBuilder extends DependencyBuilder {

        var nodes = new ArrayBuffer[String](10000)
        var dependencies: Dependencies = Nil

        val FIELD_AND_METHOD_SEPARATOR = "."

        def getID(identifier: String): Int = {
            var index = nodes.indexOf(identifier)
            if (index == -1) {
                nodes += identifier
                index = nodes.length - 1
            }
            index
        }

        def getID(classFile: ClassFile): Int =
            getID(classFile.thisClass)

        def getID(t: Type): Int =
            getID(getNameOfUnderlyingType(t))

        def getID(definingObjectType: ObjectType, field: Field): Int =
            getID(definingObjectType, field.name)

        def getID(definingObjectType: ObjectType, fieldName: String): Int =
            getID(getNameOfUnderlyingType(definingObjectType) + FIELD_AND_METHOD_SEPARATOR + fieldName)

        def getID(definingObjectType: ObjectType, method: Method): Int =
            getID(definingObjectType, method.name, method.descriptor)

        def getID(definingObjectType: ObjectType, methodName: String, methodDescriptor: MethodDescriptor): Int =
            getID(getNameOfUnderlyingType(definingObjectType) + FIELD_AND_METHOD_SEPARATOR + getMethodAsName(methodName, methodDescriptor))

        def getMethodAsName(methodName: String, methodDescriptor: MethodDescriptor): String = {
            methodName+"("+methodDescriptor.parameterTypes.map(pT ⇒ getNameOfUnderlyingType(pT)).mkString(", ")+")"
        }

        protected def getNameOfUnderlyingType(obj: Type): String =
            if (obj.isArrayType) getNameOfUnderlyingType(obj.asInstanceOf[ArrayType].componentType) else getName(obj)
        def getName(obj: Type): String =
            obj.toJava

        private val baseTypes = Array("byte", "short", "int", "long", "float", "double", "char", "boolean", "void")

        def addDependency(src: Int, trgt: Int, dType: DependencyType) {
            val srcNode = nodes(src)
            val trgtNode = nodes(trgt)
            if (baseTypes.contains(srcNode) || baseTypes.contains(trgtNode)) {
                return
            }
            //        println("addDependency: " + srcNode + "--[" + dType + "]-->" + trgtNode)
            dependencies = (srcNode, trgtNode, dType) :: dependencies
        }
    }
}

