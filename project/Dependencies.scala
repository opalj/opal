/* BSD 2-Clause License - see OPAL/LICENSE for details. */

import sbt._

/**
 * Manages the library dependencies of the subprojects of OPAL.
 *
 * @author Simon Leischnig
 */
object Dependencies {

  object version {
    val junit = "4.12"
    val scalatest = "3.0.7"
    val scalacheck = "1.14.0"

    val scalaxml = "1.2.0"
    val scalaparsercombinators = "1.1.2"
    val playjson = "2.7.3"
    val ficus = "1.4.5"
    val commonstext = "1.6"
    val txtmark = "0.16"
    val jacksonDF = "2.9.8"
    val fastutil = "8.2.2"
    val chocosolver = "4.10.0"

    val openjfx = "12.0.1"
    val controlsfx = "11.0.0"
    val scalafx = "12.0.1-R17"
  }

  object library {

    // --- general dependencies

    private[this] val osName = System.getProperty("os.name") match {
      case n if n.startsWith("Linux") ⇒ "linux"
      case n if n.startsWith("Mac") ⇒ "mac"
      case n if n.startsWith("Windows") ⇒ "win"
      case _ ⇒ throw new Exception("Unknown platform!")
    }

    def reflect(scalaVersion: String) = "org.scala-lang" % "scala-reflect" % scalaVersion

    val scalaxml = "org.scala-lang.modules"               %% "scala-xml"                % version.scalaxml
    val playjson = "com.typesafe.play"                    %% "play-json"                % version.playjson
    val ficus = "com.iheart"                              %% "ficus"                    % version.ficus
    val commonstext = "org.apache.commons"                % "commons-text"              % version.commonstext
    val scalaparsercombinators = "org.scala-lang.modules" %% "scala-parser-combinators" % version.scalaparsercombinators
    val txtmark = "es.nitaur.markdown"                    % "txtmark"                   % version.txtmark withSources () withJavadoc ()
    val jacksonDF = "com.fasterxml.jackson.dataformat"    % "jackson-dataformat-csv"    % version.jacksonDF withSources () withJavadoc ()
    val chocosolver = "org.choco-solver"                  % "choco-solver"              % version.chocosolver withSources () withJavadoc ()
    val fastutil = "it.unimi.dsi"                         % "fastutil"                  % version.fastutil withSources () withJavadoc ()
    val javafxBase = "org.openjfx" % "javafx-base" % version.openjfx classifier osName

    val javafxUI = Seq(
      "controls",
      "fxml",
      "graphics",
      "media",
      "swing",
      "web"
    ).map(m ⇒ "org.openjfx" % s"javafx-$m" % version.openjfx classifier osName)
    val scalafx = "org.scalafx"       %% "scalafx"   % version.scalafx withSources () withJavadoc ()
    val controlsfx = "org.controlsfx" % "controlsfx" % version.controlsfx withSources () withJavadoc ()

    // --- test libraries

    val junit = "junit"               % "junit"       % version.junit      % "test,it"
    val scalatest = "org.scalatest"   %% "scalatest"  % version.scalatest  % "test,it"
    val scalacheck = "org.scalacheck" %% "scalacheck" % version.scalacheck % "test,it"

  }

  import library._

  val testlibs: Seq[ModuleID] = Seq(junit, scalatest, scalacheck)

  def common(scalaVersion: String) = Seq(reflect(scalaVersion), scalaxml, playjson, ficus, fastutil)

  val si = Seq()
  val bi = Seq(commonstext)
  val br = Seq(scalaparsercombinators, scalaxml)
  val tools = Seq(txtmark, jacksonDF)
  val hermes = Seq(txtmark, jacksonDF, chocosolver, javafxBase)
  val hermesJFXUI = /* hermes ++: */ scalafx +: controlsfx +: javafxUI 

}
