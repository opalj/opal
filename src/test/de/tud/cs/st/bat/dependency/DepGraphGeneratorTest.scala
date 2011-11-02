package de.tud.cs.st.bat.dependency
import org.scalatest.Suite
import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import org.scalatest.Reporter
import org.scalatest.Stopper
import org.scalatest.Tracker
import org.scalatest.events.TestStarting
import de.tud.cs.st.bat.resolved.reader.Java6Framework
import org.scalatest.events.TestSucceeded
import org.scalatest.events.TestFailed
//import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

//@RunWith(classOf[JUnitRunner])
class DepGraphGeneratorTest extends Suite with de.tud.cs.st.util.perf.BasicPerformanceEvaluation {

  /*
	 * Registry of all class files stored in the zip files found in the test data directory.
	 */
  private val testCases = {

    var tcs = scala.collection.immutable.Map[String, (ZipFile, ZipEntry)]()

    // The location of the "test/data" directory depends on the current directory used for 
    // running this test suite... i.e. whether the current directory is the directory where
    // this class / this source file is stored or the OPAL root directory. 
    var files = new File("../../../../../../../test/classfiles").listFiles()
    if (files == null) files = new File("test/classfiles").listFiles()

    for {
      file <- files
      if (file.isFile && file.canRead && file.getName.endsWith(".zip"))
    } {
      val zipfile = new ZipFile(file)
      val zipentries = (zipfile).entries
      while (zipentries.hasMoreElements) {
        val zipentry = zipentries.nextElement
        if (!zipentry.isDirectory && zipentry.getName.endsWith(".class")) {
          val testCase = ("Read class file: " + zipfile.getName + " - " + zipentry.getName -> (zipfile, zipentry))
          tcs = tcs + testCase
        }
      }
    }

    tcs
  }

  override lazy val testNames: Set[String] = {
    val r = (Set[String]() /: (testCases.keys))(_ + _)
    r
  }

  override def tags: Map[String, Set[String]] = Map()

  override def runTest(
    testName: String,
    reporter: Reporter,
    stopper: Stopper,
    configMap: Map[String, Any],
    tracker: Tracker) {

    val ordinal = tracker.nextOrdinal
    reporter(TestStarting(ordinal, "BATTests", None, testName))
    try {
      // create dependency graph builder that only outputs the added edges directly
      val depGraphBuilder: DepGraphBuilder = new DepGraphBuilder {
        var cnt = 0

        def getID(identifier: String): Int = {
          cnt += 1
          cnt
        }

        def addEdge(src: Int, trgt: Int, eType: EdgeType) = {
          println("addEdge: " + src + "--[" + eType + "]-->" + trgt)
        }
      }
      val depGraphGen = new DepGraphGenerator(depGraphBuilder)

      val (file, entry) = testCases(testName)

      var classFile: de.tud.cs.st.bat.resolved.ClassFile = null
      classFile = Java6Framework.ClassFile(() => file.getInputStream(entry))

      // process classFile using dependency graph generator
      depGraphGen.process(classFile)

      reporter(TestSucceeded(ordinal, "BATTests", None, testName))
    } catch {
      case t: Throwable => reporter(TestFailed(ordinal, "Failure", "BATDependencyGraphTests", None, testName, Some(t)))
    }
  }
}