/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac2bc

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import scala.sys.process._

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import org.opalj.ba.CODE
import org.opalj.ba.CodeElement
import org.opalj.ba.toDA
import org.opalj.bc.Assembler
import org.opalj.bi.TestResources
import org.opalj.br.ClassFile
import org.opalj.br.Code
import org.opalj.br.Method
import org.opalj.br.analyses.Project
import org.opalj.tac.LazyDetachedTACAIKey
import org.opalj.util.InMemoryClassLoader

trait TACtoBCTest extends AnyFunSpec with Matchers {

    val testRoot: String = s"${System.getProperty("user.dir")}/OPAL/tac2bc/src/test/resources"

    def dirName: String

    def getSourceDir(originalFileName: String): String

    def description(originalFileName: String, testFileName: String): String

    def getTestFiles(file: File, fileName: String): IterableOnce[(String, String)]

    def extraFilesToLoad(testClassFileName: String, testInputDir: String): List[Path] = List.empty

    def executeTest(
        testInputDirName: String
    ): Unit = {
        // define paths
        val javaFileDir = s"$testRoot/$dirName"
        val originalInputDir: String = s"$testRoot/generatedClassFiles/$dirName/original"
        val testInputDir: String = s"$testRoot/generatedClassFiles/$dirName/$testInputDirName"
        val outputDir: String = s"$testRoot/generatedClassFiles/$dirName/generated"

        val resourcesDir: File = TestResources.locateTestResources(dirName, "tac2bc")

        // load test files from directory
        val testFiles = for {
            file <- resourcesDir.listFiles()
            fileName = file.getName
            testFile <- getTestFiles(file, fileName).iterator
        } yield testFile

        testFiles.foreach { case (testFileName, originalFileName) =>
            val originalClassFileName = originalFileName.replace(".java", ".class")
            val testClassFileName = testFileName.replace(".java", ".class")

            it(description(originalFileName, testFileName)) {

                // (1) Compile the original Java file to generate its .class file
                compileJavaFile(getSourceDir(originalFileName), originalFileName, javaFileDir, originalInputDir)

                // (2) Compile the test Java file to generate its .class file if it is not the same as the original
                if (testInputDir != originalInputDir) {
                    compileJavaFile(getSourceDir(originalFileName), testFileName, javaFileDir, testInputDir)
                }

                val project = Project(Paths.get(testInputDir, testClassFileName).toFile)

                // Load the test class file
                val classFile = project.allClassFiles.head

                // Compile the TAC from the test class file
                val tacs = TACtoBC.compileTACFromClassFile(classFile)(project.get(LazyDetachedTACAIKey))

                tacs.foreach(p => println(p))

                val byteCodes = TACtoBC.translateTACStoBC(tacs)

                // Generate the new class file
                generateClassFile(
                    classFile,
                    byteCodes,
                    outputDir,
                    testClassFileName
                )

                // Load the original class and the generated class
                val originalClass = loadClasses(List(Paths.get(originalInputDir, originalClassFileName))).head
                val classesToLoad =
                    Paths.get(outputDir, testClassFileName) +: extraFilesToLoad(testClassFileName, testInputDir)
                val generatedClass = loadClasses(classesToLoad).head

                // Compare the output of the main method in the original and generated classes
                val originalOutput = invokeMainMethod(originalClass)
                val generatedOutput = invokeMainMethod(generatedClass)

                // Assert that the outputs are the same
                originalOutput shouldEqual generatedOutput
            }
        }
    }

    def generateClassFile(
        classFile:     ClassFile,
        byteCodes:     Map[Method, IndexedSeq[CodeElement[Nothing]]],
        outputDirPath: String,
        classFileName: String
    ): Unit = {
        val outputFile = Paths.get(outputDirPath, classFileName)

        val newMethods = for (m <- classFile.methods) yield {
            m.body match {
                case None =>
                    m.copy() // methods which are native or abstract
                case _: Some[Code] =>
                    val codeAttrBuilder = CODE(byteCodes(m))
                    val newBody = codeAttrBuilder(classFile.version, m)
                    m.copy(body = Some(newBody._1))
            }
        }

        val cfWithNewInstructions = classFile.copy(methods = newMethods)
        val newRawCF = Assembler(toDA(cfWithNewInstructions))
        Files.createDirectories(outputFile.getParent)
        Files.write(outputFile, newRawCF)
    }

    def compileJavaFile(
        sourceFolder:             String,
        fileName:             String,
        javaFileDir:          String,
        inputDir: String
    ): Unit = {
        val javaFilePath = Paths.get(javaFileDir, sourceFolder, fileName).toString
        val command = s"javac -d $inputDir $javaFilePath"
        val result = command.!

        if (result != 0)
            throw new RuntimeException(s"Compilation of Java file ($fileName) failed.")
    }

    def invokeMainMethod(clazz: Class[_]): String = {
        val outputStream = new ByteArrayOutputStream()
        Console.withOut(outputStream) {
            clazz.getMethod("main", classOf[Array[String]]).invoke(null, Array[String]())
        }
        outputStream.toString.trim
    }

    def loadClasses(paths: List[Path]): Iterable[Class[_]] = {
        val classes = paths.map { classFilePath =>
            val className = classFilePath.getFileName.toString.replace(".class", "")
            className -> Files.readAllBytes(classFilePath)
        }

        val classLoader = new InMemoryClassLoader(classes.toMap)

        classes.map { case (className, _) =>
            classLoader.findClass(className)
        }
    }
}
