/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj.br.cfg

import java.io._
import scala.io.StdIn
import org.opalj.bi.TestSupport
import org.opalj.br.analyses.Project
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.PC
import org.opalj.br.instructions.Instruction
import org.opalj.bytecode.BytecodeProcessingFailedException

/**
 * @author Erich Wittenbeck
 */

/**
 * A console-based utility for debugging. This object will dump, for a given class-file,
 * the bytecode of all it's methods as txt-files, including the exception-tables.
 *
 * At the same time, it will dump the CFGs of the methods as .dot-files.
 *
 * ==Usage==
 *
 * The first console-prompt will ask for a path to a directory where the txt- and dot-files
 * are to be dumped. It must end with a '/'-character.
 *
 * The second prompt asks for the name of a JAR-file (without the .jar-suffix) contained
 * within the 'classfiles'-subdirectory of the project.
 *
 * It will then list all classfiles within the JAR. These are to be entered into the
 * console in order to commence the dumping process for each of them.
 *
 * Entering 'exit' at any point will stop the execution of this object's main method.
 */
object CFGDumper {
    def main(args: Array[String]): Unit = {

        def input(prompt: String): String = {
            print(prompt)
            StdIn.readLine
        }

        val outputDestination: String = input("Where do the dumps go? > ")

        if (outputDestination == "exit")
            return

        val jarFile: String = input("Which JAR-File? > ")

        if (jarFile == "exit")
            return

        val testJAR = "classfiles/"+jarFile+".jar"
        val testFolder = TestSupport.locateTestResources(testJAR, "br")
        val testProject = Project(testFolder)

        //   val outputDestination: String = "C:/Users/Erich Wittenbeck/Desktop/OPAL/MyTests/"+jarFile+"/"   // Für meinen Laptop
        //        val outputDestination: String = "C:/Users/User/Desktop/OPALTest/Dumps/"+jarFile+"/" // Für meinen Desktop

        println("choose one of the following:")
        for (classFile ← testProject.allClassFiles)
            println(classFile.fqn)

        while (true) {

            val jarFileEntryName: String = input("Which Class-File? > ")

            if (jarFileEntryName == "exit")
                return

            val classFile: ClassFile = testProject.classFile(ObjectType(jarFileEntryName)).get

            val methods = classFile.methods

            // Dumpen als TXT-Datei
            for (method ← methods if (method.body.nonEmpty)) {

                dumpTXT(method, outputDestination + jarFile+"/"+classFile.fqn+"/TextDumps/")

            }

            // Bauen der CFGs und dumpen als Dot-Dateien
            for (method ← methods if (method.name != "<init>"); if (method.body.nonEmpty)) {

                dumpDOT(method, outputDestination + jarFile+"/"+classFile.fqn+"/DotDumps/")
            }
        }
    }

    def printInstruction(pc: PC, inst: Instruction): Unit = println(pc+": "+inst.toString(pc))

    def dumpTXT(method: Method, outputDestination: String): Unit = {

        var byteCode: String = ""

        def instructionString(pc: PC, inst: Instruction): Unit = byteCode = byteCode + pc+": "+inst.toString(pc)+" -- "+inst.jvmExceptions /*.size*/ +"\r\n"

        method.body.get.foreach(instructionString)

        if (method.body.get.exceptionHandlers.isEmpty)
            byteCode = byteCode+"\r\n No exceptions are being handled"

        for (handler ← method.body.get.exceptionHandlers) {
            byteCode = byteCode+"From "+handler.startPC+" to "+handler.endPC+" target: "+handler.handlerPC+"; Catching: "+handler.catchType.getOrElse("None")+"\r\n"
        }

        val outputFileTXT = new File(outputDestination, method.name+".txt")

        if (!outputFileTXT.exists())
            outputFileTXT.getParentFile().mkdirs()

        try {
            val writer = new PrintWriter(outputFileTXT)
            writer.print(byteCode)
            writer.close
        } catch {
            case e: IOException ⇒ {}
        }
    }

    def dumpDOT(method: Method, outputDestination: String): Unit = {
        var cfg: ControlFlowGraph = null

        try {
            cfg = ControlFlowGraph(method)
        } catch {
            case bpfe: BytecodeProcessingFailedException ⇒ { return }
        }

        val dotString: String = cfg.toDot

        val outputFileDOT = new File(outputDestination, method.name+".dot")

        if (!outputFileDOT.exists())
            outputFileDOT.getParentFile().mkdirs()

        try {
            val writer = new PrintWriter(outputFileDOT)
            writer.print(dotString)
            writer.close
        } catch {
            case e: IOException ⇒ {}
        }
    }
}