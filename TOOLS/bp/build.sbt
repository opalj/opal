name := "BugPicker"

organization in ThisBuild := "de.opal-project"

homepage in ThisBuild := Some(url("http://www.opal-project.de/tools/bugpicker/"))

licenses in ThisBuild := Seq("BSD-2-Clause" -> url("http://opensource.org/licenses/BSD-2-Clause"))

version in ThisBuild := "1.3.0-Snapshot"

scalaVersion in ThisBuild := "2.11.6"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - BugPicker")

scalacOptions in ThisBuild ++= Seq(
	"-deprecation", "-feature", "-unchecked", "-Xlint", "-Xfuture", 
	"-Ywarn-numeric-widen", "-Ywarn-unused", "-Ywarn-unused-import", "-Ywarn-nullary-unit", "-Ywarn-nullary-override", "-Ywarn-dead-code", "-Xfatal-warnings" )


// [for sbt 0.13.8 onwards] crossPaths in ThisBuild := false

fork in run := true

mainClass in "bp" in Compile := (mainClass in "BugPickerUI" in Compile).value

fullClasspath in "bp" in Runtime ++= (fullClasspath in "BugPickerUI" in Runtime).value

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
