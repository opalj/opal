/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package av
package viz

import scala.language.reflectiveCalls

import java.io.File
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.mutable
import scala.io.Source
import scala.util.Random
import scala.util.boundary
import scala.util.boundary.break

import org.opalj.br.ArrayType
import org.opalj.br.BaseType
import org.opalj.br.ClassType
import org.opalj.br.VirtualClass
import org.opalj.br.VirtualSourceElement
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.ProjectsAnalysisApplication
import org.opalj.br.fpcf.cli.MultiProjectAnalysisConfig
import org.opalj.cli.DebugArg
import org.opalj.cli.InvertArg
import org.opalj.cli.MainPackageArg
import org.opalj.cli.PackagesArg
import org.opalj.de.DependencyExtractor
import org.opalj.de.DependencyProcessor
import org.opalj.de.DependencyType
import org.opalj.io.processSource
import org.opalj.io.writeAndOpen

/**
 * @author Tobias Becker
 */
object DependencyAnalysis extends ProjectsAnalysisApplication {

    protected class DependenciesConfig(args: Array[String]) extends MultiProjectAnalysisConfig(args) {
        val description = "Collects information about the number of dependencies a package has on other packages"

        args(MainPackageArg, InvertArg)
        generalArgs(DebugArg)
    }

    protected type ConfigType = DependenciesConfig

    protected def createConfig(args: Array[String]): DependenciesConfig = new DependenciesConfig(args)

    val template: URL = this.getClass.getResource("DependencyAnalysis.html.template")
    if (template eq null)
        throw new RuntimeException(
            "the HTML template (DependencyAnalysis.html.template) cannot be found"
        )

    val colors: Set[String] =
        Set("#E41A1C", "#FFFF33", "#FF7F00", "#999999", "#984EA3", "#377EB8", "#4DAF4A", "#F781BF", "#A65628")

    private def checkDocument(doc: String): String = {
        val pattern = "<%[A-Z_]+%>".r
        val option = pattern findFirstIn doc
        option match {
            case Some(o) => {
                println(
                    Console.YELLOW +
                        "[warn] HtmlDocument has at least one unset option " + o +
                        Console.RESET
                )
            }
            case None =>
        }
        doc
    }

    override protected def analyze(
        cp:             Iterable[File],
        analysisConfig: DependenciesConfig,
        execution:      Int
    ): (Project[URL], BasicReport) = {
        val (project, _) = analysisConfig.setupProject(cp)

        val rootPackages = project.rootPackages

        val filteredPackages = analysisConfig.get(PackagesArg)

        class TheDependencyProcessor extends DependencyProcessor {
            protected val dependencyCount = mutable.HashMap.empty[String, mutable.HashMap[String, Int]]
            protected val dependencyCounter = new AtomicInteger(0);

            def addDependency(sourcePN: String, targetPN: String): Unit = boundary {
                val sourcePackage = getPackageName(sourcePN)
                val targetPackage = getPackageName(targetPN)

                // filter by --package
                filteredPackages.foreach { relevantPackages =>
                    if (!relevantPackages.exists(p => sourcePackage.startsWith(p) || targetPackage.startsWith(p)))
                        break();
                }

                // ignore intra-package dependencies
                if (sourcePackage == targetPackage)
                    return

                val depsForSource = dependencyCount.getOrElse(sourcePackage, mutable.HashMap.empty[String, Int])
                depsForSource.update(targetPackage, depsForSource.getOrElse(targetPackage, 0) + 1)
                dependencyCount.update(sourcePackage, depsForSource)
                dependencyCounter.incrementAndGet()
            }

            override def processDependency(
                source: VirtualSourceElement,
                target: VirtualSourceElement,
                dType:  DependencyType
            ): Unit = {
                if (source.isClass && target.isClass)
                    addDependency(
                        source.asInstanceOf[VirtualClass].thisType.packageName,
                        target.asInstanceOf[VirtualClass].thisType.packageName
                    )
            }

            def getPackageName(pn: String): String = boundary {
                if (pn == "") // standard package = <default>
                    return "<default>";

                analysisConfig.get(MainPackageArg).foreach { mainPackage =>
                    if (pn.startsWith(mainPackage))
                        break(pn);
                }

                rootPackages.getOrElse(pn, pn)
            }

            def processDependency(
                source:   VirtualSourceElement,
                baseType: BaseType,
                dType:    DependencyType
            ): Unit = {}

            def processDependency(
                source:    VirtualSourceElement,
                arrayType: ArrayType,
                dType:     DependencyType
            ): Unit = {
                if (source.isClass && arrayType.componentType.isClassType)
                    addDependency(
                        source.asInstanceOf[VirtualClass].thisType.packageName,
                        arrayType.componentType.asInstanceOf[ClassType].packageName
                    )
            }

            def currentDependencyCount(source: String, target: String): Int = {
                dependencyCount.getOrElse(source, mutable.HashMap.empty[String, Int]).getOrElse(target, 0)
            }
            def currentPackages = dependencyCount.keySet
            def currentMaxDependencyCount = dependencyCounter.doubleValue()

        } // dependencyCount(source,target,anzahl)
        val dependencyProcessor = new TheDependencyProcessor()
        val dependencyExtractor = new DependencyExtractor(dependencyProcessor)

        for {
            classFile <- project.allClassFiles
        } {
            dependencyExtractor.process(classFile)
        }

        // create html file from template

        // get packages and sort them
        val packages = dependencyProcessor.currentPackages.toSeq.sorted

        val maxCount = dependencyProcessor.currentMaxDependencyCount

        var data = ("[" + packages.foldRight("")((p1, l1) =>
            "[" +
                packages.foldRight("")((p2, l2) =>
                    s"${dependencyProcessor.currentDependencyCount(p1, p2) / maxCount},$l2"
                ) + "]," + l1
        ) + "]").replaceAll(",]", "]")

        if (analysisConfig.get(InvertArg).getOrElse(false))
            data = "d3.transpose(" + data + ")"

        val cS = """ style="border-style:solid;border-width:1px;""""

        val addOut =
            if (analysisConfig.get(DebugArg).getOrElse(false))
                ("<table> <tr><th" + cS + "></th>" + packages.foldRight("</tr>")((p, l) =>
                    "<th" + cS + ">" + p + "</th>" + l
                ) + packages.foldRight("</table>")((p1, l1) =>
                    "<tr><td" + cS + "><b>" + p1 + "</b></td>" +
                        packages.foldRight("</tr>\n")((p2, l2) =>
                            "<td" + cS + ">" + (dependencyProcessor.currentDependencyCount(p1, p2)) + "</td>" + l2
                        ) + l1
                ))
            else
                ""
        // read the template
        var htmlDocument = processSource(Source.fromFile(template.getPath)(using scala.io.Codec.UTF8)) { _.mkString }

        if (!htmlDocument.contains("<%DATA%>") || !htmlDocument.contains("<%PACKAGES%>")) {
            println(Console.RED +
                "[error] The template: " + template + " is not valid." + Console.RESET)
            sys.exit(-2)
        }

        htmlDocument = htmlDocument.replace("<%TITLE%>", "DependencyAnalysis")

        htmlDocument = htmlDocument.replace("<%DATA%>", data)

        htmlDocument = htmlDocument.replace("<%ADDITIONAL_OUTPUT%>", addOut)

        htmlDocument = htmlDocument.replace(
            "<%PACKAGES%>",
            "[" + packages.foldRight("")((name, json) =>
                s"""{ "name": "$name", "color": "${Random.shuffle(colors.toList).head}"},\n""" + json
            ) + "]"
        )
        writeAndOpen(checkDocument(htmlDocument), "DependencyAnalysis", ".html")

        (project, BasicReport(packages))
    }
}
