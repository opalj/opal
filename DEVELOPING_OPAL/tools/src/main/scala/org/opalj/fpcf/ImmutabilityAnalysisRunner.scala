/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package fpcf

import java.net.URL
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import org.opalj.br.analyses.PropertyStoreKey
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport
import org.opalj.br.ClassFile
import org.opalj.fpcf.analyses.FieldMutabilityAnalysis
import org.opalj.fpcf.properties.ClassImmutability
import org.opalj.fpcf.analyses.ClassImmutabilityAnalysis
import org.opalj.fpcf.properties.TypeImmutability
import org.opalj.fpcf.analyses.TypeImmutabilityAnalysis

/**
 * Determines the immutability of the classes of a project.
 *
 * @author Michael Eichberg
 */
object ImmutabilityAnalysisRunner extends DefaultOneStepAnalysis {

    override def title: String = "determines the immutability of objects and types"

    override def description: String = "determines the immutability of objects and types"

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        import project.get

        var t = Seconds.None

        // The following measurements (t) are done such that the results are comparable with the
        // reactive async approach developed by P. Haller and Simon Gries.
        val projectStore = time { get(PropertyStoreKey) } { r ⇒ t = r.toSeconds }
        //projectStore.debug = true

        val manager = project.get(FPCFAnalysesManagerKey)
        manager.run(FieldMutabilityAnalysis)
        time {
            manager.runAll(ClassImmutabilityAnalysis, TypeImmutabilityAnalysis)
        } { r ⇒ t += r.toSeconds }

        projectStore.validate(None)

        val immutableClasses =
            projectStore.entities(ClassImmutability.key).
                filter(ep ⇒ !ep.e.asInstanceOf[ClassFile].isInterfaceDeclaration).
                groupBy { _.p }.map { kv ⇒
                    (
                        kv._1,
                        kv._2.toList.sortWith { (a, b) ⇒
                            val cfA = a.e.asInstanceOf[ClassFile]
                            val cfB = b.e.asInstanceOf[ClassFile]
                            cfA.thisType.toJava < cfB.thisType.toJava
                        }
                    )
                }

        val immutableClassesPerCategory =
            immutableClasses.map(kv ⇒ "\t\t"+kv._1+": "+kv._2.size).toList.sorted.mkString("\n")

        val immutableTypes =
            projectStore.entities(TypeImmutability.key).
                filter(ep ⇒ !ep.e.asInstanceOf[ClassFile].isInterfaceDeclaration).
                groupBy { _.p }.map { kv ⇒ (kv._1, kv._2.size) }
        val immutableTypesPerCategory =
            immutableTypes.map(kv ⇒ "\t\t"+kv._1+": "+kv._2).toList.sorted.mkString("\n")

        val immutableClassesInfo =
            immutableClasses.values.flatten.filter { ep ⇒
                !ep.e.asInstanceOf[ClassFile].isInterfaceDeclaration
            }.map { ep ⇒
                ep.e.asInstanceOf[ClassFile].thisType.toJava+
                    " => "+ep.p+
                    " => "+projectStore(ep.e, TypeImmutability.key).p
            }.mkString("\tImmutability:\n\t\t", "\n\t\t", "\n")

        BasicReport(
            "Details:\n"+
                immutableClassesInfo+
                "\nSummary (w.r.t classes):\n"+
                "\tObject Immutability:\n"+
                immutableClassesPerCategory+"\n"+
                "\tType Immutability:\n"+
                immutableTypesPerCategory+"\n"+
                "\n"+projectStore.toString(false)+"\n"+
                "The overall analysis took: "+t
        )
    }
}
