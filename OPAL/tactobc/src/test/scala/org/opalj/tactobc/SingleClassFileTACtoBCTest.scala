package org.opalj.tactobc

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import org.opalj.tactobc.TACtoBC.compileByteCodeFromClassFile
import org.opalj.util.InMemoryClassLoader

class SingleClassFileTACtoBCTest extends AnyFunSpec with Matchers with TACtoBCTest {

    describe("TACtoBC Single Class File Generation") {

        // define paths
        val projectRoot: String = System.getProperty("user.dir")
        val javaFileDirPath: String = s"$projectRoot/OPAL/tactobc/src/test/resources/javaFiles"
        val inputDirPath: String = s"$projectRoot/OPAL/tactobc/src/test/resources/generatedClassFiles/single/original"
        val outputDirPath: String = s"$projectRoot/OPAL/tactobc/src/test/resources/generatedClassFiles/single/generated"

        // load test files from directory
        val javaFilesDir = new File(javaFileDirPath)
        val javaFileNames: Seq[String] = Option(javaFilesDir.listFiles())
            .getOrElse(Array.empty)
            .filter(_.getName.endsWith(".java")).map(_.getName).toSeq

        javaFileNames.foreach { name =>
            val javaFileName = name
            val classFileName = name.replace(".java", ".class")
            it(s"should compile and generate class file for $javaFileName, create the TAC representation of it, convert it back to bytecode, generate a new .class file and compare the output of both original and generated .class files") {

                // Compile the Java file
                compileJavaFile("", javaFileName, javaFileDirPath, inputDirPath)

                // Load the original class file
                val originalClassFile =
                    new File(Paths.get(inputDirPath, classFileName).toString)

                // (1) compile bytecode
                compileByteCodeFromClassFile(originalClassFile)

                // Compile the TAC from the original class file
                val tacs = TACtoBC.compileTACFromClassFile(originalClassFile)

                tacs.foreach(p => println(p))

                val byteCodes = TACtoBC.translateTACStoBC(tacs)

                // Generate the new class file using TACtoBCTest
                generateClassFiles(
                    byteCodes,
                    inputDirPath,
                    outputDirPath,
                    classFileName
                )

                // Load the original class and the generated class
                val originalClass = loadClassFromFile(inputDirPath, classFileName)
                val generatedClass = loadClassFromFile(outputDirPath, classFileName)

                // Compare the output of the main method in the original and generated classes
                val originalOutput = invokeMainMethod(originalClass)
                val generatedOutput = invokeMainMethod(generatedClass)

                // Assert that the outputs are the same
                originalOutput shouldEqual generatedOutput
            }
        }
    }

    def loadClassFromFile(dirPath: String, classFileName: String): Class[_] = {
        val className = classFileName.replace(".class", "")
        val classFile = new File(Paths.get(dirPath, classFileName).toString)
        val classLoader = new InMemoryClassLoader(Map(className -> Files.readAllBytes(classFile.toPath)))
        classLoader.findClass(className)
    }

}
