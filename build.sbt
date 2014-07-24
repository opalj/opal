import sbtunidoc.Plugin.UnidocKeys.unidoc

name := "OPAL Library"

version := "0.1-SNAPSHOT"

organization in ThisBuild := "de.opal-project"

homepage in ThisBuild := Some(url("http://www.opal-project.de"))

licenses in ThisBuild := Seq("BSD-2-Clause" -> url("http://opensource.org/licenses/BSD-2-Clause"))

scalaVersion in ThisBuild := "2.11.2"

scalacOptions in ThisBuild ++= Seq("-deprecation", "-feature", "-unchecked", "-Xlint")

scalacOptions in (ScalaUnidoc, unidoc) ++= Opts.doc.title("OPAL - OPen Analysis Library")

scalacOptions in (ScalaUnidoc, unidoc) ++= Opts.doc.version(version.value)

// Required to get relative links in the generated source code documentation.
scalacOptions in (ScalaUnidoc, unidoc) <<=
  baseDirectory map {
    bd => Seq ("-sourcepath", bd.getAbsolutePath)
  }

scalacOptions in (ScalaUnidoc, unidoc) ++=
  Opts.doc.sourceUrl(
	"https://bitbucket.org/delors/opal/src/HEAD€{FILE_PATH}.scala?at=master"
  )

javacOptions in ThisBuild ++= Seq("-encoding", "utf8")

libraryDependencies in ThisBuild += "junit" % "junit" % "4.11" % "test"

libraryDependencies in ThisBuild += "org.scalatest" %% "scalatest" % "2.2.0" % "test"

testOptions in ThisBuild <<=
  baseDirectory map {
	bd => Seq(Tests.Argument("-u",  bd.getAbsolutePath + "/shippable/testresults"))
  }

testOptions in ThisBuild += Tests.Argument("-o")

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

EclipseKeys.executionEnvironment := Some(EclipseExecutionEnvironment.JavaSE17)

//
//
// SETTINGS REQUIRED TO PUBLISH OPAL ON MAVEN CENTRAL
//
//

publishMavenStyle in ThisBuild := true

publishTo in ThisBuild := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomExtra in ThisBuild := (
  <scm>
    <url>git@bitbucket.org:delors/opal.git</url>
    <connection>scm:git:git@bitbucket.org:delors/opal.git</connection>
  </scm>
  <developers>
    <developer>
      <id>eichberg</id>
      <name>Michael Eichberg</name>
      <url>http://www.michael-eichberg.de</url>
    </developer>
  </developers>)
