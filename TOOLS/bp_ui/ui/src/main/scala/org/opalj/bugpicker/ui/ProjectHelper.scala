/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2016
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
package org.opalj
package bugpicker
package ui

import java.io.File
import java.net.URL

import scalafx.collections.ObservableBuffer
import scalafx.stage.Stage
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.opalj.br.analyses.Project
import org.opalj.br.reader.{BytecodeInstructionsCache, Java9LibraryFramework}
import org.opalj.br.ClassFile
import org.opalj.bugpicker.ui.dialogs.LoadedFiles
import org.opalj.log.OPALLogger

import org.opalj.br.reader.Java9FrameworkWithInvokedynamicSupportAndCaching

object ProjectHelper {

    private[this] implicit val logContext = org.opalj.log.GlobalLogContext

    def setupProject(
        loadedFiles:        LoadedFiles,
        parentStage:        Stage,
        projectLogMessages: ObservableBuffer[BugPickerLogMessage]
    ): (Project[URL], Seq[File]) = {

        val files = loadedFiles.projectFiles
        val sources = loadedFiles.projectSources
        val libs = loadedFiles.libraries
        val config = loadedFiles.config
        val project = setupProject(files, libs, config, parentStage, projectLogMessages)
        (project, sources)
    }

    def setupProject(
        cpFiles:            Iterable[File],
        libcpFiles:         Iterable[File],
        config:             Option[Config],
        parentStage:        Stage,
        projectLogMessages: ObservableBuffer[BugPickerLogMessage]
    ): Project[URL] = {
        OPALLogger.info("creating project", "reading project class files")
        val cache: BytecodeInstructionsCache = new BytecodeInstructionsCache
        val Java9ClassFileReader = new Java9FrameworkWithInvokedynamicSupportAndCaching(cache)
        val Java9LibraryClassFileReader = Java9LibraryFramework

        val (classFiles, exceptions1) =
            br.reader.readClassFiles(
                cpFiles,
                Java9ClassFileReader.ClassFiles,
                (file) ⇒ OPALLogger.info(
                    "creating project",
                    "project class path member: "+file.toString
                )
            )

        val (libraryClassFiles, exceptions2) = {
            if (libcpFiles.nonEmpty) {
                OPALLogger.info("creating project", "reading library class files")
                br.reader.readClassFiles(
                    libcpFiles,
                    Java9LibraryClassFileReader.ClassFiles,
                    (file) ⇒ OPALLogger.info(
                        "creating project",
                        "library class path member: "+file.toString
                    )
                )
            } else {
                (Iterable.empty[(ClassFile, URL)], List.empty[Throwable])
            }
        }

        val allExceptions = exceptions1 ++ exceptions2
        if (allExceptions.nonEmpty) {
            for (exception ← allExceptions) {
                OPALLogger.error(
                    "creating project",
                    "an exception occured while creating project; the responsible class file is ignored",
                    exception
                )
            }
        }

        val defaultConfig = ConfigFactory.load()
        val projectConfig = config match {
            case Some(config) ⇒ config.withFallback(defaultConfig)
            case None         ⇒ defaultConfig
        }
        Project(
            classFiles,
            libraryClassFiles,
            virtualClassFiles = Traversable.empty,
            libraryClassFilesAreInterfacesOnly = true
        )(
            config = projectConfig,
            projectLogger = new BugPickerOPALLogger(projectLogMessages)
        )
    }
}
