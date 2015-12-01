package org.opalj
package ai
package domain
package l1

import java.net.URL

import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.graphs

/* 
val p = org.opalj.br.analyses.Project(new java.io.File("C:/Program Files (x86)/Java/jre1.8.0_60/lib/rt.jar"));
org.opalj.ai.domain.l1.GuidedNullpointerAnalysis.doAnalyze(p, Nil, () => false); 
*/

object GuidedNullpointerAnalysis extends DefaultOneStepAnalysis {

    override def title: String =
        "GNA"

    override def description: String =
        "GNA DESC"

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ) = {

        println("############################");
        println("# starting GNA");
        println("############################");

        val list =
            for {
                classFile ← theProject.allProjectClassFiles.par
                field ← classFile.fields

                /* DEBUG if (classFile.fqn.equals("java/lang/String")) */
            } yield {

                for {
                    constructor ← classFile.constructors
                } {
                    println("############################");
                    println("#"+classFile.fqn+"::"+constructor.name+"\t|\t"+constructor.descriptor);
                    println("############################");

                    // Get CFG for this constructor
                    val ai = new InterruptableAI[Domain]
                    val domain1 = new DefaultDomain(theProject, classFile, constructor) with domain.RecordCFG;
                    ai.performInterpretation(constructor.isStrict, constructor.body.get, domain1)(
                        ai.initialOperands(classFile, constructor, domain1),
                        ai.initialLocals(classFile, constructor, domain1)(None)
                    )

                    def canBeNull(node: org.opalj.graphs.DefaultMutableNode[List[br.PC]]): Boolean = {
                        println("----------------");
                        println(node.identifier)

                        var checkPushNull = false;

                        // check instructions of this node
                        for (pc ← node.identifier.reverse) {
                            if (checkPushNull) {
                                checkPushNull = false
                                if (domain1.code.instructions(pc).opcode ==
                                    org.opalj.br.instructions.ACONST_NULL.opcode) {
                                    return true
                                } else {
                                    return false
                                }
                            }

                            val ins = domain1.code.instructions(pc)
                            if (ins.opcode == org.opalj.br.instructions.ATHROW.opcode) {
                                return false
                            } else if (ins.opcode == org.opalj.br.instructions.PUTFIELD.opcode &&
                                field.name == ins.asInstanceOf[org.opalj.br.instructions.PUTFIELD].name) {
                                checkPushNull = true
                            }
                        }

                        // check parent nodes
                        for (node ← node.parents) {
                            def nodec: org.opalj.graphs.DefaultMutableNode[List[br.PC]] =
                                node.asInstanceOf[org.opalj.graphs.DefaultMutableNode[List[br.PC]]]
                            if (canBeNull(nodec)) {
                                return true
                            }
                        }

                        return false
                    }

                    var result = false;
                    for (node ← domain1.cfgAsGraph.exitNode.parents) {
                        if (canBeNull(node.asInstanceOf[org.opalj.graphs.DefaultMutableNode[List[br.PC]]])) {
                            result = true
                        }
                    }

                    println(field.name+" can be null:"+result)
                }
                ""
            }

        println(list.mkString("\n"));

        println("############################");
        println("# GNA finished");
        println("############################");

        BasicReport(theProject.statistics.mkString("\n"))
    }
}

