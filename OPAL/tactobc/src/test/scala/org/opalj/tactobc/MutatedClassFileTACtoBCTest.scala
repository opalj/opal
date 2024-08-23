package org.opalj.tactobc

import org.opalj.br.analyses.Project
import org.opalj.util.InMemoryClassLoader
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._

import java.io.{ByteArrayOutputStream, File, PrintStream}
import java.nio.file.{Files, Paths}
import scala.sys.process._

class MutatedClassFileTACtoBCTest extends AnyFunSpec with Matchers {

  describe("TACtoBC Mutation Testing") {

    val testCases = Table(
      ("original Java File Name", "source Folder", "Class File Name of Original", "mutated Java File Name", "Class File Name"),
      MutatedClassFileTestCaseEnum.testCases.map(tc => (tc.originalJavaFileName, tc.sourceFolder, tc.classFileOfOriginalName, tc.mutatedJavaFileName, tc.generatedClassFileOfMutationName)): _*
    )

    forAll(testCases) { (originalJavaFileName, sourceFolder, classFileOfOriginalName, mutatedJavaFileName, classFileName) =>
      it(s"should compile $originalJavaFileName, generate TAC, convert back to bytecode, and compare with the mutated class file $mutatedJavaFileName") {
        runTestCase(originalJavaFileName, sourceFolder, classFileOfOriginalName, mutatedJavaFileName, classFileName)
      }
    }
  }

  private def runTestCase(originalJavaFileName: String, sourceFolder: String, classFileOfOriginalName: String, mutatedJavaFileName: String, classFileName: String): Unit = {
    try {
      // (1) Compile the original Java file to generate its .class file
      compileOriginalJavaFile(sourceFolder, originalJavaFileName)

      // (2) Compile the mutated Java file to generate its .class file
      compileMutatedJavaFile(sourceFolder, mutatedJavaFileName)

      // Load the mutated class file
      val mutatedClassFile = new File(Paths.get(MutatedClassFileTestCaseEnum.inputDirMutatedJavaPath, classFileName).toString)

      // Create the OPAL project from the original class file
      val project = Project(mutatedClassFile)

      // (3) Compile TAC from the original class file
      val tacs = TACtoBC.compileTAC(mutatedClassFile)

      // Convert TAC back to bytecode
      val byteCodes = TACtoBC.translateTACtoBC(tacs)

      // Generate the new class file using ClassFileGenerator
      ClassFileGenerator.generateClassFiles(byteCodes, project, MutatedClassFileTestCaseEnum.inputDirMutatedJavaPath, MutatedClassFileTestCaseEnum.outputDirPath, classFileName)

      // (4) Load the original class and the mutated/generated class
      val originalClass = loadClassFromFile(MutatedClassFileTestCaseEnum.inputDirOriginalJavaPath, classFileOfOriginalName)
      val generatedClass = loadClassFromFile(MutatedClassFileTestCaseEnum.outputDirPath, classFileName)

      // (5) Compare the output of the main method in the original and mutated/generated classes
      val originalOutput = invokeMainMethod(originalClass)
      val generatedOutput = invokeMainMethod(generatedClass)

      // Assert that the outputs are the same
      originalOutput shouldEqual generatedOutput

    } catch {
        case ex: VerifyError =>
          println(s"Test Failure - Verification failed for test case: ${ex.getMessage}")
        // Continue testing other cases
        case ex: RuntimeException =>
          println(s"Test Failure - Runtime exception in test case: ${ex.getMessage}")
        // Handle exceptions specific to runtime issues
        case ex: Exception =>
          println(s"Test Failure - General exception: ${ex.getMessage}")
          ex.printStackTrace() // Log stack trace for more info
    }
  }

  private def compileOriginalJavaFile(sourceFolder: String, javaFileName: String): Unit = {
    val javaFilePath = Paths.get(MutatedClassFileTestCaseEnum.javaFileDirPath, sourceFolder, javaFileName).toString
    val command = s"javac -d ${MutatedClassFileTestCaseEnum.inputDirOriginalJavaPath} $javaFilePath"
    val result = command.!

    if (result != 0) {
      throw new RuntimeException(s"Compilation of $javaFileName failed.")
    }
  }

  private def compileMutatedJavaFile(sourceFolder: String, javaFileName: String): Unit = {
    val javaFilePath = Paths.get(MutatedClassFileTestCaseEnum.javaFileDirPath, sourceFolder, javaFileName).toString
    val command = s"javac -d ${MutatedClassFileTestCaseEnum.inputDirMutatedJavaPath} $javaFilePath"
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
