package org.opalj.tactobc

object SingleClassFileTestCaseEnum extends Enumeration {
  // Fixed directories for input and output
  val projectRoot: String = System.getProperty("user.dir")
  val javaFileDirPath: String = s"$projectRoot/OPAL/tactobc/src/test/resources/javaFiles"
  val inputDirPath: String = s"$projectRoot/OPAL/tactobc/src/test/classfilestocompare/original"
  val outputDirPath: String = s"$projectRoot/OPAL/tactobc/src/test/classfilestocompare/generated"

  // Test cases with Java and Class file names
  val testCases: Seq[TestCase] = Seq(
    TestCase("Assignment.java", "Assignment.class"),
    TestCase("ArithmeticOperations.java", "ArithmeticOperations.class"),
    TestCase("PrimitiveTypeCast.java", "PrimitiveTypeCast.class"),
    TestCase("ForLoop.java", "ForLoop.class"),
    TestCase("MethodCall.java", "MethodCall.class"),
    TestCase("Array.java", "Array.class"),
    TestCase("Parameters.java", "Parameters.class"),
    TestCase("StaticField.java", "StaticField.class"),
    TestCase("InstanceField.java", "InstanceField.class"),
    TestCase("If.java", "If.class"),
    TestCase("WhileLoop.java", "WhileLoop.class"),
    TestCase("CheckCast.java", "CheckCast.class"),
    TestCase("Compare.java", "Compare.class"),
    TestCase("Jsr.java", "Jsr.class"),
    TestCase("Constants.java", "Constants.class"),
    TestCase("Negation.java", "Negation.class"),
    TestCase("InstanceOf.java", "InstanceOf.class"),
    TestCase("InvokeInterface.java", "InvokeInterface.class"),
    TestCase("InvokeDynamic.java", "InvokeDynamic.class"),
    TestCase("Switch.java", "Switch.class"),
    TestCase("Reassignment.java", "Reassignment.class"),
    TestCase("IfZero.java", "IfZero.class")
  )

  // Case class to represent each test case
  case class TestCase(javaFileName: String, classFileName: String)
}
