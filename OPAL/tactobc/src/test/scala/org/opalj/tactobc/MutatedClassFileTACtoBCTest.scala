package org.opalj.tactobc

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Paths
import scala.collection.mutable
import scala.sys.process._

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableDrivenPropertyChecks._

import org.opalj.br.analyses.Project
import org.opalj.util.InMemoryClassLoader

class MutatedClassFileTACtoBCTest extends AnyFunSpec with Matchers {

    describe("TACtoBC Mutation Testing") {

        val testCases = Table(
            (
                "original Java File Name",
                "source Folder",
                "Class File Name of Original",
                "mutated Java File Name",
                "Class File Name"
            ),
            MutatedClassFileTestCaseEnum.testCases.map(tc =>
                (
                    tc.originalJavaFileName,
                    tc.sourceFolder,
                    tc.classFileOfOriginalName,
                    tc.mutatedJavaFileName,
                    tc.generatedClassFileOfMutationName
                )
            ): _*
        )

        forAll(testCases) {
            (
                originalJavaFileName,
                sourceFolder,
                classFileOfOriginalName,
                mutatedJavaFileName,
                classFileName
            ) =>
                it(s"should compile $originalJavaFileName, generate TAC, convert back to bytecode, " +
                    s"and compare with the mutated class file $mutatedJavaFileName") {
                    runTestCase(
                        originalJavaFileName,
                        sourceFolder,
                        classFileOfOriginalName,
                        mutatedJavaFileName,
                        classFileName
                    )
                }
        }
    }

    private def runTestCase(
        originalJavaFileName:    String,
        sourceFolder:            String,
        classFileOfOriginalName: String,
        mutatedJavaFileName:     String,
        classFileName:           String
    ): Unit = {
        try {
            // (1) Compile the original Java file to generate its .class file
            compileOriginalJavaFile(sourceFolder, originalJavaFileName)

            // (2) Compile the mutated Java file to generate its .class file
            compileMutatedJavaFile(sourceFolder, mutatedJavaFileName)

            // Load the mutated class file
            val mutatedClassFile = {
                new File(Paths.get(MutatedClassFileTestCaseEnum.inputDirMutatedJavaPath, classFileName).toString)
            }

            // Create the OPAL project from the original class file
            val project = Project(mutatedClassFile)

            // (3) Compile TAC from the original class file
            val tacs = TACtoBC.compileTAC(mutatedClassFile)

            // Convert TAC back to bytecode
            val byteCodes = TACtoBC.translateTACStoBC(tacs)

            // Generate the new class file using ClassFileGenerator
            ClassFileGenerator.generateClassFiles(
                byteCodes,
                project,
                MutatedClassFileTestCaseEnum.inputDirMutatedJavaPath,
                MutatedClassFileTestCaseEnum.outputDirPath,
                classFileName
            )

            val classesToLoad = mutable.ListBuffer[String]()
            val pathsOfClassesToLoad = mutable.ListBuffer[String]()
            classesToLoad += classFileName
            pathsOfClassesToLoad += MutatedClassFileTestCaseEnum.outputDirPath

            val extraFile: Option[File] = findExtraFile(classFileName)
            extraFile match {
                case Some(file) =>
                    println(s"Extra file needed: ${file.getAbsolutePath} \n only name: ${extraFile.get.getName}")
                    classesToLoad += extraFile.get.getName
                    pathsOfClassesToLoad += MutatedClassFileTestCaseEnum.inputDirMutatedJavaPath
                case None =>
                    println("No extra file needed.")
            }

            // (4) Load the original class and the mutated/generated class
            val originalClass =
                loadClassesFromFile(List(MutatedClassFileTestCaseEnum.inputDirOriginalJavaPath), List(classFileOfOriginalName))
            val generatedClass = loadClassesFromFile(pathsOfClassesToLoad.toList, classesToLoad.toList)

            // (5) Compare the output of the main method in the original and mutated/generated classes
            val originalOutput = invokeMainMethod(originalClass.head)
            val generatedOutput = invokeMainMethod(generatedClass.head)

            // Assert that the outputs are the same
            originalOutput shouldEqual generatedOutput

        } catch {
            case ex: VerifyError =>
                println(s"Test Failure - Verification failed for test case: ${ex.getMessage}")
                fail()
            // Continue testing other cases
            case ex: RuntimeException =>
                println(s"Test Failure - Runtime exception in test case: ${ex.getMessage}")
                fail()
            // Handle exceptions specific to runtime issues
            case ex: Exception =>
                println(s"Test Failure - General exception: ${ex.getMessage}")
                ex.printStackTrace() // Log stack trace for more info
                fail()
        }
    }

    def findExtraFile(classFileName: String): Option[File] = {
        val baseFileName = classFileName.replace(".class", "$")
        val directory = new File(MutatedClassFileTestCaseEnum.inputDirMutatedJavaPath)
        if (directory.exists && directory.isDirectory) {
            directory.listFiles
                .find(file => file.getName.startsWith(baseFileName))
        } else {
            None
        }
    }

    private def compileOriginalJavaFile(sourceFolder: String, javaFileName: String): Unit = {
        val javaFilePath = Paths.get(MutatedClassFileTestCaseEnum.javaFileDirPath, sourceFolder, javaFileName).toString
        val command = s"javac -d ${MutatedClassFileTestCaseEnum.inputDirOriginalJavaPath} $javaFilePath"
        val result = command.!

        if (result != 0) {
            throw new RuntimeException(s"Compilation of original Java file ($javaFileName) failed.")
        } else {
            println(s"Compilation of original Java file ($javaFileName) completed successfully.")
        }
    }

    private def compileMutatedJavaFile(sourceFolder: String, javaFileName: String): Unit = {
        val javaFilePath = Paths.get(MutatedClassFileTestCaseEnum.javaFileDirPath, sourceFolder, javaFileName).toString
        val command = s"javac -d ${MutatedClassFileTestCaseEnum.inputDirMutatedJavaPath} $javaFilePath"
        val result = command.!

        if (result != 0) {
            throw new RuntimeException(s"Compilation of mutated Java file ($javaFileName) failed.")
        } else {
            println(s"Compilation of mutated Java file ($javaFileName) completed successfully.")
        }
    }

    private def loadClassesFromFile(
        dirPaths:       List[String],
        classFileNames: List[String]
    ): List[Class[_]] = {
        val classMap = mutable.Map[String, Array[Byte]]()

        val filePathMap = classFileNames.map { classFileName =>
            classFileName -> dirPaths.map(dirPath => Paths.get(dirPath, classFileName).toString)
                .find(path => new File(path).exists())
                .getOrElse(throw new FileNotFoundException(
                    s"Class file $classFileName not found in provided directories"
                ))
        }.toMap

        filePathMap.foreach { case (classFileName, filePath) =>
            val className = classFileName.replace(".class", "")
            val classFile = new File(filePath)
            classMap(className) = Files.readAllBytes(classFile.toPath)
        }

        val classLoader = new InMemoryClassLoader(classMap.toMap)

        classFileNames.map { classFileName =>
            val className = classFileName.replace(".class", "")
            classLoader.findClass(className)
        }
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
