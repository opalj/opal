import AssemblyKeys._

name := "BugPicker"

version := "1.0.0"

scalaVersion := "2.11.2"

scalacOptions in (Compile, doc) := Seq("-deprecation", "-feature", "-unchecked")

scalacOptions in (Compile, doc) ++= Opts.doc.title("OPAL - BugPicker")

libraryDependencies += "org.scalafx"  %% "scalafx"   % "1.0.0-R8"

jfxSettings

JFX.addJfxrtToClasspath := true

JFX.mainClass := Some("org.opalj.bugpicker.BugPicker")

assemblySettings

jarName in assembly := "bugpicker-" + version.value + ".jar"

test in assembly := {}

mainClass in assembly := Some("org.opalj.bugpicker.BugPicker")

resourceGenerators in Compile += Def.task {
	val versionFile = new File(baseDirectory.value, "target/scala-2.11/classes/org/opalj/bugpicker/version.txt")
	versionFile.getParentFile.mkdirs()
	val writer = new java.io.PrintWriter(versionFile, "UTF-8")
	writer.append((version in Compile).value).println()
	writer.close()
	Seq(versionFile)
}.taskValue

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
