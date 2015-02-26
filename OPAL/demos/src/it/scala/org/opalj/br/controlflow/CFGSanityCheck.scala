package org.opalj.br.controlflow


//import java.net.URL
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
//import org.scalatest.Matchers
//import org.opalj.br.analyses.Project
//import org.opalj.br.MethodWithBody
//import org.opalj.br.Code
//import org.opalj.br.Method
//import org.scalatest.FunSpec
import org.opalj.ai.BaseAI
import org.opalj.ai.domain.l2.DefaultPerformInvocationsDomainWithCFG
//import scala.io.StdIn


@RunWith(classOf[JUnitRunner])
class CFGSanityCheck {
	
	
//	def input(prompt: String): String = {
//        print(prompt)
//        StdIn.readLine
//    }
	
//	val testSuite: URL = new URL(input("Which Test Suite?"))
	
	val project = org.opalj.br.TestSupport.createJREProject
	
	project.parForeachMethodWithBody()(m => {
		val(_, classFile, method) = m
		
		val result = BaseAI(classFile, method, new DefaultPerformInvocationsDomainWithCFG(project, classFile, method))
		
		result
	})
}