package org.opalj
package tac2bc

import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.Paths
import scala.Console.println
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import org.opalj.util.InMemoryClassLoader

class MutatedClassFileTACtoBCTest extends AnyFunSpec with Matchers with TACtoBCTest {

    describe("TACtoBC Mutation Testing") {

        // define paths
        val projectRoot: String = System.getProperty("user.dir")
        val javaFileDirPath: String = s"$projectRoot/OPAL/tac2bc/src/test/resources/org/opalj/tac2bc/javaFilesMutation"
        val inputDirOriginalJavaPath: String =
            s"$projectRoot/OPAL/tac2bc/src/test/resources/org/opalj/tac2bc/generatedClassFiles/mutation/original"
        val inputDirMutatedJavaPath: String =
            s"$projectRoot/OPAL/tac2bc/src/test/resources/org/opalj/tac2bc/generatedClassFiles/mutation/mutated"
        val outputDirPath: String =
            s"$projectRoot/OPAL/tac2bc/src/test/resources/org/opalj/tac2bc/generatedClassFiles/mutation/generated"

        // load test files from directory
        val javaFileDir = new File(javaFileDirPath)
        val mutationFiles: Seq[(String, String)] = Option(javaFileDir.listFiles())
            .getOrElse(Array.empty[File])
            .filter(_.isDirectory)
            .flatMap { subdir =>
                Option(subdir.listFiles()).getOrElse(Array.empty[File])
                    .filter(f => f.getName.endsWith(".java") && f.getName.contains("mutation"))
                    .map(f => (f.getName, subdir.getName))
            }
            .toSeq

        // run each file
        mutationFiles.foreach {
            case (mutatedJavaFileName, subfolder) =>
                val originalJavaFileName = s"${subfolder.capitalize}.java"
                val classFileOfOriginalName = s"${subfolder.capitalize}.class"
                val sourceFolder = s"/$subfolder"
                val classFileName = mutatedJavaFileName.replace(".java", ".class")

                println(s"$originalJavaFileName + $classFileOfOriginalName + $sourceFolder + $classFileName")

                it(s"should compile $originalJavaFileName, generate TAC, convert back to bytecode, " +
                    s"and compare with the mutated class file $mutatedJavaFileName") {
                    runTestCase(
                        originalJavaFileName,
                        sourceFolder,
                        classFileOfOriginalName,
                        mutatedJavaFileName,
                        classFileName,
                        inputDirMutatedJavaPath,
                        outputDirPath,
                        inputDirOriginalJavaPath,
                        javaFileDirPath
                    )
                }
        }
    }

    private def runTestCase(
        originalJavaFileName:     String,
        sourceFolder:             String,
        classFileOfOriginalName:  String,
        mutatedJavaFileName:      String,
        classFileName:            String,
        inputDirMutatedJavaPath:  String,
        outputDirPath:            String,
        inputDirOriginalJavaPath: String,
        javaFileDirPath:          String
    ): Unit = {
        try {
            // (1) Compile the original Java file to generate its .class file
            compileJavaFile(sourceFolder, originalJavaFileName, javaFileDirPath, inputDirOriginalJavaPath)

            // (2) Compile the mutated Java file to generate its .class file
            compileJavaFile(sourceFolder, mutatedJavaFileName, javaFileDirPath, inputDirMutatedJavaPath)

            // Load the mutated class file
            val mutatedClassFile = {
                new File(Paths.get(inputDirMutatedJavaPath, classFileName).toString)
            }

            // (3) Compile TAC from the original class file
            val tacs = TACtoBC.compileTACFromClassFile(mutatedClassFile)

            // Convert TAC back to bytecode
            val byteCodes = TACtoBC.translateTACStoBC(tacs)

            // Generate the new class file using TACtoBCTest
            generateClassFiles(
                byteCodes,
                inputDirMutatedJavaPath,
                outputDirPath,
                classFileName
            )

            val classesToLoad = mutable.ListBuffer[String]()
            val pathsOfClassesToLoad = mutable.ListBuffer[String]()
            classesToLoad += classFileName
            pathsOfClassesToLoad += outputDirPath

            // check if there are inner classes that need to be loaded
            findExtraFiles(classFileName, inputDirMutatedJavaPath, classesToLoad, pathsOfClassesToLoad)


            // (4) Load the original class and the mutated/generated class
            val originalClass =
                loadClassesFromDirs(
                    List(inputDirOriginalJavaPath),
                    List(classFileOfOriginalName)
                )
            val generatedClass = loadClassesFromDirs(pathsOfClassesToLoad.toList, classesToLoad.toList)

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

    def findExtraFiles(
        classFileName:           String,
        inputDirMutatedJavaPath: String,
        classesToLoad:           ListBuffer[String],
        pathsOfClassesToLoad:    ListBuffer[String]
    ): Unit = {
        val baseFileName = classFileName.replace(".class", "$")
        val directory = new File(inputDirMutatedJavaPath)

        val extraFiles = if (directory.exists && directory.isDirectory) {
            directory.listFiles
                .filter(file => file.getName.startsWith(baseFileName))
                .toSeq
        } else {
            Seq.empty
        }

        if (extraFiles.nonEmpty) {
            extraFiles.foreach { file =>
                println(s"Extra file needed: ${file.getAbsolutePath} \n only name: ${file.getName}")
                classesToLoad += file.getName
                pathsOfClassesToLoad += inputDirMutatedJavaPath
            }
        } else {
            println("No extra file needed.")
        }
    }

    private def loadClassesFromDirs(
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
}
