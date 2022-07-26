/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package av
package viz

import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

import scala.language.reflectiveCalls
import scala.util.Random

import org.opalj.io.writeAndOpen
import org.opalj.br.analyses.Analysis
import org.opalj.br.analyses.Project
import org.opalj.de.DependencyExtractor
import org.opalj.de.DependencyProcessor
import org.opalj.de.DependencyType

import org.opalj.br.ArrayType
import org.opalj.br.BaseType
import org.opalj.br.ObjectType
import org.opalj.br.VirtualClass
import org.opalj.br.VirtualSourceElement
import org.opalj.br.analyses.AnalysisApplication
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.ProgressManagement
import org.opalj.br.analyses.ProgressEvents

/**
 * @author Tobias Becker
 */
object DependencyAnalysis extends AnalysisApplication {

    val template = this.getClass().getResource("DependencyAnalysis.html.template")
    if (template eq null)
        throw new RuntimeException(
            "the HTML template (DependencyAnalysis.html.template) cannot be found"
        )

    val colors = Set("#E41A1C", "#FFFF33", "#FF7F00", "#999999", "#984EA3", "#377EB8", "#4DAF4A", "#F781BF", "#A65628")

    var mainPackage: String = ""
    var debug = false
    var filter: String = ""
    var inverse: Boolean = false

    def readParameter(param: String, args: Seq[String], default: String = ""): (String, Seq[String]) = {
        args.partition(_.startsWith("-"+param+"=")) match {
            case (Seq(), parameters1) => (default, parameters1)
            case (Seq(p), parameters1) => {
                if (p.startsWith("-"+param+"=\"") && p.endsWith("\""))
                    (p.substring(param.length + 3, p.length - 1), parameters1)
                else
                    (p.substring(param.length + 2), parameters1)
            }
        }
    }

    override def checkAnalysisSpecificParameters(args: Seq[String]): Iterable[String] = {

        val (mainPackage, parameters1) = readParameter("mp", args)
        this.mainPackage = mainPackage

        val (debug, parameters2) = readParameter("debug", parameters1, "false")
        this.debug = debug.toBoolean

        val (inverse, parameters3) = readParameter("inverse", parameters2, "false")
        this.inverse = inverse.toBoolean

        val (filter, parameters4) = readParameter("filter", parameters3)
        this.filter = filter

        if (parameters4.isEmpty)
            Iterable.empty
        else
            parameters4.map("unknown parameter: "+_)
    }

    override def analysisSpecificParametersDescription: String = ""+
        "[-mp=<Package-Name> (Main Package, won't be clustered. default: \"\")]\n"+
        "[-debug=<Boolean> (true, if there should be additional output. default: false)]\n"+
        "[-inverse=<Boolean> (true, if incoming and outgoing dependencies should be switched. default: false)]\n"+
        "[-filter=<Prefix> (Only show dependencies within packages with this prefix. default: \"\")]\n"

    private def checkDocument(doc: String): String = {
        val pattern = "<%[A-Z_]+%>".r
        val option = pattern findFirstIn doc
        option match {
            case Some(o) => {
                println(
                    Console.YELLOW+
                        "[warn] HtmlDocument has at least one unset option "+o +
                        Console.RESET
                )
            }
            case None =>
        }
        doc
    }

    val analysis = new Analysis[URL, BasicReport] {

        override def description: String =
            "Collects information about the number of dependencies on others packages per package."

        def analyze(
            project:                Project[URL],
            parameters:             Seq[String],
            initProgressManagement: (Int) => ProgressManagement
        ) = {

            val pm = initProgressManagement(3)
            pm.progress(1, ProgressEvents.Start, Some("setup"))

            import scala.collection.mutable.HashMap

            val rootPackages = project.rootPackages

            val dependencyProcessor = new DependencyProcessor {
                protected[this] val dependencyCount = HashMap.empty[String, HashMap[String, Int]]
                protected[this] val dependencyCounter = new AtomicInteger(0);

                def addDependency(sourcePN: String, targetPN: String): Unit = {
                    val sourcePackage = getPackageName(sourcePN)
                    val targetPackage = getPackageName(targetPN)

                    // filter by -filter=<prefix>
                    if (filter != "" && !sourcePackage.startsWith(filter) || !targetPackage.startsWith(filter))
                        return

                    // ignore interpackage dependencies
                    if (sourcePackage == targetPackage)
                        return

                    val depsForSource = dependencyCount.getOrElse(sourcePackage, HashMap.empty[String, Int])
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

                def getPackageName(pn: String): String = {
                    if (pn == "") // standard package = <default>
                        return "<default>"
                    if (mainPackage != "" && pn.startsWith(mainPackage))
                        return pn
                    rootPackages.getOrElse(pn, pn)
                }

                def processDependency(
                    source:   VirtualSourceElement,
                    baseType: BaseType,
                    dType:    DependencyType
                ): Unit = {

                }
                def processDependency(
                    source:    VirtualSourceElement,
                    arrayType: ArrayType,
                    dType:     DependencyType
                ): Unit = {
                    if (source.isClass && arrayType.componentType.isObjectType)
                        addDependency(
                            source.asInstanceOf[VirtualClass].thisType.packageName,
                            arrayType.componentType.asInstanceOf[ObjectType].packageName
                        )
                }

                def currentDependencyCount(source: String, target: String): Int = {
                    dependencyCount.getOrElse(source, HashMap.empty[String, Int]).getOrElse(target, 0)
                }
                def currentPackages = dependencyCount.keySet
                def currentMaxDependencyCount = dependencyCounter.doubleValue()

            } // dependencyCount(source,target,anzahl)
            val dependencyExtractor = new DependencyExtractor(dependencyProcessor)

            pm.progress(1, ProgressEvents.End, None)

            pm.progress(2, ProgressEvents.Start, Some("extracting dependencies"))
            for {
                classFile <- project.allClassFiles
                packageName = classFile.thisType.packageName
            } {
                dependencyExtractor.process(classFile)
            }
            pm.progress(2, ProgressEvents.End, None)

            // create html file from template
            pm.progress(3, ProgressEvents.Start, Some("creating HTML"))

            // get packages and sort them
            val packages = dependencyProcessor.currentPackages.toSeq.sorted

            val maxCount = dependencyProcessor.currentMaxDependencyCount

            var data = ("["+packages.foldRight("")(
                (p1, l1) => "["+
                    packages.foldRight("")(
                        (p2, l2) => s"${dependencyProcessor.currentDependencyCount(p1, p2) / maxCount},$l2"
                    )+"],"+l1
            )+"]").replaceAll(",]", "]")

            if (inverse)
                data = "d3.transpose("+data+")"

            val cS = """ style="border-style:solid;border-width:1px;""""

            val addOut =
                if (debug)
                    ("<table> <tr><th"+cS+"></th>"+packages.foldRight("</tr>")((p, l) => "<th"+cS+">"+p+"</th>"+l) + packages.foldRight("</table>")(
                        (p1, l1) => "<tr><td"+cS+"><b>"+p1+"</b></td>"+
                            packages.foldRight("</tr>\n")(
                                (p2, l2) => "<td"+cS+">"+(dependencyProcessor.currentDependencyCount(p1, p2))+"</td>"+l2
                            ) + l1
                    ))
                else
                    ""
            // read the template
            var htmlDocument = scala.io.Source.fromFile(template.getPath())(scala.io.Codec.UTF8).mkString

            if (!htmlDocument.contains("<%DATA%>") || !htmlDocument.contains("<%PACKAGES%>")) {
                println(Console.RED+
                    "[error] The template: "+template+" is not valid."+Console.RESET)
                sys.exit(-2)
            }

            htmlDocument = htmlDocument.replace("<%TITLE%>", "DependencyAnalysis")

            htmlDocument = htmlDocument.replace("<%DATA%>", data)

            htmlDocument = htmlDocument.replace("<%ADDITIONAL_OUTPUT%>", addOut)

            htmlDocument = htmlDocument.replace("<%PACKAGES%>", "["+packages.foldRight("")(
                (name, json) =>
                    s"""{ "name": "$name", "color": "${Random.shuffle(colors.toList).head}"},\n"""+json
            )+"]")
            writeAndOpen(checkDocument(htmlDocument), "DependencyAnalysis", ".html")

            pm.progress(3, ProgressEvents.End, None)

            BasicReport(packages)
        }
    }
}
