import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform.ScalariformKeys
import scalariform.formatter.preferences._

import no.vedaadata.sbtjavafx.JavaFXPlugin
import no.vedaadata.sbtjavafx.JavaFXPlugin.JFX

import sbtassembly.AssemblyPlugin.autoImport._

name := "BugPicker"

organization	in ThisBuild := "de.opal-project"
homepage 		in ThisBuild := Some(url("http://www.opal-project.de/tools/bugpicker/"))
licenses 		in ThisBuild := Seq("BSD-2-Clause" -> url("http://opensource.org/licenses/BSD-2-Clause"))
version 		in ThisBuild := "1.3.0-Snapshot"
scalaVersion 	in ThisBuild := "2.11.8"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - BugPicker")

scalacOptions in ThisBuild ++= Seq(
	"-deprecation", "-feature", "-unchecked", "-Xlint", "-Xfuture",
	"-Ywarn-numeric-widen", "-Ywarn-unused", "-Ywarn-unused-import", "-Ywarn-nullary-unit", "-Ywarn-nullary-override", "-Ywarn-dead-code", "-Xfatal-warnings" )


crossPaths in ThisBuild := false

fork in run := true

javaOptions in run += "-Xmx8G" // BETTER: javaOptions in run += "-Xmx16G"
javaOptions in run += "-Xms4G"

mainClass in LocalProject("bp") in Compile := (mainClass in LocalProject("BugPickerUI") in Compile).value
fullClasspath in LocalProject("bp") in Runtime ++= (fullClasspath in LocalProject("BugPickerUI") in Runtime).value

def getScalariformPreferences(dir: File) = PreferencesImporterExporter.loadPreferences(
	(file("./../../Scalariform Formatter Preferences.properties").getPath))

lazy val buildSettings = Defaults.defaultSettings ++
	SbtScalariform.scalariformSettingsWithIt ++
	Seq(ScalariformKeys.preferences <<= baseDirectory.apply(getScalariformPreferences)) ++
	Seq(Defaults.itSettings : _*) ++
	Seq(
		EclipseKeys.configurations := Set(Compile, Test, IntegrationTest),
		EclipseKeys.executionEnvironment := Some(EclipseExecutionEnvironment.JavaSE18),
		EclipseKeys.withSource := true
	) ++
	Seq(libraryDependencies ++= Seq(
		"de.opal-project" %% "bugpicker-core" % "0.9.0-SNAPSHOT",
		"de.opal-project" %% "bytecode-disassembler" % "0.9.0-SNAPSHOT"
	)) ++
	Seq(resolvers ++= Seq(
		"Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
	))

lazy val ui = Project(
	id = "BugPickerUI",
	base = file("ui"),
	settings = buildSettings ++
		JavaFXPlugin.jfxSettings ++
		Seq(
			JFX.mainClass := Option("org.opalj.bugpicker.BugPicker"),
			JFX.addJfxrtToClasspath := true
		) ++
		Seq(mainClass in (Compile, run) := Some("org.opalj.bugpicker.ui.BugPicker")) ++
		Seq(libraryDependencies += "org.scalafx"  %% "scalafx"   % "8.0.102-R11") ++
		Seq(
			resourceGenerators in Compile <+= Def.task {
				val versionFile = (baseDirectory in Compile).value / "target" / "scala-2.11" / "classes" / "org" / "opalj" / "bugpicker" / "version.txt"
				versionFile.getParentFile.mkdirs()
				IO.write(versionFile, (version in Compile).value)
				Seq(versionFile)
			}
		)
)

val zipAllSrc = taskKey[Unit]("Creates a zip file of all source files (including the build script etc.).")

zipAllSrc := {
	val s: TaskStreams = streams.value
	val bd = baseDirectory.value.getAbsolutePath + "/"
	def relativeToBase(f: File): String = f.getAbsolutePath.substring(bd.length.toInt)
	val targetFolder = new File(target.value, "scala-" + scalaBinaryVersion.value)
	targetFolder.mkdirs()
	val zipName = "bugpicker-" + version.value + "-all-source.zip"
	val zipFile = new File(targetFolder, zipName)
	val zout = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(zipFile))
	s.log.info(s"Creating all sources zip "+zipFile.toString)
	def writeFile(f: File): Unit = {
		val stream = new java.io.FileInputStream(f)
		val buffer = new Array[Byte](4096)
		var read = stream.read(buffer)
		while (read != -1) {
			zout.write(buffer, 0, read)
			read = stream.read(buffer)
		}
		stream.close()
	}
	def addToZip(f: File): Unit = {
		val e = new java.util.zip.ZipEntry(relativeToBase(f))
		if (f.isDirectory) {
			f.listFiles.foreach(addToZip)
		} else {
			zout.putNextEntry(e)
			writeFile(f)
		}
	}
	addToZip(new File(bd, "src"))
	addToZip(new File(bd, "build.sbt"))
	new File(bd, "project").listFiles.filter(_.getName.endsWith("sbt")).foreach(addToZip)
	val buildScala = new java.util.zip.ZipEntry("project/Build.scala")
	val lines = scala.io.Source.fromFile(new File(bd, "project/Build.scala")).getLines.collect {
		// make sure we take the preferences from the current directory, because we don't pack the whole of opal up
		case l if l.indexOf("Scalariform Formatter") > -1 => """		(file("./Scalariform Formatter Preferences.properties").getPath))"""
		case l => l
	}
	zout.putNextEntry(buildScala)
	zout.write(lines.mkString("\n").getBytes("UTF-8"))
	var formatterPrefs = new File("Scalariform Formatter Preferences.properties")
	if (formatterPrefs.exists) { // packing from within an unpacked sources zip, so we already have it here
		addToZip(formatterPrefs)
	} else { // here we are in the context of the whole of OPAL, so grab the preferences from the OPAL root
		zout.putNextEntry(new java.util.zip.ZipEntry("Scalariform Formatter Preferences.properties"))
		writeFile(new File(bd, "../../Scalariform Formatter Preferences.properties"))
	}
	zout.flush()
	zout.close()
	s.log.info(s"Done creating zip file.")
}
