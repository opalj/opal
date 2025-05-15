/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac2bc

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Paths
import scala.sys.process._

import org.opalj.ba.CODE
import org.opalj.ba.CodeElement
import org.opalj.ba.toDA
import org.opalj.bc.Assembler
import org.opalj.br.Method
import org.opalj.br.reader.Java8Framework

trait TACtoBCTest {

    def generateClassFiles(
        byteCodes:     Map[Method, Seq[CodeElement[Nothing]]],
        inputDirPath:  String,
        outputDirPath: String,
        classFileName: String
    ): Unit = {

        // Define Paths for input and output
        val inputDir = Paths.get(inputDirPath, classFileName)
        val outputDir = Paths.get(outputDirPath, classFileName)

        val cf = Java8Framework.ClassFile(() => Files.newInputStream(inputDir)).head

        val newMethods = {
            for (m <- cf.methods) yield {
                m.body match {
                    case None =>
                        m.copy() // methods which are native and abstract ...
                    case Some(originalBody) =>
                        val methodDescriptor = m.descriptor
                        byteCodes.find {
                            case (method, _) =>
                                method.name == m.name &&
                                    method.descriptor == methodDescriptor
                        } match {
                            case Some((_, instructions)) =>
                                val codeAttrBuilder = CODE(instructions.toIndexedSeq)
                                val newBody = codeAttrBuilder(cf.version, m)
                                m.copy(body = Some(newBody._1))
                            case None =>
                                throw new RuntimeException(s"Warning: No bytecode found for method ${m.name}")
                        }
                }
            }
        }
        val cfWithNewInstructions = cf.copy(methods = newMethods)
        val newRawCF = Assembler(toDA(cfWithNewInstructions))
        Files.createDirectories(outputDir.getParent)
        val newClassFile = Files.write(outputDir, newRawCF)
    }

    def compileJavaFile(
        sourceFolder:             String,
        javaFileName:             String,
        javaFileDirPath:          String,
        inputDirOriginalJavaPath: String
    ): Unit = {
        val javaFilePath = Paths.get(javaFileDirPath, sourceFolder, javaFileName).toString
        val command = s"javac -d $inputDirOriginalJavaPath $javaFilePath"
        val result = command.!

        if (result != 0)
            throw new RuntimeException(s"Compilation of original Java file ($javaFileName) failed.")
    }

    def invokeMainMethod(clazz: Class[_]): String = {
        val outputStream = new ByteArrayOutputStream()
        val printStream = new PrintStream(outputStream)
        Console.withOut(printStream) {
            clazz.getMethod("main", classOf[Array[String]]).invoke(null, Array[String]())
        }
        outputStream.toString.trim
    }
}
