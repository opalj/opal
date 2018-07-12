/* BSD 2-Clause License - see OPAL/LICENSE for details. */
    package org.opalj
package br
package analyses

    import java.util.concurrent.{ConcurrentHashMap ⇒ CHMap}

    import org.opalj.collection.immutable.ConstArray
import org.opalj.log.OPALLogger
import org.opalj.log.LogContext
import org.opalj.br.Method
import org.opalj.br.instructions.INVOKESPECIAL
import org.opalj.br.instructions.INVOKESTATIC
import org.opalj.br.instructions.INVOKEVIRTUAL
import org.opalj.br.instructions.INVOKEINTERFACE

    /**
 * Computes and stores all potentially called methods/call targets for a specific method call in
 * a specific context and caches the results.
 *
 * ==Thread Safety==
 * This data-structure is thread safe.
 *
 * @author Michael Eichberg
 */
class TypeBasedCallTargets( final val project: SomeProject) {
    
            private[this] implicit final def logContext: LogContext = project.logContext
    
            private[this] final val specialCallTargets = new CHMap[MethodTarget, Set[Method]]
    
            def specialCall(i: INVOKESPECIAL): Set[Method] = {
                specialCall(i.declaringClass, i.name, i.descriptor)
            }
    
            def specialCall(
                declaringClass: ObjectType,
                name:           String,
                descriptor:     MethodDescriptor
                ): Set[Method] = {
                val target = new MethodTarget(declaringClass, name, descriptor)
                // IMPROVE [Scala 2.12] use lambda expression
                    specialCallTargets.computeIfAbsent(
                        target,
                        new java.util.function.Function[MethodTarget, Set[Method]] {
                                def apply(target: MethodTarget): Set[Method] = {
                                        project.specialCall(declaringClass, name, descriptor) match {
                                            case Success(m) ⇒ Set(m)
                                            case _          ⇒ Set.empty
                                        }
                                    }
                            }
                        )
            }
    
            private[this] final val staticCallTargets = new CHMap[MethodTarget, Set[Method]]
    
            def staticCall(i: INVOKESTATIC): Set[Method] = {
                staticCall(i.declaringClass, i.isInterface, i.name, i.descriptor)
            }
    
            def staticCall(
                declaringClass: ObjectType,
                isInterface:    Boolean,
                name:           String,
                descriptor:     MethodDescriptor
                ): Set[Method] = {
                val target = new MethodTarget(declaringClass, name, descriptor)
                // IMPROVE [Scala 2.12] use lambda expression
                    staticCallTargets.computeIfAbsent(
                        target,
                        new java.util.function.Function[MethodTarget, Set[Method]] {
                                def apply(target: MethodTarget): Set[Method] = {
                                        project.staticCall(declaringClass, isInterface, name, descriptor) match {
                                            case Success(m) ⇒ Set(m)
                                            case _          ⇒ Set.empty
                                        }
                                    }
                            }
                        )
            }
    
            // ---------------------------------------------------------------------------------------------
        //
            // VIRTUAL METHODS HANDLING
        //
            // ---------------------------------------------------------------------------------------------
    
            val isExtensible: ObjectType ⇒ Answer = project.get(TypeExtensibilityKey)
    
            final def filterAlwaysOverriddenMethods(methods: Set[Method]): Set[Method] = {
                import project.classHierarchy.processSubtypes
                import ConstArray.find
                // Let's do some post-processing to determine if the current result contains
                    // methods defined by abstract classes/interfaces, which are actually no call
                // targets, because they are always overridden in subclasses and we know that
                    // the interface is not extensible.
                val filteredMethods = methods.filter { m ⇒
                        val declaringClassFile = project.classFile(m)
                        val declaringClass = declaringClassFile.thisType
                        // TODO Improve by using "isDirectlyExtensible" instead!
                            !declaringClassFile.isAbstract || isExtensible(declaringClass).isYesOrUnknown || {
                                // Do not keep the method if it is always overridden by
                                    // the first concrete subtype.
                                val alwaysOverridden = processSubtypes(declaringClass, false)(true) { (t, subtype) ⇒
                                        // ... here, always overridden by a concrete subclass...
                        
                                        // - we don't know the subtype => keep m (==> false)
                                            // - we know the subtype
                                        //      => it is not an abstract type and it does not override m
                                            //         => keep m (==> false)
                                        //      => it is not an abstract type and it overrides m
                                            //         => analyze siblings; (ignore subtypes)
                                        //      => it is an abstract type and it does not override m
                                            //         => analyze subtypes (and siblings afterwards)
                                        //      => it is an interface and it does override m
                                            //         => analyze siblings; (ignore subtypes)
                                        //   => we have analyzed all subtypes ==> true
                                            val result = project.instanceMethods.get(subtype) match {
                                        case Some(instanceMethods) ⇒
                                                    // now let's check if we can find a matching method...
                                                        //find(instanceMethods) { mdc ⇒ mdc.compareWithPublicMethod(m) } match {
                                                    find(instanceMethods) { mdc ⇒
                                                            mdc.compareAccessibilityAware(declaringClass, m)
                                                        } match {
                                                        case Some(mdc) ⇒
                                                            if (mdc.declaringClass == declaringClass) {
                                                                // the method is not overridden
                                                                project.classFile(subtype) match {
                                                                    case Some(cf) if cf.isAbstract ⇒
                                                                        ( /*still*/ true, false, /*abort=*/ false)
                                                                    case _ /* not abstract or unknown */ ⇒
                                                                        (false, false /* irrelevant */ , /*abort=*/ true)
                                                                }
                                                            } else {
                                                                // the method is overridden by (some!) subclass
                                                                (true, /*skip subtypes=*/ true, /*abort=*/ false)
                                                            }
                                                        case None ⇒
                                                                OPALLogger.error(
                                                                        "internal [safe, but imprecise fallback chosen]",
                                                                        s"didn't find ${m.toJava(declaringClass)} in: "+
                                                                                instanceMethods.mkString("{ ", ", ", " }")
                                                                        )
                                                                (false, false /*actually irrelevant*/ , true)
                                                    }
                                        case None ⇒
                                                    // <=> we are lacking crucial information
                                                        (false, false /*actually irrelevant*/ , true)
                                    }
                                        result
                                    }
                                !alwaysOverridden
                            }
                    }
                if (methods.size == filteredMethods.size) methods else filteredMethods
            }
    
            private[this] final val interfaceCallTargets = new CHMap[MethodTarget, Set[Method]]
    
            def interfaceCall(i: INVOKEINTERFACE): Set[Method] = {
                interfaceCall(i.declaringClass, i.name, i.descriptor)
            }
    
            def interfaceCall(
                declaringClass: ObjectType,
                name:           String,
                descriptor:     MethodDescriptor
                ): Set[Method] = {
                val target = new MethodTarget(declaringClass, name, descriptor)
                // IMPROVE [Scala 2.12] use lambda expression
                    interfaceCallTargets.computeIfAbsent(
                        target,
                        new java.util.function.Function[MethodTarget, Set[Method]] {
                                def apply(target: MethodTarget): Set[Method] = {
                                        val methods = project.interfaceCall(declaringClass, name, descriptor)
                                        filterAlwaysOverriddenMethods(methods)
                                    }
                            }
                        )
            }
    
            /**
         * Determines all call targets.
     *
     * @see `virtualCall(String, INVOKEVIRTUAL)` for further details.
     */
        final def virtualCall(callingContext: ClassFile, i: INVOKEVIRTUAL): Set[Method] = {
                virtualCall(callingContext.thisType.packageName, i)
            }
    
            /**
         * Determines all call targets.
     *
     * @see `virtualCall(String, INVOKEVIRTUAL)` for further details.
     */
        final def virtualCall(callingContext: ObjectType, i: INVOKEVIRTUAL): Set[Method] = {
                virtualCall(callingContext.packageName, i)
            }
    
            private[this] final val samePackageVirtualCallTargets = new CHMap[MethodTarget, Set[Method]]
        private[this] final val otherVirtualCallTargets = new CHMap[MethodTarget, Set[Method]]
        private[this] final val arrayMethodsCallTargets = new CHMap[(String, MethodDescriptor), Set[Method]]
    
            final def virtualCall(callerPackageName: String, i: INVOKEVIRTUAL): Set[Method] = {
                virtualCall(callerPackageName, i.declaringClass, i.name, i.descriptor)
            }
    
            def virtualCall(
                callerPackageName: String,
                declaringType:     ReferenceType,
                name:              String,
                descriptor:        MethodDescriptor
                ): Set[Method] = {
                // the calling context is only relevant for protected and package visible methods
                    // I.e.,
                // package a {
                    //      class X { /*package visible*/ void foo(){} }
                //      ... { X x = new Y(); x.foo();  }
                    // }
                // package b {
                    //      class Y extends X { protected void foo(){} }
                // }
            
                import project.resolveClassMethodReference
                if (declaringType.isArrayType) {
                        val target = (name, descriptor)
                        return arrayMethodsCallTargets.computeIfAbsent(
                                target,
                                new java.util.function.Function[(String, MethodDescriptor), Set[Method]] {
                                        def apply(target: (String, MethodDescriptor)): Set[Method] = {
                                                resolveClassMethodReference(ObjectType.Object, name, descriptor) match {
                                                    case Success(m) ⇒ Set(m)
                                                    case _          ⇒ Set.empty
                                                }
                                            }
                                    }
                                )
                    }
        
                    val declaringClass = declaringType.asObjectType
                val target = new MethodTarget(declaringClass, name, descriptor)
                if (callerPackageName == declaringClass.packageName) {
                        samePackageVirtualCallTargets.computeIfAbsent(
                                target,
                                new java.util.function.Function[MethodTarget, Set[Method]] {
                                        def apply(target: MethodTarget): Set[Method] = {
                                                val methodCandidates = project.virtualCall(
                                                        callerPackageName,
                                                        declaringClass, name, descriptor
                                                        )
                                                val methods = filterAlwaysOverriddenMethods(methodCandidates)
                                                val cachedMethods = otherVirtualCallTargets.get(target)
                                                // ATTENTION: A size based comparison is not possible due to
                                                    // package level methods.
                                                if (cachedMethods == methods)
                                                        cachedMethods // reuse existing set!
                                                else
                                                    methods
                                            }
                                    }
                                )
                    } else {
                        otherVirtualCallTargets.computeIfAbsent(
                                target,
                                new java.util.function.Function[MethodTarget, Set[Method]] {
                                        def apply(target: MethodTarget): Set[Method] = {
                                                val methodCandidates = project.virtualCall(
                                                        callerPackageName,
                                                        declaringClass, name, descriptor
                                                        )
                                                val methods = filterAlwaysOverriddenMethods(methodCandidates)
                                                val cachedMethods = samePackageVirtualCallTargets.get(target)
                                                // ATTENTION: A size based comparison is not possible due to
                                                    // package level methods.
                                                if (cachedMethods == methods)
                                                        cachedMethods // reuse existing set!
                                                else
                                                    methods
                                            }
                                    }
                                )
                    }
            }
    
        }

/* BSD 2-Clause License - see OPAL/LICENSE for details. */
    package org.opalj
package br
package analyses

    /**
 * The ''key'' object to get the object which provides call target information based on the
 * static call target information available.
 *
 * @author Michael Eichberg
 */
object TypeBasedCallTargetsKey extends ProjectInformationKey[TypeBasedCallTargets] {
    
            /**
         * The [[TypeBasedCallTargetsAnalysis]] has no special prerequisites.
     *
     * @return `Nil`.
     */
        override protected def requirements: Seq[ProjectInformationKey[Nothing]] = Nil
    
            override protected def compute(project: SomeProject): TypeBasedCallTargets = {
                new TypeBasedCallTargets(project)
            }
    }
