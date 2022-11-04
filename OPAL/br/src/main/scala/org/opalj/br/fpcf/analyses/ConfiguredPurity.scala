/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package fpcf
package analyses

import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

import org.opalj.br.analyses.DeclaredMethods
import org.opalj.br.analyses.DeclaredMethodsKey
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.ClassExtensibilityKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.fpcf.properties.Purity

/**
 * @author Dominik Helm
 */
class ConfiguredPurity(
        project:         SomeProject,
        declaredMethods: DeclaredMethods
) {

    private case class PurityValue(
            cf:    String,
            m:     String,
            desc:  String,
            p:     String,
            conds: Option[Seq[String]]
    )

    private val classExtensibility = project.get(ClassExtensibilityKey)

    private val toSet = project.config.as[Seq[PurityValue]](
        "org.opalj.fpcf.analyses.ConfiguredPurity.purities"
    )

    private val methods: Map[DeclaredMethod, Purity] = (
        for {
            PurityValue(className, methodName, descriptor, property, conditions) <- toSet.toSet

            po = Purity(property)
            if po.isDefined

            if conditions forall {
                _ forall { typeName =>
                    val ot = ObjectType(typeName)
                    project.classHierarchy.hasSubtypes(ot).isNo && classExtensibility(ot).isNo
                }
            }

            mdo = if (descriptor == "*") None else Some(MethodDescriptor(descriptor))

            ms = if (className == "*") {
                project.allMethods.filter { m =>
                    m.name == methodName && mdo.forall(_ == m.descriptor)
                }.map(declaredMethods(_))
            } else {
                val classType = ObjectType(className)

                mdo match {
                    case Some(md) => Seq(
                        declaredMethods(classType, classType.packageName, classType, methodName, md)
                    )
                    case None => project.classFile(classType).map { cf =>
                        cf.findMethod(methodName).map(declaredMethods(_))
                    }.getOrElse(Seq.empty)

                }
            }

            dm <- ms
        } yield {
            dm -> po.get
        }
    ).toMap

    def wasSet(dm: DeclaredMethod): Boolean = methods.contains(dm)

    def purity(dm: DeclaredMethod): Purity = methods(dm)

}

object ConfiguredPurityKey extends ProjectInformationKey[ConfiguredPurity, Nothing] {

    override def requirements(project: SomeProject): ProjectInformationKeys = Seq(PropertyStoreKey, DeclaredMethodsKey)

    override def compute(project: SomeProject): ConfiguredPurity = {
        val declaredMethods = project.get(DeclaredMethodsKey)
        new ConfiguredPurity(project, declaredMethods)
    }
}
