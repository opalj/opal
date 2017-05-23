name := "OPAL Library"

// SNAPSHOT
version 		in ThisBuild := "0.9.0-SNAPSHOT"
// NEXT version 		in ThisBuild := "0.8.14"
// RELEASED version 		in ThisBuild := "0.8.13" // MAY 19th, 2017
// RELEASED version 		in ThisBuild := "0.8.12" // April 28th, 2017
// RELEASED version 		in ThisBuild := "0.8.11" // April 14th, 2017
// RELEASED version 		in ThisBuild := "0.8.10"
// RELEASED version 		in ThisBuild := "0.8.9"

organization 	in ThisBuild := "de.opal-project"
homepage 		in ThisBuild := Some(url("http://www.opal-project.de"))
licenses 		in ThisBuild := Seq("BSD-2-Clause" -> url("http://opensource.org/licenses/BSD-2-Clause"))

// [for sbt 0.13.8 onwards] crossPaths in ThisBuild := false

scalaVersion 	in ThisBuild := "2.11.11"
//scalaVersion 	in ThisBuild := "2.12.2"

scalacOptions 	in ThisBuild ++= Seq(
		"-target:jvm-1.8",
		"-deprecation", "-feature", "-unchecked",
		"-Xlint", "-Xfuture", "-Xfatal-warnings",
		"-Ywarn-numeric-widen", "-Ywarn-nullary-unit", "-Ywarn-nullary-override",
		"-Ywarn-unused", "-Ywarn-unused-import", "-Ywarn-dead-code"
)

scalacOptions in (ScalaUnidoc, unidoc) ++= Opts.doc.title("OPAL - OPen Analysis Library")
scalacOptions in (ScalaUnidoc, unidoc) ++= Opts.doc.version(version.value)

resolvers in ThisBuild += Resolver.jcenterRepo
resolvers in ThisBuild += "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/"

// the tests/analysis are already parallelized
parallelExecution in ThisBuild := false
parallelExecution in Global := false

logBuffered in ThisBuild := false

javacOptions in ThisBuild ++= Seq("-encoding", "utf8")

testOptions in ThisBuild := {
		baseDirectory.map(bd =>
    		Seq(Tests.Argument("-u",  bd.getAbsolutePath + "/shippable/testresults"))
		).value
	}

testOptions in ThisBuild += Tests.Argument("-o")

javaFormattingSettingsFilename := "Eclipse Java Formatter Preferences.xml"

// Required to get relative links in the generated source code documentation.
scalacOptions in (ScalaUnidoc, unidoc) :=  {
		baseDirectory.map(bd => Seq ("-sourcepath", bd.getAbsolutePath)).value
	}

scalacOptions in (ScalaUnidoc, unidoc) ++=
	Opts.doc.sourceUrl(
		"https://bitbucket.org/delors/opal/src/HEAD€{FILE_PATH}.scala?"+
			(if (isSnapshot.value) "at=develop" else "at=master")
    )

javaOptions in ThisBuild ++= Seq(
	"-Xmx3G", "-Xms1024m", "-Xnoclassgc",
	"-XX:NewRatio=1", "-XX:SurvivorRatio=8", "-XX:+UseParallelGC","-XX:+AggressiveOpts")

addCommandAlias("compileAll","; copyResources ; scalastyle ; test:compile ; test:scalastyle ; it:scalariformFormat ; it:scalastyle ; it:compile ")

addCommandAlias("buildAll","; compileAll ; unidoc ;  publishLocal ")

addCommandAlias("cleanAll","; clean ; test:clean ; it:clean ; cleanFiles ; cleanCache ; cleanLocal ")

addCommandAlias("cleanBuild","; project OPAL ; cleanAll ; buildAll ")

EclipseKeys.executionEnvironment := Some(EclipseExecutionEnvironment.JavaSE18)
EclipseKeys.withSource := true
EclipseKeys.withJavadoc := true

//
//
// Publish jars to Eclipse plugin project
//
//
//addCommandAlias("copyToEclipsePlugin", "; set publishTo in ThisBuild := Some(Resolver.file(\"file\", new File(\"TOOLS/ep/lib\"))) ; publish")

// To run the task: OPAL/publish::generateSite
val generateSite = taskKey[File]("creates the OPAL website") in Compile
generateSite := {
    // NOTE: Currently we keep all pages in memory during the transformation process... but, this
    // should nevertheless work for a very long time!

    val sourceFolder = sourceDirectory.value / "site"
    val targetFolder = resourceManaged.value / "site"

    // 0. generate Scaladoc
    val runUnidoc = (unidoc in Compile).value

    val s: TaskStreams = streams.value
    val log = s.log

    val siteGenerationNecessary =
        !targetFolder.exists ||
        (sourceFolder ** "*").get.exists{ sourceFile =>
            if(sourceFile.newerThan(targetFolder) && !sourceFile.isHidden) {
                log.info(s"at least $sourceFile was updated: ${sourceFile.lastModified} > ${targetFolder.lastModified} (current time: ${System.currentTimeMillis})")
                true
            } else {
                false
            }
        }

    if(siteGenerationNecessary) {
        log.info("generating site using: "+sourceFolder / "site.conf")

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
        for {folder <- config.getStringList("folders").asScala} {
            IO.copyDirectory(
                sourceFolder / folder,
                targetFolder / folder
            )
        }

        for {resource <- config.getStringList("resources").asScala} {
            IO.copyFile(
                sourceFolder / resource,
                targetFolder / resource
            )
        }

        // 2.3 pre-process pages
        val mdParserOptions = new MutableDataSet();
        val mdParser = Parser.builder(mdParserOptions).build();
        val mdToHTMLRenderer = HtmlRenderer.builder(mdParserOptions).build();
        val pages = for (page <- config.getAnyRefList("pages").asScala) yield {
            page match {
                case pageConfig : java.util.Map[_,_] =>
                    val sourceFileName = pageConfig.get("source").toString
                    val sourceFile = sourceFolder / sourceFileName
                    val sourceContent = fromFile(sourceFile).getLines.mkString("\n")
                    // 2.3.1 process each page:
                    val (baseFileName,htmlContent) =
                        if(sourceFileName.endsWith(".md")) {
                            val mdDocument = mdParser.parse(sourceContent)
                            (
                                sourceFileName.substring(0,sourceFileName.length-3),
                                mdToHTMLRenderer.render(mdDocument)
                            )
                        } else if(sourceFileName.endsWith(".snippet.html")) {
                            (
                                sourceFileName.substring(0,sourceFileName.length-13),
                                sourceContent
                            )
                        } else {
                            throw new RuntimeException("unsupported content file: "+sourceFileName)
                        }

                    // 2.3.2 copy page specific page resources (optional):
                    pageConfig.get("resources") match {
                        case resources : java.util.List[_]=>
                            for{resource <- resources.asScala} {
                                IO.copyFile(
                                    sourceFolder / resource.toString,
                                    targetFolder / resource.toString
                                )
                            }

                        case null => /* OK - resources are optional */

                        case c => throw new RuntimeException("unsupported resource configuration: "+c)
                    }
                    (
                        /* name without extension */ baseFileName,
                        /* the File object */ sourceFile,
                        /* the title */ pageConfig.get("title").toString,
                        /* the content */ htmlContent.toString,
                        /* use banner */ Option(pageConfig.get("useBanner")).getOrElse(false)
                    )

                case sectionTitle : String =>
                    // the entry in the site.conf was "just" a titel of some subsection
                    (
                        null,
                        null,
                        sectionTitle,
                        null,
                        false
                    )

                case _ => throw new RuntimeException("unsupported page configuration: "+page)
            }
        }
        val toc /*Traversable[(String,String)]*/ = pages.map{page =>
            val (baseFileName, _, title, _, _) = page
            (baseFileName,title)
        }

        // 2.4 create HTML pages
        val engine = new TemplateEngine
        val defaultTemplate = sourceFolder / "default.template.html.ssp"
        for {(baseFileName, sourceFile, title, html, useBanner) <- pages if baseFileName ne null} {
            val htmlFile = targetFolder / (baseFileName + ".html")
            val completePage = engine.layout(
                defaultTemplate.toString,
                Map("title" -> title, "content" -> html, "toc" -> toc, "useBanner" -> useBanner)
            )
            Files.write(htmlFile.toPath,completePage.getBytes(Charset.forName("UTF8")))
            log.info(s"converted $sourceFile to $htmlFile using $defaultTemplate")
        }
    }

    targetFolder.setLastModified(System.currentTimeMillis())

    // (End)
    targetFolder
}

compile := {
    val r = (compile in Compile).value
    (generateSite in Compile).value
    r
}

//
//
// SETTINGS REQUIRED TO PUBLISH OPAL ON MAVEN CENTRAL
//
//

publishMavenStyle in ThisBuild := true

publishTo in ThisBuild := {
	val nexus = "https://oss.sonatype.org/"
	if (isSnapshot.value)
		Some("snapshots" at nexus + "content/repositories/snapshots")
	else
		Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

publishArtifact in Test := false

pomExtra in ThisBuild := (
  <scm>
    <url>git@bitbucket.org:delors/opal.git</url>
    <connection>scm:git:git@bitbucket.org:delors/opal.git</connection>
  </scm>
  <developers>
    <developer>
      <id>eichberg</id>
      <name>Michael Eichberg</name>
      <url>http://www.michael-eichberg.de</url>
    </developer>
    <developer>
      <id>reif</id>
      <name>Michael Reif</name>
    </developer>
  </developers>
)
