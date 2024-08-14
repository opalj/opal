package org.opalj.tactobc

import org.opalj.br.analyses.Project
import org.opalj.tactobc.TACtoBC.compileByteCode
import org.opalj.util.InMemoryClassLoader
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._

import java.io.{ByteArrayOutputStream, File, PrintStream}
import java.nio.file.{Files, Paths}
import scala.Console.println
import scala.sys.process._

class SingleClassFileTACtoBCTest extends AnyFunSpec with Matchers {

  describe("TACtoBC Single Class File Generation") {

    val testCases = Table(
      ("Java File Name", "Class File Name"),
      SingleClassFileTestCaseEnum.testCases.map(tc => (tc.javaFileName, tc.classFileName)): _*
    )

    forAll(testCases) { (javaFileName, classFileName) =>
      it(s"should compile and generate class file for $javaFileName, create the TAC representation of it, convert it back to bytecode, generate a new .class file and compare the output of both original and generated .class files") {

        // Compile the Java file
        compileJavaFile(javaFileName)

        // Load the original class file
        val originalClassFile = new File(Paths.get(SingleClassFileTestCaseEnum.inputDirPath, classFileName).toString)

        // Create the OPAL project from the original class file
        val project = Project(originalClassFile)

        //(1) compile bytecode
        compileByteCode(originalClassFile)

        // Compile the TAC from the original class file
        val tacs = TACtoBC.compileTAC(originalClassFile)
        // Print out TAC
        tacs.foreach { case (method, tac) =>
          tac.detach()
          println(s"Method: ${method.toJava}")
          println(tac.toString)
          println("\n")
        }
        val byteCodes = TACtoBC.translateTACtoBC(tacs)

        // Generate the new class file using ClassFileGenerator
        ClassFileGenerator.generateClassFiles(byteCodes, project, SingleClassFileTestCaseEnum.inputDirPath, SingleClassFileTestCaseEnum.outputDirPath, classFileName)

        // Load the original class and the generated class
        val originalClass = loadClassFromFile(SingleClassFileTestCaseEnum.inputDirPath, classFileName)
        val generatedClass = loadClassFromFile(SingleClassFileTestCaseEnum.outputDirPath, classFileName)

        // Compare the output of the main method in the original and generated classes
        val originalOutput = invokeMainMethod(originalClass)
        val generatedOutput = invokeMainMethod(generatedClass)

        // Assert that the outputs are the same
        originalOutput shouldEqual generatedOutput
      }
    }
  }

  private def compileJavaFile(javaFileName: String): Unit = {
    val javaFilePath = Paths.get(SingleClassFileTestCaseEnum.javaFileDirPath, javaFileName).toString
    val command = s"javac -d ${SingleClassFileTestCaseEnum.inputDirPath} $javaFilePath"
    val result = command.!

    if (result != 0) {
      throw new RuntimeException(s"Compilation of $javaFileName failed.")
    }
  }

  private def loadClassFromFile(dirPath: String, classFileName: String): Class[_] = {
    val className = classFileName.replace(".class", "")
    val classFile = new File(Paths.get(dirPath, classFileName).toString)
    val classLoader = new InMemoryClassLoader(Map(className -> Files.readAllBytes(classFile.toPath)))
    classLoader.findClass(className)
  }

  private def invokeMainMethod(clazz: Class[_]): String = {
    val outputStream = new ByteArrayOutputStream()
    val printStream = new PrintStream(outputStream)
    Console.withOut(printStream) {
      clazz.getMethod("main", classOf[Array[String]]).invoke(null, Array[String]())
    }
    outputStream.toString.trim
  }
}
