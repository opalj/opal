package org.opalj.br.controlflow

import java.io._
import scala.io.StdIn
import scala.collection.immutable.HashSet
//import org.opalj.bi.TestSupport
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.instructions._
import org.opalj.br.PC
//import org.opalj.br.analyses._
import org.opalj.br.reader.Java8FrameworkWithCaching
import org.opalj.br.reader.BytecodeInstructionsCache
import org.opalj.bi.reader.ClassFileReader

object CFGDumper {

    def main(args: Array[String]): Unit = {

        val jarFile: String = input("Which JAR-File? > ")

        //   val outputDestination: String = "C:/Users/Erich Wittenbeck/Desktop/OPAL/MyTests/"+jarFile+"/"   // Für meinen Laptop
        val outputDestination: String = "C:/Users/User/Desktop/OPALTest/Dumps/"+jarFile+"/" // Für meinen Desktop

        val reader = new Java8FrameworkWithCaching(new BytecodeInstructionsCache)

        while (true) {

            val jarFileEntryName: String = input("Which Class-File? > ")

            if (jarFileEntryName == "exit")
                return

            //            val classFile: ClassFile = testProject.classFile(ObjectType(jarFileEntryName)).get
            val classFile: ClassFile = reader.ClassFile("C:/Users/User/Desktop/bup/classfiles/"+jarFile+".jar", jarFileEntryName+".class").head

            val methods = classFile.methods

            // Dumpen als TXT-Datei
            for (method ← methods) {
                dumpTXT(method, outputDestination + classFile.fqn+"/TextDumps/")

            }

            // Bauen der CFGs und dumpen als Dot-Dateien
            for (method ← methods if (method.name != "<init>")) {

                dumpDOT(method, outputDestination + classFile.fqn+"/DotDumps/")
            }
        }
    }

    def input(prompt: String): String = {
        print(prompt)
        StdIn.readLine
    }

    def printInstruction(pc: PC, inst: Instruction): Unit = println(pc+": "+inst.toString(pc))

    def dumpTXT(method: Method, outputDestination: String): Unit = {

        var byteCode: String = ""

        def instructionString(pc: PC, inst: Instruction): Unit = byteCode = byteCode + pc+": "+inst.toString(pc)+" -- "+inst.runtimeExceptions.size+"\r\n"

        method.body.get.foreach(instructionString)

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
        val cfg = ControlFlowGraph(method)

        val dotString = cfg.toDot

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