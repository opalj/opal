package org.opalj.br.controlflow

import java.net.URL
import java.io.File
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.ParallelTestExecution
import org.opalj.ai.BaseAI
import org.opalj.ai.domain.l1.DefaultDomainWithCFG
import org.opalj.collection.UShortSet
import org.opalj.br.analyses.Project
//import org.opalj.br.controlflow.CFGDumper
import scala.io.StdIn

@RunWith(classOf[JUnitRunner])
class CFGSanityCheck extends FunSpec with Matchers {

//	def input(prompt: String): String = {
//		print(prompt)
//		StdIn.readLine
//	}
//	
//	val testSuitePath: String = input("Select Test Suite > ")
//	
//	val project: Project[URL] = Project(new File(testSuitePath))
	
	val project = org.opalj.br.TestSupport.createJREProject
	
	println(project.methodsCount+" Methods in project")
	
	describe("Both Kind of CFG should have the same successors for all PCs") {
		
		project.parForeachMethodWithBody()(m => {
			
//			println("New Loop Iteration")
			
			val (_, classFile, method) = m

			val recordCFG = BaseAI(classFile, method, new DefaultDomainWithCFG(project, classFile, method))

			val cfg = ControlFlowGraph(method)

			cfg.traverseWithFunction { block =>
				{
					block match {
						case bb: BasicBlock => {
							val successorPCs1: UShortSet = recordCFG.domain.regularSuccessorsOf(bb.endPC)
							
							val successorPCs2: UShortSet = org.opalj.collection.mutable.UShortSet.empty
							for(successor <- bb.successors){
								successorPCs2 + successor.asInstanceOf[BasicBlock].startPC
							}
							
							it(" Testing with normal BasicBlocks"){
								successorPCs1 should be(successorPCs2)
							}
							
						}
						case _ => { /*Hier gehört noch ein Fall für ExceptionHandling hin.*/}
					}
				}
			}

		})
		
		println("Finish!")
	}

}