/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package tac
package cg
package android

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.jdk.CollectionConverters._
import scala.util.control.Exception.allCatch

import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.EntryPointFinder
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType

/**
 * The AndroidEntryPointFinder considers specific methods of app components as entry points.
 * Requires Android Manifest to be loaded.
 *
 * @author Tom Nikisch
 *         Julius Naeumann
 */
object AndroidEntryPointsFinder extends EntryPointFinder {
    val configKey = "org.opalj.tac.cg.android.AndroidEntryPointsFinder"
    override def collectEntryPoints(project: SomeProject): Iterable[Method] = {
        val entryPoints = ArrayBuffer.empty[Method]
        val defaultEntryPoints = mutable.Map.empty[String, List[(String, Option[MethodDescriptor])]]

        val defaultEntry = project.config.getConfig(configKey)
        defaultEntry.root().entrySet().forEach { entry =>
            val d = entry.getKey
            val entryMethods = defaultEntry.getConfigList(d).asScala.map {
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
                         objectType:          ObjectType,
                         possibleEntryPoints: List[(String, Option[MethodDescriptor])],
                         project:             SomeProject
                       ): Set[Method] = {
        var entryPoints = Set.empty[Method]
        val classHierarchy = project.classHierarchy
        classHierarchy.allSubclassTypes(objectType, reflexive = true).flatMap(project.classFile).
          foreach { subclassType =>
              for (possibleEntryPoint <- possibleEntryPoints) {
                  if (possibleEntryPoint._2.isEmpty) {
                      for (method <- subclassType.findMethod(possibleEntryPoint._1) if method.body.isDefined) {
                          entryPoints += method
                      }
                  } else {
                      for (method <- subclassType.findMethod(possibleEntryPoint._1, possibleEntryPoint._2.get) if method.body.isDefined) {
                          entryPoints += method
                      }
                  }
              }
          }
        entryPoints
    }
}
