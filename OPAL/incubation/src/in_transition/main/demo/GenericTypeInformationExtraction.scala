/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package analyses

import java.net.URL
import org.opalj.ai.analyses.{MethodReturnValuesAnalysis ⇒ TheAnalysis}

/**
 * Demonstrates how to extract generic type information.
 *
 * @author Michael Eichberg
 */
object GenericTypeInformationExtraction extends ProjectAnalysisApplication {

    override def title: String = "demonstrates how to extract generic type information associated with a class"

    override def description: String = TheAnalysis.description

    override def doAnalyze(
        theProject:    Project[URL],
        parameters:    Seq[String],
        isInterrupted: () ⇒ Boolean
    ): BasicReport = {
        // THE NAME OF THE CONTAINER (HERE) HAS TO BE AN INTERFACE NAME!
        //        val containerPackageName = "java/lang/"
        //        val containerSimpleName = "Iterable"
        //        val containerSimpleName = "Comparable"

        //        val containerPackageName = "java/util/"
        //        val containerSimpleName = "Iterator"
        //    val containerSimpleName = "Collection"
        //    val containerSimpleName = "Comparator"
        //val containerSimpleName = "Enumeration"

        val containerPackageName = "java/util/concurrent/"
        val containerSimpleName = "Future"

        val containerName = containerPackageName + containerSimpleName
        val containerType = ObjectType(containerName)
        if (!theProject.classHierarchy.isKnown(containerType))
            return BasicReport(s"the type $containerType is unknown");

        val subtypes = theProject.classHierarchy.allSubtypes(containerType, false)

        val typeBindingSubtypes = for {
            subtype ← subtypes
            subtypeClassFile ← theProject.classFile(subtype).toSeq
            //ClassSignature(_, _, List(ClassTypeSignature(_, SimpleClassTypeSignature(_, _, t),_))) ← iteratorClassFile.classSignature

            ClassSignature(
                _, //None,
                _, //ClassTypeSignature(Some("java/lang/"), SimpleClassTypeSignature("Object", None), List()),
                // we match the (indirect) subclasses of the interface later on...
                superInterfacesSignature
                ) ← subtypeClassFile.classSignature

            componentType ← superInterfacesSignature.collectFirst {
                // 1. the hard way....
                //                case ClassTypeSignature(
                //                    Some(`containerPackageName`),
                //                    SimpleClassTypeSignature(
                //                        `containerSimpleName`,
                //                        List(
                //                            ProperTypeArgument(
                //                                None,
                //                                ClassTypeSignature(
                //                                    Some(packageName),
                //                                    SimpleClassTypeSignature(
                //                                        /*"Integer"*/ simpleName,
                //                                        _ /* None*/ ),
                //                                    Nil //List()
                //                                    )
                //                                )
                //                            )
                //                        ),
                //                    _ //List()
                //                    ) ⇒
                //                    ObjectType(packageName + simpleName)
                //
                // 2. using a custom (specialized) matcher
                case SimpleGenericType(`containerType`, componentType) ⇒ componentType
            }
        } yield {
            (subtype, componentType)
        }

        val allAffectedSubtypes =
            typeBindingSubtypes.foldLeft(Set.empty[(ObjectType, ObjectType)]) { (s, t) ⇒
                val (subtype, componentType) = t
                s ++ theProject.classHierarchy.allSubtypes(subtype, true).map((_, componentType))
            }

        val allAffectedSubtypesAsStrings =
            allAffectedSubtypes.map(p ⇒
                p._1.toJava+" inherits from "+containerType.toJava+"<"+p._2.toJava+">")

        BasicReport(
            allAffectedSubtypesAsStrings.mkString(
                "Implementations of "+containerName+":\n",
                "\n\n",
                "\nFound: "+allAffectedSubtypes.size+"("+subtypes.size+")"
            )
        )
    }
}
