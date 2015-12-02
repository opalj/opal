/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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

import scala.collection.Map
import java.util.concurrent.{ConcurrentHashMap ⇒ JCHashMap}
import scala.collection.immutable.Set
import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer

/**
 * An index that enables the efficient lookup of potential
 * call by signature resolution interface methods
 * given the method's name and the descriptor type.
 *
 * @note To get call by signature resolution information call [[Project.get]] and pass in
 * 		the [[CallBySignatureResolutionKey]] object.
 *
 * @author Michael Reif
 */
class CallBySignatureResolution private (
        val project: SomeProject,
        val methods: Map[(ObjectType /*InterfaceType*/ , String, MethodDescriptor), Set[Method]]
) {

    /**
     * Given the `name` and `descriptor` of a method declared by an interface and the `declaringClass`
     * where the method is declared, all those  methods are returned that have a matching name and
     * descriptor and are declared in the same package. All those methods are implemented
     * by classes (not interfaces) that '''do not inherit''' from the respective interface and
     * which may have a subclass (in the future) that may implement the interface.
     *
     * Hence, when we compute the call graph for a library the returned methods may (in general)
     * be call targets.
     *
     * @note This method assumes the closed packages assumption
     */
    def findMethods(
        name:       String,
        descriptor: MethodDescriptor,
        declClass:  ObjectType
    ): Set[Method] = {

        assert(
            project.classFile(declClass).map(_.isInterfaceDeclaration).getOrElse(true),
            s"the declaring class ${declClass.toJava} does not define an interface type"
        )

        val tripleKey = (declClass, name, descriptor)
        methods.get(tripleKey).getOrElse(Set.empty[Method])
    }

    def statistics(): Map[String, Any] = {
        Map(
            "number of different method interfaceType/name/descriptor pairs" →
                methods.size,
            "number of class methods with method signatures matching non-implemented interface methods" →
                methods.view.foldLeft(Set.empty[Method]) {
                    (theMethods, targets: ((ObjectType, String, MethodDescriptor), Iterable[Method])) ⇒
                        theMethods.++(targets._2)
                }.size
        )
    }

    def methodReferenceStatistics(): Iterable[String] = {
        for {
            ((interfaceType, name, descriptor), theMethods) ← methods
        } yield {
            val methodInfo = theMethods.map(project.classFile(_).thisType.toJava).mkString("classes={", ",", "}")
            s"${descriptor.toJava(name)} => $methodInfo"
        }
    }
}

/**
 * Factory to create [[CallBySignatureResolution]] information.
 *
 * @author Michael Reif
 */
object CallBySignatureResolution {

    /*
   * A method is considered inheritable by if:
   * 	- it is either public or projected
   *  - OPA is applied and the method is package private
   *  
   * @note This does not consider the visibility of the method's class,
   *       hence, it should be checked separately if the class can be subclassed.
   */
    private[this] def isInheritableMethod(
        method: Method
    )(
        implicit
        analysisMode: AnalysisMode
    ): Boolean = {
        if (method.isPrivate)
            return false;

        if (method.isPackagePrivate)
            // package visible methods are only inheritable under OPA
            // if the AnalysisMode isn't OPA, it can only be CPA
            //  => call by signature does not matter in the an APP context
            return (analysisMode eq AnalysisModes.OPA)

        // method is public or protected
        true
    }

    private[this] def hasSubclassProxyForMethod(
        classType:          ObjectType,
        method:             Method,
        interfaceClassFile: ClassFile,
        project:            SomeProject,
        cache:              JCHashMap[Method, Answer]
    )(
        implicit
        analysisMode: AnalysisMode
    ): Answer = {

        cache.get(method) match {
            case null   ⇒
            case answer ⇒ return answer;
        }

        val classHierarchy = project.classHierarchy
        val methodName = method.name
        val methodDescriptor = method.descriptor

        val subtypes = ListBuffer.empty ++= classHierarchy.directSubtypesOf(classType)
        while (subtypes.nonEmpty) {
            val subtype = subtypes.head
            project.classFile(subtype) match {
                case Some(subclass) if subclass.isClassDeclaration || subclass.isEnumDeclaration ⇒
                    if (subclass.findMethod(methodName, methodDescriptor).isEmpty)
                        if (subclass.isPublic) {
                            // the original method is now visible (and not shadowed)
                            cache.put(method, Yes)
                            return Yes;
                        } else
                            subtypes ++= classHierarchy.directSubtypesOf(subtype)
                // we need to continue our search for a class that makes the method visible
                case None ⇒
                    // The type hierarchy is obviously not downwards closed; i.e.,
                    // the project configuration is rather strange! 
                    cache.put(method, Unknown)
                    return Unknown;
                case _ ⇒
            }
            subtypes -= subtype
        }

        // non of the subtypes acts as proxy
        cache.put(method, No)
        return No;
    }

    private[this] def hasSubclassInheritingTheInterface(
        classType:     ObjectType,
        method:        Method,
        interfaceType: ObjectType,
        project:       SomeProject,
        cache:         JCHashMap[(Method, ObjectType), Answer]
    )(
        implicit
        analysisMode: AnalysisMode
    ): Answer = {
        val key = (method, interfaceType)
        cache.get(key) match {
            case null   ⇒
            case answer ⇒ return answer;
        }

        val classHierarchy = project.classHierarchy
        val methodName = method.name
        val methodDescriptor = method.descriptor

        val subtypes = ListBuffer.empty ++= classHierarchy.directSubtypesOf(classType)
        while (subtypes.nonEmpty) {
            val subtype = subtypes.head
            project.classFile(subtype) match {
                case Some(subclass) if subclass.isClassDeclaration || subclass.isEnumDeclaration ⇒
                    if (subclass.findMethod(methodName, methodDescriptor).isEmpty)
                        if (classHierarchy.isSubtypeOf(subtype, interfaceType).isYes) {
                            cache.put(key, Yes)
                            // the original method is now visible and the interface is implemented
                            return Yes;
                        } else
                            // we need to continue our search for a class that makes the method visible
                            subtypes ++= classHierarchy.directSubtypesOf(subtype)

                case None ⇒
                    cache.put(key, Unknown)
                    return Unknown;
                case _ ⇒ /* do nothing */
            }
            subtypes -= subtype
        }

        // non of the subtypes acts as proxy
        cache.put(key, No)
        No
    }

    def apply(project: SomeProject, isInterrupted: () ⇒ Boolean): CallBySignatureResolution = {
        implicit val analysisMode = project.analysisMode
        if (AnalysisModes.isApplicationLike(analysisMode))
            throw new IllegalArgumentException(
                "call-by-signature resolution for application (like) is not supported"
            )

        val projectIndex = project.get(ProjectIndexKey)
        val classHierarchy = project.classHierarchy

        val proxyCache = new JCHashMap[Method, Answer]((project.methodsCount * 0.1).toInt)
        val subclassCache = new JCHashMap[(Method, ObjectType), Answer]((project.methodsCount * 0.2).toInt)

        val methods = new JCHashMap[(ObjectType, String, MethodDescriptor), Set[Method]] /*methods defined in classes with matching signatures*/ (project.projectMethodsCount / 4)
        val visitedInterfacesMap = new JCHashMap[Method, ListBuffer[ObjectType]]()

        def callBySignatureEvaluation(
            interfaceClassFile: ClassFile,
            method:             Method,
            targetMethods:      Iterable[Method] = Set.empty
        ): Unit = {

            val methodName = method.name
            val methodDescriptor = method.descriptor
            val interfaceType = interfaceClassFile.thisType
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

                if ((clazzType eq ObjectType.Object))
                    return ;

                if (classHierarchy.isSubtypeOf(clazzType, interfaceType).isYes /* we want to get a sound overapprox. not: OrUnknown*/ )
                    return ;

                if (hasSubclassInheritingTheInterface(clazzType, m, interfaceType, project, subclassCache).isYes)
                    return ;

                if (!clazzClassFile.isPublic &&
                    (analysisMode eq AnalysisModes.CPA) &&
                    !hasSubclassProxyForMethod(clazzType, m, interfaceClassFile, project, proxyCache).isYesOrUnknown)
                    return ;

                val methodTripleKey = (interfaceType, methodName, methodDescriptor)
                methods.get(methodTripleKey) match {
                    case null ⇒
                        methods.put(methodTripleKey, Set(m))
                    case theMethods ⇒
                        methods.put(methodTripleKey, theMethods + m)
                }
            }

            val possibleCbsTargets =
                if (targetMethods.isEmpty)
                    projectIndex.findMethods(methodName, methodDescriptor).view.filter { _.isPublic }
                else
                    targetMethods

            possibleCbsTargets foreach analyzeMethod
            classHierarchy.foreachSubtype(interfaceType) { subtype ⇒
                project.classFile(subtype) match {
                    case (Some(cf)) ⇒ {
                        if (cf.isInterfaceDeclaration) {
                            visitedInterfacesMap.get(method) match {
                                case null ⇒ {
                                    visitedInterfacesMap.put(method, ListBuffer(subtype))
                                    callBySignatureEvaluation(cf, method, possibleCbsTargets)
                                }
                                case typeList ⇒ {
                                    if (!typeList.contains(subtype)) {
                                        visitedInterfacesMap.put(method, typeList.+=(subtype))
                                        callBySignatureEvaluation(cf, method, possibleCbsTargets)
                                    }
                                }
                            }
                        }
                    }
                    case None ⇒
                }
            }
        }

        // Execution starts here: 

        project.parForeachClassFile(isInterrupted) { cf ⇒
            if (cf.isInterfaceDeclaration &&
                classHierarchy.allSuperinterfacetypes(cf.thisType, reflexive = false).isEmpty) {
                cf.methods.foreach { method ⇒
                    if (!method.isStatic)
                        callBySignatureEvaluation(cf, method)
                }

            }
        }

        new CallBySignatureResolution(project, methods.asScala)

    }
}