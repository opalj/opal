/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.tactobc

import org.opalj.ba.CodeAttributeBuilder.computeStackMapTable
import org.opalj.ba.toDA
import org.opalj.bc.Assembler
import org.opalj.br.{ArrayType, Code, CompactLineNumberTable, LocalVariable, LocalVariableTable, Method, ObjectType, StackMapTable}

import scala.Console.println
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.Instruction
import org.opalj.br.reader.Java8Framework
import org.opalj.da.ClassFileReader.ClassFile
import org.opalj.io.writeAndOpen

import java.io.ByteArrayInputStream
import java.nio.file.{Files, Paths}
import org.opalj.tac._
import org.opalj.value.ValueInformation

import java.io.File
import scala.collection.immutable.ArraySeq
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters.EnumerationHasAsScala

object TACtoBC {

  def main(args: Array[String]): Unit = {
    if (args.length != 2) {
      println("Usage: ListClassFiles <path to input class files directory> <path to output class files directory>")
      return
    }

    val inputDirPath = args(0)
    val outputDirPath = args(1)
    val inputDir = new File(inputDirPath)
    if (!inputDir.exists() || !inputDir.isDirectory) {
      println(s"Directory ${inputDir.getPath} does not exist or is not a directory.")
      return
    }

    val outputDir = new File(outputDirPath)
    if (!outputDir.exists()) {
      outputDir.mkdirs()
    }

    val classFiles = listClassFiles(inputDir)
    classFiles.foreach{
      classfile =>
        //todo: figure out how to get the input stream of the file
        //(1) compile bytecode
        compileByteCode(classfile)
        //(2) compile tac
        val tacs = compileTAC(classfile)
        // Print out TAC
        tacs.foreach { case (method, tac) =>
          tac.detach()
          println(s"Method: ${method.toJava}")
          println(tac.toString)
          println("\n")
        }
        //(3) generate bc from compiled tac
        // > Print out the translation from TAC to Bytecode
        val byteCodes = translateTACtoBC(tacs)
        byteCodes.foreach { case (method, bytecode) =>
          println(s"Method: ${method.toJava}")
          bytecode.foreach(instr => println(instr.toString))
        }
        //(4) generate .class files from translation
        val p = Project(classfile)
        generateClassFiles(byteCodes, p, inputDirPath, outputDirPath, classfile.getName)
        // println(classfile.getAbsolutePath)))
    }
  }

  def listClassFiles(directory: File): List[File] = {
    directory.listFiles().toList.filter(_.getName.endsWith(".class"))
  }

  def generateClassFiles(byteCodes: Map[Method, ArrayBuffer[(Int, Instruction)]], p: Project[_], inputDirPath: String, outputDirPath: String, classFileName: String): Unit = {
    //val helloWorldPath = "org/opalj/tactobc/testingtactobc/"
    //val benchmarkPath = "jnt/scimark2/"
    //val TheType = ObjectType(benchmarkPath.concat(classFileName.replace(".class", "")))

    // Debugging: Print the location of the class loader and resources
    val loader = this.getClass.getClassLoader
    val resources = loader.getResources("").asScala.toList
    println(s"ClassLoader: $loader")
    println(s"Resources: ${resources.mkString(", ")}")

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
            //Using find because of the extra methods that do contain the name of the method but are not part of the original file
            byteCodes.find(bc => bc._1.name.contains(m.name)) match {
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
                  100,
                  100,
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
                  100,
                  100,
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

    // Let's see the old class file...
    val odlCFHTML = ClassFile(in).head.toXHTML(None)
    val oldCFHTMLFile = writeAndOpen(odlCFHTML, "original", ".html")
    println("original: " + oldCFHTMLFile)

    // Let's see the new class file...
    val newCF = ClassFile(() => new ByteArrayInputStream(newRawCF)).head.toXHTML(None)
    println("genetated from TAC: " + writeAndOpen(newCF, "translated", ".html"))

    //println("Class file GeneratedHelloWorldToStringDALEQUEE.class has been generated." + newClass)
    // Let's test that the new class does what it is expected to do... (we execute the
    // instrumented method)
    //todo: the map should have all class files
    //val cl = new InMemoryClassLoader(Map((TheType.toJava, newRawCF)))
    //val newClass = cl.findClass(TheType.toJava)
    //val instance = newClass.getDeclaredConstructor().newInstance()
    //newClass.getMethod("main", (Array[String]()).getClass).invoke(null, null)
  }

  /**
   * Compiles the Three-Address Code (TAC) representation for all methods in the given .class file.
   *
   * @param file A File object representing the .class file to be analyzed and compiled into TAC.
   * @return A Map associating each method in the class file with its corresponding TAC representation.
   */
  def compileTAC(file: File): Map[Method, AITACode[TACMethodParameter, ValueInformation]] = {
    val p = Project(file)
    val tacProvider = p.get(LazyDetachedTACAIKey)

    // Store the TAC results in a map
    val methodTACMap = scala.collection.mutable.Map.empty[Method, AITACode[TACMethodParameter, ValueInformation]]

    for {
      cf <- p.allProjectClassFiles
      m <- cf.methods
      if m.body.isDefined
    } {
      val tac = tacProvider(m)
      methodTACMap += (m -> tac)
    }

    methodTACMap.toMap
  }

  /**
   * Compiles and prints the bytecode representation for all methods in the given .class file.
   *
   * @param file The .class file or JAR archive to be analyzed.
   * @return A Map associating each method in the class file with its bytecode instructions.
   */
  def compileByteCode(file: File): Map[Method, Array[String]] = {
    val p = Project(file)

    // A map to store the bytecode representation of each method
    val methodByteCodeMap = scala.collection.mutable.Map.empty[Method, Array[String]]

    for {
      cf <- p.allProjectClassFiles
      method <- cf.methods
      if method.body.isDefined
    } {
      // Convert the body's instructions to a human-readable format
      val instructions = method.body.get.instructions.zipWithIndex.map { case (instr, index) =>
        s"$index: ${instr}"
      }
      methodByteCodeMap += (method -> instructions.toArray)

      // Print the bytecode for each method
      println(s"Method: ${method.toJava}")
      instructions.foreach(println)
    }

    methodByteCodeMap.toMap
  }

  /**
   * Translates the TAC representations of methods back to bytecode, encapsulated within OPAL's Code structure.
   *
   * This method iterates over each method's TAC representation and generates a corresponding sequence of
   * bytecode instructions, effectively reversing the process of TAC generation.
   *
   * @param tacs A Map containing the TAC representations of methods to be translated back to bytecode.
   * @return A Map associating each method with its newly generated bytecode, wrapped in OPAL's Code structure.
   */
  def translateTACtoBC(tacs: Map[Method, AITACode[TACMethodParameter, ValueInformation]]): Map[Method, ArrayBuffer[(Int, Instruction)]] = {
    tacs.map { case (method, tacCode) =>
      // Convert the TAC representation back to bytecode for each method
      val bytecodeInstructions = translateSingleTACtoBC(method, tacCode)
      method -> bytecodeInstructions
    }
  }

  /**
   * Converts the TAC Stmts of a single method into bytecode instructions.
   *
   * This helper method processes one method's TAC representation at a time, converting it into a sequence
   * of bytecode instructions. It handles various types of TAC statements and expressions, translating them
   * into their equivalent bytecode form.
   *
   * @param tac The TAC representation of a method to be converted into bytecode.
   * @return An array of bytecode instructions representing the method's functionality
   */
  def translateSingleTACtoBC(method: Method, tac: AITACode[TACMethodParameter, ValueInformation]): ArrayBuffer[(Int, Instruction)] = {
    val tacStmts = tac.stmts.zipWithIndex
    //first pass -> prepare the LVIndexes to map the Variable to Indexes
    FirstPass.prepareLVIndexes(method, tacStmts)
    //second pass -> generate Bytecode Instructions from TAC Stmts
    val generatedByteCodeWithPC = ArrayBuffer[(Int, Instruction)]()
    val tacTargetToByteCodePcs = ArrayBuffer[(Int, Int)]()
    val switchCases = ArrayBuffer[(Int, Int)]() // To store switch case targets
    SecondPass.translateStmtsToInstructions(tacStmts, generatedByteCodeWithPC, tacTargetToByteCodePcs, switchCases)
    //third pass -> this time through the translated bytecode to calculate the right branching targets
    ThirdPass.updateTargetsOfJumpInstructions(tacStmts, generatedByteCodeWithPC, tacTargetToByteCodePcs, switchCases)
  }
}
