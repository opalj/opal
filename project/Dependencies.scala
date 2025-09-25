/* BSD 2-Clause License - see OPAL/LICENSE for details. */

import sbt.*

/**
 * Manages the library dependencies of the subprojects of OPAL.
 *
 * @author Simon Leischnig
 */
object Dependencies {

    object version {
        val junit = "4.13.2"
        val scalatest = "3.2.19"
        val scalatestjunit = "3.2.19.1"
        val scalacheck = "3.2.19.0"

        val scalaxml = "2.4.0"
        val scalaparsercombinators = "2.4.0"
        val scalaparallelcollections = "1.2.0"
        val playjson = "3.0.5"
        val ficus = "1.5.2"
        val pureconfigcore = "0.17.9"
        val commonstext = "1.14.0"
        val txtmark = "0.16"
        val jacksonDF = "2.20.0"
        val fastutil = "8.5.16"
        val scallop = "5.2.0"
        val apkparser = "2.6.10"
        val scalagraphcore = "2.0.3"
        val scalagraphdot = "2.0.0"

        val openjfx = "22.0.1"
    }

    object library {

        // --- general dependencies

        private val osName = System.getProperty("os.name") match {
            case n if n.startsWith("Linux")   => "linux"
            case n if n.startsWith("Mac")     => "mac"
            case n if n.startsWith("Windows") => "win"
            case _                            => throw new Exception("Unknown platform!")
        }

        def reflect(scalaVersion: String): ModuleID = "org.scala-lang" % "scala-reflect" % "2.13.17" // scalaVersion No scala-reflect available for Scala 3

        val scalaxml = "org.scala-lang.modules" %% "scala-xml" % version.scalaxml
        val scalaparallelcollections =
            "org.scala-lang.modules" %% "scala-parallel-collections" % version.scalaparallelcollections
        val playjson = "org.playframework" %% "play-json" % version.playjson
        val ficus = "com.iheart" %% "ficus" % version.ficus
        val pureconfig = "com.github.pureconfig" %% "pureconfig-core" % version.pureconfigcore
        val commonstext = "org.apache.commons" % "commons-text" % version.commonstext
        val scalaparsercombinators =
            "org.scala-lang.modules" %% "scala-parser-combinators" % version.scalaparsercombinators
        val txtmark = "es.nitaur.markdown" % "txtmark" % version.txtmark withSources () withJavadoc ()
        val jacksonDF =
            "com.fasterxml.jackson.dataformat" % "jackson-dataformat-csv" % version.jacksonDF withSources () withJavadoc ()
        val fastutil = "it.unimi.dsi" % "fastutil" % version.fastutil withSources () withJavadoc ()
        val scallop = "org.rogach" %% "scallop" % version.scallop
        val javafxBase = "org.openjfx" % "javafx-base" % version.openjfx classifier osName
        val apkparser = "net.dongliu" % "apk-parser" % version.apkparser
        val scalagraphcore = "org.scala-graph" %% "graph-core" % version.scalagraphcore
        val scalagraphdot = "org.scala-graph" %% "graph-dot" % version.scalagraphdot

        // --- test related dependencies

        val junit = "junit" % "junit" % version.junit % "test,it"
        val scalatest = "org.scalatest" %% "scalatest" % version.scalatest % "test,it"
        val scalatestjunit = "org.scalatestplus" %% "junit-4-13" % version.scalatestjunit % "test,it"
        val scalacheck = "org.scalatestplus" %% "scalacheck-1-18" % version.scalacheck % "test,it"
    }

    import library.*

    val testlibs: Seq[ModuleID] = Seq(junit, scalatest, scalatestjunit, scalacheck)

    def common(scalaVersion: String) =
        Seq(reflect(scalaVersion), scalaparallelcollections, scalaxml, playjson, ficus, pureconfig, fastutil, scallop)

    val si = Seq(scalagraphcore, scalagraphdot)
    val bi = Seq(commonstext)
    val br = Seq(scalaparsercombinators, scalaxml)
    val tac = Seq()
    val ifds = Seq()
    val ide = Seq()
    val tools = Seq(txtmark, jacksonDF)
    val hermes = Seq(txtmark, jacksonDF, javafxBase)
    val apk = Seq(apkparser, scalaxml)
    val ce = Seq(commonstext)
}
