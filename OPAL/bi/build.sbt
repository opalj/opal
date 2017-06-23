/* The following task compiles those Java files to the class files (`.class`) that are used
 * by subsequent tests. To ensure that always the same class file is generated, we use the
 * Eclipse Java compiler (4.6.1); this version is fixed!
 */
// FOR DETAILS SEE ALSO: bi/src/test/fixtures-java/Readme.md

unmanagedResourceDirectories in Test += (sourceDirectory in Test).value / "fixtures-java"

val compileTestFixtures = taskKey[Seq[File]]("compiles the Java test projects using Eclipse") in Test
compileTestFixtures := {
    TestFixtureCompilation.run(
			streams.value,
			(resourceManaged in Test).value,
			(sourceDirectory in Test).value
		)
}

resourceGenerators in Test += compileTestFixtures.taskValue
