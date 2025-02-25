/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tactobc

import java.nio.file.Files
import java.nio.file.Paths
import scala.Console.println

import org.opalj.ba.CODE
import org.opalj.ba.CodeElement
import org.opalj.ba.toDA
import org.opalj.bc.Assembler
import org.opalj.br.Method
import org.opalj.br.reader.Java8Framework

object ClassFileGenerator {

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
                                // Print out the translation from TAC to Bytecode
                                println(s"Original Instructions for method $m: \n${originalBody.instructions.mkString("\n ")}")
                                println(s"New Instructions for method $m: \n${instructions.mkString("\n")}")

                                val codeAttrBuilder = CODE(instructions.toIndexedSeq)
                                val newBody = codeAttrBuilder(cf.version, m)
                                m.copy(body = Some(newBody._1))
                            case None =>
                                println(s"Warning: No bytecode found for method ${m.name}. Keeping original method body.")
                                m.copy()
                        }
                }
            }
        }
        val cfWithNewInstructions = cf.copy(methods = newMethods)
        val newRawCF = Assembler(toDA(cfWithNewInstructions))
        Files.createDirectories(outputDir.getParent)
        val newClassFile = Files.write(outputDir, newRawCF)
        println("Created class file: " + newClassFile.toAbsolutePath)
    }
}
