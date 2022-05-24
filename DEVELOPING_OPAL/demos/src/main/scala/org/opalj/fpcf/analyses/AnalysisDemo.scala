/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import org.opalj.br.analyses.ProjectAnalysisApplication
import org.opalj.br.analyses.Project
import java.net.URL
import org.opalj.br.Method

/**
 * @author Michael Reif
 */
trait AnalysisDemo extends ProjectAnalysisApplication {

    def entitiesByProperty[P <: Property](
        property: P
    )(
        implicit
        propertyStore: PropertyStore
    ): Iterator[Entity] = {
        propertyStore.entities(property, property)
    }

    def finalReport(infoStrings: Iterable[String], caption: String): String = {
        infoStrings.mkString(s"\n$caption:", "\n\t", s"\nTotal: ${infoStrings.size}\n\n")
    }
}

trait MethodAnalysisDemo extends AnalysisDemo {

    def buildMethodInfo(
        entities:    Iterable[SomeEPS],
        withJarInfo: Boolean           = false
    )(
        implicit
        project: Project[URL]
    ): Iterable[String] = {
        entities map { eps =>
            val method = eps.e.asInstanceOf[Method]
            val methodString = getVisibilityModifier(method)+" "+method.name
            val classFile = method.classFile
            val jarInfo = if (withJarInfo)
                project.source(classFile.thisType)
            else ""
            val classVisibility = if (classFile.isPublic) "public" else ""
            s"$jarInfo\n\t $classVisibility "+classFile.thisType.toJava+" | "+methodString
        }
    }

    private[this] def getVisibilityModifier(method: Method): String = {
        method.visibilityModifier.map(v => v.javaName.get).getOrElse("")
    }
}
