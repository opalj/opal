package de.tud.cs.st.bat.resolved
package dependency
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
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import DependencyType._

@RunWith(classOf[JUnitRunner])
class BasicDepExtractorTest extends Suite with de.tud.cs.st.util.perf.BasicPerformanceEvaluation {

  /*
   * Registry of all class files stored in the zip files found in the test data directory.
   */
  private val testCases = {

    var tcs = scala.collection.immutable.Map[String, (ZipFile, ZipEntry)]()

    // The location of the "test/data" directory depends on the current directory used for 
    // running this test suite... i.e. whether the current directory is the directory where
    // this class / this source file is stored or the BAT's root directory. 
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
          val testCase = ("Extract dependencies of class file: " + zipfile.getName + " - " + zipentry.getName -> (zipfile, zipentry))
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
    reporter(TestStarting(ordinal, "BasicDependencyExtractorTests", None, testName))
    try {
      // create dependency builder that only outputs the added dependencies directly
      val depBuilder: DepBuilder = new DepBuilder {
        var cnt = 0

        def getID(identifier: String): Int = {
          cnt += 1
          cnt
        }

        def addDep(src: Int, trgt: Int, dType: DependencyType) = {
          //The next line was uncommented to speed up the test runs
          //println("addDep: " + src + "--[" + dType + "]-->" + trgt)
        }
      }
      val dependencyExtractor = new DepExtractor(depBuilder)

      val (file, entry) = testCases(testName)

      var classFile: de.tud.cs.st.bat.resolved.ClassFile = null
      classFile = Java6Framework.ClassFile(() => file.getInputStream(entry))

      // process classFile using dependency graph generator
      dependencyExtractor.process(classFile)

      reporter(TestSucceeded(ordinal, "BasicDependencyExtractorTests", None, testName))
    } catch {
      case t: Throwable => reporter(TestFailed(ordinal, "Failure", "BasicDependencyExtractorTests", None, testName, Some(t)))
    }
  }
}