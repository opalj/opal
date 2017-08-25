/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
import sbt.Keys.TaskStreams

/**
 * Definition of the tasks and settings to generate the OPAL Website (www.opal-project.de)
 *
 * @author Michael Eichberg
 * @author Simon Leischnig
 */
object SiteGeneration {

    def generateSite(
        sourceDirectory: File,
        resourceManaged: File,
        streams:         TaskStreams,
        disassemblerJAR: File
    ): File = {

        // NOTE: Currently we keep all pages in memory during the transformation process... but, this
        // should nevertheless work for a very long time!

        val sourceFolder = sourceDirectory / "site"
        val targetFolder = resourceManaged / "site"
        val s: TaskStreams = streams
        val log = s.log

        val siteGenerationNecessary =
            !targetFolder.exists ||
                (sourceFolder ** "*").get.exists { sourceFile ⇒
                    if (sourceFile.newerThan(targetFolder) && !sourceFile.isHidden) {
                        log.info(
                            s"At least $sourceFile was updated: "+
                                s"${sourceFile.lastModified} > ${targetFolder.lastModified}"+
                                s"(current time: ${System.currentTimeMillis})"
                        )
                        true
                    } else {
                        false
                    }
                }

        // 0.1. generate OPALDisassembler.
        val disassemblerJARTarget = targetFolder / "artifacts" / disassemblerJAR.getName
        IO.copyFile(disassemblerJAR, disassemblerJARTarget)
        log.info("Copied bytecode disassembler to: "+disassemblerJARTarget)

        if (siteGenerationNecessary) {
            log.info("Generating site using: "+sourceFolder / "site.conf")

            import java.io.File
            import java.nio.charset.Charset
            import java.nio.file.Files
            import scala.collection.JavaConverters._
            import scala.io.Source.fromFile
            import com.typesafe.config.ConfigFactory
            import com.vladsch.flexmark.ast.Node
            import com.vladsch.flexmark.html.HtmlRenderer
            import com.vladsch.flexmark.parser.Parser
            import com.vladsch.flexmark.util.options.MutableDataSet
            import org.fusesource.scalate.TemplateEngine

            import java.util.Arrays;

            // 1. read config
            val config = ConfigFactory.parseFile(sourceFolder / "site.conf")

            // 2.1 copy folders
            for { folder ← config.getStringList("folders").asScala } {
                IO.copyDirectory(
                    sourceFolder / folder,
                    targetFolder / folder
                )
            }

            for { resource ← config.getStringList("resources").asScala } {
                IO.copyFile(
                    sourceFolder / resource,
                    targetFolder / resource
                )
            }

            // 2.3 pre-process pages
            val mdParserOptions = new MutableDataSet()
            val mdParser = Parser.builder(mdParserOptions).build()
            val mdToHTMLRenderer = HtmlRenderer.builder(mdParserOptions).build()
            val pages = for (page ← config.getAnyRefList("pages").asScala) yield {
                page match {
                    case pageConfig: java.util.Map[_, _] ⇒
                        val sourceFileName = pageConfig.get("source").toString
                        val sourceFile = sourceFolder / sourceFileName
                        val sourceContent = fromFile(sourceFile).getLines.mkString("\n")
                        // 2.3.1 process each page:
                        val (baseFileName, htmlContent) =
                            if (sourceFileName.endsWith(".md")) {
                                val mdDocument = mdParser.parse(sourceContent)
                                (
                                    sourceFileName.substring(0, sourceFileName.length - 3),
                                    mdToHTMLRenderer.render(mdDocument)
                                )
                            } else if (sourceFileName.endsWith(".snippet.html")) {
                                (
                                    sourceFileName.substring(0, sourceFileName.length - 13),
                                    sourceContent
                                )
                            } else {
                                val message = "unsupported content file: "+sourceFileName
                                throw new RuntimeException(message)
                            }

                        // 2.3.2 copy page specific page resources (optional):
                        pageConfig.get("resources") match {
                            case resources: java.util.List[_] ⇒
                                for { resource ← resources.asScala } {
                                    IO.copyFile(
                                        sourceFolder / resource.toString,
                                        targetFolder / resource.toString
                                    )
                                }

                            case null ⇒ /* OK - resources are optional */

                            case c ⇒
                                val message = "unsupported resource configuration: "+c
                                throw new RuntimeException(message)
                        }
                        (
                            /* name without extension */ baseFileName,
                            /* the File object */ sourceFile,
                            /* the title */ pageConfig.get("title").toString,
                            /* the content */ htmlContent.toString,
                            /* use banner */ Option(pageConfig.get("useBanner")).getOrElse(false)
                        )

                    case sectionTitle: String ⇒
                        // the entry in the site.conf was "just" a titel of some subsection
                        (
                            null,
                            null,
                            sectionTitle,
                            null,
                            false
                        )

                    case _ ⇒
                        throw new RuntimeException("unsupported page configuration: "+page)
                }
            }
            val toc /*Traversable[(String,String)]*/ = pages.map { page ⇒
                val (baseFileName, _, title, _, _) = page
                (baseFileName, title)
            }

            // 2.4 create HTML pages
            val engine = new TemplateEngine
            val defaultTemplate = sourceFolder / "default.template.html.ssp"
            for {
                (baseFileName, sourceFile, title, html, useBanner) ← pages
                if baseFileName ne null
            } {
                val htmlFile = targetFolder / (baseFileName+".html")
                val completePage = engine.layout(
                    defaultTemplate.toString,
                    Map(
                        "title" → title,
                        "content" → html,
                        "toc" → toc,
                        "useBanner" → useBanner
                    )
                )
                Files.write(htmlFile.toPath, completePage.getBytes(Charset.forName("UTF8")))
                log.info(s"Converted $sourceFile to $htmlFile using $defaultTemplate")
            }
        }

        targetFolder.setLastModified(System.currentTimeMillis())

        // (End)
        targetFolder
    }
}
