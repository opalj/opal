/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package domain

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec
import java.net.URL
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.ConcurrentLinkedQueue

import scala.jdk.CollectionConverters._
import org.opalj.util.PerformanceEvaluation
import org.opalj.util.PerformanceEvaluation.time
import org.opalj.br.analyses.Project
import org.opalj.br.TestSupport.createJREProject
import org.opalj.br.Method
import org.opalj.br.instructions.JSR
import org.opalj.br.instructions.JSR_W
import org.opalj.br.reader.BytecodeInstructionsCache
import org.opalj.br.reader.Java8FrameworkWithCaching

/**
 * Tests if we are able to collect useful and self-consistent def/use information for the entire
 * test suite.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class RecordDefUseTest extends AnyFunSpec with Matchers {

    protected[this] object DominatorsPerformanceEvaluation extends PerformanceEvaluation

    protected[this] class DefUseDomain(
            val method:  Method,
            val project: Project[URL]
    ) extends CorrelationalDomain
        with TheProject
        with TheMethod
        with ThrowAllPotentialExceptionsConfiguration
        with DefaultHandlingOfMethodResults
        with IgnoreSynchronization
        with l1.DefaultReferenceValuesBinding
        with l1.NullPropertyRefinement
        with l0.DefaultTypeLevelIntegerValues
        with l0.DefaultTypeLevelLongValues
        with l0.DefaultTypeLevelFloatValues
        with l0.DefaultTypeLevelDoubleValues
        with l0.TypeLevelPrimitiveValuesConversions
        with l0.TypeLevelInvokeInstructions
        with l0.TypeLevelFieldAccessInstructions
        with l0.TypeLevelDynamicLoads
        with l0.TypeLevelLongValuesShiftOperators
        with RecordDefUse // <=== we are going to test!

    protected[this] class RefinedDefUseDomain(
            method:  Method,
            project: Project[URL]
    ) extends DefUseDomain(method, project)
        with RefineDefUseUsingOrigins // this should not really affect the results...

    protected[this] def analyzeDefUse(
        m:                        Method,
        r:                        AIResult { val domain: DefUseDomain },
        identicalOrigins:         AtomicLong,
        refinedDefUseInformation: Boolean
    ): Unit = {
        val d: r.domain.type = r.domain
        val dt = DominatorsPerformanceEvaluation.time(Symbol("Dominators")) { d.dominatorTree }
        val liveInstructions = r.evaluatedInstructions
        val code = m.body.get
        val codeSize = code.codeSize

        // (1) TEST
        // Tests if the dominator tree information is consistent
        //
        liveInstructions.iterator.foreach(pc => if (pc != 0) dt.dom(pc) should be < codeSize)

        val instructions = code.instructions
        val ehs = code.exceptionHandlers

        for {
            (ops, pc) <- r.operandsArray.iterator.zipWithIndex
            if ops ne null // let's filter only the executed instructions
            instruction = instructions(pc)
            if !instruction.isStackManagementInstruction
        } {
            val usedOperands = instruction.numberOfPoppedOperands(NotRequired)

            // (2) TEST
            // Tests if the def => use information is consistent; i.e., a use lists
            // the def site
            //

            // An instruction which pushes a value, is not necessarily a "valid"
            // def-site which creates a new value.
            // E.g. StackManagementInstructions, Checkcasts,
            // LoadLocalVariableInstructions, but also INVOKE instructions
            // of functions whose return value is ignored are not "def-sites".
            d.safeUsedBy(pc) foreach { useSite =>
                // let's see if we have a corresponding use...
                val useInstruction = instructions(useSite)
                val poppedOperands = useInstruction.numberOfPoppedOperands(NotRequired)
                val hasDefSite =
                    (0 until poppedOperands).exists { poIndex =>
                        d.operandOrigin(useSite, poIndex).contains(pc)
                    } || {
                        useInstruction.readsLocal &&
                            d.localOrigin(useSite, useInstruction.indexOfReadLocal).contains(pc)
                    }
                if (!hasDefSite) {
                    fail(s"use at $useSite has no def site $pc ($instruction)")
                }
            }
            d.safeExternalExceptionsUsedBy(pc) foreach { useSite =>
                // let's see if we have a corresponding use...
                val useInstruction = instructions(useSite)
                val poppedOperands = useInstruction.numberOfPoppedOperands(NotRequired)
                val hasDefSite =
                    (0 until poppedOperands).exists { poIndex =>
                        val defSites = d.operandOrigin(useSite, poIndex)
                        defSites.contains(ai.ValueOriginForMethodExternalException(pc)) ||
                            defSites.contains(ai.ValueOriginForImmediateVMException(pc))
                    } || {
                        useInstruction.readsLocal &&
                            d.localOrigin(useSite, useInstruction.indexOfReadLocal).contains(pc)
                    }
                if (!hasDefSite) {
                    fail(s"exception use at $useSite has no def site $pc ($instruction)")
                }
            }

            // (3) TEST
            // Tests if the def/use information for reference values corresponds to the
            // def/use information (implicitly) collected by the corresponding domain.
            //
            for { (op, opIndex) <- ops.iterator.zipWithIndex } {
                val defUseOrigins =
                    try {
                        d.operandOrigin(pc, opIndex)
                    } catch {
                        case t: Throwable => fail(s"pc=$pc[operand=$opIndex] no def/use info", t)
                    }
                val domainOrigins = d.origins(op)
                domainOrigins foreach { o =>
                    if (!(
                        defUseOrigins.contains(o) ||
                        defUseOrigins.exists(duo => ehs.exists(_.handlerPC == duo))
                    )) {
                        val instruction = code.instructions(pc)
                        val isHandler = code.exceptionHandlers.exists(_.handlerPC == pc)
                        val message =
                            s"{pc=$pc:$instruction[isHandler=$isHandler][operand=$opIndex] "+
                                s"deviating def/use info: "+
                                s"domain=$domainOrigins vs defUse=$defUseOrigins}"
                        val messageHeader =
                            if (refinedDefUseInformation) "[using domain.origin]" else ""
                        fail(messageHeader + message)
                    }
                }
                identicalOrigins.incrementAndGet

                // (4) TEST
                // Tests if the use => def information is consistent; i.e., a def lists
                // the (current) use site
                //
                // Only the operands that are used by the current instruction are
                // expected to pop-up in the def-sites... and only if the instruction
                // is a relevant use-site
                if (opIndex < usedOperands &&
                    // we already tested: !instruction.isStackManagementInstruction
                    !instruction.isStoreLocalVariableInstruction) {
                    defUseOrigins foreach { defUseOrigin => // the origins of a value...
                        val defUseUseSites = d.usedBy(defUseOrigin)
                        if (defUseUseSites == null) {
                            val belongsToSubroutine = code.belongsToSubroutine()
                            val defSiteBelongsToSubroutine =
                                belongsToSubroutine(underlyingPC(defUseOrigin))
                            val useSiteBelongsToSubroutine =
                                belongsToSubroutine(pc)
                            fail(
                                s"$pc(belongs to subroutine $useSiteBelongsToSubroutine): "+
                                    s"uses sites of $defUseOrigin (belongs to subroutine "+
                                    s"$defSiteBelongsToSubroutine) is null"
                            )
                        } else if (!defUseUseSites.contains(pc)) {
                            // Recall that the current instruction is not a stack management
                            // instruction...
                            val i = instructions(underlyingPC(defUseOrigin))
                            val belongsToSubroutine = code.belongsToSubroutine()
                            val defSiteBelongsToSubroutine =
                                belongsToSubroutine(underlyingPC(defUseOrigin))
                            val useSiteBelongsToSubroutine =
                                belongsToSubroutine(pc)
                            fail(
                                s"${underlyingPC(defUseOrigin)}@$i: the use sites of "+
                                    s"the value with the origin $defUseOrigin (belongs to "+
                                    s"subroutine $defSiteBelongsToSubroutine) does not list "+
                                    s"the instruction\n$pc@${instructions(pc)}"+
                                    s"(belongs to subroutine: $useSiteBelongsToSubroutine) "+
                                    s"as a use site (use sites=$defUseUseSites)"
                            )
                        }
                    }
                }
            }
        }
    }

    protected[this] def analyzeProject(name: String, project: Project[URL]): Unit = {
        info(s"$name contains ${project.methodsCount} methods")

        val identicalOrigins = new AtomicLong(0)
        val failures = new ConcurrentLinkedQueue[(Method, Throwable)]

        time {
            project.parForeachMethodWithBody() { methodInfo =>
                val m = methodInfo.method
                try {
                    val aiResult = BaseAI(m, new DefUseDomain(m, project))
                    analyzeDefUse(m, aiResult, identicalOrigins, refinedDefUseInformation = false)
                } catch {
                    case t: Throwable => failures.add((m, t.fillInStackTrace))
                }
            }
        } { t => info(s"using the record def use origin information took ${t.toSeconds}") }

        time {
            project.parForeachMethodWithBody() { methodInfo =>
                val m = methodInfo.method
                try {
                    val aiResult = BaseAI(m, new RefinedDefUseDomain(m, project))
                    analyzeDefUse(m, aiResult, identicalOrigins, refinedDefUseInformation = true)
                } catch {
                    case t: Throwable => failures.add((m, t.fillInStackTrace))
                }
            }
        } { t => info(s"using the reference domain's origin information took ${t.toSeconds}") }

        val baseMessage = s"origin information of ${identicalOrigins.get} values is identical"
        if (failures.size > 0) {
            val failureMessages = for { (m, exception) <- failures.asScala } yield {
                var root: Throwable = exception
                var location: String = ""
                do {
                    location += {
                        val st = root.getStackTrace
                        if (st != null && st.length > 0) {
                            st.take(5).map { ste =>
                                s"${ste.getClassName}{ ${ste.getMethodName}:${ste.getLineNumber}}"
                            }.mkString("; ")
                        } else {
                            "<location unavailable>"
                        }
                    }
                    root = root.getCause
                    if (root != null)
                        location += "\n- next cause -\n"
                } while (root != null)
                val containsJSR =
                    m.body.get.instructionIterator.find(i => i.opcode == JSR.opcode || i.opcode == JSR_W.opcode)
                val details = exception.getMessage.replace("\n", "\n\t")
                s"${m.toJava}[containsJSR=$containsJSR;\n\t"+
                    s"${exception.getClass.getSimpleName}:\n\t$details;\n\tlocation: $location]"
            }

            val errorMessageHeader = s"${failures.size} exceptions occured ($baseMessage) in:\n"
            fail(failureMessages.mkString(errorMessageHeader, "\n", "\n"))
        } else {
            info(baseMessage)
        }
    }

    //
    // TEST DRIVER
    //

    describe("using the DefUseDomain") {

        val reader = new Java8FrameworkWithCaching(new BytecodeInstructionsCache)

        def evaluateProject(projectName: String, projectFactory: () => Project[URL]): Unit = {
            it(s"should be possible to compute def/use information for all methods of $projectName") {
                DominatorsPerformanceEvaluation.resetAll()
                val project = projectFactory()
                time {
                    analyzeProject(projectName, project)
                } { t => info("the analysis took (real time): "+t.toSeconds) }
                val effort = DominatorsPerformanceEvaluation.getTime(Symbol("Dominators")).toSeconds
                info(s"computing dominator information took (CPU time): $effort")
            }
        }

        evaluateProject("the JDK", () => createJREProject())

        var projectsCount = 0
        br.TestSupport.allBIProjects(reader, None) foreach { biProject =>
            val (projectName, projectFactory) = biProject
            evaluateProject(projectName, projectFactory)
            projectsCount += 1
        }
        info(s"analyzed $projectsCount projects w.r.t. the correctness of the def-use information")
    }
}
