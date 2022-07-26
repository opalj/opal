/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ba

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.lang.Boolean.FALSE
import java.io.File
import java.io.DataInputStream
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicInteger
import com.typesafe.config.ConfigValueFactory.fromAnyRef
import org.opalj.bytecode.JRELibraryFolder
import org.opalj.bi.TestResources.locateTestResources
import org.opalj.bi.TestResources.allBITestJARs
import org.opalj.br.reader.BytecodeInstructionsCache
import org.opalj.br.reader.Java9FrameworkWithCaching
import org.opalj.br.reader.BytecodeOptimizer.SimplifyControlFlowKey

import scala.collection.parallel.CollectionConverters.ImmutableIterableIsParallelizable

/**
 * Smoketest if we can convert every class file using the "Bytecode Representation" back to a
 * class file using the naive representation (Bytecode Disassembler).
 *
 * @author Michael Eichberg
 */
@RunWith(classOf[JUnitRunner])
class BRtoBATest extends AnyFlatSpec with Matchers {

    behavior of "toDA(...br.ClassFile)"

    val ClassFileReader = {
        val testConfig = BaseConfig.withValue(SimplifyControlFlowKey, fromAnyRef(FALSE))

        object Framework extends Java9FrameworkWithCaching(new BytecodeInstructionsCache) {
            override def defaultConfig = testConfig
        }
        Framework
    }

    def process(file: File): Unit = {
        val entriesCount = new AtomicInteger(0)

        val Lock = new Object
        var exceptions: List[Throwable] = Nil

        for { (brClassFile1, url) <- ClassFileReader.ClassFiles(file).par } {

            try {
                // PART 1... just serialize the file...
                // this may have - in comparison with the original class file -
                //  - a new (optimal) constant pool,
                //  - reordered fields,
                //  - reordered methods
                val daClassFile1 = toDA(brClassFile1)
                val rawClassFile1 = org.opalj.bc.Assembler(daClassFile1)

                // PART 2... recreate the class file from the serialized file
                val rawClassFileIn = new DataInputStream(new ByteArrayInputStream(rawClassFile1))
                val brClassFile2 = ClassFileReader.ClassFile(rawClassFileIn).head

                // PART 3... compare the class files...
                brClassFile1.findDissimilarity(brClassFile2) should be(None)

                entriesCount.incrementAndGet()
            } catch {
                case e: Exception =>
                    Lock.synchronized {
                        Console.err.println(s"reading/writing of $url -> failed: ${e.getMessage}\n")
                        e.printStackTrace(Console.err)
                        val details = e.getMessage+"; "+e.getClass.getSimpleName
                        val message = s"$url(${brClassFile1.thisType.toJava}): "+details
                        val newException = new RuntimeException(message, e)
                        exceptions = newException :: exceptions
                    }

            }
        }

        if (exceptions.nonEmpty) {
            val succeededCount = entriesCount.get
            val message =
                exceptions.mkString(
                    s"generating the naive representation failed ${exceptions.size} times:\n",
                    "\n",
                    s"\n(successfully processed: $succeededCount class files)\n"
                )
            fail(message)
        } else {
            info(s"successfully transformed ${entriesCount.get} class files")
        }
    }

    val jmodsFile = locateTestResources("classfiles/Java9-selected-jmod-module-info.classes.zip", "bi")
    for {
        file <- JRELibraryFolder.listFiles() ++ allBITestJARs() ++ List(jmodsFile)
        if file.isFile
        if file.canRead
        if file.length() > 0
        fileName = file.getName
        if fileName.endsWith(".jar") || fileName.endsWith(".zip") || fileName.endsWith(".jmod")
    } {
        it should s"be able to convert every class of $file from br to ba" in { process(file) }
    }
}
