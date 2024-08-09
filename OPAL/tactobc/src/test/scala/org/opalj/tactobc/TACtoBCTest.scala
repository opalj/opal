package org.opalj.tactobc

import org.opalj.br.analyses.Project
import org.opalj.util.InMemoryClassLoader
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.io.{ByteArrayOutputStream, File, PrintStream}
import java.nio.file.{Files, Paths}

class TACtoBCTest extends AnyFunSpec with Matchers {

  describe("TACtoBC Class File Generation") {

    TestCaseEnum.values.foreach { testCase =>
      it(s"should generate a class file for ${testCase.asInstanceOf[TestCaseEnum.TestCase].classFileName} that produces the expected output") {
        runTestForCase(testCase.asInstanceOf[TestCaseEnum.TestCase])
      }
    }
  }

  private def runTestForCase(testCase: TestCaseEnum.TestCase): Unit = {
    // Load the original class file
    val originalClassFile = new File(Paths.get(testCase.inputDirPath, testCase.classFileName).toString)

    // Create the OPAL project from the original class file
    val project = Project(originalClassFile)

    // Compile the TAC from the original class file
    val tacs = TACtoBC.compileTAC(originalClassFile)

    val byteCodes = TACtoBC.translateTACtoBC(tacs)

    // Generate the new class file using ClassFileGenerator
    ClassFileGenerator.generateClassFiles(byteCodes, project, testCase.inputDirPath, testCase.outputDirPath, testCase.classFileName)

    // Load the original class and the generated class
    val originalClass = loadClassFromFile(testCase)
    val generatedClass = loadClassFromFile(testCase)

    // Compare the output of the main method or another method in the original and generated classes
    val originalOutput = invokeMainMethod(originalClass)
    val generatedOutput = invokeMainMethod(generatedClass)

    // Assert that the outputs are the same
    originalOutput shouldEqual generatedOutput
  }

  private def loadClassFromFile(testCase: TestCaseEnum.TestCase): Class[_] = {
    val classFileName = testCase.classFileName.replace(".class", "")

    // If the package name is empty, use just the class file name
    val className = if (testCase.packageName.isEmpty) {
      classFileName
    } else {
      s"${testCase.packageName}.$classFileName"
    }

    // Read the .class file as a byte array
    val classFile = new File(Paths.get(testCase.outputDirPath, testCase.classFileName).toString)
    val classLoader = new InMemoryClassLoader(Map(className -> Files.readAllBytes(classFile.toPath)))

    classLoader.findClass(className)
  }


  private def invokeMainMethod(clazz: Class[_]): String = {
    // Capture the output of the main method
    val outputStream = new ByteArrayOutputStream()
    val printStream = new PrintStream(outputStream)
    Console.withOut(printStream) {
      // Invoke the main method
      clazz.getMethod("main", classOf[Array[String]]).invoke(null, Array[String]())
    }
    outputStream.toString.trim
  }
}
