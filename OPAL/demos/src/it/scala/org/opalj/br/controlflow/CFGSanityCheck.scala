package org.opalj.br.controlflow

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.ParallelTestExecution
import org.opalj.ai.BaseAI
import org.opalj.ai.domain.l1.DefaultDomainWithCFG
import org.opalj.collection.UShortSet

@RunWith(classOf[JUnitRunner])
class CFGSanityCheck extends FunSpec with Matchers {


	val project = org.opalj.br.TestSupport.createJREProject
	describe("Both Kind of CFG should have the same successors for all PCs") {
		
		project.parForeachMethodWithBody()(m => {
			val (_, classFile, method) = m

			val recordCFG = BaseAI(classFile, method, new DefaultDomainWithCFG(project, classFile, method))

			val cfg = ControlFlowGraph(method)

			cfg.traverseWithFunction { block =>
				{
					block match {
						case bb: BasicBlock => {
							val successorPCs1: UShortSet = recordCFG.domain.regularSuccessorsOf(bb.endPC)
							
							var successorPCs2: UShortSet = org.opalj.collection.mutable.UShortSet.empty
							bb.successors.foreach[Unit] { successor => successorPCs2 + successor.asInstanceOf[BasicBlock].startPC }

							successorPCs1 should be(successorPCs2)
						}
						case _ => { /*Hier gehört noch ein Fall für ExceptionHandling hin.*/}
					}
				}
			}

		})
	}

}