/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.analyses.cg

import org.opalj.br.analyses.ProjectIndexKey
import org.opalj.br.analyses.ProjectInformationKey
import org.opalj.br.analyses.ProjectInformationKeys
import org.opalj.br.analyses.SomeProject
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor

import scala.collection.mutable.OpenHashMap

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
        val cbsTargets = new OpenHashMap[Method, List[Method]]
        val index = p.get(ProjectIndexKey)
        val isOverridableMethod = p.get(IsOverridableMethodKey)
//        val toDeclaredMethod = p.get(DeclaredMethodsKey)

        val cache = new OpenHashMap[MethodDescriptor, OpenHashMap[String, List[Method]]].empty

        for {
            classFile ← p.allClassFiles if classFile.isInterfaceDeclaration
            method ← classFile.methods
            if !method.isPrivate &&
                !method.isStatic &&
                (classFile.isPublic || isOverridableMethod(method).isYesOrUnknown)
        } {
            val descriptor = method.descriptor
            val methodName = method.name

            val results = cache.get(descriptor).map(_.get(methodName)).getOrElse(None)

            val targets = if(results.nonEmpty) {
              results.get
            } else {
                val ts = index.findMethods(methodName, descriptor).filter { m ⇒
                    m.classFile.isClassDeclaration && isOverridableMethod(m).isYesOrUnknown
                }//.map(toDeclaredMethod(_))

                if(cache.contains(descriptor)){
                  cache.get(descriptor).get.put(methodName, ts)
                } else {
                    val newMap = new OpenHashMap[String, List[Method]]
                    newMap.put(methodName, ts)
                  cache.put(descriptor, newMap)
                }

              ts
            }

            //val dm = toDeclaredMethod(method)
            cbsTargets.put(method, targets)
        }

        new CallBySignatureTargets(cbsTargets)
    }
}

class CallBySignatureTargets private[analyses] (
        val data: scala.collection.Map[Method, List[Method]]
) {
    /**
     * Returns all call-by-signature targets of the given method. If the method is not known,
     * `null` is returned. If the method is known a non-null (but potentially empty)
     * [[org.opalj.collection.immutable.ConstArray]] is returned.
     */
    def apply(m: Method): List[Method] = data.getOrElse(m, List.empty)
}