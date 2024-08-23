package org.opalj.tactobc

object MutatedClassFileTestCaseEnum extends Enumeration {

  // Fixed directories for input and output
  val projectRoot: String = System.getProperty("user.dir")
  val javaFileDirPath: String = s"$projectRoot/OPAL/tactobc/src/test/resources/javaFilesMutation"
  val inputDirOriginalJavaPath: String = s"$projectRoot/OPAL/tactobc/src/test/classfilestocompare/mutation/original"
  val inputDirMutatedJavaPath: String = s"$projectRoot/OPAL/tactobc/src/test/classfilestocompare/mutation/mutated"
  val outputDirPath: String = s"$projectRoot/OPAL/tactobc/src/test/classfilestocompare/mutation/generated"

  // Test cases with Java and Class file names
  val testCases: Seq[TestCase] = Seq(
    TestCase("ArithmeticOperations.java", "/arithmeticOperations", "ArithmeticOperations.class", "ArithmeticOperations_mutation_1.java", "ArithmeticOperations_mutation_1.class"),
    TestCase("ArithmeticOperations.java", "/arithmeticOperations", "ArithmeticOperations.class", "ArithmeticOperations_mutation_2.java", "ArithmeticOperations_mutation_2.class"),
    TestCase("ArithmeticOperations.java", "/arithmeticOperations", "ArithmeticOperations.class", "ArithmeticOperations_mutation_3.java", "ArithmeticOperations_mutation_3.class"),
    TestCase("ArithmeticOperations.java", "/arithmeticOperations", "ArithmeticOperations.class", "ArithmeticOperations_mutation_4.java", "ArithmeticOperations_mutation_4.class"),
    TestCase("ArithmeticOperations.java", "/arithmeticOperations", "ArithmeticOperations.class", "ArithmeticOperations_mutation_5.java", "ArithmeticOperations_mutation_5.class"),
    TestCase("Array.java", "/array", "Array.class", "Array_mutation_1.java", "Array_mutation_1.class"),
    TestCase("Array.java", "/array", "Array.class", "Array_mutation_2.java", "Array_mutation_2.class"),
    TestCase("Array.java", "/array", "Array.class", "Array_mutation_3.java", "Array_mutation_3.class"),
    TestCase("Array.java", "/array", "Array.class", "Array_mutation_4.java", "Array_mutation_4.class"),
    TestCase("Array.java", "/array", "Array.class", "Array_mutation_5.java", "Array_mutation_5.class"),
    TestCase("Assignment.java", "/assignment", "Assignment.class", "Assignment_mutation_1.java", "Assignment_mutation_1.class"),
    TestCase("Assignment.java", "/assignment", "Assignment.class", "Assignment_mutation_2.java", "Assignment_mutation_2.class"),
    TestCase("Assignment.java", "/assignment", "Assignment.class", "Assignment_mutation_3.java", "Assignment_mutation_3.class"),
    TestCase("Assignment.java", "/assignment", "Assignment.class", "Assignment_mutation_4.java", "Assignment_mutation_4.class"),
    TestCase("Assignment.java", "/assignment", "Assignment.class", "Assignment_mutation_5.java", "Assignment_mutation_5.class"),
    TestCase("BigNumbers.java", "/bigNumbers", "BigNumbers.class", "BigNumbers_mutation_1.java", "BigNumbers_mutation_1.class"),
    TestCase("BigNumbers.java", "/bigNumbers", "BigNumbers.class", "BigNumbers_mutation_2.java", "BigNumbers_mutation_2.class"),
    TestCase("BigNumbers.java", "/bigNumbers", "BigNumbers.class", "BigNumbers_mutation_3.java", "BigNumbers_mutation_3.class"),
    TestCase("BigNumbers.java", "/bigNumbers", "BigNumbers.class", "BigNumbers_mutation_4.java", "BigNumbers_mutation_4.class"),
    TestCase("BigNumbers.java", "/bigNumbers", "BigNumbers.class", "BigNumbers_mutation_5.java", "BigNumbers_mutation_5.class"),
    TestCase("CheckCast.java", "/checkCast", "CheckCast.class", "CheckCast_mutation_1.java", "CheckCast_mutation_1.class"),
    TestCase("CheckCast.java", "/checkCast", "CheckCast.class", "CheckCast_mutation_2.java", "CheckCast_mutation_2.class"),
    TestCase("CheckCast.java", "/checkCast", "CheckCast.class", "CheckCast_mutation_3.java", "CheckCast_mutation_3.class"),
    TestCase("CheckCast.java", "/checkCast", "CheckCast.class", "CheckCast_mutation_4.java", "CheckCast_mutation_4.class"),
    TestCase("CheckCast.java", "/checkCast", "CheckCast.class", "CheckCast_mutation_5.java", "CheckCast_mutation_5.class"),
    TestCase("Compare.java", "/compare", "Compare.class", "Compare_mutation_1.java", "Compare_mutation_1.class"),
    TestCase("Compare.java", "/compare", "Compare.class", "Compare_mutation_2.java", "Compare_mutation_2.class"),
    TestCase("Compare.java", "/compare", "Compare.class", "Compare_mutation_3.java", "Compare_mutation_3.class"),
    TestCase("Compare.java", "/compare", "Compare.class", "Compare_mutation_4.java", "Compare_mutation_4.class"),
    TestCase("Compare.java", "/compare", "Compare.class", "Compare_mutation_5.java", "Compare_mutation_5.class"),
    TestCase("Constants.java", "/constants", "Constants.class", "Constants_mutation_1.java", "Constants_mutation_1.class"),
    TestCase("Constants.java", "/constants", "Constants.class", "Constants_mutation_2.java", "Constants_mutation_2.class"),
    TestCase("Constants.java", "/constants", "Constants.class", "Constants_mutation_3.java", "Constants_mutation_3.class"),
    TestCase("Constants.java", "/constants", "Constants.class", "Constants_mutation_4.java", "Constants_mutation_4.class"),
    TestCase("Constants.java", "/constants", "Constants.class", "Constants_mutation_5.java", "Constants_mutation_5.class"),
    TestCase("ForLoop.java", "/forLoop", "ForLoop.class", "ForLoop_mutation_1.java", "ForLoop_mutation_1.class"),
    TestCase("ForLoop.java", "/forLoop", "ForLoop.class", "ForLoop_mutation_2.java", "ForLoop_mutation_2.class"),
    TestCase("ForLoop.java", "/forLoop", "ForLoop.class", "ForLoop_mutation_3.java", "ForLoop_mutation_3.class"),
    TestCase("ForLoop.java", "/forLoop", "ForLoop.class", "ForLoop_mutation_4.java", "ForLoop_mutation_4.class"),
    TestCase("ForLoop.java", "/forLoop", "ForLoop.class", "ForLoop_mutation_5.java", "ForLoop_mutation_5.class"),
    TestCase("If.java", "/if", "If.class", "If_mutation_1.java", "If_mutation_1.class"),
    TestCase("If.java", "/if", "If.class", "If_mutation_2.java", "If_mutation_2.class"),
    TestCase("If.java", "/if", "If.class", "If_mutation_3.java", "If_mutation_3.class"),
    TestCase("If.java", "/if", "If.class", "If_mutation_4.java", "If_mutation_4.class"),
    TestCase("If.java", "/if", "If.class", "If_mutation_5.java", "If_mutation_5.class"),
    TestCase("If.java", "/ifZero", "If.class", "If_mutation_1.java", "If_mutation_1.class"),
    TestCase("If.java", "/ifZero", "If.class", "If_mutation_2.java", "If_mutation_2.class"),
    TestCase("If.java", "/ifZero", "If.class", "If_mutation_3.java", "If_mutation_3.class"),
    TestCase("If.java", "/ifZero", "If.class", "If_mutation_4.java", "If_mutation_4.class"),
    TestCase("If.java", "/ifZero", "If.class", "If_mutation_5.java", "If_mutation_5.class"),
    TestCase("InstanceField.java", "/instanceField", "InstanceField.class", "InstanceField_mutation_1.java", "InstanceField_mutation_1.class"),
    TestCase("InstanceField.java", "/instanceField", "InstanceField.class", "InstanceField_mutation_2.java", "InstanceField_mutation_2.class"),
    TestCase("InstanceField.java", "/instanceField", "InstanceField.class", "InstanceField_mutation_3.java", "InstanceField_mutation_3.class"),
    TestCase("InstanceField.java", "/instanceField", "InstanceField.class", "InstanceField_mutation_4.java", "InstanceField_mutation_4.class"),
    TestCase("InstanceField.java", "/instanceField", "InstanceField.class", "InstanceField_mutation_5.java", "InstanceField_mutation_5.class"),
    TestCase("InstanceOf.java", "/instanceOf", "InstanceOf.class", "InstanceOf_mutation_1.java", "InstanceOf_mutation_1.class"),
    TestCase("InstanceOf.java", "/instanceOf", "InstanceOf.class", "InstanceOf_mutation_2.java", "InstanceOf_mutation_2.class"),
    TestCase("InstanceOf.java", "/instanceOf", "InstanceOf.class", "InstanceOf_mutation_3.java", "InstanceOf_mutation_3.class"),
    TestCase("InstanceOf.java", "/instanceOf", "InstanceOf.class", "InstanceOf_mutation_4.java", "InstanceOf_mutation_4.class"),
    TestCase("InstanceOf.java", "/instanceOf", "InstanceOf.class", "InstanceOf_mutation_5.java", "InstanceOf_mutation_5.class")
  )

  // Case class to represent each test case
  case class TestCase(originalJavaFileName: String, sourceFolder: String, classFileOfOriginalName: String, mutatedJavaFileName: String, generatedClassFileOfMutationName: String)
}
