/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tactobc

import org.opalj.ba.CodeAttributeBuilder.computeStackMapTable
import org.opalj.ba.toDA
import org.opalj.bc.Assembler
import org.opalj.br.{ArrayType, Code, CompactLineNumberTable, LocalVariable, LocalVariableTable, Method, ObjectType, StackMapTable}
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.Instruction
import org.opalj.br.reader.Java8Framework

import java.nio.file.{Files, Paths}
import scala.Console.println
import scala.collection.immutable.ArraySeq
import scala.collection.mutable.ArrayBuffer

object ClassFileGenerator {

  def generateClassFiles(byteCodes: Map[Method, ArrayBuffer[(Int, Instruction)]], p: Project[_], inputDirPath: String, outputDirPath: String, classFileName: String): Unit = {
    //val helloWorldPath = "org/opalj/tactobc/testingtactobc/"
//    val benchmarkPath = "jnt/scimark2/"
//    val TheType = ObjectType(benchmarkPath.concat(classFileName.replace(".class", "")))

    // Debugging: Print the location of the class loader and resources
//    val loader = this.getClass.getClassLoader
//    val resources = loader.getResources("").asScala.toList
//    println(s"ClassLoader: $loader")
//    println(s"Resources: ${resources.mkString(", ")}")
    // Dynamically determine the type and resource stream
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
            val methodSignature = m.descriptor.toJava(m.name)
            byteCodes.find {
              case (method, _) =>
                method.name == m.name &&
                  method.descriptor.toJava(method.name) == methodSignature
            } match {
              case Some((_, instructions)) =>
                // Prepare new instructions array with null values where necessary
                val maxPc = instructions.map(_._1).max
                val newInstructionsWithNulls = new Array[Instruction](maxPc + 1)

                // Initialize array with nulls
                for (i <- newInstructionsWithNulls.indices) {
                  newInstructionsWithNulls(i) = null
                }

                // Fill in actual instructions
                for ((pc, instruction) <- instructions) {
                  newInstructionsWithNulls(pc) = instruction
                }

                // Print each instruction with its PC - to see if the NULLS are placed correctly
                println(s"Instructions for method ${m.name}:")
                newInstructionsWithNulls.zipWithIndex.foreach {
                  case (instruction, pc) =>
                    if (instruction == null) {
                      println(s"PC $pc: NULL")
                    } else {
                      println(s"PC $pc: ${instruction.toString}")
                    }
                }

                // Print out the translation from TAC to Bytecode with nulls
                //println("newInstrWithNulls" + newInstructionsWithNulls.foreach(instruction => println(instruction.toString)))
                // Debugging: Print the instructions being passed to the new Code object
                println(s"Original Instructions for ${m.name}: ${originalBody.instructions.mkString(", ")}")
                println(s"New Instructions for ${m.name}: ${newInstructionsWithNulls.mkString(", ")}")
                //ToDo: use CodeAttribute builder
                val attributesOfOriginalBody = originalBody.attributes
                //Todo:
                val newLocalVariableTable = LocalVariableTable(ArraySeq(
                  LocalVariable(0, maxPc + 1, "args", ArrayType(ObjectType("java/lang/String")), 0)
                ))

                // Remove CompactLineNumberTable attribute
                val newAttributes = attributesOfOriginalBody.filterNot(_.isInstanceOf[CompactLineNumberTable])
                // Replace the LocalVariableTable attribute in the original attributes
                val finalAttributes = newAttributes.map {
                  case _: LocalVariableTable => newLocalVariableTable
                  case other => other
                }

                val newBody = Code(
                  //todo: use the size of the local variables map
                  10000,
                  10000,
                  newInstructionsWithNulls,
                  originalBody.exceptionHandlers,
                  finalAttributes)

                //todo: StackMapTable needs the localVariableTable to be able to be computed
                println(s"New body for method ${m.name}: $newBody")
                println(attributesOfOriginalBody)
                val result = m.copy(body = Some(newBody))

                result
              case None =>
                println(s"Warning: No bytecode found for method ${m.name}. Keeping original method body.")
                m.copy()
            }
        }
      }
    }
    val cfWithNewInstructions = cf.copy(methods = newMethods)
    val newMethodsForReal = {
      for (m <- cfWithNewInstructions.methods) yield {
        m.body match {
          case None =>
            m.copy() // methods which are native and abstract ...
          case Some(originalBody) =>
            //Using find because of the extra methods that do contain the name of the method but are not part of the original file
            byteCodes.find(bc => bc._1.name.contains(m.name)) match {
              case Some((_, instructions)) =>
                //ToDo: use CodeAttribute builder
                val attributesOfOriginalBody = originalBody.attributes

                val newStackMapTable = computeStackMapTable(m)(p.classHierarchy)
                //live variable
                // Replace the LocalVariableTable attribute in the original attributes
                val newAttributes1 = attributesOfOriginalBody.map {
                  case _: StackMapTable => newStackMapTable
                  case other => other
                }
                val newBody1 = Code(
                  //todo: use the size of the local variables map
                  10000,
                  10000,
                  originalBody.instructions,
                  originalBody.exceptionHandlers,
                  newAttributes1)

                //todo: StackMapTable needs the localVariableTable to be able to be computed
                println(s"New body for method ${m.name}: $newBody1")
                println(attributesOfOriginalBody)
                val result = m.copy(body = Some(newBody1))

                val it = result.body.get.iterator
                val n = it.next()
                val n1 = it.next()
                print(n.toString + n1.toString)

                result
              case None =>
                println(s"Warning: No bytecode found for method ${m.name}. Keeping original method body.")
                m.copy()
            }
        }
      }
    }
    val cfWithNewInstructionsForReal = cf.copy(methods = newMethodsForReal)
    val newRawCF = Assembler(toDA(cfWithNewInstructionsForReal))
    val outputClassFilePath = Paths.get(outputFilePath)
    Files.createDirectories(outputClassFilePath.getParent)
    val newClassFile = Files.write(outputClassFilePath, newRawCF)
    println("Created class file: " + newClassFile.toAbsolutePath)
    //println("Class file GeneratedHelloWorldToStringDALEQUEE.class has been generated." + newClass)
    // Let's test that the new class does what it is expected to do... (we execute the
    // instrumented method)
    //todo: the map should have all class files
//    val cl = new InMemoryClassLoader(Map((TheType.toJava, newRawCF)))
//    val newClass = cl.findClass(TheType.toJava)
//    //val instance = newClass.getDeclaredConstructor().newInstance()
//    newClass.getMethod("main", (Array[String]()).getClass).invoke(null, Array[String]())
  }
}
