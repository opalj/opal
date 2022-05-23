/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.opalj.bi.TestResources.locateTestResources
import org.opalj.bytecode.JRELibraryFolder

import java.io.File
import org.opalj.br.analyses.Project
import org.opalj.util.PerformanceEvaluation.time

import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable

/**
 * Tests that all methods of the JDK can be converted to a three address representation.
 *
 * @author Michael Eichberg
 * @author Roberts Kolosovs
 */
@RunWith(classOf[JUnitRunner])
class TACNaiveIntegrationTest extends AnyFunSpec with Matchers {

    val jreLibFolder: File = JRELibraryFolder
    val biClassfilesFolder: File = locateTestResources("classfiles", "bi")

    def checkFolder(folder: File): Unit = {
        if (Thread.currentThread().isInterrupted) return ;

        var errors: List[(String, Throwable)] = Nil
        val successfullyCompleted = new java.util.concurrent.atomic.AtomicInteger(0)
        val mutex = new Object
        for {
            file <- folder.listFiles()
            if !Thread.currentThread().isInterrupted
            if file.isFile && file.canRead && file.getName.endsWith(".jar")
            project = Project(file)
            ch = project.classHierarchy
            cf <- project.allProjectClassFiles.par
            if !Thread.currentThread().isInterrupted
            m <- cf.methods
            body <- m.body
        } {
            try {
                // without using AIResults
                val TACode(params, tacNaiveCode, _, cfg, _) = TACNaive(
                    method = m,
                    classHierarchy = ch,
                    optimizations = AllNaiveTACodeOptimizations
                )
                ToTxt(params, tacNaiveCode, cfg, true, true, true)
            } catch {
                case e: Throwable => this.synchronized {
                    val methodSignature = m.toJava
                    mutex.synchronized {
                        println(methodSignature+" - size: "+body.instructions.length)
                        e.printStackTrace(Console.out)
                        if (e.getCause != null) {
                            println("\tcause:")
                            e.getCause.printStackTrace()
                        }
                        println(
                            body.instructions.
                                zipWithIndex.
                                filter(_._1 != null).
                                map(_.swap).
                                mkString("Instructions:\n\t", "\n\t", "\n")
                        )
                        println(
                            body.exceptionHandlers.mkString("Exception Handlers:\n\t", "\n\t", "\n")
                        )
                        errors = (s"$file:$methodSignature", e) :: errors
                    }
                }
            }
            successfullyCompleted.incrementAndGet()
        }
        if (errors.nonEmpty) {
            val message =
                errors.
                    map(_.toString()+"\n").
                    mkString(
                        "Errors thrown:\n",
                        "\n",
                        "successfully transformed methods: "+successfullyCompleted.get+
                            "; failed methods: "+errors.size+"\n"
                    )
            fail(message)
        }
    }

    describe("creating the three-address representation using the naive transformation approach") {

        it("it should be able to convert all methods of the JDK") {
            time {
                checkFolder(jreLibFolder)
            } { t => info(s"conversion took ${t.toSeconds}") }
        }

        it("it should be able to convert all methods of the set of collected class files") {
            time {
                checkFolder(biClassfilesFolder)
            } { t => info(s"conversion took ${t.toSeconds}") }
        }

    }

}
