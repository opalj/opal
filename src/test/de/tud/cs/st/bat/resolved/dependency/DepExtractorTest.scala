package de.tud.cs.st.bat.resolved
package dependency
import org.scalatest.FunSuite
import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import org.scalatest.Reporter
import org.scalatest.Stopper
import org.scalatest.Tracker
import org.scalatest.events.TestStarting
import de.tud.cs.st.bat.resolved.reader.Java6Framework
import org.scalatest.events.TestSucceeded
import org.scalatest.events.TestFailed
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import DependencyType._

/**
 * @author Thomas Schlosser
 */
@RunWith(classOf[JUnitRunner])
class DepExtractorTest extends FunSuite with de.tud.cs.st.util.perf.BasicPerformanceEvaluation {

  type Dependency = (String, String, DependencyType)
  type Dependencies = List[Dependency]

  /*
   * All class files stored in the zip file "DepExtractorTestProject" found in the test data directory.
   */
  private val testClasses = {
    var classFiles = List.empty[de.tud.cs.st.bat.resolved.ClassFile]
    // The location of the "test/data" directory depends on the current directory used for 
    // running this test suite... i.e. whether the current directory is the directory where
    // this class / this source file is stored or the OPAL root directory. 
    var file = new File("../../../../../../../test/classfiles/DepExtractorTestProject.zip")
    if (!file.exists()) file = new File("test/classfiles/DepExtractorTestProject.zip")
    if (!file.exists() || !file.isFile || !file.canRead || !file.getName.endsWith(".zip")) throw new Exception("Required zip file 'DepExtractorTestProject.zip' in 'test/classfiles' could not be found or read!")
    val zipfile = new ZipFile(file)
    val zipentries = (zipfile).entries
    while (zipentries.hasMoreElements) {
      val zipentry = zipentries.nextElement
      if (!zipentry.isDirectory && zipentry.getName.endsWith(".class")) {
        classFiles :+= Java6Framework.ClassFile(() => zipfile.getInputStream(zipentry))
      }
    }
    classFiles
  }

  test("Dependency extraction") {
    // create dependency builder that collects all added dependencies
    val depBuilder = new DepBuilder {
      var nodes = Array.empty[String]
      var deps: Dependencies = List()

      def getID(identifier: String): Int = {
        var index = nodes.indexOf(identifier)
        if (index == -1) {
          nodes :+= identifier
          index = nodes.length - 1
        }
        index
      }

      def addDep(src: Int, trgt: Int, dType: DependencyType) = {
        val srcNode = nodes(src)
        val trgtNode = nodes(trgt)
        //        println("addDep: " + srcNode + "--[" + dType + "]-->" + trgtNode)
        deps :+= (srcNode, trgtNode, dType)
      }
    }
    val dependencyExtractor = new DepExtractor(depBuilder)

    for (classFile <- testClasses) {
      // process classFile using dependency extractor
      dependencyExtractor.process(classFile)
    }

    //verification...
    implicit val aDeps = new AssertableDependencies(depBuilder.deps)

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
    //TODO: generics have also to be considered
    //TODO: make dependency types more fine-grained...especially the types used in InstructionDepExtractor!
    // ...and change the corresponding verification

    assert(aDeps.deps.isEmpty, "Too many [" + aDeps.deps.size + "] dependencies have been extracted:\n" + aDeps.deps.mkString("\n"))
  }

  private def assertTestClass(implicit aDeps: AssertableDependencies) {
    //    package test;
    //
    //    import java.util.ArrayList;
    //    import java.util.List;
    //    
    //    public class TestClass implements TestInterface {
    aDeps.assertDependency("test.TestClass", "test.TestInterface", IMPLEMENTS)
    aDeps.assertDependency("test.TestClass", "java.lang.Object", EXTENDS)
    assertImplicitDefaultConstructor("test.TestClass")
    //        public void testMethod() {
    aDeps.assertDependency("test.TestClass.testMethod()", "test.TestClass", IS_DEFINED_IN)
    //    	List<String> list = new ArrayList<String>();
    //TODO: add: aDeps.assertDependency("test.TestClass.testMethod()", "java.util.List", USED_TYPE)
    aDeps.assertDependency("test.TestClass.testMethod()", "java.util.ArrayList", USED_TYPE)
    aDeps.assertDependency("test.TestClass.testMethod()", "java.util.ArrayList.<init>()", METHOD_CALL)
    aDeps.assertDependency("test.TestClass.testMethod()", "java.util.ArrayList", USED_TYPE)
    //    	list.add(null);
    //TODO: check why this fails: aDeps.assertDependency("test.TestClass.testMethod()", "java.util.List.add(java.lang.String)", METHOD_CALL)
    // -> Parameter type results from a generic type. Hence, it refers to java.lang.Object.
    aDeps.assertDependency("test.TestClass.testMethod()", "java.util.List.add(java.lang.Object)", METHOD_CALL)
    aDeps.assertDependency("test.TestClass.testMethod()", "java.util.List", USED_TYPE)
    aDeps.assertDependency("test.TestClass.testMethod()", "java.lang.Object", USED_TYPE)
    //        }
    //    
    //        public String testMethod(Integer i, int j) {
    aDeps.assertDependency("test.TestClass.testMethod(java.lang.Integer, int)", "test.TestClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer", PARAMETER_TYPE)
    aDeps.assertDependency("test.TestClass.testMethod(java.lang.Integer, int)", "java.lang.String", RETURN_TYPE)
    //    	if (i != null && i.intValue() > j) {
    aDeps.assertDependency("test.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer.intValue()", METHOD_CALL)
    aDeps.assertDependency("test.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer", USED_TYPE)
    //    	    return i.toString();
    aDeps.assertDependency("test.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer.toString()", METHOD_CALL)
    aDeps.assertDependency("test.TestClass.testMethod(java.lang.Integer, int)", "java.lang.Integer", USED_TYPE)
    aDeps.assertDependency("test.TestClass.testMethod(java.lang.Integer, int)", "java.lang.String", USED_TYPE)
    //    	}
    //    	return String.valueOf(j);
    aDeps.assertDependency("test.TestClass.testMethod(java.lang.Integer, int)", "java.lang.String.valueOf(int)", METHOD_CALL)
    aDeps.assertDependency("test.TestClass.testMethod(java.lang.Integer, int)", "java.lang.String", USED_TYPE)
    aDeps.assertDependency("test.TestClass.testMethod(java.lang.Integer, int)", "java.lang.String", USED_TYPE)
    //        }
    //    }
  }

  private def assertTestInterface(implicit aDeps: AssertableDependencies) {
    //    package test;
    //
    //    public interface TestInterface {
    aDeps.assertDependency("test.TestInterface", "java.lang.Object", EXTENDS)
    //        void testMethod();
    aDeps.assertDependency("test.TestInterface.testMethod()", "test.TestInterface", IS_DEFINED_IN)
    //    
    //        String testMethod(Integer i, int j);
    aDeps.assertDependency("test.TestInterface.testMethod(java.lang.Integer, int)", "test.TestInterface", IS_DEFINED_IN)
    aDeps.assertDependency("test.TestInterface.testMethod(java.lang.Integer, int)", "java.lang.Integer", PARAMETER_TYPE)
    aDeps.assertDependency("test.TestInterface.testMethod(java.lang.Integer, int)", "java.lang.String", RETURN_TYPE)
    //    }
  }

  private def assertMarkerInterface(implicit aDeps: AssertableDependencies) {
    //    package test.sub;
    //
    //    public interface MarkerInterface {
    aDeps.assertDependency("test.sub.MarkerInterface", "java.lang.Object", EXTENDS)
    //    
    //    }
  }

  private def assertDeprecatedInterface(implicit aDeps: AssertableDependencies) {
    //    package test.sub;
    //
    //    import test.TestInterface;
    //    
    //    @Deprecated
    //    public interface DeprecatedInterface extends TestInterface, MarkerInterface {
    aDeps.assertDependency("test.sub.DeprecatedInterface", "java.lang.Object", EXTENDS)
    aDeps.assertDependency("test.sub.DeprecatedInterface", "test.TestInterface", IMPLEMENTS)
    aDeps.assertDependency("test.sub.DeprecatedInterface", "test.sub.MarkerInterface", IMPLEMENTS)
    aDeps.assertDependency("test.sub.DeprecatedInterface", "java.lang.Deprecated", ANNOTATION_TYPE)
    //    
    //        @Deprecated
    //        public void deprecatedMethod();
    aDeps.assertDependency("test.sub.DeprecatedInterface.deprecatedMethod()", "test.sub.DeprecatedInterface", IS_DEFINED_IN)
    aDeps.assertDependency("test.sub.DeprecatedInterface.deprecatedMethod()", "java.lang.Deprecated", ANNOTATION_TYPE)
    //    
    //        public void methodDeprParam(@Deprecated int i);
    aDeps.assertDependency("test.sub.DeprecatedInterface.methodDeprParam(int)", "test.sub.DeprecatedInterface", IS_DEFINED_IN)
    aDeps.assertDependency("test.sub.DeprecatedInterface.methodDeprParam(int)", "java.lang.Deprecated", PARAMETER_ANNOTATION_TYPE)
    //    }
  }

  private def assertFieldsClass(implicit aDeps: AssertableDependencies) {
    //    package test;
    //
    //    public class FieldsClass {
    aDeps.assertDependency("test.FieldsClass", "java.lang.Object", EXTENDS)
    assertImplicitDefaultConstructor("test.FieldsClass")
    //        public final static String CONSTANT = "constant";
    aDeps.assertDependency("test.FieldsClass.CONSTANT", "test.FieldsClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.FieldsClass.CONSTANT", "java.lang.String", FIELD_TYPE)
    aDeps.assertDependency("test.FieldsClass.CONSTANT", "java.lang.String", CONSTANT_VALUE_TYPE)
    //        private Integer i;
    aDeps.assertDependency("test.FieldsClass.i", "test.FieldsClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.FieldsClass.i", "java.lang.Integer", FIELD_TYPE)
    //    
    //        @Deprecated
    //        protected int deprecatedField;
    aDeps.assertDependency("test.FieldsClass.deprecatedField", "test.FieldsClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.FieldsClass.deprecatedField", "java.lang.Deprecated", ANNOTATION_TYPE)
    //    
    //        private Integer readField() {
    aDeps.assertDependency("test.FieldsClass.readField()", "test.FieldsClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.FieldsClass.readField()", "java.lang.Integer", RETURN_TYPE)
    //    	return i;
    aDeps.assertDependency("test.FieldsClass.readField()", "test.FieldsClass.i", FIELD_READ)
    aDeps.assertDependency("test.FieldsClass.readField()", "java.lang.Integer", USED_TYPE)
    //        }
    //    
    //        private void writeField(Integer j) {
    aDeps.assertDependency("test.FieldsClass.writeField(java.lang.Integer)", "test.FieldsClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.FieldsClass.writeField(java.lang.Integer)", "java.lang.Integer", PARAMETER_TYPE)
    //    	i = j;
    aDeps.assertDependency("test.FieldsClass.writeField(java.lang.Integer)", "test.FieldsClass.i", FIELD_WRITE)
    aDeps.assertDependency("test.FieldsClass.writeField(java.lang.Integer)", "java.lang.Integer", USED_TYPE)
    //        }
    //    
    //        public Integer readWrite(Integer j) {
    aDeps.assertDependency("test.FieldsClass.readWrite(java.lang.Integer)", "test.FieldsClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", PARAMETER_TYPE)
    aDeps.assertDependency("test.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", RETURN_TYPE)
    //    	Integer result = readField();
    aDeps.assertDependency("test.FieldsClass.readWrite(java.lang.Integer)", "test.FieldsClass.readField()", METHOD_CALL)
    aDeps.assertDependency("test.FieldsClass.readWrite(java.lang.Integer)", "test.FieldsClass", USED_TYPE)
    aDeps.assertDependency("test.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", USED_TYPE)
    //    	writeField(j);
    aDeps.assertDependency("test.FieldsClass.readWrite(java.lang.Integer)", "test.FieldsClass.writeField(java.lang.Integer)", METHOD_CALL)
    aDeps.assertDependency("test.FieldsClass.readWrite(java.lang.Integer)", "test.FieldsClass", USED_TYPE)
    aDeps.assertDependency("test.FieldsClass.readWrite(java.lang.Integer)", "java.lang.Integer", USED_TYPE)
    //    	return result;
    //        }
    //    }
  }

  private def assertOuterAndInnerClass(implicit aDeps: AssertableDependencies) {
    //    package test;
    //
    //    public class OuterClass {
    aDeps.assertDependency("test.OuterClass", "java.lang.Object", EXTENDS)
    assertImplicitDefaultConstructor("test.OuterClass")
    //        class InnerClass {
    aDeps.assertDependency("test.OuterClass$InnerClass", "java.lang.Object", EXTENDS)
    aDeps.assertDependency("test.OuterClass$InnerClass", "test.OuterClass", IS_DEFINED_IN)
    //	//implicit field:
    aDeps.assertDependency("test.OuterClass$InnerClass.this$0", "test.OuterClass$InnerClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.OuterClass$InnerClass.this$0", "test.OuterClass", FIELD_TYPE)
    //		public InnerClass(Integer i) {
    aDeps.assertDependency("test.OuterClass$InnerClass.<init>(test.OuterClass, java.lang.Integer)", "test.OuterClass$InnerClass", IS_DEFINED_IN)
    //	//implicit constructor parameter:
    aDeps.assertDependency("test.OuterClass$InnerClass.<init>(test.OuterClass, java.lang.Integer)", "test.OuterClass", PARAMETER_TYPE)
    aDeps.assertDependency("test.OuterClass$InnerClass.<init>(test.OuterClass, java.lang.Integer)", "java.lang.Integer", PARAMETER_TYPE)
    aDeps.assertDependency("test.OuterClass$InnerClass.<init>(test.OuterClass, java.lang.Integer)", "java.lang.Object.<init>()", METHOD_CALL)
    aDeps.assertDependency("test.OuterClass$InnerClass.<init>(test.OuterClass, java.lang.Integer)", "java.lang.Object", USED_TYPE)
    //	// write to implicit field:
    aDeps.assertDependency("test.OuterClass$InnerClass.<init>(test.OuterClass, java.lang.Integer)", "test.OuterClass$InnerClass.this$0", FIELD_WRITE)
    aDeps.assertDependency("test.OuterClass$InnerClass.<init>(test.OuterClass, java.lang.Integer)", "test.OuterClass", USED_TYPE)
    //		}
    //        }
    //    }
  }

  private def assertEnclosingMethodAndInnerClass(implicit aDeps: AssertableDependencies) {
    //    package test;
    //    
    //    public class EnclosingMethodClass {
    aDeps.assertDependency("test.EnclosingMethodClass", "java.lang.Object", EXTENDS)
    assertImplicitDefaultConstructor("test.EnclosingMethodClass")
    //  //implicit field definition in the default constructor
    aDeps.assertDependency("test.EnclosingMethodClass.<init>()", "test.EnclosingMethodClass$1", USED_TYPE)
    aDeps.assertDependency("test.EnclosingMethodClass.<init>()", "test.EnclosingMethodClass$1.<init>(test.EnclosingMethodClass)", METHOD_CALL)
    aDeps.assertDependency("test.EnclosingMethodClass.<init>()", "test.EnclosingMethodClass$1", USED_TYPE)
    aDeps.assertDependency("test.EnclosingMethodClass.<init>()", "test.EnclosingMethodClass", USED_TYPE) // parameter
    aDeps.assertDependency("test.EnclosingMethodClass.<init>()", "test.EnclosingMethodClass.enclosingField", FIELD_WRITE)
    aDeps.assertDependency("test.EnclosingMethodClass.<init>()", "java.lang.Object", USED_TYPE) // field type
    //  //implicit field definition in the class initialization method
    aDeps.assertDependency("test.EnclosingMethodClass.<clinit>()", "test.EnclosingMethodClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.EnclosingMethodClass.<clinit>()", "test.EnclosingMethodClass$2", USED_TYPE)
    aDeps.assertDependency("test.EnclosingMethodClass.<clinit>()", "test.EnclosingMethodClass$2.<init>()", METHOD_CALL)
    aDeps.assertDependency("test.EnclosingMethodClass.<clinit>()", "test.EnclosingMethodClass$2", USED_TYPE)
    aDeps.assertDependency("test.EnclosingMethodClass.<clinit>()", "test.EnclosingMethodClass.staticEnclosingField", FIELD_WRITE)
    aDeps.assertDependency("test.EnclosingMethodClass.<clinit>()", "java.lang.Object", USED_TYPE) // field type
    //    
    //        public Object enclosingField = new Object() {
    aDeps.assertDependency("test.EnclosingMethodClass.enclosingField", "test.EnclosingMethodClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.EnclosingMethodClass.enclosingField", "java.lang.Object", FIELD_TYPE)
    //        };
    aDeps.assertDependency("test.EnclosingMethodClass$1", "java.lang.Object", EXTENDS)
    //	//implicit field:
    aDeps.assertDependency("test.EnclosingMethodClass$1.this$0", "test.EnclosingMethodClass$1", IS_DEFINED_IN)
    aDeps.assertDependency("test.EnclosingMethodClass$1.this$0", "test.EnclosingMethodClass", FIELD_TYPE)
    //	//implicit constructor:
    aDeps.assertDependency("test.EnclosingMethodClass$1.<init>(test.EnclosingMethodClass)", "test.EnclosingMethodClass$1", IS_DEFINED_IN)
    aDeps.assertDependency("test.EnclosingMethodClass$1.<init>(test.EnclosingMethodClass)", "test.EnclosingMethodClass", PARAMETER_TYPE)
    aDeps.assertDependency("test.EnclosingMethodClass$1.<init>(test.EnclosingMethodClass)", "java.lang.Object.<init>()", METHOD_CALL)
    aDeps.assertDependency("test.EnclosingMethodClass$1.<init>(test.EnclosingMethodClass)", "java.lang.Object", USED_TYPE)
    //	// write to implicit field:
    aDeps.assertDependency("test.EnclosingMethodClass$1.<init>(test.EnclosingMethodClass)", "test.EnclosingMethodClass$1.this$0", FIELD_WRITE)
    aDeps.assertDependency("test.EnclosingMethodClass$1.<init>(test.EnclosingMethodClass)", "test.EnclosingMethodClass", USED_TYPE)
    //
    //        public static Object staticEnclosingField = new Object() {
    aDeps.assertDependency("test.EnclosingMethodClass.staticEnclosingField", "test.EnclosingMethodClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.EnclosingMethodClass.staticEnclosingField", "java.lang.Object", FIELD_TYPE)
    //        };
    aDeps.assertDependency("test.EnclosingMethodClass$2", "java.lang.Object", EXTENDS)
    //	//implicit constructor:
    aDeps.assertDependency("test.EnclosingMethodClass$2.<init>()", "test.EnclosingMethodClass$2", IS_DEFINED_IN)
    aDeps.assertDependency("test.EnclosingMethodClass$2.<init>()", "java.lang.Object.<init>()", METHOD_CALL)
    aDeps.assertDependency("test.EnclosingMethodClass$2.<init>()", "java.lang.Object", USED_TYPE)
    //
    //        public void enclosingMethod() {
    aDeps.assertDependency("test.EnclosingMethodClass.enclosingMethod()", "test.EnclosingMethodClass", IS_DEFINED_IN)
    //    	new Object() {
    aDeps.assertDependency("test.EnclosingMethodClass$3", "java.lang.Object", EXTENDS)
    aDeps.assertDependency("test.EnclosingMethodClass$3", "test.EnclosingMethodClass.enclosingMethod()", IS_DEFINED_IN)
    //	//implicit field:
    aDeps.assertDependency("test.EnclosingMethodClass$3.this$0", "test.EnclosingMethodClass$3", IS_DEFINED_IN)
    aDeps.assertDependency("test.EnclosingMethodClass$3.this$0", "test.EnclosingMethodClass", FIELD_TYPE)
    //	//implicit constructor:
    aDeps.assertDependency("test.EnclosingMethodClass$3.<init>(test.EnclosingMethodClass)", "test.EnclosingMethodClass$3", IS_DEFINED_IN)
    aDeps.assertDependency("test.EnclosingMethodClass$3.<init>(test.EnclosingMethodClass)", "test.EnclosingMethodClass", PARAMETER_TYPE)
    aDeps.assertDependency("test.EnclosingMethodClass$3.<init>(test.EnclosingMethodClass)", "java.lang.Object.<init>()", METHOD_CALL)
    aDeps.assertDependency("test.EnclosingMethodClass$3.<init>(test.EnclosingMethodClass)", "java.lang.Object", USED_TYPE)
    //	// write to implicit field:
    aDeps.assertDependency("test.EnclosingMethodClass$3.<init>(test.EnclosingMethodClass)", "test.EnclosingMethodClass$3.this$0", FIELD_WRITE)
    aDeps.assertDependency("test.EnclosingMethodClass$3.<init>(test.EnclosingMethodClass)", "test.EnclosingMethodClass", USED_TYPE)
    //    	    public void innerMethod() {
    aDeps.assertDependency("test.EnclosingMethodClass$3.innerMethod()", "test.EnclosingMethodClass$3", IS_DEFINED_IN)
    //    	    }
    //    	}.innerMethod();
    aDeps.assertDependency("test.EnclosingMethodClass.enclosingMethod()", "test.EnclosingMethodClass$3", USED_TYPE)
    aDeps.assertDependency("test.EnclosingMethodClass.enclosingMethod()", "test.EnclosingMethodClass$3.<init>(test.EnclosingMethodClass)", METHOD_CALL)
    aDeps.assertDependency("test.EnclosingMethodClass.enclosingMethod()", "test.EnclosingMethodClass$3", USED_TYPE) // declaring class
    aDeps.assertDependency("test.EnclosingMethodClass.enclosingMethod()", "test.EnclosingMethodClass", USED_TYPE) // method parameter

    aDeps.assertDependency("test.EnclosingMethodClass.enclosingMethod()", "test.EnclosingMethodClass$3.innerMethod()", METHOD_CALL)
    aDeps.assertDependency("test.EnclosingMethodClass.enclosingMethod()", "test.EnclosingMethodClass$3", USED_TYPE)
    //        }
    //    }
  }

  private def assertExceptionTestClass(implicit aDeps: AssertableDependencies) {
    //    package test;
    //    
    //    import java.util.FormatterClosedException;
    //    
    //    import javax.naming.OperationNotSupportedException;
    //    
    //    public class ExceptionTestClass {
    aDeps.assertDependency("test.ExceptionTestClass", "java.lang.Object", EXTENDS)
    assertImplicitDefaultConstructor("test.ExceptionTestClass")
    //    
    //        public void testMethod() throws IllegalStateException,
    //    	    OperationNotSupportedException {
    aDeps.assertDependency("test.ExceptionTestClass.testMethod()", "test.ExceptionTestClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.ExceptionTestClass.testMethod()", "java.lang.IllegalStateException", THROWS_EXCEPTION_TYPE)
    aDeps.assertDependency("test.ExceptionTestClass.testMethod()", "javax.naming.OperationNotSupportedException", THROWS_EXCEPTION_TYPE)
    //    	throw new FormatterClosedException();
    aDeps.assertDependency("test.ExceptionTestClass.testMethod()", "java.util.FormatterClosedException", USED_TYPE)
    aDeps.assertDependency("test.ExceptionTestClass.testMethod()", "java.util.FormatterClosedException.<init>()", METHOD_CALL)
    aDeps.assertDependency("test.ExceptionTestClass.testMethod()", "java.util.FormatterClosedException", USED_TYPE)
    //        }
    //
    //        public void catchMethod() {
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "test.ExceptionTestClass", IS_DEFINED_IN)
    //    	try {
    //    	    try {
    //    		testMethod();
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "test.ExceptionTestClass.testMethod()", METHOD_CALL)
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "test.ExceptionTestClass", USED_TYPE)
    //    	    } catch (IllegalStateException e) {
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "java.lang.IllegalStateException", CATCHED_EXCEPTION_TYPE)
    //    	    }
    //    	} catch (Exception e) {
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "java.lang.Exception", CATCHED_EXCEPTION_TYPE)
    //    	} finally{
    //    	    Integer.valueOf(42);
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "java.lang.Integer.valueOf(int)", METHOD_CALL)
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "java.lang.Integer", USED_TYPE)
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "java.lang.Integer", USED_TYPE) // return type
    //TODO: check if multi-dependencies from finally blocks can be eliminated!
    // The next six dependencies result from required special handling of the finally block
    // Depending on the way the finally block were reached it has to throw an Exception or return normally
    // Hence, the bytecode contains the  finally block three times.
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "java.lang.Integer.valueOf(int)", METHOD_CALL)
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "java.lang.Integer", USED_TYPE)
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "java.lang.Integer", USED_TYPE) // return type
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "java.lang.Integer.valueOf(int)", METHOD_CALL)
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "java.lang.Integer", USED_TYPE)
    aDeps.assertDependency("test.ExceptionTestClass.catchMethod()", "java.lang.Integer", USED_TYPE) // return type
    //    	}
    //        }
    //    }
  }

  private def assertTestAnnotation(implicit aDeps: AssertableDependencies) {
    //    package test;
    //    
    //    import java.lang.annotation.ElementType;
    //    
    //    public @interface TestAnnotation {
    aDeps.assertDependency("test.TestAnnotation", "java.lang.Object", EXTENDS)
    aDeps.assertDependency("test.TestAnnotation", "java.lang.annotation.Annotation", IMPLEMENTS)
    //        public abstract String stringValue() default "default";
    aDeps.assertDependency("test.TestAnnotation.stringValue()", "test.TestAnnotation", IS_DEFINED_IN)
    aDeps.assertDependency("test.TestAnnotation.stringValue()", "java.lang.String", RETURN_TYPE)
    //    
    //        public abstract Class<?> classValue() default String.class;
    aDeps.assertDependency("test.TestAnnotation.classValue()", "test.TestAnnotation", IS_DEFINED_IN)
    aDeps.assertDependency("test.TestAnnotation.classValue()", "java.lang.Class", RETURN_TYPE)
    aDeps.assertDependency("test.TestAnnotation.classValue()", "java.lang.String", USED_DEFAULT_CLASS_VALUE_TYPE)
    //    
    //        public abstract ElementType enumValue() default ElementType.TYPE;
    aDeps.assertDependency("test.TestAnnotation.enumValue()", "test.TestAnnotation", IS_DEFINED_IN)
    aDeps.assertDependency("test.TestAnnotation.enumValue()", "java.lang.annotation.ElementType", RETURN_TYPE)
    aDeps.assertDependency("test.TestAnnotation.enumValue()", "java.lang.annotation.ElementType", USED_DEFAULT_ENUM_VALUE_TYPE)
    aDeps.assertDependency("test.TestAnnotation.enumValue()", "java.lang.annotation.ElementType.TYPE", USED_ENUM_VALUE)
    //    
    //        public abstract SuppressWarnings annotationValue() default @SuppressWarnings("default");
    aDeps.assertDependency("test.TestAnnotation.annotationValue()", "test.TestAnnotation", IS_DEFINED_IN)
    aDeps.assertDependency("test.TestAnnotation.annotationValue()", "java.lang.SuppressWarnings", RETURN_TYPE)
    aDeps.assertDependency("test.TestAnnotation.annotationValue()", "java.lang.SuppressWarnings", USED_DEFAULT_ANNOTATION_VALUE_TYPE)
    //    
    //        public abstract Class<?>[] arrayClassValue() default { String.class,
    //    	    Integer.class };
    aDeps.assertDependency("test.TestAnnotation.arrayClassValue()", "test.TestAnnotation", IS_DEFINED_IN)
    aDeps.assertDependency("test.TestAnnotation.arrayClassValue()", "java.lang.Class[]", RETURN_TYPE)
    aDeps.assertDependency("test.TestAnnotation.arrayClassValue()", "java.lang.String", USED_DEFAULT_CLASS_VALUE_TYPE)
    aDeps.assertDependency("test.TestAnnotation.arrayClassValue()", "java.lang.Integer", USED_DEFAULT_CLASS_VALUE_TYPE)
    //    }
  }

  private def assertAnnotationDefaultAttributeTestClass(implicit aDeps: AssertableDependencies) {
    //    package test;
    //    
    //    import java.lang.annotation.ElementType;
    //    
    //    @TestAnnotation
    //    public class AnnotationDefaultAttributeTestClass {
    aDeps.assertDependency("test.AnnotationDefaultAttributeTestClass", "java.lang.Object", EXTENDS)
    aDeps.assertDependency("test.AnnotationDefaultAttributeTestClass", "test.TestAnnotation", ANNOTATION_TYPE)
    assertImplicitDefaultConstructor("test.AnnotationDefaultAttributeTestClass")
    //    
    //        @TestAnnotation(stringValue = "noDefault", classValue = Integer.class, enumValue = ElementType.METHOD, annotationValue = @SuppressWarnings("noDefault"), arrayClassValue = {
    //    	    Long.class, Boolean.class })
    //        public void testMethod() {
    aDeps.assertDependency("test.AnnotationDefaultAttributeTestClass.testMethod()", "test.AnnotationDefaultAttributeTestClass", IS_DEFINED_IN)
    aDeps.assertDependency("test.AnnotationDefaultAttributeTestClass.testMethod()", "test.TestAnnotation", ANNOTATION_TYPE)
    aDeps.assertDependency("test.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.Integer", USED_DEFAULT_CLASS_VALUE_TYPE)
    aDeps.assertDependency("test.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.annotation.ElementType", USED_DEFAULT_ENUM_VALUE_TYPE)
    aDeps.assertDependency("test.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.annotation.ElementType.METHOD", USED_ENUM_VALUE)
    aDeps.assertDependency("test.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.SuppressWarnings", USED_DEFAULT_ANNOTATION_VALUE_TYPE)
    aDeps.assertDependency("test.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.Long", USED_DEFAULT_CLASS_VALUE_TYPE)
    aDeps.assertDependency("test.AnnotationDefaultAttributeTestClass.testMethod()", "java.lang.Boolean", USED_DEFAULT_CLASS_VALUE_TYPE)
    //        }
    //    }
  }

  private def assertImplicitDefaultConstructor(className: String)(implicit aDeps: AssertableDependencies) {
    //	//implicit constructor:
    aDeps.assertDependency(className + ".<init>()", className, IS_DEFINED_IN)
    aDeps.assertDependency(className + ".<init>()", "java.lang.Object.<init>()", METHOD_CALL)
    aDeps.assertDependency(className + ".<init>()", "java.lang.Object", USED_TYPE)
  }

  class AssertableDependencies(var deps: Dependencies) {
    def assertDependency(src: String, trgt: String, dType: DependencyType) {
      val dep = (src, trgt, dType)
      if (deps.contains(dep)) {
        deps = deps diff List(dep)
        //        println("verified dependency: " + src + "--[" + dType + "]-->" + trgt)
      } else {
        throw new AssertionError("Dependency " + dep + " was not extracted successfully!\nRemaining dependencies:\n" + deps.mkString("\n"))
      }
    }
  }

}
