/* BSD 2-Clause License - see OPAL/LICENSE for details. */
import sbt._

/**
 * Manages the library dependencies of the Bugpicker project.
 *
 * @author Simon Leischnig
 */
object Dependencies {

    lazy val version = new {
        val opal = "0.9.0-SNAPSHOT"
        val scalafx = "8.0.144-R12"
    }

    lazy val library = new {
        val bugpickerCore = "de.opal-project" %% "bugpicker-core" % version.opal
        val ba = "de.opal-project" %% "bytecode-disassembler" % version.opal

        val scalafx = "org.scalafx" %% "scalafx" % version.scalafx
    }

    import library._

    val buildlevel = Seq(bugpickerCore, ba)
    val ui = Seq(scalafx)

}
