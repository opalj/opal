/* BSD 2-Clause License - see OPAL/LICENSE for details. */

import sbt._

/**
 * Manages the library dependencies of the subprojects of OPAL.
 *
 * @author Simon Leischnig
 */
object Dependencies {

  object version {
    val junit = "4.13.2"
    val scalatest = "3.2.12"
    val scalatestjunit = "3.2.5.0"
    val scalacheck = "3.2.12.0"

    val scalaxml = "1.3.0"
    val scalaparsercombinators = "1.1.2"
    val scalaparallelcollections = "1.0.4"
    val playjson = "2.9.2"
    val ficus = "1.5.0"
    val commonstext = "1.9"
    val txtmark = "0.16"
    val jacksonDF = "2.12.2"
    val fastutil = "8.5.4"

    val openjfx = "16"
  }

  object library {

    // --- general dependencies

    private[this] val osName = System.getProperty("os.name") match {
      case n if n.startsWith("Linux") => "linux"
      case n if n.startsWith("Mac") => "mac"
      case n if n.startsWith("Windows") => "win"
      case _ => throw new Exception("Unknown platform!")
    }

    def reflect(scalaVersion: String): ModuleID = "org.scala-lang" % "scala-reflect" % scalaVersion

    val scalaxml = "org.scala-lang.modules"                 %% "scala-xml"                  % version.scalaxml
    val scalaparallelcollections = "org.scala-lang.modules" %% "scala-parallel-collections" % version.scalaparallelcollections
    val playjson = "com.typesafe.play"                      %% "play-json"                  % version.playjson
    val ficus = "com.iheart"                                %% "ficus"                      % version.ficus
    val commonstext = "org.apache.commons"                  % "commons-text"                % version.commonstext
    val scalaparsercombinators = "org.scala-lang.modules"   %% "scala-parser-combinators"   % version.scalaparsercombinators
    val txtmark = "es.nitaur.markdown"                      % "txtmark"                     % version.txtmark withSources () withJavadoc ()
    val jacksonDF = "com.fasterxml.jackson.dataformat"      % "jackson-dataformat-csv"      % version.jacksonDF withSources () withJavadoc ()
    val fastutil = "it.unimi.dsi"                           % "fastutil"                    % version.fastutil withSources () withJavadoc ()
    val javafxBase = "org.openjfx" % "javafx-base" % version.openjfx classifier osName

    // --- test related dependencies

    val junit =          "junit"              % "junit"           % version.junit          % "test,it"
    val scalatest =      "org.scalatest"     %% "scalatest"       % version.scalatest      % "test,it"
    val scalatestjunit = "org.scalatestplus" %% "junit-4-13"      % version.scalatestjunit % "test,it"
    val scalacheck =     "org.scalatestplus" %% "scalacheck-1-16" % version.scalacheck     % "test,it"
  }

  import library._

  val testlibs: Seq[ModuleID] = Seq(junit, scalatest, scalatestjunit, scalacheck)

  def common(scalaVersion: String) = Seq(reflect(scalaVersion), scalaparallelcollections, scalaxml, playjson, ficus, fastutil)

  val si = Seq()
  val bi = Seq(commonstext)
  val br = Seq(scalaparsercombinators, scalaxml)
  val tools = Seq(txtmark, jacksonDF)
  val hermes = Seq(txtmark, jacksonDF, javafxBase)

}
