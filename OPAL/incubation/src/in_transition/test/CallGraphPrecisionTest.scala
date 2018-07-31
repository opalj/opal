/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSpec
import org.scalatest.Matchers
/*import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.ai.analyses.cg.CallGraph
import org.opalj.ai.analyses.cg.CHACallGraphAlgorithmConfiguration
import org.opalj.ai.analyses.cg.CallGraphFactory
import org.opalj.ai.analyses.cg.ComputedCallGraph
import org.opalj.ai.analyses.cg.VTACallGraphAlgorithmConfiguration
import org.opalj.ai.analyses.cg.DefaultVTACallGraphDomain
import org.opalj.ai.analyses.cg.ExtVTACallGraphDomain
import org.opalj.ai.analyses.cg.BasicVTACallGraphDomain
import org.opalj.ai.analyses.cg.UnresolvedMethodCall
import org.opalj.ai.analyses.cg.CallGraphConstructionException
import org.opalj.ai.analyses.cg.CallGraphComparison
import org.opalj.ai.analyses.cg.VTAWithPreAnalysisCallGraphAlgorithmConfiguration
import org.opalj.ai.analyses.cg.BasicVTAWithPreAnalysisCallGraphDomain
*/
/**
 * Compares the precision of different call graphs.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class CallGraphPrecisionTest extends FunSpec with Matchers {
    /* TURNED OFF FOR THE TIME BEING
    describe("call graphs") {
        info("loading the JRE")
        val project = org.opalj.br.TestSupport.createRTJarProject
        val entryPoints = () ⇒ CallGraphFactory.defaultEntryPointsForLibraries(project)
        info("loaded the JRE")

        describe("result of calculating a callgraph") {

            var CHACG: CallGraph = null
            var CHACGUnresolvedCalls: List[UnresolvedMethodCall] = null
            var CHACGCreationExceptions: List[CallGraphConstructionException] = null
            var VTACG: CallGraph = null

            it("calculating the CHA based call graph multiple times using the same project should create the same call graph") {
                info("calculating the CHA based call graph (1)")
                val ComputedCallGraph(theCHACG, unresolvedCalls, creationExceptions) =
                    CallGraphFactory.create(
                        project,
                        entryPoints,
                        new CHACallGraphAlgorithmConfiguration(project)
                    )
                CHACG = theCHACG
                CHACGUnresolvedCalls = unresolvedCalls
                CHACGCreationExceptions = creationExceptions

                info("calculating the CHA based call graph (2)")
                val ComputedCallGraph(newCHACG, _, _) =
                    CallGraphFactory.create(
                        project,
                        entryPoints,
                        new CHACallGraphAlgorithmConfiguration(project)
                    )

                info("comparing the call graphs")
                val (unexpected, additional) =
                    CallGraphComparison(project, theCHACG, newCHACG)
                unexpected should be(empty)
                additional should be(empty)
            }

            it("calculating the CHA based Call Graph for the same JAR multiple times should create the same call graph") {
                info("loading the JRE (again)")
                val newProject = org.opalj.br.TestSupport.createRTJarProject

                info("calculating the CHA based call graph using the new project")
                val ComputedCallGraph(newCHACG, newCHACGUnresolvedCalls, newCHACGCreationExceptions) =
                    CallGraphFactory.create(
                        newProject,
                        () ⇒ CallGraphFactory.defaultEntryPointsForLibraries(newProject),
                        new CHACallGraphAlgorithmConfiguration(newProject)
                    )

                info("comparing the call graphs")
                // we cannot compare the call graphs using the standard CallGraphComparison
                // functionality since the Method objects etc. are not comparable.
                CHACG.callsCount should be(newCHACG.callsCount)
                CHACG.calledByCount should be(newCHACG.calledByCount)
                CHACGUnresolvedCalls.size should be(newCHACGUnresolvedCalls.size)
                CHACGCreationExceptions.size should be(newCHACGCreationExceptions.size)

                val mutex = new Object
                var deviations = List.empty[String]

                newProject.parForeachClassFile() { newClassFile ⇒
                    val classFile = project.classFile(newClassFile.thisType).get
                    val newMethodsIterator = newClassFile.methods.iterator
                    val methodsIterator = classFile.methods.iterator

                    if (newClassFile.methods.size != classFile.methods.size) {
                        mutex.synchronized {
                            val newMethods = newClassFile.methods.map(_.toJava()).mkString("\n", ",\n", "\n")
                            val currentMethods = classFile.methods.map(_.toJava()).mkString("\n", ",\n", "\n")
                            deviations =
                                s"the classfiles for type ${newClassFile.thisType} contain "+
                                    s"different methods: ${newMethods} vs. ${currentMethods}" ::
                                    deviations
                        }
                    } else {
                        while (newMethodsIterator.hasNext) {
                            val newMethod = newMethodsIterator.next()
                            val method = methodsIterator.next()

                            if (newMethod.toJava != method.toJava) {
                                fail(s"the methods associated with the class ${classFile.thisType} differ")
                            }

                            val methodCalledBy = CHACG.calledBy(method)
                            val newMethodCalledBy = newCHACG.calledBy(newMethod)
                            if (methodCalledBy.size != newMethodCalledBy.size) {
                                val mcb = methodCalledBy
                                mutex.synchronized {
                                    deviations =
                                        s"the method ${classFile.thisType.toJava}{ ${method.toJava} } "+
                                            s"is not called by the same methods: $mcb vs. ${newMethodCalledBy} " ::
                                            deviations
                                }
                            }
                            CHACG.calls(method).size should be(newCHACG.calls(newMethod).size)
                        }
                    }
                }

                if (deviations.nonEmpty)
                    fail(deviations.mkString("\n"))
            }

            it("calculating the (Default) VTA based call graph multiple times for the same project should create the same call graph") {
                info("calculating the (Default) VTA based call graph (1)")

                val ComputedCallGraph(theVTACG, _, _) =
                    CallGraphFactory.create(
                        project,
                        entryPoints,
                        new VTAWithPreAnalysisCallGraphAlgorithmConfiguration(project) {
                            override def Domain(
                                classFile: ClassFile,
                                method:    Method
                            ): CallGraphDomain =
                                new DefaultVTACallGraphDomain(
                                    project,
                                    fieldValueInformation, methodReturnValueInformation,
                                    cache,
                                    classFile, method
                                )
                        }
                    )
                VTACG = theVTACG

                info("calculating the (Default) VTA based call graph (2)")
                val ComputedCallGraph(newVTACG, _, _) =
                    CallGraphFactory.create(
                        project,
                        entryPoints,
                        new VTAWithPreAnalysisCallGraphAlgorithmConfiguration(project) {
                            override def Domain(
                                classFile: ClassFile,
                                method:    Method
                            ): CallGraphDomain =
                                new DefaultVTACallGraphDomain(
                                    project,
                                    fieldValueInformation, methodReturnValueInformation,
                                    cache, classFile, method
                                )
                        }
                    )

                info("comparing the call graphs")
                val (unexpected, additional) =
                    CallGraphComparison(project, theVTACG, newVTACG)
                unexpected should be(empty)
                additional should be(empty)
            }

            it("the call graph created using CHA should be less precise than the one created using VTA") {
                val (unexpected, _ /*additional*/ ) =
                    CallGraphComparison(project, CHACG, VTACG)
                if (unexpected.nonEmpty)
                    fail("the comparison of the CHA and the default VTA based call graphs failed:\n"+
                        unexpected.mkString("\n")+"\n")
            }
        }

        describe("the relation between different configurations of VTA callgraphs") {

            it("a VTA based call graph created using a less precise domain should be less precise than one created using a more precise domain") {

                info("calculating the basic VTA based call graph")
                val ComputedCallGraph(basicVTACG, _, _) =
                    CallGraphFactory.create(
                        project,
                        entryPoints,
                        new VTACallGraphAlgorithmConfiguration(project) {
                            override def Domain(
                                classFile: ClassFile,
                                method:    Method
                            ): CallGraphDomain =
                                new BasicVTACallGraphDomain(
                                    project, cache, classFile, method
                                )
                        }
                    )

                info("calculating the default VTA with pre analysis based call graph")
                val ComputedCallGraph(basicVTAWithPreAnalysisCG, _, _) =
                    CallGraphFactory.create(
                        project,
                        entryPoints,
                        new VTAWithPreAnalysisCallGraphAlgorithmConfiguration(project) {
                            override def Domain(
                                classFile: ClassFile,
                                method:    Method
                            ): CallGraphDomain =
                                new BasicVTAWithPreAnalysisCallGraphDomain(
                                    project,
                                    fieldValueInformation, methodReturnValueInformation,
                                    cache,
                                    classFile, method
                                )
                        }
                    )

                {
                    val (unexpected, _ /* additional*/ ) =
                        CallGraphComparison(project, basicVTACG, basicVTAWithPreAnalysisCG)
                    if (unexpected.nonEmpty)
                        fail("the comparison of the basic VTA with the one using pre analyses failed:\n"+
                            unexpected.mkString("\n")+"\n")

                }

                info("calculating the default VTA based call graph")
                val ComputedCallGraph(defaultVTACG, _, _) =
                    CallGraphFactory.create(
                        project,
                        entryPoints,
                        new VTAWithPreAnalysisCallGraphAlgorithmConfiguration(project) {
                            override def Domain(
                                classFile: ClassFile,
                                method:    Method
                            ): CallGraphDomain =
                                new DefaultVTACallGraphDomain(
                                    project,
                                    fieldValueInformation, methodReturnValueInformation,
                                    cache,
                                    classFile, method
                                )
                        }
                    )

                {
                    val (unexpected, _ /*additional*/ ) =
                        CallGraphComparison(project, basicVTACG, defaultVTACG)
                    if (unexpected.nonEmpty)
                        fail("the comparison of the basic and the default VTA based call graphs failed:\n"+
                            unexpected.mkString("\n")+"\n")

                }

                info("calculating the more precise (Ext) VTA based call graph")
                val ComputedCallGraph(extVTACG, _, _) =
                    CallGraphFactory.create(
                        project,
                        entryPoints,
                        new VTAWithPreAnalysisCallGraphAlgorithmConfiguration(project) {
                            override def Domain(
                                classFile: ClassFile,
                                method:    Method
                            ): CallGraphDomain =
                                new ExtVTACallGraphDomain(
                                    project,
                                    fieldValueInformation, methodReturnValueInformation,
                                    cache,
                                    classFile, method
                                )
                        }
                    )

                info("comparing the variants of the VTA based call graphs")

                {
                    val (unexpected, _ /* additional*/ ) =
                        CallGraphComparison(project, defaultVTACG, extVTACG)
                    if (unexpected.nonEmpty)
                        fail("the comparison of the default and the ext VTA based call graphs failed:\n"+
                            unexpected.mkString("\n")+"\n")
                }

            }
        }
    }
    */
}
