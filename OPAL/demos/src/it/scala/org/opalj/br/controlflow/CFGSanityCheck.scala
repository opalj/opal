package org.opalj.br.controlflow

//import java.net.URL
//import java.io.File
//import org.junit.runner.RunWith
//import org.scalatest.junit.JUnitRunner
//import org.scalatest.FunSpec
//import org.scalatest.Matchers
//import org.scalatest.ParallelTestExecution
import org.opalj.ai.BaseAI
import org.opalj.ai.domain.l1.DefaultDomainWithCFG
import org.opalj.collection.UShortSet
//import org.opalj.br.analyses.Project
//import scala.io.StdIn

//@RunWith(classOf[JUnitRunner])
object CFGSanityCheck /*extends FunSpec with Matchers */ {

	def main(args: Array[String]): Unit = {
		
		val errorOutputDestination: String = "C:/Users/User/Desktop/OPALTest/SanityCheck/"
		
		val project = org.opalj.br.TestSupport.createJREProject

		println(project.methodsCount + " Methods in project")

		project.parForeachMethodWithBody()(m => {

			val (_, classFile, method) = m

			val recordCFG = BaseAI(classFile, method, new DefaultDomainWithCFG(project, classFile, method))

			val cfg = ControlFlowGraph(method)
			
			var successes: Int = 0

			cfg.traverseWithFunction { block =>
				{
//					val code = method.body.get

					block match {
						case bb: BasicBlock => {

							val regularSuccessors: UShortSet = recordCFG.domain.regularSuccessorsOf(bb.endPC)

							for (successorBlock <- bb.successors if (successorBlock.isInstanceOf[BasicBlock])) {
								
								val startPC = successorBlock.asInstanceOf[BasicBlock].startPC
								
								if(!regularSuccessors.contains(startPC)){
									println("Error: Mismatch of the two CFGs at method "+method.name+" at Instruction #"+bb.endPC+" was not to be found in "+regularSuccessors)
									println("So far, "+successes+" graphs were correct")
									
									CFGDumper.dumpTXT(method, errorOutputDestination)
									CFGDumper.dumpDOT(method, errorOutputDestination)
									
									sys.exit
								}
								
								successes += 1
							}

						}
						case cb: CatchBlock => {}
						case _ => {}
					}
				}
			}

		})

		println("Finish!")

	}

}