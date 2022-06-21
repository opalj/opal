/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

import java.net.URL
import org.opalj.br.analyses.OneStepAnalysis
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.AnalysisApplication
import org.opalj.br.analyses.BasicReport

import scala.collection.parallel.CollectionConverters.IterableIsParallelizable

/**
 * Counts the number of native methods.
 *
 * @author Michael Eichberg
 */
object PublicMethodsInNonRestrictedPackagesCounter extends AnalysisApplication {

    val restrictedPackages = List( // set of restricted packages for Java 7
        "sun/",
        "com/sun/xml/internal/",
        "com/sun/imageio/",
        "com/sun/istack/internal/",
        "com/sun/jmx/",
        "com/sun/proxy/",
        "com/sun/org/apache/bcel/internal/",
        "com/sun/org/apache/regexp/internal/",
        "com/sun/org/apache/xerces/internal/",
        "com/sun/org/apache/xpath/internal/",
        "com/sun/org/apache/xalan/internal/extensions/",
        "com/sun/org/apache/xalan/internal/lib/",
        "com/sun/org/apache/xalan/internal/res/",
        "com/sun/org/apache/xalan/internal/templates/",
        "com/sun/org/apache/xalan/internal/utils/",
        "com/sun/org/apache/xalan/internal/xslt/",
        "com/sun/org/apache/xalan/internal/xsltc/cmdline/",
        "com/sun/org/apache/xalan/internal/xsltc/compiler/",
        "com/sun/org/apache/xalan/internal/xsltc/trax/",
        "com/sun/org/apache/xalan/internal/xsltc/util/",
        "com/sun/org/apache/xml/internal/res/",
        "com/sun/org/apache/xml/internal/serializer/utils/",
        "com/sun/org/apache/xml/internal/utils/",
        "com/sun/org/apache/xml/internal/security/",
        "com/sun/org/glassfish/",
        "org/jcp/xml/dsig/internal/",
        "com/sun/java/accessibility/"
    )

    val analysis = new OneStepAnalysis[URL, BasicReport] {

        override def description =
            "Counts the number of public/protected methods in non-restricted packages."

        def doAnalyze(
            project:       Project[URL],
            parameters:    Seq[String],
            isInterrupted: () => Boolean
        ) = {
            val methods =
                (
                    for {
                        classFile <- project.allClassFiles.par
                        if classFile.isPublic
                        if !restrictedPackages.exists(classFile.fqn.startsWith(_))
                        method <- classFile.methods
                        if method.body.isDefined
                        if method.isPublic || (method.isProtected && !classFile.isFinal)
                        referenceParametersCount = method.parameterTypes.count(_.isReferenceType)
                    } yield {
                        method.toJava(referenceParametersCount.toString)
                    }
                ).seq

            BasicReport(
                s"${methods.size} public and protected methods in non-restricted packages found:\n"+
                    methods.mkString("\t", "\n\t", "\n")
            )
        }
    }
}
