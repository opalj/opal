/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package av
package viz

import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

import scala.language.reflectiveCalls
import scala.collection.mutable.HashMap
import scala.util.Random

import org.opalj.util.writeAndOpen
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
import org.opalj.br.analyses.Analysis
import org.opalj.br.analyses.AnalysisExecutor
import org.opalj.br.analyses.BasicReport
import org.opalj.br.analyses.ProgressManagement
import org.opalj.br.analyses.EventType
import org.opalj.br.analyses.Project

/**
 * @author Tobias Becker
 */
object DependencyAnalysis extends AnalysisExecutor {

    val template = this.getClass().getResource("DependencyAnalysis.html.template")
    val colors = Set("#E41A1C", "#FFFF33", "#FF7F00", "#999999", "#984EA3", "#377EB8", "#4DAF4A", "#F781BF", "#A65628")

    var mainPackage: String = ""
    var debug = false
    var filter: String = ""
    var inverse: Boolean = false

    def readParameter(param: String, args: Seq[String], default: String = ""): (String, Seq[String]) = {
        args.partition(_.startsWith("-"+param+"=")) match {
            case (Seq(), parameters1) ⇒ (default, parameters1)
            case (Seq(p), parameters1) ⇒ {
                if (p.startsWith("-"+param+"=\"") && p.endsWith("\""))
                    (p.substring(param.length + 3, p.length - 1), parameters1)
                else
                    (p.substring(param.length + 2), parameters1)
            }
        }
    }

    override def checkAnalysisSpecificParameters(args: Seq[String]): Boolean = {

        val (mainPackage, parameters1) = readParameter("mp", args)
        this.mainPackage = mainPackage

        val (debug, parameters2) = readParameter("debug", parameters1, "false")
        this.debug = debug.toBoolean

        val (inverse, parameters3) = readParameter("inverse", parameters2, "false")
        this.inverse = inverse.toBoolean

        val (filter, parameters4) = readParameter("filter", parameters3)
        this.filter = filter

        parameters4.isEmpty
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
            case Some(o) ⇒ {
                println(
                    Console.YELLOW+
                        "[warn] HtmlDocument has at least one unset option "+o +
                        Console.RESET)
            }
            case None ⇒
        }
        doc
    }

    val analysis = new Analysis[URL, BasicReport] {

        override def description: String =
            "Collects information about the number of dependencies on others packages per package."

        def analyze(
            project: Project[URL],
            parameters: Seq[String],
            initProgressManagement: (Int) ⇒ ProgressManagement) = {

            val pm = initProgressManagement(3)
            pm.progress(1, EventType.Start, Some("setup"))

            import scala.collection.mutable.{ HashSet, HashMap }
            // Collect the number of outgoing dependencies per package 
            // FQPN = FullyQualifiedPackageName
            val dependenciesPerFQPN = HashMap.empty[String, Int]

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
                    dType: DependencyType): Unit = {
                    if (source.isClass && target.isClass)
                        addDependency(source.asInstanceOf[VirtualClass].thisType.packageName, target.asInstanceOf[VirtualClass].thisType.packageName)
                }

                def getPackageName(pn: String): String = {
                    if (pn == "") // standard package = <default>
                        return "<default>"
                    if (mainPackage != "" && pn.startsWith(mainPackage))
                        return pn
                    rootPackages.getOrElse(pn, pn)
                }

                def processDependency(
                    source: VirtualSourceElement,
                    baseType: BaseType,
                    dType: DependencyType): Unit = {

                }
                def processDependency(
                    source: VirtualSourceElement,
                    arrayType: ArrayType,
                    dType: DependencyType): Unit = {
                    if (source.isClass && arrayType.componentType.isObjectType)
                        addDependency(source.asInstanceOf[VirtualClass].thisType.packageName, arrayType.componentType.asInstanceOf[ObjectType].packageName)
                }

                def currentDependencyCount(source: String, target: String): Int = {
                    dependencyCount.getOrElse(source, HashMap.empty[String, Int]).getOrElse(target, 0)
                }
                def currentPackages = dependencyCount.keySet
                def currentMaxDependencyCount = dependencyCounter.doubleValue()

            } // dependencyCount(source,target,anzahl)
            val dependencyExtractor = new DependencyExtractor(dependencyProcessor)

            pm.progress(1, EventType.End, None)

            pm.progress(2, EventType.Start, Some("extracting dependencies"))
            for {
                classFile ← project.classFiles
                packageName = classFile.thisType.packageName
            } {
                dependencyExtractor.process(classFile)
            }
            pm.progress(2, EventType.End, None)

            // create html file from template
            pm.progress(3, EventType.Start, Some("creating HTML"))

            // get packages and sort them
            var packages = dependencyProcessor.currentPackages.toSeq.sorted

            var maxCount = dependencyProcessor.currentMaxDependencyCount

            var data = ("["+packages.foldRight("")(
                (p1, l1) ⇒ "["+
                    packages.foldRight("")(
                        (p2, l2) ⇒ (dependencyProcessor.currentDependencyCount(p1, p2) / maxCount)+","+l2)+"],"+l1)+"]").replaceAll(",]", "]")

            if (inverse)
                data = "d3.transpose("+data+")"

            val cS = """ style="border-style:solid;border-width:1px;""""

            val addOut =
                if (debug)
                    ("<table> <tr><th"+cS+"></th>"+packages.foldRight("</tr>")((p, l) ⇒ "<th"+cS+">"+p+"</th>"+l) + packages.foldRight("</table>")(
                        (p1, l1) ⇒ "<tr><td"+cS+"><b>"+p1+"</b></td>"+
                            packages.foldRight("</tr>\n")(
                                (p2, l2) ⇒ "<td"+cS+">"+(dependencyProcessor.currentDependencyCount(p1, p2))+"</td>"+l2) + l1))
                else
                    ""
            // read the the template
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
                (name, json) ⇒
                    s"""{ "name": "$name", "color": "${Random.shuffle(colors.toList).head}"},\n"""+json)+"]")
            writeAndOpen(checkDocument(htmlDocument), "DependencyAnalysis", ".html")

            pm.progress(3, EventType.End, None)

            BasicReport(packages)
        }
    }
}