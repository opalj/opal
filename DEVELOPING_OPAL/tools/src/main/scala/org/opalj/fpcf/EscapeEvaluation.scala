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

import java.io.{File, FileOutputStream, PrintWriter}
import java.net.URL
import java.util.Calendar

import org.opalj.ai.common.SimpleAIKey
import org.opalj.ai.domain.l0.PrimitiveTACAIDomain
import org.opalj.ai.domain.l1.DefaultDomainWithCFGAndDefUse
import org.opalj.ai.domain.l2.DefaultPerformInvocationsDomainWithCFGAndDefUse
import org.opalj.br.{ArrayAllocationSite, Method, ObjectAllocationSite}
import org.opalj.br.analyses.{AllocationSites, FormalParameter, Project, PropertyStoreKey, VirtualFormalParameter}
import org.opalj.fpcf.analyses.{ReturnValueFreshnessAnalysis, TypeEscapeAnalysis}
import org.opalj.fpcf.analyses.escape.{InterProceduralEscapeAnalysis, SimpleEscapeAnalysis}
import org.opalj.fpcf.properties._
import org.opalj.log.{GlobalLogContext, LogContext, LogMessage, OPALLogger}
import org.opalj.tac._
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.util.Seconds

class DevNullLogger extends OPALLogger {
    override def log(message: LogMessage)(implicit ctx: LogContext): Unit = {}
}

object EscapeEvaluation {

    def main(args: Array[String]): Unit = {
        OPALLogger.updateLogger(GlobalLogContext, new DevNullLogger())

        val begin = Calendar.getInstance()
        println(begin.getTime)

        val path = args(0)
        val d = new File(path)
        val projects = if (d.exists && d.isDirectory) {
            d.listFiles.filter(_.isDirectory).toList
        } else {
            List()
        }

        val evaluationDir = new File("evaluation/")
        if (evaluationDir.exists())
            throw new RuntimeException("Please delete the evaluation directory")

        evaluationDir.mkdir

        for {
            projectDir ← projects
            target = new File(projectDir, "project/bin.zip")
            analysis ← List(SimpleEscapeAnalysis, InterProceduralEscapeAnalysis)
            domainID ← 0 to 2
            i ← 0 to 0 //todo 100
        } {
            try {
                val projectEvaluationDir = new File(evaluationDir, projectDir.getName)
                if (!projectEvaluationDir.exists()) projectEvaluationDir.mkdir()

                val analaysisDir = new File(projectEvaluationDir, analysis.name)
                if (!analaysisDir.exists()) analaysisDir.mkdir()

                val dir = new File(analaysisDir, s"l$domainID")
                if (!dir.exists()) dir.mkdir()

                println(s"${projectDir.getName} - l$domainID - ${analysis.name}")
                var projectTime: Seconds = Seconds.None
                var tacTime: Seconds = Seconds.None
                var propertyStoreTime: Seconds = Seconds.None
                var analysisTime: Seconds = Seconds.None
                var synchTime: Seconds = Seconds.None
                var returnTime: Seconds = Seconds.None
                var typeTime: Seconds = Seconds.None

                val project = time {
                    val p = Project(target)
                    setDomain(p, domainID)
                    p
                } { t ⇒ projectTime = t.toSeconds }

                val tacai = time {
                    val tacai = project.get(DefaultTACAIKey)
                    val errors = project.parForeachMethodWithBody() { mi ⇒ tacai(mi.method) }
                    errors.foreach { e ⇒ println(s"generating 3-address code failed: $e") }
                    tacai
                } { t ⇒ tacTime = t.toSeconds }

                val propertyStore = time {
                    PropertyStoreKey.makeAllocationSitesAvailable(project)
                    PropertyStoreKey.makeFormalParametersAvailable(project)
                    if (analysis eq InterProceduralEscapeAnalysis) {
                        PropertyStoreKey.makeVirtualFormalParametersAvailable(project)
                    }
                    project.get(PropertyStoreKey)
                } { t ⇒ propertyStoreTime = t.toSeconds }

                time {
                    analysis.start(project, propertyStore)
                    propertyStore.waitOnPropertyComputationCompletion()
                } { t ⇒ analysisTime = t.toSeconds }

                time {
                    ReturnValueFreshnessAnalysis.start(project, propertyStore)
                    propertyStore.waitOnPropertyComputationCompletion()
                } { t ⇒ returnTime = t.toSeconds }

                time {
                    TypeEscapeAnalysis.start(project, propertyStore)
                    propertyStore.waitOnPropertyComputationCompletion()
                } { t ⇒ typeTime = t.toSeconds }

                val allocationSites = propertyStore.context[AllocationSites]
                val objects = time {
                    for {
                        method ← project.allMethodsWithBody
                        (_, as) ← allocationSites(method)
                        EP(_, escape) = propertyStore(as, EscapeProperty.key)
                        if EscapeViaNormalAndAbnormalReturn lessOrEqualRestrictive escape
                        code = tacai(method).stmts
                        defSite = code indexWhere (stmt ⇒ stmt.pc == as.pc)
                        if defSite != -1
                        stmt = code(defSite)
                        if stmt.astID == Assignment.ASTID
                        Assignment(_, DVar(_, uses), New(_, _) | NewArray(_, _, _)) = code(defSite)
                        if uses exists { use ⇒
                            code(use) match {
                                case MonitorEnter(_, v) if v.asVar.definedBy.contains(defSite) ⇒ true
                                case _ ⇒ false
                            }
                        }
                    } yield as
                } { t ⇒ synchTime = t.toSeconds }

                if (i != 0) {
                    val times = new File(dir, "timings.csv")
                    val timesNew = !times.exists()
                    val outTimes = new PrintWriter(new FileOutputStream(times, true))
                    try {
                        if (timesNew) {
                            times.createNewFile()
                            outTimes.println("project;tac;propertyStore;analysis")
                        }
                        outTimes.println(s"$projectTime;$tacTime;$propertyStoreTime;$analysisTime")
                    } finally {
                        if (outTimes != null) outTimes.close()
                    }

                    val typeTimeFile = new File(dir, "type-timings.csv")
                    val typeNew = !typeTimeFile.exists()
                    val outTypeTimes = new PrintWriter(new FileOutputStream(typeTimeFile, true))
                    try {
                        if (typeNew) {
                            typeTimeFile.createNewFile()
                            outTypeTimes.println("project;tac;propertyStore;escape;types")
                        }
                        outTypeTimes.println(s"$projectTime;$tacTime;$propertyStoreTime;$analysisTime;$typeTime")
                    } finally {
                        if (outTypeTimes != null) outTypeTimes.close()
                    }

                    val returnTimeFile = new File(dir, "return-timings.csv")
                    val returnNew = !returnTimeFile.exists()
                    val outReturnTimes = new PrintWriter(new FileOutputStream(returnTimeFile, true))
                    try {
                        if (returnNew) {
                            returnTimeFile.createNewFile()
                            outReturnTimes.println("project;tac;propertyStore;escape;return")
                        }
                        outReturnTimes.println(s"$projectTime;$tacTime;$propertyStoreTime;$analysisTime;$returnTime")
                    } finally {
                        if (outReturnTimes != null) outReturnTimes.close()
                    }

                    val synchTimeFile = new File(dir, "synch-timings.csv")
                    val synchNew = !synchTimeFile.exists()
                    val outSynchTimes = new PrintWriter(new FileOutputStream(synchTimeFile, true))
                    try {
                        if (synchNew) {
                            synchTimeFile.createNewFile()
                            outSynchTimes.println("project;tac;propertyStore;escape;synch")
                        }
                        outSynchTimes.println(s"$projectTime;$tacTime;$propertyStoreTime;$analysisTime;$synchTime")
                    } finally {
                        if (outSynchTimes != null) outSynchTimes.close()
                    }

                } else {
                    val no = propertyStore.entities(NoEscape)
                    val c = propertyStore.entities(EscapeInCallee)
                    val p = propertyStore.entities(EscapeViaParameter)
                    val r = propertyStore.entities(EscapeViaReturn)
                    val a = propertyStore.entities(EscapeViaAbnormalReturn)
                    val ar = propertyStore.entities(EscapeViaNormalAndAbnormalReturn)
                    val pr = propertyStore.entities(EscapeViaParameterAndReturn)
                    val ap = propertyStore.entities(EscapeViaParameterAndAbnormalReturn)
                    val rap = propertyStore.entities(EscapeViaParameterAndNormalAndAbnormalReturn)
                    val s = propertyStore.entities(EscapeViaStaticField) ++ propertyStore.entities(EscapeViaHeapObject) ++ propertyStore.entities(GlobalEscape)

                    val atNo = propertyStore.entities(EscapeProperty.key).collect { case EP(e, AtMost(NoEscape)) ⇒ e }
                    val atC = propertyStore.entities(EscapeProperty.key).collect { case EP(e, AtMost(EscapeInCallee)) ⇒ e }
                    val atR = propertyStore.entities(EscapeProperty.key).collect { case EP(e, AtMost(EscapeViaReturn)) ⇒ e }
                    val atA = propertyStore.entities(EscapeProperty.key).collect { case EP(e, AtMost(EscapeViaAbnormalReturn)) ⇒ e }
                    val atP = propertyStore.entities(EscapeProperty.key).collect { case EP(e, AtMost(EscapeViaParameter)) ⇒ e }
                    val atAR = propertyStore.entities(EscapeProperty.key).collect { case EP(e, AtMost(EscapeViaNormalAndAbnormalReturn)) ⇒ e }
                    val atPR = propertyStore.entities(EscapeProperty.key).collect { case EP(e, AtMost(EscapeViaParameterAndReturn)) ⇒ e }
                    val atAP = propertyStore.entities(EscapeProperty.key).collect { case EP(e, AtMost(EscapeViaParameterAndAbnormalReturn)) ⇒ e }
                    val atRAP = propertyStore.entities(EscapeProperty.key).collect { case EP(e, AtMost(EscapeViaParameterAndNormalAndAbnormalReturn)) ⇒ e }

                    val as = new File(dir, "allocation-sites.csv")
                    val outAS = new PrintWriter(as)
                    try {
                        outAS.println("!/?;{};{c};{r};{a};{p};{r,a};{r,p};{a,p};{r,a,p};{s}")
                        outAS.println(s"!;${countAS(no)};${countAS(c)};${countAS(r)};${countAS(a)};${countAS(p)};${countAS(ar)};${countAS(pr)};${countAS(ap)};${countAS(rap)};${countAS(s)}")
                        outAS.println(s"?;${countAS(atNo)};${countAS(atC)};${countAS(atR)};${countAS(atA)};${countAS(atP)};${countAS(atAR)};${countAS(atPR)};${countAS(atAP)};${countAS(atRAP)};")
                    } finally {
                        if (outAS != null) outAS.close()
                    }

                    val arrays = new File(dir, "arrays.csv")
                    val outAR = new PrintWriter(arrays)
                    try {
                        outAR.println("!/?;{};{c};{r};{a};{p};{r,a};{r,p};{a,p};{r,a,p};{s}")
                        outAR.println(s"!;${countAr(no)};${countAr(c)};${countAr(r)};${countAr(a)};${countAr(p)};${countAr(ar)};${countAr(pr)};${countAr(ap)};${countAr(rap)};${countAr(s)}")
                        outAR.println(s"?;${countAr(atNo)};${countAr(atC)};${countAr(atR)};${countAr(atA)};${countAr(atP)};${countAr(atAR)};${countAr(atPR)};${countAr(atAP)};${countAr(atRAP)};")
                    } finally {
                        if (outAR != null) outAR.close()
                    }

                    val fp = new File(dir, "formal-parameters.csv")
                    val outFP = new PrintWriter(fp)
                    try {
                        outFP.println("!/?;{};{c};{r};{a};{p};{r,a};{r,p};{a,p};{r,a,p};{s}")
                        outFP.println(s"!;${countFP(no)};${countFP(c)};${countFP(r)};${countFP(a)};${countFP(p)};${countFP(ar)};${countFP(pr)};${countFP(ap)};${countFP(rap)};${countFP(s)}")
                        outFP.println(s"?;${countFP(atNo)};${countFP(atC)};${countFP(atR)};${countFP(atA)};${countFP(atP)};${countFP(atAR)};${countFP(atPR)};${countFP(atAP)};${countFP(atRAP)};")
                    } finally {
                        if (outFP != null) outFP.close()
                    }

                    val vfp = new File(dir, "virtual-formal-parameters.csv")
                    val outVFP = new PrintWriter(vfp)
                    try {
                        outVFP.println("!/?;{};{c};{r};{a};{p};{r,a};{r,p};{a,p};{r,a,p};{s}")
                        outVFP.println(s"!;${countVFP(no)};${countVFP(c)};${countVFP(r)};${countVFP(a)};${countVFP(p)};${countVFP(ar)};${countVFP(pr)};${countVFP(ap)};${countVFP(rap)};${countVFP(s)}")
                        outVFP.println(s"?;${countVFP(atNo)};${countVFP(atC)};${countVFP(atR)};${countVFP(atA)};${countVFP(atP)};${countVFP(atAR)};${countVFP(atPR)};${countVFP(atAP)};${countVFP(atRAP)};")
                    } finally {
                        if (outVFP != null) outVFP.close()
                    }

                    val synchFile = new File(dir, "synchronization.csv")
                    val outSynch = new PrintWriter(synchFile)
                    try {
                        outSynch.println("objects")
                        for (ob ← objects) {
                            outSynch.println(ob)
                        }
                    } finally {
                        if (outSynch != null) outSynch.close()
                    }

                    val typeFile = new File(dir, "type.csv")
                    val outType = new PrintWriter(typeFile)
                    try {
                        val globalType = propertyStore.entities(GlobalType)
                        val localType = propertyStore.entities(PackageLocalType)
                        val maybeLocalType = propertyStore.entities(MaybePackageLocalType)
                        outType.println("local;global;maybe")
                        outType.println(s"${localType.size};${globalType.size};${maybeLocalType.size}")
                    } finally {
                        if (outType != null) outType.close()
                    }

                    val returnFile = new File(dir, "return.csv")
                    val outReturn = new PrintWriter(returnFile)
                    try {
                        val fresh = propertyStore.entities(FreshReturnValue)
                        val notFresh = propertyStore.entities(NoFreshReturnValue)
                        val primitive = propertyStore.entities(PrimitiveReturnValue)
                        outReturn.println("fresh;primitive;not fresh")
                        outReturn.println(s"${fresh.size};${primitive.size};${notFresh.size}")
                    } finally {
                        if (outReturn != null) outReturn.close()
                    }

                }
            } catch {
                case e: Throwable ⇒ println(Calendar.getInstance().getTime); println(e.getMessage)
            }
            System.gc()
        }

        val end = Calendar.getInstance()

        println(end.getTime)
    }

    def setDomain(project: Project[URL], level: Int): Unit = {
        val domain = level match {
            case 0 ⇒ (m: Method) ⇒ new PrimitiveTACAIDomain(project, m)
            case 1 ⇒ (m: Method) ⇒ new DefaultDomainWithCFGAndDefUse(project, m)
            case 2 ⇒ (m: Method) ⇒ new DefaultPerformInvocationsDomainWithCFGAndDefUse(project, m)
            case _ ⇒ throw new RuntimeException("Unsupported domain level")
        }
        project.getOrCreateProjectInformationKeyInitializationData(SimpleAIKey, domain)
    }

    def countAS(entities: Traversable[Entity]): Int = entities.count(_.isInstanceOf[ObjectAllocationSite])
    def countAr(entities: Traversable[Entity]): Int = entities.count(_.isInstanceOf[ArrayAllocationSite])
    def countFP(entities: Traversable[Entity]): Int = entities.count(_.isInstanceOf[FormalParameter])
    def countVFP(entities: Traversable[Entity]): Int = entities.count(_.isInstanceOf[VirtualFormalParameter])
}
