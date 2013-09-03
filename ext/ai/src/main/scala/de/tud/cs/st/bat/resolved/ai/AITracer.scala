/* License (BSD Style License):
 * Copyright (c) 2009 - 2013
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
 *  - Neither the name of the Software Technology Group or Technische
 *    Universität Darmstadt nor the names of its contributors may be used to
 *    endorse or promote products derived from this software without specific
 *    prior written permission.
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
package de.tud.cs.st
package bat
package resolved
package ai

/**
 * Defines the interface between the abstract interpreter and the module for
 * tracing the interpreter's behavior. In general, BATAI calls the defined methods
 * at the specified point in time.
 *
 * @author Michael Eichberg
 */
trait AITracer {

    /**
     * Called by BATAI before an instruction is evaluated.
     */
    def instructionEvalution[D <: Domain[_]](
        domain: D,
        pc: Int,
        instruction: Instruction,
        operands: List[D#DomainValue],
        locals: Array[D#DomainValue]): Unit

    /**
     * Called whenever two paths converge and, hence, two values need
     * to be merged.
     */
    def merge[D <: Domain[_]](
        pc: Int,
        thisOperands: D#Operands,
        thisLocals: D#Locals,
        otherOperands: D#Operands,
        otherLocals: D#Locals, result: Update[(D#Operands, D#Locals)])

    /**
     * Called when the analyzed method throws an exception that is not catched within
     * the method.
     */
    def abnormalReturn[D <: Domain[_]](pc: Int, exception: D#DomainValue)

}

/**
 * A tracer that prints out a trace's results on the console.
 *
 * @author Michael Eichberg
 */
trait ConsoleTracer extends AITracer {

    def instructionEvalution[D <: Domain[_]](
        domain: D,
        pc: Int,
        instruction: Instruction,
        operands: List[D#DomainValue],
        locals: Array[D#DomainValue]): Unit = {

        println(
            pc+":"+instruction+" [\n"+
                operands.mkString("\toperands:\n\t\t", ",\n\t\t", "\n\t;\n") +
                locals.map(l ⇒ if (l eq null) "-" else l.toString).zipWithIndex.map(v ⇒ v._2+":"+v._1).
                mkString("\tlocals:\n\t\t", ",\n\t\t", "\n")+"\t]")
    }

    def merge[D <: Domain[_]](
        pc: Int,
        thisOperands: D#Operands,
        thisLocals: D#Locals,
        otherOperands: D#Operands,
        otherLocals: D#Locals, result: Update[(D#Operands, D#Locals)]) {

        print(Console.BLUE + pc+": MERGE :")
        result match {
            case NoUpdate ⇒ println("no changes")
            case u @ SomeUpdate((updatedOperands, updatedLocals)) ⇒
                println(u.updateType)
                println(
                    thisOperands.
                        zip(otherOperands).
                        map(v ⇒ "given "+v._1+"\n\t\tmerge "+v._2).
                        zip(updatedOperands).
                        map(v ⇒ v._1+"\n\t\t=>    "+v._2).
                        mkString("\tOperands:\n\t\t", "\n\t\t----------------\n\t\t", "")
                )
                println(
                    thisLocals.
                        zipWithIndex.
                        filter(v ⇒ v._1 ne null).
                        map(v ⇒ v._2+":\n\t\tgiven "+v._1).
                        zip(otherLocals).
                        map(v ⇒ v._1+"\n\t\tmerge "+v._2).
                        zip(updatedLocals).
                        map(v ⇒ v._1+"\n\t\t=>    "+v._2).
                        mkString("\tLocals:\n\t\t", ",\n\t\t", "")
                )
        }
        println(Console.RESET)
    }

    def abnormalReturn[D <: Domain[_]](pc: Int, exception: D#DomainValue) {
        println(Console.BOLD +
            Console.RED +
            pc+": RETURN FROM METHOD DUE TO UNHANDLED EXCEPTION :"+exception +
            Console.RESET)
    }

}

/**
 * A small interpreter that enables us to easily perform the abstract interpretation of a
 * specific method.
 *
 * @author Michael Eichberg
 */
object InterpretMethod {

    import de.tud.cs.st.util.ControlAbstractions._

    private object AI extends AI {

        def isInterrupted = Thread.interrupted()

        val tracer = Some(new ConsoleTracer {})
    }

    def main(args: Array[String]) {
        if (args.size != 3) {
            println("You have to specify the method that should be interpreted.")
            println("\tFirst parameter: a jar file or class file or a directory containing jar files or class files")
            println("\tSecond parameter: the name of a class in binary notation (use \"/\" as the package separator")
            println("\tThird parameter: the name of a method of the class")
            return ;
        }
        val fileName = args(0)
        val className = args(1)
        val methodName = args(2)

        val file = new java.io.File(fileName)
        val classFiles =
            try {
                reader.Java7Framework.ClassFiles(file)
            } catch {
                case e: Exception ⇒
                    println(Console.RED+"cannot read file: "+e.getMessage() + Console.RESET)
                    return ;
            }
        val classFile =
            classFiles.map(_._1).find(_.thisClass.className == className) match {
                case Some(classFile) ⇒ classFile
                case None ⇒
                    println(Console.RED+"cannot find the class: "+className + Console.RESET)
                    return ;
            }
        val method =
            classFile.methods.find(_.name == methodName) match {
                case Some(method) ⇒ method
                case None ⇒
                    println(Console.RED+"cannot find the method: "+methodName + Console.RESET)
                    return ;
            }

        val result = AI(classFile, method, new domain.ConfigurableDefaultDomain((classFile, method)))
        println(result)

    }

}
