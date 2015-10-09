package org.opalj.br.cfg

import org.opalj.br.TestSupport
//import org.opalj.bytecode.BytecodeProcessingFailedException

/**
 * @author Erich Wittenbeck
 *
 * This just reads in a lot of classfiles and builds CFGs for all methods
 * within them.
 *
 * If it runs through without any exceptions, the test is considered 'passed'.
 */
object BuildCFGsForJRE {

    def main(args: Array[String]): Unit = {
        val testProjectJRE = TestSupport.createJREProject

        testProjectJRE.parForeachMethodWithBody(() ⇒ false)((m) ⇒ {
            val (_, _, method) = m

            ControlFlowGraph(method)

        })
        
        val testProjectRT = TestSupport.createRTJarProject

        testProjectRT.parForeachMethodWithBody(() ⇒ false)((m) ⇒ {
            val (_, _, method) = m

            ControlFlowGraph(method)

        })
    }

}