/* BSD 2-Clause License - see OPAL/LICENSE for details. */
import sbt._
import sbt.Keys._
import sbt.internal.util.ManagedLogger

/**
 * @author Simon Leischnig
 */
object ScalacConfiguration {

  def scalacDefaultsFile(baseDir: File) = baseDir / "scalac.options"

  def scalacLocalPrecedenceFile(baseDir: File) = baseDir / "scalac.options.local" // git ignored

  /**
   * Merges the default and the local configuration and returns the options that take precedence.
   * Logs a warning or throws an exception if a critical problem is detected.
   *
   * Currently, local options take precedence over git-managed ones. If local scalac options are
   * present, the symmetric difference between both scalacOptionSets is calculated, and a hard
   * coded whitelist is used to check if this difference is Ok. If the whitelist doesn't cover
   * all elements that are in the difference, a warning is logged.
   */
  def localAndManagedOptionsHandler(
      managedOptions: Seq[ScalacOptionEntry],
      localOptions: Option[Seq[ScalacOptionEntry]],
      managedOptFile: File,
      localOptFile: File,
      logger: ManagedLogger
  ): Seq[ScalacOptionEntry] = {

    val whitelist = Set("-Xdisable-assertions") // changing these options never issue a warning
    val issueWarningOnNonWhitelistedDifference = true

    if (localOptions.isDefined) {
      // Returns the symmetric difference of scalac option names in local and git-managed sets
      // (option name: before any ":" as specified in ScalacOptionEntry)
      def symDifference(
          managedOptions: Seq[ScalacOptionEntry],
          localOptions: Seq[ScalacOptionEntry]
      ): Set[ScalacOptionEntry] = {
        val localOptSet: Set[ScalacOptionEntry] = localOptions.toSet
        val managedOptSet: Set[ScalacOptionEntry] = managedOptions.toSet
        val union = localOptSet.union(managedOptSet)
        val symmetricDifference = union diff (localOptSet intersect managedOptSet)
        symmetricDifference
      }

      val symmetricDifference = symDifference(managedOptions, localOptions.get)
      val significantDifference =
        symmetricDifference.filterNot(diffEntry => whitelist.contains(diffEntry.optionName))
      if (significantDifference.nonEmpty) {
        if (issueWarningOnNonWhitelistedDifference) {
          val differences = significantDifference.map(_.optionName)
          logger.log(
            Level.Warn,
            s"Your local and global scalac options differ significantly: $differences."
          )
        }
      }

      localOptions.get
    } else {
      managedOptions
    }
  }

  def parseScalacOptions(cfgFile: File): Seq[ScalacOptionEntry] = {
    val trimmedLines = IO.readLines(cfgFile).map(_.trim).filterNot(_.isEmpty)
    val withoutComments =
      trimmedLines.filterNot(line => line.startsWith("#") || line.startsWith("//"))
    val splitArgs = withoutComments.flatMap(line => line.split(' '))
    splitArgs.map(ScalacOptionEntry.apply)
  }

  val globalScalacOptions: Def.SettingsDefinition = {
    ThisBuild / scalacOptions ++= {
      val logger: ManagedLogger = streams.value.log
      val baseDir = baseDirectory.value
      val defaultFile = scalacDefaultsFile(baseDir)
      val localFile = scalacLocalPrecedenceFile(baseDir)

      if (!defaultFile.exists()) {
        // the following terminates the build!
        sys.error(s"The global scalac configuration file $defaultFile is missing!");
      }
      val managedScalacOptions: Seq[ScalacOptionEntry] = parseScalacOptions(defaultFile)

      val localScalacOptions: Option[Seq[ScalacOptionEntry]] =
        if (localFile.exists()) Some(parseScalacOptions(localFile)) else None

      val parsedOptions =
        localAndManagedOptionsHandler(
          managedScalacOptions,
          localScalacOptions,
          defaultFile,
          localFile,
          logger
        )
      parsedOptions.map(_.entry)
    }
  }

  case class ScalacOptionEntry(entry: String) {
    def optionName = entry.takeWhile(_ != ':')
  }

}
