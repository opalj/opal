name := "OPAL Library"

version := "0.1-SNAPSHOT"

organization in ThisBuild := "de.opal-project"

homepage in ThisBuild := Some(url("http://www.opal-project.de"))

licenses in ThisBuild := Seq("BSD-2-Clause" -> url("http://opensource.org/licenses/BSD-2-Clause"))

scalaVersion in ThisBuild := "2.10.4"

scalacOptions in ThisBuild ++= Seq("-deprecation", "-feature", "-unchecked")

scalacOptions in (Compile, doc) ++= Opts.doc.title("OPAL - Open Analysis Library") 

javacOptions in ThisBuild ++= Seq("-encoding", "utf8") 

libraryDependencies in ThisBuild += "junit" % "junit" % "4.11" % "test"

libraryDependencies in ThisBuild += "org.scalatest" % "scalatest_2.10" % "2.1.0" % "test"

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Resource

//
//
// SETTINGS RELATED TO THE PUBLISHING OF OPAL ON MAVEN CENTRAL
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