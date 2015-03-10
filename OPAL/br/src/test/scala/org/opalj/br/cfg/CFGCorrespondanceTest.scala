package org.opalj.br.cfg

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSpec
import org.scalatest.Matchers
import org.scalatest.ParallelTestExecution
import org.opalj.bi.TestSupport
import org.opalj.br.analyses.Project
import org.opalj.br.ObjectType

import scala.collection.immutable.HashSet

@RunWith(classOf[JUnitRunner])
class CFGCorrespondanceTest extends FunSpec with Matchers  {

	
	val testJAR = "classfiles/cfgtest8.jar"
    val testFolder = TestSupport.locateTestResources(testJAR, "br")
    val testProject = Project(testFolder)
    
    val testClass = testProject.classFile(ObjectType("ExceptionCode")).get
    
    describe("Testing if correspondances between Blocks are found correctly"){
		
		it("with only an if-clause in the finally-handler"){
			
			val tryFinallyCFG = ControlFlowGraph(testClass.findMethod("tryFinally").get)
			
			var block = tryFinallyCFG.findCorrespondingBlockForPC(42)
			
			block should be(HashSet(BasicBlock(6)))
			
			block = tryFinallyCFG.findCorrespondingBlockForPC(71)
			
			block should be(HashSet(BasicBlock(32)))
			
			block = tryFinallyCFG.findCorrespondingBlockForPC(62)
			
			block should be(HashSet(BasicBlock(23)))
			
			block = tryFinallyCFG.findCorrespondingBlockForPC(78)
			
			block should be(HashSet(BasicBlock(39)))
		}
		
		it("also with loops"){
			val testCFG = ControlFlowGraph(testClass.findMethod("loopExceptionWithCatchReturn").get)
			
			var block = testCFG.findCorrespondingBlockForPC(63)
			
			block should be(HashSet(BasicBlock(5)))
			
			block = testCFG.findCorrespondingBlockForPC(68)
			
			block should be(HashSet(BasicBlock(9)))
			
			block = testCFG.findCorrespondingBlockForPC(75)
			
			block should be(HashSet(BasicBlock(16)))
			
			block = testCFG.findCorrespondingBlockForPC(88)
			
			block should be(HashSet(BasicBlock(29)))
			
		}
		
		it("with multiple handlers for one regular execution path"){
			
			val testCFG = ControlFlowGraph(testClass.findMethod("highlyNestedFinally").get)
			
			val blocks = testCFG.findCorrespondingBlockForPC(30)
			
			blocks should be(HashSet(BasicBlock(59), BasicBlock(85), BasicBlock(105), BasicBlock(119)))
		}
	}
}