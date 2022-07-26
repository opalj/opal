/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package hermes
package queries
package util

import scala.collection.mutable

import org.opalj.da.ClassFile
import org.opalj.br.MethodWithBody
import org.opalj.br.ObjectType
import org.opalj.br.analyses.Project
import org.opalj.br.instructions.Instruction
import org.opalj.br.instructions.MethodInvocationInstruction

/**
 * A predefined query for finding simple API features. It supports - in particular -
 * features that check for certain API calls. Subclasses are only required to define
 * a `Chain` of `APIFeatures`.
 *
 * Example of an `apiFeature` declaration in a subclass:
 * {{{
 * override def apiFeatures: Chain[APIFeatures] = Chain[APIFeature](
 *  val Unsafe = ObjectType("sun/misc/Unsafe")
 *
 *  StaticAPIMethod(Unsafe, "getUnsafe", MethodDescriptor("()Lsun/misc/Unsafe;")),
 *  APIFeatureGroup(
 *      Chain(InstanceAPIMethod(Unsafe, "allocateInstance")),
 *      "Unsafe - Alloc"
 *  ),
 *  APIFeatureGroup(
 *      Chain(
 *          InstanceAPIMethod(Unsafe, "arrayIndexScale"),
 *          InstanceAPIMethod(Unsafe, "arrayBaseOffset")
 *      ),
 *      "Unsafe - Array"
 *  )
 * )
 * }}}
 *
 * @author Michael Reif
 */
abstract class APIFeatureQuery(implicit hermes: HermesConfig) extends FeatureQuery {

    def apiFeatures: List[APIFeature]

    /**
     * The unique ids of the computed features.
     */
    override lazy val featureIDs: Seq[String] = apiFeatures.map(_.featureID)

    /**
     * Returns the set of all relevant receiver types.
     */
    private[this] final lazy val apiTypes: Set[ObjectType] = {
        apiFeatures.foldLeft(Set.empty[ObjectType])(_ ++ _.apiMethods.map(_.declClass))
    }

    /**
     * Analyzes the project and extracts the feature information.
     *
     * @note '''Every query should regularly check that its thread is not interrupted!''' E.g.,
     *       using `Thread.currentThread().isInterrupted()`.
     */
    override def apply[S](
        projectConfiguration: ProjectConfiguration,
        project:              Project[S],
        rawClassFiles:        Iterable[(ClassFile, S)]
    ): IterableOnce[Feature[S]] = {

        val classHierarchy = project.classHierarchy
        import classHierarchy.allSubtypes
        import project.isProjectType

        def getClassFileLocation(objectType: ObjectType): Option[ClassFileLocation[S]] = {
            val classFile = project.classFile(objectType)
            classFile.flatMap { cf => project.source(cf).map(src => ClassFileLocation(src, cf)) }
        }

        var occurrencesCount = apiFeatures.foldLeft(Map.empty[String, Int])(
            (result, feature) => result + ((feature.featureID, 0))
        )

        // TODO Use LocationsContainer
        val locations = mutable.Map.empty[String, List[Location[S]]]

        for {
            classFeature <- apiFeatures.collect { case ce: ClassExtension => ce }
            featureID = classFeature.featureID
            subtypes = allSubtypes(classFeature.declClass, reflexive = false).filter(isProjectType)
            size = subtypes.size
            if size > 0
        } {
            val count = occurrencesCount(featureID) + size
            occurrencesCount += ((featureID, count))

            for {
                subtype <- subtypes
                if project.isProjectType(subtype)
                classFileLocation <- getClassFileLocation(subtype)
            } {
                locations += ((
                    featureID,
                    classFileLocation :: locations.getOrElse(featureID, List.empty)
                ))
            }
        }

        // Checking method API features

        for {
            cf <- project.allProjectClassFiles
            if !isInterrupted()
            source <- project.source(cf)
            m @ MethodWithBody(code) <- cf.methods
            pcAndInvocation <- code collect ({ case mii: MethodInvocationInstruction => mii }: PartialFunction[Instruction, MethodInvocationInstruction])
            pc = pcAndInvocation.pc
            mii = pcAndInvocation.value
            declClass = mii.declaringClass
            if declClass.isObjectType
            if apiTypes.contains(declClass.asObjectType)
            apiFeature <- apiFeatures
            featureID = apiFeature.featureID
            APIMethod <- apiFeature.apiMethods
            if APIMethod.matches(mii)
        } {
            val l = InstructionLocation(source, m, pc)
            locations += ((featureID, l :: locations.getOrElse(featureID, List.empty)))
            val count = occurrencesCount(featureID) + 1
            occurrencesCount = occurrencesCount + ((featureID, count))
        }

        apiFeatures.map { apiFeature =>
            val featureID = apiFeature.featureID
            Feature(
                featureID,
                occurrencesCount(featureID),
                locations.getOrElse(featureID, List.empty)
            )
        }
    }

}
