/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.util.zip.ZipFile
import java.io.DataInputStream
import java.io.ByteArrayInputStream
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.opalj.io.process
import org.opalj.br.reader._
import org.opalj.bi.TestResources.allBITestJARs
import org.opalj.bytecode.JRELibraryFolder

import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable

/**
 * This test(suite) just loads a very large number of class files to make sure the library
 * can handle them and to test "corner" cases. Basically, we test for NPEs,
 * ArrayIndexOutOfBoundExceptions and similar issues.
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class TestClassFilesTest extends AnyFlatSpec with Matchers /*INTENTIONALLY NOT PARALLELIZED*/ {

    behavior of "OPAL's ClassFiles reader"

    var count = 0
    for {
        file <- allBITestJARs() ++ Iterator(JRELibraryFolder)
        if file.length() > 0
        if file.getName.endsWith(".jar") || file.getName.endsWith(".jmod")
    } {
        count += 1
        it should s"be able to parse the class files in $file" in {
            val testedForBeingIsomorphicCount = new java.util.concurrent.atomic.AtomicInteger(0)
            val testedForBeingNotIsomorphicCount = new java.util.concurrent.atomic.AtomicInteger(0)
            val testedMethods = new java.util.concurrent.atomic.AtomicBoolean(false)

            def simpleValidator(classFilePair: (ClassFile, ClassFile)): Unit = {
                val (classFile, classFileTwin) = classFilePair
                assert(!(classFile.thisType.fqn eq null))

                val selfDiff = classFile.findDissimilarity(classFile)
                if (selfDiff.nonEmpty)
                    fail(s"the $classFile is not jvm equal to itself: "+selfDiff.get)

                val twinDiff = classFile.findDissimilarity(classFileTwin)
                if (twinDiff.nonEmpty)
                    fail(s"the $classFile is not jvm equal to its twin: "+twinDiff.get)

                for (MethodWithBody(body) <- classFile.methods.par) {
                    body.belongsToSubroutine() should not be (null)

                    testedMethods.set(true)
                    var isomorphicCount = 0
                    var notIsomorphicCount = 0
                    var lastPC = -1
                    body iterate { (pc, instruction) =>
                        assert(
                            instruction.isIsomorphic(pc, pc)(body),
                            s"$instruction should be isomorphic to itself"
                        )
                        isomorphicCount += 1

                        if (lastPC >= 0 && instruction.opcode != body.instructions(lastPC).opcode) {
                            notIsomorphicCount += 1
                            assert(
                                !instruction.isIsomorphic(pc, lastPC)(body),
                                s"$instruction should not be isomorphic to ${body.instructions(lastPC)}"
                            )
                            assert(
                                !body.instructions(lastPC).isIsomorphic(lastPC, pc)(body),
                                s"${body.instructions(lastPC)} should not be isomorphic to $instruction"
                            )
                        }

                        lastPC = pc
                    }
                    testedForBeingIsomorphicCount.addAndGet(isomorphicCount)
                    testedForBeingNotIsomorphicCount.addAndGet(notIsomorphicCount)
                }
            }

            var classFilesCount = 0
            val jarFile = new ZipFile(file)
            val jarEntries = jarFile.entries
            while (jarEntries.hasMoreElements) {
                val jarEntry = jarEntries.nextElement
                if (!jarEntry.isDirectory && jarEntry.getName.endsWith(".class")) {
                    val data = new Array[Byte](jarEntry.getSize.toInt)
                    val entryInputStream = jarFile.getInputStream(jarEntry)
                    process(new DataInputStream(entryInputStream))(_.readFully(data))
                    import Java9Framework.ClassFile
                    val classFiles1 = ClassFile(new DataInputStream(new ByteArrayInputStream(data)))
                    val classFiles2 = ClassFile(new DataInputStream(new ByteArrayInputStream(data)))
                    classFilesCount += 1
                    classFiles1.zip(classFiles2) foreach simpleValidator
                }
            }
            info(s"loaded $classFilesCount class files")

            if (testedMethods.get) {
                assert(testedForBeingIsomorphicCount.get > 0)
                assert(testedForBeingNotIsomorphicCount.get > 0)
                info(s"compared ${testedForBeingIsomorphicCount.get} instructions for being isomorphic")
                info(s"compared ${testedForBeingNotIsomorphicCount.get} instructions for being not isomorphic")
            }
        }
    }
    assert(count > 0)

}
