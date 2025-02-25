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
import org.opalj.util.InMemoryClassLoader

import scala.Console.println

class MutatedClassFileTACtoBCTest extends AnyFunSpec with Matchers {

    describe("TACtoBC Mutation Testing") {

        val projectRoot: String = System.getProperty("user.dir")
        val javaFileDirPath: String = s"$projectRoot/OPAL/tactobc/src/test/resources/javaFilesMutation"
        val inputDirOriginalJavaPath: String =
            s"$projectRoot/OPAL/tactobc/src/test/resources/generatedClassFiles/mutation/original"
        val inputDirMutatedJavaPath: String =
            s"$projectRoot/OPAL/tactobc/src/test/resources/generatedClassFiles/mutation/mutated"
        val outputDirPath: String =
            s"$projectRoot/OPAL/tactobc/src/test/resources/generatedClassFiles/mutation/generated"

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

        mutationFiles.foreach(f => println(f))

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
            compileJavaFile(
                sourceFolder,
                originalJavaFileName,
                javaFileDirPath,
                inputDirOriginalJavaPath
            )

            // (2) Compile the mutated Java file to generate its .class file
            compileJavaFile(sourceFolder, mutatedJavaFileName, javaFileDirPath, inputDirMutatedJavaPath)

            // Load the mutated class file
            val mutatedClassFile = {
                new File(Paths.get(inputDirMutatedJavaPath, classFileName).toString)
            }

            // Create the OPAL project from the original class file
//            val project = Project(mutatedClassFile)

            // (3) Compile TAC from the original class file
            val tacs = TACtoBC.compileTACFromClassFile(mutatedClassFile)

            // Print out TAC
//            tacs.foreach { case (method, tac) =>
//                tac.detach()
//                println(s"TAC for Method: ${method.toJava} \n↓\n $tac")
//            }

            // Convert TAC back to bytecode
            val byteCodes = TACtoBC.translateTACStoBC(tacs)

            // Generate the new class file using ClassFileGenerator
            ClassFileGenerator.generateClassFiles(
                byteCodes,
                inputDirMutatedJavaPath,
                outputDirPath,
                classFileName
            )

            val classesToLoad = mutable.ListBuffer[String]()
            val pathsOfClassesToLoad = mutable.ListBuffer[String]()
            classesToLoad += classFileName
            pathsOfClassesToLoad += outputDirPath

            val extraFiles = findExtraFiles(classFileName, inputDirMutatedJavaPath)

            if (extraFiles.nonEmpty) {
                extraFiles.foreach { file =>
                    println(s"Extra file needed: ${file.getAbsolutePath} \n only name: ${file.getName}")
                    classesToLoad += file.getName
                    pathsOfClassesToLoad += inputDirMutatedJavaPath
                }
            } else {
                println("No extra file needed.")
            }

            // (4) Load the original class and the mutated/generated class
            val originalClass =
                loadClassesFromFile(
                    List(inputDirOriginalJavaPath),
                    List(classFileOfOriginalName)
                )
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

    def findExtraFiles(classFileName: String, inputDirMutatedJavaPath: String): Seq[File] = {
        val baseFileName = classFileName.replace(".class", "$")
        val directory = new File(inputDirMutatedJavaPath)

        if (directory.exists && directory.isDirectory) {
            directory.listFiles
                .filter(file => file.getName.startsWith(baseFileName))
                .toSeq
        } else {
            Seq.empty
        }
    }

    private def compileJavaFile(
        sourceFolder:             String,
        javaFileName:             String,
        javaFileDirPath:          String,
        inputDirOriginalJavaPath: String
    ): Unit = {
        val javaFilePath = Paths.get(javaFileDirPath, sourceFolder, javaFileName).toString
        val command = s"javac -d ${inputDirOriginalJavaPath} $javaFilePath"
        val result = command.!

        if (result != 0) {
            throw new RuntimeException(s"Compilation of original Java file ($javaFileName) failed.")
        } else {
            println(s"Compilation of original Java file ($javaFileName) completed successfully.")
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
