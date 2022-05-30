/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses
package cg

import scala.collection.mutable
import net.ceedubs.ficus.Ficus._
import org.opalj.collection.mutable.ArrayMap

/**
 * Determines whether a class or interface is '''directly extensible by a (yet unknown)
 * client application/library'''. A class/interface is directly extensible if a developer can
 * define a direct - not transitive - subtype that is not part of the given application/library.
 *
 * This analysis uses the [[ClosedPackages]] information.
 *
 * @author Michael Reif
 * @author Michael Eichberg
 */
abstract class ClassExtensibility extends (ObjectType => Answer) {

    /** See [[isClassExtensible]]. */
    final override def apply(t: ObjectType): Answer = this.isClassExtensible(t)

    /**
     * Determines whether the given class can directly be extended by (yet unknown) code.
     */
    def isClassExtensible(t: ObjectType): Answer
}

abstract class AbstractClassExtensibility extends ClassExtensibility {

    val project: SomeProject

    /**
     * Enables subclasses to explicitly specify the (non-)extensible types. This enables
     * users to use domain knowledge to override the result of the base analysis.
     *
     * @note See [[AbstractClassExtensibility#parseSpecifiedClassesList]] for how to use OPAL's
     *       configuration to configure sets of object types.
     *
     * @return  Those types for which the direct extensibility is explicit configured.
     */
    protected[this] def configuredExtensibleClasses: Iterator[(ObjectType, Answer)] = Iterator.empty

    /**
     * Get the list of configured types using the configured config key.
     *
     * @param   simpleKey The simple name of the config key that will be used to get a list of
     *          configured object types. [[ClassExtensibilityKey.ConfigKeyPrefix]].
     * @return  A list of [[ObjectType]]s. The semantic of those types is encoded by the
     *          respective analysis;
     *          [[AbstractClassExtensibility#configuredExtensibleClasses]].
     */
    protected[this] def parseSpecifiedClassesList(simpleKey: String): List[ObjectType] = {
        val completeKey = ClassExtensibilityKey.ConfigKeyPrefix + simpleKey
        val fqns = project.config.as[Option[List[String]]](completeKey).getOrElse(List.empty)

        import project.classHierarchy

        fqns.flatMap { fqn =>
            // We chose "/." to identify all subtypes, because we can only use a character
            // (sequence) that contains an invalid character in a JVM identifier.
            if (fqn.endsWith("/.")) {
                val ot = ObjectType(fqn.substring(0, fqn.length - 2))
                classHierarchy.allSubtypes(ot, reflexive = true)
            } else {
                List(ObjectType(fqn))
            }
        }
    }

    private[this] val classExtensibility: ArrayMap[Answer] = {

        val isClosedPackage = project.get(ClosedPackagesKey)

        val configuredTypes: mutable.LongMap[Answer] = mutable.LongMap.empty[Answer] ++ configuredExtensibleClasses.map { e =>
            val (ot, answer) = e
            (ot.id.toLong, answer)
        }

        val allClassFiles = project.allClassFiles
        val entries = ObjectType.objectTypesCount
        val extensibility = allClassFiles.foldLeft(ArrayMap[Answer](entries)) { (r, classFile) =>
            val objectType = classFile.thisType
            val isExtensible = {
                val configured = configuredTypes.get(objectType.id.toLong)
                if (configured.isDefined)
                    configured.get
                else if (classFile.isEffectivelyFinal ||
                    classFile.isEnumDeclaration ||
                    classFile.isAnnotationDeclaration)
                    No
                else if (classFile.isPublic)
                    Yes
                else if (isClosedPackage(objectType.packageName))
                    No
                else // => non public class in an open package...
                    Yes
            }
            r(objectType.id) = isExtensible
            r

        }
        extensibility
    }

    /**
     * Determines whether the given class can directly be extended by (yet unknown) code.
     */
    def isClassExtensible(t: ObjectType): Answer = classExtensibility.get(t.id).getOrElse(Unknown)
}

class DefaultClassExtensibility(val project: SomeProject) extends AbstractClassExtensibility

/**
 * Determines whether a type is directly extensible by a (yet unknown)
 * client application/library using the base analysis [[ClassExtensibility]]
 * and an explicitly configured list of extensible types where the list overrides the findings of
 * the analysis. This enables domain specific configurations.
 *
 * Additional configuration has to be done using OPAL's configuration file. To specify
 * extensible classes/interfaces the following key has to be used:
 * `[[ClassExtensibilityKey.ConfigKeyPrefix]] + extensibleClasses`
 *
 * @example The following example configuration would consider `java/util/Math` and
 *          `com/example/Type` as ''extensible''.
 *          {{{
 *          org.opalj.br.analyses.cg.ClassExtensibilityKey.extensibleClasses =
 *              ["java/util/Math", "com/example/Type"]
 *          }}}
 */
class ConfiguredExtensibleClasses(val project: SomeProject) extends AbstractClassExtensibility {

    /**
     * Returns the types which are extensible.
     */
    override def configuredExtensibleClasses: Iterator[(ObjectType, Yes.type)] = {
        parseSpecifiedClassesList("extensibleClasses").iterator.map(t => (t, Yes))
    }
}

/**
 * Determines whether a type is directly extensible by a (yet unknown)
 * client application/library using the base analysis [[ClassExtensibility]]
 * and an explicitly configured list of final (not extensible) types; the configuration
 * is always considered first. This enables domain specific configurations.
 *
 * Additional configuration has to be done using OPAL's configuration file. To specify
 * classes/interfaces that are not extensible/that are final the following key has to be used:
 * `[[ClassExtensibilityKey.ConfigKeyPrefix]] + finalClasses`
 *
 * @example The following example configuration would consider `java/util/Math` and
 *          `com/exmaple/Type` as ''not extensible''.
 *          {{{
 *          org.opalj.br.analyses.cg.ClassExtensibilityKey.finalClasses =
 *              ["java/util/Math", "com/example/Type"]
 *          }}}
 */
class ConfiguredFinalClasses(val project: SomeProject) extends AbstractClassExtensibility {

    /**
     * Returns the types which are not extensible/which are final.
     */
    override def configuredExtensibleClasses: Iterator[(ObjectType, No.type)] = {
        parseSpecifiedClassesList("finalClasses").iterator.map(t => (t, No))
    }
}

class ClassHierarchyIsNotExtensible(val project: SomeProject) extends ClassExtensibility {

    def isClassExtensible(t: ObjectType): Answer = No
}

