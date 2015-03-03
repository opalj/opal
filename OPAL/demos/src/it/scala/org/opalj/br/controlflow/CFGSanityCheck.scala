package org.opalj.br.controlflow


//import org.junit.runner.RunWith
//import org.scalatest.junit.JUnitRunner
//import org.scalatest.FunSpec
//import org.scalatest.Matchers
//import org.scalatest.ParallelTestExecution
import org.opalj.ai.BaseAI
import org.opalj.ai.domain.l1.DefaultDomainWithCFG
import org.opalj.br.instructions.ATHROW
import org.opalj.collection.UShortSet

//@RunWith(classOf[JUnitRunner])
object CFGSanityCheck /*extends FunSpec with Matchers */ {

    def main(args: Array[String]): Unit = {

        val errorOutputDestination: String = "C:/Users/User/Desktop/OPALTest/SanityCheck/"

          val project = org.opalj.br.TestSupport.createJREProject

        println(project.methodsCount+" Methods in project")

        project.parForeachMethodWithBody()(m ⇒ {

            val (_, classFile, method) = m

            val recordCFG = BaseAI(classFile, method, new DefaultDomainWithCFG(project, classFile, method))

            val cfg = ControlFlowGraph(method)

            val code = method.body.get

            for (block ← cfg.AllBlocks) {
                block match {
                    case bb: BasicBlock ⇒ {

                        val regularSuccessors: UShortSet = recordCFG.domain.regularSuccessorsOf(bb.endPC)

                        var potentialSuccessors: UShortSet = org.opalj.collection.mutable.UShortSet.empty

                        if (!(code.instructions(bb.endPC) == ATHROW)) {

                            for (successorBlock ← bb.successors if (successorBlock.isInstanceOf[BasicBlock])) {

                                potentialSuccessors = potentialSuccessors + successorBlock.asInstanceOf[BasicBlock].startPC

                            }

                            for (pc ← regularSuccessors) {
                                if (!potentialSuccessors.contains(pc)) {
                                    println("Error: Mismatch of the two CFGs at method "+method.name+" at Instruction #"+bb.endPC+": "+pc+" was not to be found in "+potentialSuccessors)

                                    CFGDumper.dumpTXT(method, errorOutputDestination)
                                    CFGDumper.dumpDOT(method, errorOutputDestination)

                                    sys.exit
                                }
                            }
                        }

                    }
                    case cb: CatchBlock ⇒ { /* Hier muss noch was passieren */ }
                    case _              ⇒ { /*Start- und Exitblöcke*/ }
                }
            }
        })

        println("Finish! ")

    }

}