/* BSD 2-Clause License - see OPAL/LICENSE for details. */
import sbt._
import sbt.Keys._
import com.jsuereth.sbtpgp.PgpKeys._
import sbt.sbtpgp.Compat.publishSignedConfigurationTask

object PublishingOverwrite {

  val onSnapshotOverwriteSettings = Seq(
    publishConfiguration := withOverwrite(publishConfiguration.value, isSnapshot.value),
    publishSignedConfiguration := withOverwrite(
      publishSignedConfigurationTask.value,
      isSnapshot.value
    ),
    publishLocalConfiguration ~= (_.withOverwrite(true)),
    publishLocalSignedConfiguration ~= (_.withOverwrite(true))
  )

  private def withOverwriteEnabled(config: PublishConfiguration) = {
    config.withOverwrite(true)
  }

  private def withOverwrite(config: PublishConfiguration, isSnapshot: Boolean) = {
    config.withOverwrite(config.overwrite || isSnapshot)
  }

}
