/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses
package cg

//import java.util.concurrent.ConcurrentHashMap

import scala.collection.mutable

/**
 * This analysis determines whether a method can be (transitively) overridden by a yet unknown
 * type. I.e., it determines whether the set of methods which potentially overrides a given method
 * is downwards (w.r.t. the class hierarchy) closed or not. For those methods, where the set is
 * downwards closed, it is always possible and meaningful to compute abstractions that abstract
 * over all (virtual) methods defined by all subtypes. For convenience purposes, it is possible
 * to also test non-overridable methods (constructors, static initializers, static methods, and
 * private instance methods) for overridability. In that case the answer will always be No.
 *
 * @note This class does not provide any caching, i.e., if this information will be queried over
 *       and over again some caching should be implemented.
 *
 * @author Michael Reif
 * @author Michael Eichberg
 */
private[analyses] class IsOverridableMethodAnalysis(
    project:           SomeProject,
    isClassExtensible: ClassType => Answer,
    isTypeExtensible:  ClassType => Answer
) extends (Method => Answer) {

    // private val cache: ConcurrentHashMap[Method, Answer] = new ConcurrentHashMap()

    private def isAlwaysFinallyOverridden(classType: ClassType, method: Method): Answer = {
        if (isClassExtensible(classType).isYes && !method.isFinal)
            return No;

        import project.classHierarchy
        import project.instanceMethods
        val methodName = method.name
        val methodDescriptor = method.descriptor
        val methodPackageName = method.classFile.thisType.packageName

        val worklist = mutable.Queue.empty[ClassType]

        def addDirectSubclasses(ct: ClassType): Unit = {
            classHierarchy.directSubclassesOf(ct).foreach(worklist.enqueue(_))
        }

        while (worklist.nonEmpty) {
            val ct = worklist.dequeue()
            if (isTypeExtensible(ct).isYesOrUnknown) {
                val cf = project.classFile(ct)
                val subtypeMethod = cf.flatMap(_.findMethod(methodName, methodDescriptor))
                if (subtypeMethod.isEmpty || !subtypeMethod.get.isFinal ||
                    subtypeMethod.get.isPrivate || // private methods don't override
                    (
                        // let's test if this "final override", is for a different method...
                        method.isPackagePrivate &&
                        subtypeMethod.get.declaringClassFile.thisType.packageName !=
                            classType.packageName &&
                            !subtypeMethod.get.isPackagePrivate /**/ && {
                                // ... the original method is package private
                                // ... both methods are defined in different packages
                                // ... the subtypeMethod is protected or public
                                val candidateMethods = instanceMethods(ct).iterator.filter(mdc =>
                                    mdc.name == methodName && mdc.descriptor == methodDescriptor
                                )
                                // if we still have the original method in the list then this method
                                // does not override that method...
                                candidateMethods.exists(mdc => mdc.packageName == methodPackageName)
                            }
                    )
                ) {
                    // the type as a whole is extensible and
                    // the method is not (finally) overridden by this type...
                    isClassExtensible(ct) match {
                        case Yes     => return No;
                        case Unknown => return Unknown;
                        case No      => addDirectSubclasses(ct)
                    }
                }
                // ... if the method is final we do not need to consider further subtypes
            }
            // ... if the type as a whole is not extensible the method cannot be overridden
        }

        Yes
    }

    def apply(method: Method): Answer = {
        if (method.isPrivate || method.isStatic || method.isInitializer || method.isFinal)
            return No;

        val ct = method.declaringClassFile.thisType
        val isExtensibleType = isTypeExtensible(ct)

        if (isExtensibleType.isNoOrUnknown)
            // We "inherit" the (non/unknown)extensibility of the class in this case:
            return isExtensibleType;

        // the type is extensible... Let's check the method:
        isAlwaysFinallyOverridden(ct, method).negate
    }
}
