/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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
package observers

import instructions.FieldReadAccess

import java.io.File
import java.net.URL

/**
 * Identifies usages of the Observer Design Pattern.
 *
 * (See the accompanying presentation for further details.)
 *
 * @author Linus Armakola
 * @author Michael Eichberg
 */
object ObserverPatternUsage extends AnalysisExecutor {

    val analysis = new OneStepAnalysis[URL, BasicReport] {

        override def description: String =
            "Loads all classes stored in the jar files and analyses the usage of the observer pattern."

        def doAnalyze(
            project: Project[URL],
            parameters: Seq[String],
            isInterrupted: () ⇒ Boolean): BasicReport = {
            if (project.classHierarchy.rootTypes.tail.nonEmpty) {
                Console.err.println(
                    "Warning: Class Hierarchy Not Complete: "+
                        project.classHierarchy.rootTypes.
                        filter(_ != ObjectType.Object).
                        map(_.toJava).mkString(", "))
            }
            val appClassFiles = project.projectClassFiles.toSeq
            val libClassFiles = project.libraryClassFiles.toSeq
            println("Application:\n\tClasses:"+appClassFiles.size)
            println("\tMethods:"+appClassFiles.foldLeft(0)(_ + _.methods.filter(!_.isSynthetic).size))
            println("\tNon-final Fields:"+appClassFiles.foldLeft(0)(_ + _.fields.filter(!_.isFinal).size))
            println("Library:\n\tClasses:"+libClassFiles.size)
            println("Overall "+project.statistics)

            val appTypes = appClassFiles.map(_.thisType).toSet
            val libTypes = libClassFiles.map(_.thisType).toSet
            val classHierarchy = project.classHierarchy
            import classHierarchy.allSubtypes

            // PART 0 - Identifying Observers 
            // Collect all classes that end with "Observer" or "Listener" or which are 
            // subclasses of them.
            var allObserverInterfaces: Set[ObjectType] = Set.empty
            var appObserverInterfaces: Set[ObjectType] = Set.empty
            var appObserverClasses: Set[ObjectType] = Set.empty
            val allObservers = {
                var observers = Set.empty[ObjectType]
                classHierarchy foreachKnownType { objectType ⇒
                    // this is the Fully Qualified binary Name (fqn) e.g., java/lang/Object
                    val fqn = objectType.fqn
                    if (!observers.contains(objectType) &&
                        (fqn.endsWith("Observer") || fqn.endsWith("Listener"))) {
                        val observerTypes = allSubtypes(objectType, true)
                        observers ++= observerTypes
                        observerTypes foreach { objectType ⇒
                            // If this class is "just" an interface and this type is later
                            // used in the code, it is extremely likely that we have a 
                            // relationship to the pattern; the error margin is very low.
                            // This set is relevant to rule out cases such as identifying 
                            // a class as being observable, because it has a field of type, 
                            // e.g., JButton (which is an observer, but for different 
                            // elements.)
                            if (classHierarchy.isInterface(objectType)) {
                                allObserverInterfaces += objectType
                                if (appTypes.contains(objectType))
                                    appObserverInterfaces += objectType
                            } else {
                                if (appTypes.contains(objectType))
                                    appObserverClasses += objectType
                            }
                        }
                    }
                }
                observers
            }

            val allObserverTypes = allObserverInterfaces ++
                // we also want to include classes such as WindowAdapater which are 
                // pure implementations of an observer interface
                (allObservers filter { observerType ⇒
                    if (project.classFile(observerType).isDefined) { // check that the project is complete
                        val observerClassFile = project.classFile(observerType).get
                        if (!observerClassFile.isInterfaceDeclaration) {
                            val implObsIntfs = observerClassFile.interfaceTypes.filter(allObserverInterfaces.contains(_))

                            implObsIntfs.nonEmpty && (
                                observerClassFile.methods forall { method ⇒
                                    method.isInitialzer || method.isSynthetic ||
                                        // one of the implemented observer interfaces defines the method
                                        implObsIntfs.exists(observerInterface ⇒
                                            project.classFile(observerInterface).isDefined &&
                                                classHierarchy.lookupMethodInInterface(
                                                    project.classFile(observerInterface).get,
                                                    method.name,
                                                    method.descriptor,
                                                    project).isDefined
                                        )
                                }
                            )
                        } else
                            false
                    } else
                        false
                })

            // PART 1 - Identifying Fields That Store Observers
            val observerFields = {
                var observerFields = Set.empty[(ClassFile, Field)]
                for {
                    appType ← appTypes
                    classFile ← project.classFile(appType) //.toSeq
                    field ← classFile.fields
                    if field.fieldType.isReferenceType
                } {
                    field.fieldType match {
                        case ArrayType(ot: ObjectType) if (allObserverTypes.contains(ot)) ⇒
                            observerFields += ((classFile, field))
                        case ot: ObjectType ⇒
                            if (allObserverTypes.contains(ot))
                                observerFields += ((classFile, field))
                            else { // check if it is a container type
                                field.fieldTypeSignature match {
                                    case Some(GenericContainer(c, ot: ObjectType)) if allObserverTypes.contains(ot) ⇒
                                        observerFields += ((classFile, field))
                                    case _ ⇒
                                    /* Ignore */
                                }
                            }
                        case _ ⇒ /* Ignore */
                    }
                }
                observerFields
            }

            // Part 3 - Identifying Observables
            val observables = observerFields.map(_._1)

            // PART 4 - Identifying Methods That Are Related To Managing Observers
            // I.e., methods that have a parameter that is of the type of the observer
            // e.g., addListener(Listener l)... removeListener(Listener l)
            var observerManagementMethods: Set[(ClassFile, Method)] = Set.empty
            // I.e., methods which access a field that stores observers
            var observerNotificationMethods: Set[(ClassFile, Method)] = Set.empty
            for {
                observable ← observables
                observableType = observable.thisType
                omFieldNames = observerFields.filter(_._1 == observable).map(_._2.name)
                method ← observable.methods
            } {
                val hasMethodObserverParameter =
                    method.parameterTypes exists { pt ⇒
                        pt.isObjectType && allObserverInterfaces.contains(pt.asObjectType)
                    }
                if (hasMethodObserverParameter) {
                    observerManagementMethods += ((observable, method))
                } else if (method.body.isDefined &&
                    method.body.get.instructions.exists({
                        case FieldReadAccess(`observableType`, name, _) if omFieldNames.contains(name) ⇒ true
                        case _ ⇒ false
                    })) {
                    observerNotificationMethods += ((observable, method))
                }
            }

            // -------------------------------------------------------------------------------
            // OUTPUT
            // -------------------------------------------------------------------------------
            import Console.{ BOLD, RESET }
            BasicReport(
                (BOLD+"Observer types in project ("+allObserverTypes.size+"): "+RESET + allObserverTypes.map(_.toJava).mkString(", ")) +
                    (BOLD+"Observer interfaces in application ("+appObserverInterfaces.size+"): "+RESET + appObserverInterfaces.map(_.toJava).mkString(", ")) +
                    (BOLD+"Observer classes in application ("+appObserverClasses.size+"): "+RESET + appObserverClasses.map(_.toJava).mkString(", ")) +
                    (BOLD+"Fields to store observers in application ("+observerFields.size+"): "+RESET + observerFields.map(e ⇒ e._1.thisType.toJava+"{ "+e._2.fieldType.toJava+" "+e._2.name+" }").mkString(", ")) +
                    (BOLD+"Methods to manage observers in application ("+observerManagementMethods.size+"): "+RESET + observerManagementMethods.map(e ⇒ e._1.thisType.toJava+"{ "+e._2.toJava+" }").mkString(", ")) +
                    (BOLD+"Methods that are related to observers in application ("+observerNotificationMethods.size+"): "+RESET + observerNotificationMethods.map(e ⇒ e._1.thisType.toJava+"{ "+e._2.toJava+" }").mkString(", "))
            )
        }
    }
}