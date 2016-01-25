package org.opalj
package fpcf
package analysis
package methods

import org.opalj.br.{ClassFile, ObjectType, Method}
import org.opalj.br.analyses._

import scala.collection.mutable.ListBuffer

/**
 * This analysis determines on-demand the call-by-signature targets of an interface method.
 * It is in particular relevant when analyzing software libraries and not whole applications,
 * since applications cannot be extended anymore w.r.t. to subtyping.
 *
 * The analysis will take care, that it will not be executed, when some application is analyzed.
 *
 * @author Michael Reif
 */
class CallBySignatureTargetAnalysis private (
        val project: SomeProject
) extends FPCFAnalysis {

    def determineCallBySignatureTargets(
        projectIndex: ProjectIndex
    )(method: Method): Property = {

        val methodName = method.name
        val methodDescriptor = method.descriptor
        val interfaceClassFile = project.classFile(method)

        val interfaceType = interfaceClassFile.thisType
        val classHierarchy = project.classHierarchy

        val cbsTargets = ListBuffer.empty[Method]

        def analyzeMethod(m: Method): Unit = {
            if (m.isAbstract)
                return ;

            if (!isInheritableMethod(m))
                return ;

            val clazzClassFile = project.classFile(m)
            if (!clazzClassFile.isClassDeclaration)
                return ;

            if (clazzClassFile.isEffectivelyFinal)
                return ;

            val clazzType = clazzClassFile.thisType

            if (clazzType eq ObjectType.Object)
                return ;

            if (classHierarchy.isSubtypeOf(clazzType, interfaceType).isYes /* we want to get a sound overapprox. not: OrUnknown*/ )
                return ;

            if (hasSubclassInheritingTheInterface(clazzType, m, interfaceType, project).isYes)
                return ;

            if (!clazzClassFile.isPublic &&
                isClosedLibrary &&
                !hasSubclassProxyForMethod(clazzType, m, interfaceClassFile, project).isYesOrUnknown)
                return ;

            cbsTargets += m
        }

        val potentialTargets = projectIndex.findMethods(methodName, methodDescriptor).view.filter {
            _.isPublic
        }

        potentialTargets foreach analyzeMethod

        if (cbsTargets.isEmpty)
            NoResolution
        else
            CbsTargets(cbsTargets.toSet)
    }

    /*
* A method is considered inheritable by if:
* 	- it is either public or projected
*  - OPA is applied and the method is package private
*
* @note This does not consider the visibility of the method's class,
*       hence, it should be checked separately if the class can be subclassed.
*/
    @inline private[this] def isInheritableMethod(
        method: Method
    ): Boolean = {

        if (method.isPrivate)
            false
        else if (method.isPackagePrivate)
            // package visible methods are only inheritable under OPA
            // if the AnalysisMode isn't OPA, it can only be CPA
            //  => call by signature does not matter in the an APP context
            isOpenLibrary
        else
            // method is public or protected
            true
    }

    private[this] def hasSubclassProxyForMethod(
        classType:          ObjectType,
        method:             Method,
        interfaceClassFile: ClassFile,
        project:            SomeProject
    ): Answer = {

        val classHierarchy = project.classHierarchy
        val methodName = method.name
        val methodDescriptor = method.descriptor

        var isUnknown = false

        val subClassIterator = classHierarchy.allSubclasses(classType, reflexive = false)
        while (subClassIterator.hasNext) {
            val subclass = subClassIterator.next
            project.classFile(subclass) match {
                case Some(subclassCf) ⇒
                    if (subclassCf.isPublic
                        && subclassCf.findMethod(methodName, methodDescriptor).isEmpty)
                        return Yes;
                case None ⇒
                    // do not return here, we can still find a valid subclass
                    isUnknown = true
            }
        }

        if (isUnknown)
            Unknown
        else
            No
    }

    private[this] def hasSubclassInheritingTheInterface(
        classType:     ObjectType,
        method:        Method,
        interfaceType: ObjectType,
        project:       SomeProject
    ): Answer = {
        val classHierarchy = project.classHierarchy
        val methodName = method.name
        val methodDescriptor = method.descriptor

        val itr = classHierarchy.allSubclasses(classType, reflexive = false)
        var isUnknown = false

        while (itr.hasNext) {
            val subtype = itr.next
            project.classFile(subtype) match {
                case Some(subclassFile) ⇒
                    if (subclassFile.findMethod(methodName, methodDescriptor).isEmpty
                        && classHierarchy.isSubtypeOf(subtype, interfaceType).isYes)
                        return Yes;
                case None ⇒
                    isUnknown = false
            }
        }

        if (isUnknown)
            Unknown
        else
            No
    }
}

object CallBySignatureTargetAnalysis
        extends FPCFAnalysisRunner {

    override def derivedProperties: Set[PropertyKind] = Set(IsExtensible)

    protected[analysis] def start(
        project:       SomeProject,
        propertyStore: PropertyStore
    ): FPCFAnalysis = {
        val analysis = new CallBySignatureTargetAnalysis(project)
        val projectIndex = project.get(ProjectIndexKey)
        val cbsTargetComputationFunction =
            (analysis.determineCallBySignatureTargets(projectIndex) _).asInstanceOf[Entity ⇒ Property]
        propertyStore <<! (CallBySignatureKey, cbsTargetComputationFunction)
        analysis
    }
}