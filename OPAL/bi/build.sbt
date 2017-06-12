name := "Bytecode Infrastructure"

scalacOptions in (Compile, doc) := Opts.doc.title("OPAL - Bytecode Infrastructure")
scalacOptions in (Compile, console) := Seq("-deprecation")

libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.5"

/* The following task compiles those Java files to the class files (`.class`) that are used
 * by subsequent tests. To ensure that always the same class file is generated, we use the
 * Eclipse Java compiler (4.6.1); this version is fixed!
 */
// FOR DETAILS SEE ALSO: bi/src/test/fixtures-java/Readme.md

unmanagedResourceDirectories in Test += (sourceDirectory in Test).value / "fixtures-java"

val fixtureCompile = taskKey[Seq[File]]("compilation of java projects against fixed eclipse compiler") in Test
fixtureCompile := {

    FixtureCompilation.doCompilationTask(
			streams.value,
			(resourceManaged in Test).value,
			(sourceDirectory in Test).value
		)

}

resourceGenerators in Test += fixtureCompile.taskValue
