/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.analyses.cg

import org.opalj.br.analyses.ProjectIndexKey
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.Method
import org.opalj.br.ObjectType

import scala.collection.immutable.ArraySeq
import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
 * The ''key'' object to get the interface methods for which call-by-signature resolution
 * needs to be done.
 *
 * @note To get call-by-signature information use the [[org.opalj.br.analyses.Project]]'s `get`
 * method and pass in `this` object.
 *
 * @see [[CallBySignatureResolution]] for further information.
 *
 * @author Michael Reif
 */
object CallBySignatureKey extends ProjectInformationKey[CallBySignatureTargets, Nothing] {

    override def requirements(project: SomeProject): ProjectInformationKeys = List(
        ProjectIndexKey,
        ClosedPackagesKey,
        ClassExtensibilityKey,
        TypeExtensibilityKey,
        IsOverridableMethodKey
    )

    override def compute(p: SomeProject): CallBySignatureTargets = {
        val cbsTargets = mutable.HashMap.empty[Method, ArraySeq[ObjectType]]
        val index = p.get(ProjectIndexKey)
        val isOverridableMethod = p.get(IsOverridableMethodKey)

        for {
            classFile <- p.allClassFiles if classFile.isInterfaceDeclaration
            method <- classFile.methods
            if !method.isPrivate &&
                !method.isStatic &&
                (classFile.isPublic || isOverridableMethod(method).isYesOrUnknown)
        } {
            val descriptor = method.descriptor
            val methodName = method.name
            val declType = classFile.thisType

            import p.classHierarchy
            val potentialMethods = index.findMethods(methodName, descriptor)

            var i = 0
            val targets = ListBuffer.empty[ObjectType]
            while (i < potentialMethods.size) {
                val m = potentialMethods(i)
                val cf = m.classFile
                val targetType = cf.thisType
                val qualified = cf.isClassDeclaration &&
                    isOverridableMethod(m).isYesOrUnknown &&
                    classHierarchy.isASubtypeOf(targetType, declType).isNoOrUnknown

                if (qualified) {
                    targets += m.declaringClassFile.thisType
                }
                i = i + 1
            }

            cbsTargets.put(method, ArraySeq.empty ++ targets)
        }

        new CallBySignatureTargets(cbsTargets)
    }
}

class CallBySignatureTargets private[analyses] (
        val data: scala.collection.Map[Method, ArraySeq[ObjectType]]
) {
    /**
     * Returns all call-by-signature targets of the given method. If the method is not known,
     * `null` is returned. If the method is known a non-null (but potentially empty)
     * [[scala.collection.immutable.ArraySeq]] is returned.
     */
    def apply(m: Method): ArraySeq[ObjectType] = data.getOrElse(m, ArraySeq.empty)
}