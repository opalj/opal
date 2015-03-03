package org.opalj.br.controlflow

//import org.junit.runner.RunWith
//import org.scalatest.junit.JUnitRunner
//import org.scalatest.FunSpec
//import org.scalatest.Matchers
//import org.scalatest.ParallelTestExecution
import org.opalj.ai.BaseAI
import org.opalj.ai.domain.l1.DefaultDomainWithCFG
//import org.opalj.br.instructions.ATHROW
import org.opalj.collection.UShortSet
//import org.opalj.br.analyses.Project
//import java.io.File

//@RunWith(classOf[JUnitRunner])
object CFGSanityCheck /*extends FunSpec with Matchers */ {

	def main(args: Array[String]): Unit = {

		val errorOutputDestination: String = "C:/Users/User/Desktop/OPALTest/SanityCheck/"

		val project = org.opalj.br.TestSupport.createJREProject
//	    val project = Project(new File("C:/Program Files/Java/jdk1.8.0_31"))

		println(project.methodsCount + " Methods in project")

		project.parForeachMethodWithBody()(m ⇒ {

			val (_, classFile, method) = m

			val recordCFG = BaseAI(classFile, method, new DefaultDomainWithCFG(project, classFile, method))

			val cfg = ControlFlowGraph(method)

			for (block ← cfg.AllBlocks if (block.isInstanceOf[BasicBlock])) {

				val bb: BasicBlock = block.asInstanceOf[BasicBlock]

				var regularCFGSuccessors: UShortSet = org.opalj.collection.mutable.UShortSet.empty

				for (successorBlock ← bb.successors if (successorBlock.isInstanceOf[BasicBlock])) {

					regularCFGSuccessors = regularCFGSuccessors + successorBlock.asInstanceOf[BasicBlock].startPC

				}

				var exceptionalCFGSuccessors: UShortSet = recordCFG.domain.exceptionHandlerSuccessorsOf(bb.endPC)

				for (successorBlock <- bb.catchBlockSuccessors) {

					exceptionalCFGSuccessors = exceptionalCFGSuccessors + successorBlock.handlerPC
				}

				for (pc ← recordCFG.domain.regularSuccessorsOf(bb.endPC)) {
					if (!regularCFGSuccessors.contains(pc)) {
						println("Error: Mismatch of the two CFGs at method " + method.name + " at Instruction #" + bb.endPC + ": " + pc + " was not to be found in " + regularCFGSuccessors)

						CFGDumper.dumpTXT(method, errorOutputDestination)
						CFGDumper.dumpDOT(method, errorOutputDestination)

						sys.exit
					}
				}

				for (pc <- recordCFG.domain.exceptionHandlerSuccessorsOf(bb.endPC)) {
					if (!exceptionalCFGSuccessors.contains(pc)) {
						println("Error: Mismatch of the two CFGs at method " + method.name + " at handler for #" + bb.endPC + ": " + pc + " was not to be found in " + exceptionalCFGSuccessors)

						CFGDumper.dumpTXT(method, errorOutputDestination)
						CFGDumper.dumpDOT(method, errorOutputDestination)

						sys.exit
					}
				}
			}
		})

		println("Finish! ")

	}

}