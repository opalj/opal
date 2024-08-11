package org.opalj.tactobc

object SingleClassFileTestCaseEnum extends Enumeration {
  // Fixed directories for input and output
  val projectRoot: String = System.getProperty("user.dir")
  val javaFileDirPath: String = s"$projectRoot/OPAL/tactobc/src/test/resources/javaFiles"
  val inputDirPath: String = s"$projectRoot/OPAL/tactobc/src/test/classfilestocompare/original"
  val outputDirPath: String = s"$projectRoot/OPAL/tactobc/src/test/classfilestocompare/generated"

  // Test cases with Java and Class file names
  val testCases: Seq[TestCase] = Seq(
    TestCase("HelloWorld.java", "HelloWorld.class"),
    TestCase("HelloSofi.java", "HelloSofi.class"),
    TestCase("Assignment.java", "Assignment.class"),
    TestCase("ArithmeticOperations.java", "ArithmeticOperations.class"),
    TestCase("PrimitiveTypeCast.java", "PrimitiveTypeCast.class"),
    TestCase("ForLoop.java", "ForLoop.class")
  )

  // Case class to represent each test case
  case class TestCase(javaFileName: String, classFileName: String)
}
