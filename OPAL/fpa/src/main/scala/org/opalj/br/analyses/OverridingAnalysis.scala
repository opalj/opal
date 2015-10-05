package org.opalj.br.analyses

import org.opalj.fpa.AssumptionBasedFixpointAnalysis
import org.opalj.br.analyses.fp.ProjectAccessibility
import org.opalj.fp.Result
import org.opalj.br.analyses.fp.PackageLocal
import java.net.URL
import org.opalj.fp.PropertyStore
import org.opalj.fp.PropertyComputationResult
import org.opalj.br.Method
import org.opalj.fp.Entity
import org.opalj.fpa.FilterEntities
import org.opalj.fp.Property
import org.opalj.fp.PropertyKey
import org.opalj.br.ClassFile

sealed trait Overridden extends Property {
    final def key = Overridden.Key
}

object Overridden {
    final val Key = PropertyKey.create("Overridden", NonOverridden)
}

case object IsOverridden extends Overridden { final val isRefineable = false }

case object PotentiallyOverridden extends Overridden { final val isRefineable = false }

case object NonOverridden extends Overridden { final val isRefineable = false }

/**
 * @author Michael Reif
 *
 * This Analysis determines the ´Overridden´ property of a method. A method is considered as overridden
 * if it is overridden in every subclass.
 */
object OverridingAnalysis
        extends AssumptionBasedFixpointAnalysis
        with FilterEntities[Method] {

    val propertyKey = ProjectAccessibility.Key

    /**
     * Determines the [[Overridden]] property of static methods considering shadowing of methods
     * provided by super classes. It is tailored to entry point set computation where we have to consider different kind
     * of assumption depending on the analyzed program.
     *
     * Computational differences regarding static methods are :
     *  - private methods can be handled equal in every context
     *  - if OPA is met, all package visible classes are visible which implies that all non-private methods are
     *    visible too
     *  - if CPA is met, methods in package visible classes are not visible by default.
     *
     */
    def determineProperty(
        method: Method)(
            implicit project: Project[URL],
            store: PropertyStore): PropertyComputationResult = {

        if (method.isPrivate || method.isFinal)
            return Result(method, NonOverridden)

        // this would be way more efficient when caluclating entry points
        // but if you want to have a dedicated looking at the isOverridden property,
        // it will lead to incorrect results
        /*if (isOpenPackagesAssumption)
            return Result(method, Global) */

        val declaringClass = project.classFile(method)

        if (declaringClass.isFinal)
            return Result(method, NonOverridden)

        val methodDescriptor = method.descriptor
        val methodName = method.name

        val subtypes = project.classHierarchy.allSubtypes(declaringClass.thisType, false)

        val visibleClass: ClassFile ⇒ Boolean = _.isPublic || isOpenPackagesAssumption

        subtypes foreach { subtype ⇒
            project.classFile(subtype) map { classFile ⇒
                val potentialMethod = classFile.findMethod(methodName, methodDescriptor)
                val couldInheritMethod = potentialMethod.isEmpty || !potentialMethod.map { curMethod ⇒
                    curMethod.visibilityModifier.equals(method.visibilityModifier)
                }.getOrElse(false)

                if (couldInheritMethod) {
                    val superuperTypes = project.classHierarchy.allSupertypes(subtype, false)

                    // the check, if the superclass is visible, leads to wrong results in terms
                    // of the Overridden property
                    val inheritsMethod = !superuperTypes.exists { supertype ⇒
                        project.classFile(supertype).map { supClassFile ⇒
                            supClassFile.findMethod(methodName, methodDescriptor).nonEmpty &&
                                visibleClass(supClassFile)
                        }.getOrElse(true) && (supertype ne declaringClass.thisType)
                    }

                    if (inheritsMethod)
                        return Result(method, NonOverridden)
                }
            }
        }

        if (visibleClass(declaringClass))
            return Result(method, PotentiallyOverridden)

        // If no subtype is found, the method is not accessible
        Result(method, IsOverridden)
    }

    val entitySelector: PartialFunction[Entity, Method] = {
        case m: Method if !m.isStatic && m.body.isDefined ⇒ m
    }
}