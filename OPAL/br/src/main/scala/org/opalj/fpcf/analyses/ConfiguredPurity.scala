/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package fpcf
package analyses

import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import org.opalj.br.DeclaredMethod
import org.opalj.br.MethodDescriptor
import org.opalj.br.ObjectType
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.ClassExtensibilityKey
import org.opalj.fpcf.properties.Purity

class ConfiguredPurity(
        project:         SomeProject,
        propertyStore:   PropertyStore,
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

    private val methods: Set[DeclaredMethod] =
        for {
            PurityValue(className, methodName, descriptor, property, conditions) ← toSet.toSet

            po = Purity(property)
            if po.isDefined

            if conditions forall {
                _ forall { typeName ⇒
                    val ot = ObjectType(typeName)
                    project.classHierarchy.hasSubtypes(ot).isNo && classExtensibility(ot).isNo
                }
            }

            mdo = if (descriptor == "*") None else Some(MethodDescriptor(descriptor))

            ms = if (className == "*") {
                project.allMethods.filter(m ⇒
                    m.name == methodName && mdo.forall(_ == m.descriptor)).map(declaredMethods(_))
            } else {
                val cfo = project.classFile(ObjectType(className))
                val mo = cfo.map { cf ⇒
                    mdo match {
                        case Some(md) ⇒ cf.findMethod(methodName, MethodDescriptor(descriptor)).toIterable
                        case None     ⇒ cf.findMethod(methodName).toIterable
                    }
                }
                mo.map(_.map(declaredMethods(_))).getOrElse(Iterable.empty)
            }

            dm ← ms
        } yield {
            propertyStore.set(dm, po.get)
            dm.asInstanceOf[DeclaredMethod]
        }

    def wasSet(dm: DeclaredMethod): Boolean = {
        methods.contains(dm)
    }
}

object ConfiguredPurityKey extends ProjectInformationKey[ConfiguredPurity, Nothing] {

    def requirements = Seq(PropertyStoreKey, DeclaredMethodsKey)

    override protected def compute(project: SomeProject): ConfiguredPurity = {
        new ConfiguredPurity(
            project,
            project.get(PropertyStoreKey),
            project.get(DeclaredMethodsKey)
        )
    }
}
