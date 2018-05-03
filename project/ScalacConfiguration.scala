/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2018
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
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
        scalacOptions in ThisBuild ++= {
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
                    managedScalacOptions, localScalacOptions, defaultFile, localFile,
                    logger
                )
            parsedOptions.map(_.entry)
        }
    }

    case class ScalacOptionEntry(entry: String) {
        def optionName = entry.takeWhile(_ != ':')
    }

}
