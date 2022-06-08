/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses
package observers

import java.net.URL
import org.opalj.br.instructions.FieldReadAccess

/**
 * Identifies usages of the Observer Design Pattern.
 *
 * (See the accompanying presentation for further details.)
 *
 * @author Linus Armakola
 * @author Michael Eichberg
 */
object ObserverPatternUsage extends ProjectAnalysisApplication {

    override def description: String =
        "Loads all classes stored in the jar files and analyses the usage of the observer pattern."

    def doAnalyze(
        project:       Project[URL],
        parameters:    Seq[String],
        isInterrupted: () => Boolean
    ): BasicReport = {
        val appClassFiles = project.allProjectClassFiles
        val libClassFiles = project.allLibraryClassFiles
        println("Application:\n\tClasses:"+appClassFiles.size)
        println("\tMethods:"+appClassFiles.foldLeft(0)(_ + _.methods.filter(!_.isSynthetic).size))
        println("\tNon-final Fields:"+appClassFiles.foldLeft(0)(_ + _.fields.filter(!_.isFinal).size))
        println("Library:\n\tClasses:"+libClassFiles.size)
        println("Overall "+project.statistics)

        val appTypes = appClassFiles.map(_.thisType).toSet
        //val libTypes = libClassFiles.map(_.thisType).toSet
        val classHierarchy = project.classHierarchy
        import classHierarchy.allSubtypes
        import classHierarchy.isInterface

        // PART 0 - Identifying Observers
        // Collect all classes that end with "Observer" or "Listener" or which are
        // subclasses of them.
        var allObserverInterfaces: Set[ObjectType] = Set.empty
        var appObserverInterfaces: Set[ObjectType] = Set.empty
        var appObserverClasses: Set[ObjectType] = Set.empty
        val allObservers = {
            var observers = Set.empty[ObjectType]
            classHierarchy foreachKnownType { objectType =>
                // this is the Fully Qualified binary Name (fqn) e.g., java/lang/Object
                val fqn = objectType.fqn
                if (!observers.contains(objectType) &&
                    (fqn.endsWith("Observer") || fqn.endsWith("Listener"))) {
                    val observerTypes = allSubtypes(objectType, true)
                    observers ++= observerTypes
                    observerTypes foreach { objectType =>
                        // If this class is "just" an interface and this type is later
                        // used in the code, it is extremely likely that we have a
                        // relationship to the pattern; the error margin is very low.
                        // This set is relevant to rule out cases such as identifying
                        // a class as being observable, because it has a field of type,
                        // e.g., JButton (which is an observer, but for different
                        // elements.)
                        if (isInterface(objectType).isYes) {
                            allObserverInterfaces += objectType
                            if (appTypes.contains(objectType)) appObserverInterfaces += objectType
                        } else {
                            if (appTypes.contains(objectType)) appObserverClasses += objectType
                        }
                    }
                }
            }
            observers
        }

        val allObserverTypes = allObserverInterfaces ++
            // we also want to include classes such as WindowAdapater which are
            // pure implementations of an observer interface
            (allObservers filter { observerType =>
                if (project.classFile(observerType).isDefined) { // check that the project is complete
                    val observerClassFile = project.classFile(observerType).get
                    if (!observerClassFile.isInterfaceDeclaration) {
                        val implObsIntfs = observerClassFile.interfaceTypes.filter(allObserverInterfaces.contains(_))

                        implObsIntfs.nonEmpty && (
                            observerClassFile.methods forall { method =>
                                method.isInitializer || method.isSynthetic ||
                                    // one of the implemented observer interfaces defines the method
                                    implObsIntfs.exists(observerInterface =>
                                        project.classFile(observerInterface).isDefined &&
                                            project.resolveInterfaceMethodReference(
                                                observerInterface,
                                                method.name,
                                                method.descriptor
                                            ).isDefined)
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
                appType <- appTypes
                classFile <- project.classFile(appType) //.toSeq
                field <- classFile.fields
                if field.fieldType.isReferenceType
            } {
                field.fieldType match {
                    case ArrayType(ot: ObjectType) if (allObserverTypes.contains(ot)) =>
                        observerFields += ((classFile, field))
                    case ot: ObjectType =>
                        if (allObserverTypes.contains(ot))
                            observerFields += ((classFile, field))
                        else { // check if it is a container type
                            field.fieldTypeSignature match {
                                case Some(SimpleGenericType(_, ot: ObjectType)) if allObserverTypes.contains(ot) =>
                                    observerFields += ((classFile, field))
                                case _ =>
                                /* Ignore */
                            }
                        }
                    case _ => /* Ignore */
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
            observable <- observables
            observableType = observable.thisType
            omFieldNames = observerFields.filter(_._1 == observable).map(_._2.name)
            method <- observable.methods
        } {
            val hasMethodObserverParameter =
                method.parameterTypes exists { pt =>
                    pt.isObjectType && allObserverInterfaces.contains(pt.asObjectType)
                }
            if (hasMethodObserverParameter) {
                observerManagementMethods += ((observable, method))
            } else if (method.body.isDefined &&
                method.body.get.instructions.exists({
                    case FieldReadAccess(`observableType`, name, _) if omFieldNames.contains(name) => true
                    case _ => false
                })) {
                observerNotificationMethods += ((observable, method))
            }
        }

        // -------------------------------------------------------------------------------
        // OUTPUT
        // -------------------------------------------------------------------------------
        import Console.{BOLD, RESET}
        BasicReport(
            (BOLD+"Observer types in project ("+allObserverTypes.size+"): "+RESET + allObserverTypes.map(_.toJava).mkString(", ")) +
                (BOLD+"Observer interfaces in application ("+appObserverInterfaces.size+"): "+RESET + appObserverInterfaces.map(_.toJava).mkString(", ")) +
                (BOLD+"Observer classes in application ("+appObserverClasses.size+"): "+RESET + appObserverClasses.map(_.toJava).mkString(", ")) +
                (BOLD+"Fields to store observers in application ("+observerFields.size+"): "+RESET + observerFields.map(e => e._1.thisType.toJava+"{ "+e._2.fieldType.toJava+" "+e._2.name+" }").mkString(", ")) +
                (BOLD+"Methods to manage observers in application ("+observerManagementMethods.size+"): "+RESET + observerManagementMethods.map(e => e._1.thisType.toJava+"{ "+e._2.signatureToJava(false)+" }").mkString(", ")) +
                (BOLD+"Methods that are related to observers in application ("+observerNotificationMethods.size+"): "+RESET + observerNotificationMethods.map(e => e._1.thisType.toJava+"{ "+e._2.signatureToJava(false)+" }").mkString(", "))
        )
    }
}
