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
import org.opalj.br.analyses.Project
import org.opalj.br.reader.Java8Framework

object ClassFileGenerator {

    def generateClassFiles(
        byteCodes:     Map[Method, Seq[CodeElement[Nothing]]],
        p:             Project[_],
        inputDirPath:  String,
        outputDirPath: String,
        classFileName: String
    ): Unit = {

        // Define Paths for input and output
        val inputFilePath = Paths.get(inputDirPath, classFileName).toString
        val outputFilePath = Paths.get(outputDirPath, classFileName).toString

        val in = () => {
            val stream = Files.newInputStream(Paths.get(inputFilePath))
            if (stream == null) throw new RuntimeException(s"Resource not found: $inputFilePath")
            stream
        }

        val cf = Java8Framework.ClassFile(in).head
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
                                println(s"New Instructions for method $m: \n${instructions.mkString("\n ")}")

                                val attributesOfOriginalBody = originalBody.attributes

                                val codeAttrBuilder = CODE(instructions.toIndexedSeq)
                                val newBody = codeAttrBuilder(cf.version, m)

                                // todo: StackMapTable needs the localVariableTable to be able to be computed
                                println(s"New body for method ${m.name}: $newBody")
                                println(attributesOfOriginalBody)
                                val result = m.copy(body = Some(newBody._1))

                                result
                            case None =>
                                println(s"Warning: No bytecode found for method ${m.name}. Keeping original method body.")
                                m.copy()
                        }
                }
            }
        }
        val cfWithNewInstructions = cf.copy(methods = newMethods)

        val newRawCF = Assembler(toDA(cfWithNewInstructions))

        val outputClassFilePath = Paths.get(outputFilePath)

        Files.createDirectories(outputClassFilePath.getParent)

        val newClassFile = Files.write(outputClassFilePath, newRawCF)
        println("Created class file: " + newClassFile.toAbsolutePath)
        // println("Class file GeneratedHelloWorldToStringDALEQUEE.class has been generated." + newClass)
        // Let's test that the new class does what it is expected to do... (we execute the
        // instrumented method)
        // todo: the map should have all class files
//    val cl = new InMemoryClassLoader(Map((TheType.toJava, newRawCF)))
//    val newClass = cl.findClass(TheType.toJava)
//    //val instance = newClass.getDeclaredConstructor().newInstance()
//    newClass.getMethod("main", (Array[String]()).getClass).invoke(null, Array[String]())
    }
}
