/* BSD 2-Clause License - see OPAL/LICENSE for details. */
import sbt._
import sbt.Keys.TaskStreams

import play.twirl.compiler.TwirlCompiler
import play.twirl.api.Html

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
      streams: TaskStreams,
      disassemblerJAR: File,
      projectSerializerJAR: File
  ): File = {

    // NOTE: Currently we keep all pages in memory during the transformation process... but, this
    // should nevertheless work for a very long time!

    val sourceFolder = sourceDirectory / "site"
    val targetFolder = resourceManaged / "site"
    val s: TaskStreams = streams
    val log = s.log

    val siteGenerationNecessary =
      !targetFolder.exists ||
        (sourceFolder ** "*").get.exists { sourceFile =>
          if (sourceFile.newerThan(targetFolder) && !sourceFile.isHidden) {
            log.info(
              s"At least $sourceFile was updated: " +
                s"${sourceFile.lastModified} > ${targetFolder.lastModified}" +
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
    log.info("Copied bytecode disassembler to: " + disassemblerJARTarget)
    // 0.2. generate ProjectSerializer
    val projectSerializerJARTarget = targetFolder / "artifacts" / projectSerializerJAR.getName
    IO.copyFile(projectSerializerJAR, projectSerializerJARTarget)
    log.info("Copied project serializer to: " + projectSerializerJARTarget)

    if (siteGenerationNecessary) {
      log.info("Generating site using: " + sourceFolder / "site.conf")

      import java.nio.charset.Charset
      import java.nio.file.Files
      import scala.collection.JavaConverters._
      import scala.io.Source.fromFile

      import com.typesafe.config.ConfigFactory
      import com.vladsch.flexmark.html.HtmlRenderer
      import com.vladsch.flexmark.parser.Parser
      import com.vladsch.flexmark.util.data.MutableDataSet

      // 1. read config
      val config = ConfigFactory.parseFile(sourceFolder / "site.conf")

      // 2.1 copy folders
      for { folder <- config.getStringList("folders").asScala } {
        IO.copyDirectory(
          sourceFolder / folder,
          targetFolder / folder
        )
      }

      for { resource <- config.getStringList("resources").asScala } {
        IO.copyFile(
          sourceFolder / resource,
          targetFolder / resource
        )
      }

      // 2.3 pre-process pages
      val mdParserOptions = new MutableDataSet().set[java.lang.Boolean](HtmlRenderer.RENDER_HEADER_ID, true).set[java.lang.Boolean](HtmlRenderer.GENERATE_HEADER_ID, true)
      val mdParser = Parser.builder(mdParserOptions).build()
      val mdToHTMLRenderer = HtmlRenderer.builder(mdParserOptions).build()
      val pages = for (page <- config.getAnyRefList("pages").asScala) yield {
        page match {
          case pageConfig: java.util.Map[_, _] =>
            val sourceFileName = pageConfig.get("source").toString
            val sourceFile = sourceFolder / sourceFileName
            val sourceStream = fromFile(sourceFile)
            val sourceContent = sourceStream.getLines.mkString("\n")
            sourceStream.close()
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
                val message = "unsupported content file: " + sourceFileName
                throw new RuntimeException(message)
              }

            // 2.3.2 copy page specific page resources (optional):
            pageConfig.get("resources") match {
              case resources: java.util.List[_] =>
                for { resource <- resources.asScala } {
                  IO.copyFile(
                    sourceFolder / resource.toString,
                    targetFolder / resource.toString
                  )
                }

              case null => /* OK - resources are optional */

              case c =>
                val message = "unsupported resource configuration: " + c
                throw new RuntimeException(message)
            }
            (
              /* name without extension */ baseFileName,
              /* the file object */ sourceFile,
              /* the title */ pageConfig.get("title").toString,
              /* the content */ htmlContent,
              /* use banner */ Option(pageConfig.get("useBanner")).getOrElse(false).asInstanceOf[Boolean],
              /* show in TOC */ Option(pageConfig.get("inTOC")).getOrElse(true).asInstanceOf[Boolean]
            )

          case sectionTitle: String =>
            // the entry in the site.conf was "just" a title of some subsection
            (
              null,
              null,
              sectionTitle,
              null,
              false,
              true
            )

          case _ =>
            throw new RuntimeException("unsupported page configuration: " + page)
        }
      }
      val toc /*Traversable[(String,String)]*/ = pages.filter(_._6).map { page =>
        val (baseFileName, _, title, _, _, _) = page
        (baseFileName, title)
      }

      // 2.4 create HTML pages
      val virtualTemplate = TwirlCompiler.compileVirtual(
        IO.read(sourceFolder/ "defaultTemplate.scala.html"),
        sourceFolder / "defaultTemplate.scala.html",
        sourceFolder,
        "play.twirl.api.HtmlFormat.Appendable",
        "play.twirl.api.HtmlFormat",
        additionalImports = TwirlCompiler.DefaultImports
      )
      for {
        (baseFileName, sourceFile, title, html, useBanner, _) <- pages
        if baseFileName ne null
      } {
        val htmlFile = targetFolder / (baseFileName + ".html")
        val completePage = buildPageFromTemplate(title, html, toc, useBanner)(virtualTemplate.content)
        Files.write(htmlFile.toPath, completePage.toString().getBytes(Charset.forName("UTF8")))
        log.info(s"Converted $sourceFile to $htmlFile using defaultTemplate")
      }
    }

    targetFolder.setLastModified(System.currentTimeMillis())

    // (End)
    targetFolder
  }

  def buildPageFromTemplate(title: String,
      content: String,
      toc: Traversable[(String, String)],
      useBanner: Boolean
    )(htmlTemplate: String): Html = {
    import scala.reflect.runtime.universe._
    import scala.tools.reflect.ToolBox
    // remove package declaration
    val sanitizedTemplate = htmlTemplate.linesIterator.filter(line => !line.startsWith("package")).mkString("\n")
    // map toc to String
    val tocString = toc.map { case (left, right) => ("\"" + left + "\"", "\"" + right + "\"") }.mkString("Seq(", ",", ")")

    val tb = runtimeMirror(this.getClass.getClassLoader).mkToolBox()
    val tree = tb.parse(sanitizedTemplate +
      s"""
         |defaultTemplate(\"$title\",\"\"\"$content\"\"\",$tocString,$useBanner)
         |""".stripMargin
    )
    tb.eval(tree).asInstanceOf[Html]
  }
}
