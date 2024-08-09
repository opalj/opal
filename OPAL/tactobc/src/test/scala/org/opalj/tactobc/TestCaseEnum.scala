package org.opalj.tactobc

object TestCaseEnum extends Enumeration {
  type TestCaseEnum = TestCase

  val HelloWorld = TestCase(
    inputDirPath = "/home/sofia/Thesis/opal/OPAL/tactobc/src/test/testingtactobc/original",
    outputDirPath = "/home/sofia/Thesis/opal/OPAL/tactobc/src/test/testingtactobc/generated",
    classFileName = "HelloWorld.class",
    packageName = ""
  )
  val HelloSofi = TestCase(
    inputDirPath = "/home/sofia/Thesis/opal/OPAL/tactobc/src/test/testingtactobc/original",
    outputDirPath = "/home/sofia/Thesis/opal/OPAL/tactobc/src/test/testingtactobc/generated",
    classFileName = "HelloSofi.class",
    packageName = ""
  )

  case class TestCase(inputDirPath: String, outputDirPath: String, classFileName: String, packageName: String) extends super.Val
}
