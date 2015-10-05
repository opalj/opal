package org.opalj.fpa.demo.util

import org.opalj.br.analyses.DefaultOneStepAnalysis
import org.opalj.fp.Property
import org.opalj.fp.PropertyStore
import org.opalj.fp.Entity
import org.opalj.br.analyses.Project
import java.net.URL
import org.opalj.br.Method
import org.opalj.br.ClassFile
import org.opalj.bi.VisibilityModifier

/**
 * @author Michael Reif
 */
trait AnalysisDemo extends DefaultOneStepAnalysis {

    def entitiesByProperty(
        property: Property)(
            implicit propertyStore: PropertyStore): Traversable[(Entity, Property)] =
        propertyStore(property.key).filter { ep ⇒
            val isFactoryMethod = ep._2
            isFactoryMethod == property
        }

    def finalReport(infoStrings: Traversable[String], caption: String): String =
        infoStrings.mkString(s"\n$caption:", "\n\t", s"\nTotal: ${infoStrings.size}\n\n")
}

trait MethodAnalysisDemo extends AnalysisDemo {

    def buildMethodInfo(
        entities: Traversable[(Entity, Property)],
        withJarInfo: Boolean = false)(
            implicit project: Project[URL]): Traversable[String] = entities.map { e ⇒
        val method = e._1.asInstanceOf[Method]
        val methodString = getVisibilityModifier(method)+" "+method.name
        val classFile = project.classFile(method)
        val jarInfo = if (withJarInfo)
            project.source(classFile.thisType)
        else ""
        val classVisibility = if (classFile.isPublic) "public" else ""
        jarInfo + s"\n\t $classVisibility "+classFile.thisType.toJava+" | "+methodString
    }

    private[this] def getVisibilityModifier(method: Method): String = {
        method.visibilityModifier map { mod ⇒
            if (mod.javaName.nonEmpty) mod.javaName.get else ""
        } getOrElse ""
    }
}

trait ClassAnalysisDemo extends AnalysisDemo {

    def buildClassInfo(
        entities: Traversable[(Entity, Property)],
        withJarInfo: Boolean = false)(
            implicit project: Project[URL]): Traversable[String] = entities.map { e ⇒
        val classFile = e._1.asInstanceOf[ClassFile]
        val jarInfo = if (withJarInfo)
            project.source(classFile.thisType)
        else ""
        jarInfo+"\n\t "+classFile.thisType.toJava
    }
}