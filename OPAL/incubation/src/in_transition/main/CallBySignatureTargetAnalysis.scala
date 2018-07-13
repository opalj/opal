/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package fpcf
package analyses

import scala.collection.mutable
import org.opalj.fpcf.properties.NoCBSTargets
import org.opalj.fpcf.properties.CBSTargets
import org.opalj.fpcf.properties.CallBySignature
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.ProjectIndex
import org.opalj.br.Method
import org.opalj.br.ObjectType
import org.opalj.br.MethodDescriptor
import org.opalj.br.analyses.ProjectIndexKey
import org.opalj.fpcf.properties.NotInheritableByNewTypes
import org.opalj.fpcf.properties.InheritableByNewTypes

/**
 * This analysis computes the CallBySignature property.
 * I.e., it determines the call-by-signature targets of an interface method.
 *
 * It is in particular relevant when analyzing software libraries or extensible application
 * frameworks. In case of "simple" (i.e., non-extensible) applications, call-by-siganture
 * is irrelevant.
 *
 * @note    This analysis implements a direct property computation that is only executed when
 *          required.
 * @author  Michael Reif
 */
class CallBySignatureTargetAnalysis private (val project: SomeProject) extends FPCFAnalysis {

    private def propertyComputation(

        projectIndex:      ProjectIndex,
        isApplicationMode: Boolean
    )(
        e: Entity
    ): PropertyComputationResult = {
        Result(e, determineCallBySignatureTargets(projectIndex, isApplicationMode)(e))
    }

    def determineCallBySignatureTargets(
        projectIndex:      ProjectIndex,
        isApplicationMode: Boolean
    )(
        e: Entity
    ): Property = {
        if (isApplicationMode)
            return NoCBSTargets;

        val method: Method = e.asInstanceOf[Method]
        val methodName = method.name
        val methodDescriptor = method.descriptor

        import projectIndex.findMethods

        val interfaceClassFile = method.classFile
        val interfaceType = interfaceClassFile.thisType

        val cbsTargets = mutable.Set.empty[Method]

        def analyzePotentialCBSTarget(cbsCallee: Method): Unit = {
            if (!cbsCallee.isPublic)
                return ;

            if (cbsCallee.isAbstract)
                return ;

            if (!isInheritableMethod(cbsCallee))
                return ;

            val cbsCalleeDeclaringClass = cbsCallee.classFile

            if (!cbsCalleeDeclaringClass.isClassDeclaration)
                return ;

            if (cbsCalleeDeclaringClass.isEffectivelyFinal)
                return ;

            val cbsCalleeDeclaringType = cbsCalleeDeclaringClass.thisType

            if (cbsCalleeDeclaringType eq ObjectType.Object)
                return ;

            if (classHierarchy.isSubtypeOf(cbsCalleeDeclaringType, interfaceType).isYes /* we want to get a sound overapprox. not: OrUnknown*/ )
                return ;

            if (hasSubclassWhichInheritsFromInterface(cbsCalleeDeclaringType, interfaceType, methodName, methodDescriptor).isYes)
                return ;

            if (isClosedLibrary &&
                cbsCalleeDeclaringClass.isPackageVisible &&
                (propertyStore(cbsCallee, InheritableByNewTypes.Key).p eq NotInheritableByNewTypes))
                return ;

            cbsTargets += cbsCallee
        }
        findMethods(methodName, methodDescriptor) foreach analyzePotentialCBSTarget

        if (cbsTargets.isEmpty) NoCBSTargets else CBSTargets(cbsTargets)
    }

    /**
     * A method is considered inheritable if:
     * $ - it is either public or projected
     * $ - OPA is applied and the method is package private
     *
     * @note This does not consider the visibility of the method's declaring class.
     *   Hence, it should be checked separately if the class can be subclassed.
     */
    @inline private[this] def isInheritableMethod(method: Method): Boolean = {

        if (method.isPackagePrivate)
            // package visible methods are only inheritable under OPA
            // if the AnalysisMode isn't OPA, it can only be CPA
            //  => call by signature does not matter in the an APP context
            isOpenLibrary
        else
            !method.isPrivate
    }

    private[this] def hasSubclassWhichInheritsFromInterface(
        classType:        ObjectType,
        interfaceType:    ObjectType,
        methodName:       String,
        methodDescriptor: MethodDescriptor
    ): Answer = {

        val itr = classHierarchy.allSubclassTypes(classType, reflexive = false)
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

object CallBySignatureTargetAnalysis extends FPCFEagerAnalysisScheduler {

    override def derivedProperties: Set[PropertyKind] = Set(CallBySignature.Key)

    override def usedProperties: Set[PropertyKind] = Set(InheritableByNewTypes.Key)

    protected[fpcf] def start(
        project:       SomeProject,
        propertyStore: PropertyStore
    ): FPCFAnalysis = {
        val analysis = new CallBySignatureTargetAnalysis(project)
        val projectIndex = project.get(ProjectIndexKey)
        val isApplicationMode = AnalysisModes.isApplicationLike(project.analysisMode)
        propertyStore.scheduleLazyPropertyComputation(
            CallBySignature.Key,
            analysis.propertyComputation(projectIndex, isApplicationMode)
        )
        analysis
    }
}
