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
package br
package analyses

import scala.collection.mutable

import net.ceedubs.ficus.Ficus._

/**
 * Determines whether a type (class or interface) is directly extensible by a (yet unknown)
 * client application/library. A type is directly extensible if a developer could have
 * defined a direct - not transitive - subtype that is not part of the given application/library.
 *
 * @author Michael Reif
 */
class DirectTypeExtensibilityInformation(val project: SomeProject) extends (ObjectType ⇒ Answer) {

    private[this] lazy val typeExtensibility: Map[ObjectType, Answer] = compute

    /**
     * Determines whether the given type can directly be extended by a (yet unknown)
     * library/application.
     */
    def apply(t: ObjectType): Answer = typeExtensibility.get(t).getOrElse(Unknown)

    def compute: Map[ObjectType, Answer] = {
        val extensibility = mutable.Map.empty[ObjectType, Answer]
        val isClosedPackage = project.get(ClosedPackagesKey)

        project.allClassFiles.foreach { classFile ⇒

            val objectType = classFile.thisType

            if (classFile.isEffectivelyFinal ||
                classFile.isEnumDeclaration ||
                classFile.isAnnotationDeclaration)
                extensibility.put(objectType, No)
            else if (classFile.isPublic)
                extensibility.put(objectType, Yes)
            else if (isClosedPackage(objectType.fqn))
                extensibility.put(objectType, No)
            else
                extensibility.put(objectType, Yes)
        }

        overwriteTypeExtensibility.foreach { entry ⇒
            val (ot, ans) = entry
            if (extensibility.get(ot).nonEmpty)
                extensibility.update(ot, ans)
            else
                extensibility.put(ot, ans)
        }

        extensibility.toMap
    }

    /**
     * This method allows to overwrite the computed information whether a type is extensible or not. This
     * facilitates to use domain knowledge to set the otherwise computed property for certain classes.
     *
     * @note See [[DirectTypeExtensibilityInformation.parseConfig()]] for information how to parse the
     *      config key.
     */
    def overwriteTypeExtensibility: Set[(ObjectType, Answer)] = Set.empty

    /**
     *
     * @param key The config key that will be parsed. The prefix of the key is defined by
     *            [[DirectTypeExtensibilityKey.ConfigKeyPrefix]].
     * @return A list of ObjectTypes that fulfill a certain property. The semantic of those types
     *         has to be encoded in [[DirectTypeExtensibilityInformation.overwriteTypeExtensibility]].
     */
    protected[analyses] def parseConfig(key: String): Set[ObjectType] = {

        val fqns = project.config.as[Option[List[String]]](
            DirectTypeExtensibilityKey.ConfigKeyPrefix + key
        ).getOrElse(List.empty)

        import project.classHierarchy

        fqns.map { fqn ⇒
            if (fqn.endsWith("&")) {
                val ot = ObjectType(fqn.substring(0, fqn.length - 1))
                classHierarchy.allSubtypes(ot, true)
            } else {
                Set(ObjectType(fqn))
            }
        }.toSet.flatten
    }
}

/**
 * An analysis that determines whether a class/interface is directly extensible by a (yet unknown)
 * client application/library. Beneath the computation it allows a user to configure additional
 * types that shall be considered as extensible. Therefore, it allows a user to integrate domain
 * knowledge into the analysis.
 *
 * Additional configuration has to be done over the global project configuration. In order to configure
 * classes/interfaces as extensible the following key has to be used:
 *
 * [[DirectTypeExtensibilityKey.ConfigKeyPrefix]] + extensibleTypes
 *
 * ## Example configuration ##
 *
 * The following example configuration would consider ''java/util/Math'' and ''com/exmamle/Type''
 * as extensible.
 *
 * {{{
 *   org.opalj.br.analyses.DirectTypeExtensibilityKey.extensibleTypes = ["java/util/Math", "com/example/Type"]
 * }}}
 */
class ConfigureExtensibleTypes(
        project: SomeProject
) extends DirectTypeExtensibilityInformation(project) {

    /**
     * Overwrites all given types with the extensibility property.
     *
     * @return List of tuples where each tuple has the form: (''ObjectType'', Yes).
     */
    override def overwriteTypeExtensibility: Set[(ObjectType, Answer)] = {
        val configuredTypes = parseConfig("extensibleTypes")
        configuredTypes.map((_, Yes))
    }
}

/**
 * Determines whether a class/interface is directly extensible by a (yet unknown)
 * client application/library. Beneath the computation it allows a user to configure additional
 * types that shall not be considered as extensible. Therefore, it allows a user to integrate domain
 * knowledge into the analysis.
 *
 * Additional configuration has to be done over the global project configuration. In order to configure
 * classes/interfaces not as extensible the following key has to be used:
 *
 * [[DirectTypeExtensibilityKey.ConfigKeyPrefix]] + finalTypes
 *
 * @example The following example configuration would consider ''java/util/Math'' and
 *          ''com/exmamle/Type'' as ''not extensible''.
 *
 *          {{{
 *          org.opalj.br.analyses.DirectTypeExtensibilityKey.finalTypes =
 *              ["java/util/Math", "com/example/Type"]
 *          }}}
 */
class ConfigureFinalTypes(
        override val project: SomeProject
) extends DirectTypeExtensibilityInformation(project) {

    /**
     * Overwrites all given types with the extensibility property.
     *
     * @return List of tuples where each tuple has the form: (''ObjectType'', No).
     */
    override def overwriteTypeExtensibility: Set[(ObjectType, Answer)] = {
        val configuredTypes = parseConfig("finalTypes")
        configuredTypes.map((_, No))
    }
}
