/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg
package android

import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.EntryPointFinder
import org.opalj.br.{Method, MethodDescriptor, ObjectType}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters._
import scala.util.control.Exception.allCatch

/**
 * The AndroidEntryPointFinder considers specific methods of app components as entry points.
 * It does not work for androidx
 *
 * @author Tom Nikisch
 */
object AndroidEntryPointsFinder extends EntryPointFinder {

    override def collectEntryPoints(project: SomeProject): Iterable[Method] = {
        val entryPoints = ArrayBuffer.empty[Method]
        val defaultEntryPoints = mutable.Map.empty[String, List[(String, Option[MethodDescriptor])]]
        val defEntry = project.config.getConfig("org.opalj.tac.cg.android.AndroidEntryPointsFinder")
        defEntry.root().entrySet().forEach { e =>
            val d = e.getKey
            val entryMethods = defEntry.getConfigList(d).asScala.map {
                entry =>
                    (
                        entry.getString("name"),
                        allCatch.opt(entry.getString("descriptor")).map(MethodDescriptor(_))
                    )
            }.toList
            defaultEntryPoints += (d -> entryMethods)
        }

        for ((superClass, methodList) <- defaultEntryPoints) {
            entryPoints ++= findEntryPoints(ObjectType(superClass), methodList, project)
        }
        entryPoints
    }

    def findEntryPoints(
        ot:                  ObjectType,
        possibleEntryPoints: List[(String, Option[MethodDescriptor])],
        project:             SomeProject
    ): Set[Method] = {
        var entryPoints = Set.empty[Method]
        val classHierarchy = project.classHierarchy
        classHierarchy.allSubclassTypes(ot, reflexive = true).flatMap(project.classFile).foreach { sc =>
            for (pep <- possibleEntryPoints) {
                if (pep._2.isEmpty) {
                    for (m <- sc.findMethod(pep._1) if m.body.isDefined) {
                        entryPoints += m
                    }
                } else {
                    for (m <- sc.findMethod(pep._1, pep._2.get) if m.body.isDefined) {
                        entryPoints += m
                    }
                }
            }
        }
        entryPoints
    }
}
