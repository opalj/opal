/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
import scala.collection.JavaConverters._
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds
import org.opalj.br.analyses.SourceElementsPropertyStoreKey
import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.BasicReport
import org.opalj.br.ClassFile
import org.opalj.fpcf.analysis.immutability.ObjectImmutabilityAnalysis
import org.opalj.fpcf.analysis.immutability.ObjectImmutability
import org.opalj.fpcf.analysis.immutability.TypeImmutabilityAnalysis
import org.opalj.fpcf.analysis.immutability.TypeImmutability
import org.opalj.fpcf.analysis.fields.FieldMutabilityAnalysis
import org.opalj.fpcf.analysis.FPCFAnalysesManagerKey
import org.opalj.fpcf.analysis.extensibility.ClassExtensibilityAnalysis
import org.opalj.fpcf.analysis.extensibility.IsExtensible

/**
 * Runs the immutability analysis.
 *
 * @author Michael Eichberg
 */
object ImmutabilityAnalysis extends DefaultOneStepAnalysis {

    override def title: String = "determines the immutability of objects and types"

    override def description: String = "determines the immutability of objects and types"

    override def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {

        val projectStore = project.get(SourceElementsPropertyStoreKey)
        //projectStore.debug = true

        val manager = project.get(FPCFAnalysesManagerKey)
        var t = Seconds.None
        time {
            manager.run(ClassExtensibilityAnalysis)
            manager.runAll(
                FieldMutabilityAnalysis,
                ObjectImmutabilityAnalysis,
                TypeImmutabilityAnalysis
            )
        } { r ⇒ t = r.toSeconds }

        projectStore.validate(None)

        val extensibleClasses =
            projectStore.entities(IsExtensible).asScala.
                groupBy(_._2).
                map { entry ⇒
                    (
                        entry._1,
                        entry._2.
                        map(_._1.thisType.toJava).toList.sorted.
                        mkString("\n\t\t\t", "\n\t\t\t", "\n")
                    )
                }

        val immutableClasses =
            projectStore.entities(ObjectImmutability.key).
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
                groupBy { _.p }.map { kv ⇒
                    (kv._1, kv._2.size)
                }
        val immutableTypesPerCategory =
            immutableTypes.map(kv ⇒ "\t\t"+kv._1+": "+kv._2).toList.sorted.mkString("\n")

        val immutableClassesInfo =
            immutableClasses.values.flatten.filter { ep ⇒
                !ep.e.asInstanceOf[ClassFile].isInterfaceDeclaration
            }.map { ep ⇒
                ep.e.asInstanceOf[ClassFile].thisType.toJava+
                    " => "+ep.p+
                    " => "+projectStore(ep.e, TypeImmutability.key).get
            }.mkString("\tImmutability:\n\t\t", "\n\t\t", "\n")

        BasicReport(
            "Details:\n"+
                extensibleClasses.mkString("\tExtensible Classes:\n\t\t", "\n\t\t", "\n") +
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
