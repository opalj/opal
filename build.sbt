name := "OPAL Library"

// SNAPSHOT
version 		in ThisBuild := "0.9.0-SNAPSHOT"
// NEXT version 		in ThisBuild := "0.8.13"
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

val generateSite = taskKey[Seq[File]]("creates the OPAL Website")
//unmanagedResourceDirectories in Package += (sourceDirectory in Package).value / "site"
//resourceGenerators in Package += Def.task
generateSite := {

    // NOTE: Currently we keep all pages in memory during the transformation process... but, this
    // should nevertheless work for a very long time!

    val s: TaskStreams = streams.value
    val log = s.log
    log.info("Generating site using: "+sourceDirectory.value / "site" / "site.conf")

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
    val config = ConfigFactory.parseFile(sourceDirectory.value / "site" / "site.conf")

    // 2.1 copy folders
    for {folder <- config.getStringList("folders").asScala} {
        IO.copyDirectory(
            sourceDirectory.value / "site" / folder,
            resourceManaged.value / "site" / folder
        )
    }

    // 2.2 pre-process pages
    val mdParserOptions = new MutableDataSet();
    val mdParser = Parser.builder(mdParserOptions).build();
    val mdToHTMLRenderer = HtmlRenderer.builder(mdParserOptions).build();
    val pages = for (page <- config.getAnyRefList("pages").asScala) yield {
        page match {
            case pageConfig : java.util.Map[_,_] =>
                val sourceFileName = pageConfig.get("source").toString
                val sourceFile = sourceDirectory.value / "site" / sourceFileName
                val sourceContent = fromFile(sourceFile).getLines.mkString("\n")
                // 2.2.1 convert files:
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

                // 2.2.2 copy page specific page resources (optional):
                pageConfig.get("resources") match {
                    case resources : java.util.List[_]=>
                        for{resource <- resources.asScala} {
                            IO.copyFile(
                                sourceDirectory.value / "site" / resource.toString,
                                resourceManaged.value / "site" / resource.toString
                            )
                        }

                    case null => /* OK - resources are optional */

                    case c => throw new RuntimeException("unsupported resource configuration: "+c)
                }
                (
                    /* name without extension */ baseFileName,
                    /* the File object */ sourceFile,
                    /* the title */ pageConfig.get("title").toString,
                    /* the content */ htmlContent.toString
                )

            case _ => throw new RuntimeException("unsupported page configuration: "+page)
        }
    }
    val links /*Traversable[(String,String)]*/ = pages.map{page =>
        val (baseFileName, _, title, _) = page
        (baseFileName,title)
    }

    // 2.3 create HTML pages
    val engine = new TemplateEngine
    val defaultTemplate = sourceDirectory.value / "site" / "default.template.html.ssp"
    for {(baseFileName, sourceFile, title, html) <- pages} {
        val htmlFile = resourceManaged.value / "site" / (baseFileName + ".html")
        val completePage = engine.layout(
            defaultTemplate.toString,
            Map("title" -> title, "content" -> html, "links" -> links)
        )
        Files.write(htmlFile.toPath,completePage.getBytes(Charset.forName("UTF8")))
        log.info(s"converted $sourceFile to $htmlFile usinng $defaultTemplate")
    }

    // (End) Return generated files
    Seq.empty[File]
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
