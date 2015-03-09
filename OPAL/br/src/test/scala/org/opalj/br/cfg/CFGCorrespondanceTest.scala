package org.opalj.br.cfg

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.ParallelTestExecution
import org.opalj.bi.TestSupport
import org.opalj.br.analyses.Project
import org.opalj.br.ObjectType


@RunWith(classOf[JUnitRunner])
class CFGCorrespondanceTest extends FunSpec with Matchers  {

	
	val testJAR = "classfiles/cfgtest8.jar"
    val testFolder = TestSupport.locateTestResources(testJAR, "br")
    val testProject = Project(testFolder)
    
    val testClass = testProject.classFile(ObjectType("ExceptionCode")).get
    
    describe("Testing if correspondances between Blocks are found correctly"){
		
		it("with only an if-clause in the finally-handler"){
			
			val tryFinallyCFG = ControlFlowGraph(testClass.findMethod("tryFinally").get)
			
			var block = tryFinallyCFG.findCorrespondingBlockForPC(42).get
			
			block should be(BasicBlock(6))
			
			block = tryFinallyCFG.findCorrespondingBlockForPC(71).get
			
			block should be(BasicBlock(32))
			
			block = tryFinallyCFG.findCorrespondingBlockForPC(62).get
			
			block should be(BasicBlock(23))
			
			block = tryFinallyCFG.findCorrespondingBlockForPC(78).get
			
			block should be(BasicBlock(39))
		}
		
		it("also with loops"){
			val testCFG = ControlFlowGraph(testClass.findMethod("loopExceptionWithCatchReturn").get)
			
			var block = testCFG.findCorrespondingBlockForPC(63).get
			
			block should be(BasicBlock(5))
			
			block = testCFG.findCorrespondingBlockForPC(68).get
			
			block should be(BasicBlock(9))
			
			block = testCFG.findCorrespondingBlockForPC(75).get
			
			block should be(BasicBlock(16))
			
			block = testCFG.findCorrespondingBlockForPC(88).get
			
			block should be(BasicBlock(29))
			
		}
	}
}