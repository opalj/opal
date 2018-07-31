/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package ai
package analyses
package cg

import scala.collection.Set
import org.opalj.br.Method
import org.opalj.br.MethodDescriptor
import org.opalj.br.MethodSignature
import org.opalj.br.ObjectType
import org.opalj.br.ClassHierarchy
import org.opalj.br.analyses.SomeProject
import org.opalj.br.analyses.cg.InstantiableClassesKey

trait Callees {

    implicit def project: SomeProject

    final private[this] lazy val instantiableClasses = project.get(InstantiableClassesKey)

    final private[this] def classHierarchy: ClassHierarchy = project.classHierarchy

    def cache: CallGraphCache[MethodSignature, Set[Method]]

    @inline def callees(
        caller:             Method,
        declaringClassType: ObjectType,
        isInterface:        Boolean,
        name:               String,
        descriptor:         MethodDescriptor
    ): Set[Method] = {

        assert(
            classHierarchy.isInterface(declaringClassType) == Answer(isInterface),
            s"callees - inconsistent isInterface information for $declaringClassType"
        )

        def classesFilter(m: Method) = !instantiableClasses.isNotInstantiable(m.classFile.thisType)

        def callTargets() = {
            if (isInterface)
                project.
                    interfaceCall(declaringClassType, name, descriptor).
                    filter(classesFilter)
            else {
                val callerPackage = caller.classFile.thisType.packageName
                project.
                    virtualCall(callerPackage, declaringClassType, name, descriptor).
                    filter(classesFilter)
            }
        }

        if (!isInterface) {
            // Only if the method is public, we can cache the result to avoid that we cache
            // wrong results...
            project.resolveClassMethodReference(declaringClassType, name, descriptor) match {
                case Success(resolvedMethod) ⇒
                    if (!resolvedMethod.isPublic)
                        return callTargets();
                // else ... go on with the analysis and try to cache the results.
                case _ ⇒
                    // no caching...
                    callTargets()
            }
        }

        classHierarchy.hasSubtypes(declaringClassType) match {
            case Yes ⇒
                val methodSignature = new MethodSignature(name, descriptor)
                cache.getOrElseUpdate(declaringClassType, methodSignature)(
                    callTargets(),
                    syncOnEvaluation = true //false
                )

            case /* no caching; not worth the effort... */ No ⇒ callTargets()

            case /*Unknown <=> the type is unknown */ _       ⇒ Set.empty
        }
    }

}
