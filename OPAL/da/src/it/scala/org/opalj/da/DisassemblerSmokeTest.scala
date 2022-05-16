/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import org.opalj.bi.TestResources
import org.opalj.concurrent.OPALHTBoundedExecutionContextTaskSupport
import org.opalj.util.PerformanceEvaluation
import org.opalj.util.Seconds

import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable

/**
 * This test(suite) just loads a very large number of class files and creates
 * the xHTML representation of the classes. It basically tests if we can load and
 * process a large number of different classes without exceptions (smoke test).
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class DisassemblerSmokeTest extends AnyFunSpec with Matchers {

    describe("the Disassembler") {
        val jmodsZip = TestResources.locateTestResources(
            "classfiles/Java9-selected-jmod-module-info.classes.zip",
            "bi"
        )
        val jreLibraryFolder = bytecode.JRELibraryFolder
        val specialResources = Iterable(jmodsZip, jreLibraryFolder)
        for { file <- bi.TestResources.allBITestJARs() ++ specialResources } {

            describe(s"(when processing $file)") {

                val classFiles: List[(ClassFile, URL)] = {
                    var exceptions: List[(AnyRef, Throwable)] = Nil
                    var seconds: Seconds = Seconds.None
                    val classFiles = PerformanceEvaluation.time {
                        val Lock = new Object
                        val exceptionHandler = (source: AnyRef, throwable: Throwable) => {
                            Lock.synchronized {
                                exceptions ::= ((source, throwable))
                            }
                        }

                        val classFiles = ClassFileReader.ClassFiles(file, exceptionHandler)

                        // Check that we have something to process...
                        if (file.getName != "Empty.jar" && classFiles.isEmpty) {
                            throw new UnknownError(s"the file/folder $file is empty")
                        }

                        classFiles
                    } { t => seconds = t.toSeconds }
                    info(s"reading of ${classFiles.size} class files took $seconds")

                    it(s"reading should not result in exceptions") {
                        if (exceptions.nonEmpty) {
                            info(exceptions.mkString(s"exceptions while reading $file:\n", "\n", ""))
                            fail(s"reading of $file resulted in ${exceptions.size} exceptions")
                        }
                    }

                    classFiles
                }

                it(s"should be able to create the xHTML representation for every class") {

                    val classFilesGroupedByPackage = classFiles.groupBy { e =>
                        val (classFile, _ /*url*/ ) = e
                        val fqn = classFile.thisType.asJava
                        if (fqn.contains('.'))
                            fqn.substring(0, fqn.lastIndexOf('.'))
                        else
                            "<default>"
                    }
                    info(s"identified ${classFilesGroupedByPackage.size} packages")

                    val exceptions: Iterable[(URL, Exception)] =
                        (for { (packageName, classFiles) <- classFilesGroupedByPackage } yield {
                            val transformationCounter = new AtomicInteger(0)
                            val parClassFiles = classFiles.par
                            parClassFiles.tasksupport = OPALHTBoundedExecutionContextTaskSupport
                            PerformanceEvaluation.time {
                                (
                                    for { (classFile, url) <- parClassFiles } yield {
                                        var result: Option[(URL, Exception)] = None
                                        try {
                                            classFile.toXHTML(None).label should be("html")
                                            transformationCounter.incrementAndGet()
                                        } catch {
                                            case e: Exception =>
                                                e.printStackTrace()
                                                result = Some((url, e))
                                        }
                                        result
                                    }
                                ).seq.flatten
                            } { t =>
                                info(
                                    s"transformation of ${transformationCounter.get} class files "+
                                        s"in $packageName (parallelized) took ${t.toSeconds}"
                                )
                            }
                        }).flatten

                    if (exceptions.nonEmpty) {
                        info(exceptions.mkString(s"exceptions while reading $file:\n", "\n", ""))
                        fail(s"reading of $file resulted in ${exceptions.size} exceptions")
                    }
                }
            }
        }
    }
}
