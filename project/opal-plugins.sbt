lazy val root = (project in file(".")) dependsOn fixturecompileplugin
lazy val fixturecompileplugin = ProjectRef(
  file("../DEVELOPING_OPAL/plugins/sbt-java-fixture-compiler"),
  "sbt-java-fixture-compiler"
)

addSbtPlugin("de.opal-project" % "sbt-java-fixture-compiler" % "1.0.1")
