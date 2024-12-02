/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.analyses.cg

import org.opalj.br.Field
import org.opalj.br.ObjectType
import org.opalj.br.ReferenceType
import org.opalj.br.analyses.SomeProject
import org.opalj.collection.immutable.UIDSet
import org.opalj.log.LogContext
import org.opalj.log.OPALLogger

import net.ceedubs.ficus.Ficus._

/**
 * This trait represents objects that detect fields that should be considered to be instantiated no matter what. This
 * can be useful e.g. when fields are natively set by the JVM, but also when dealing with libraries. The functionality
 * is similar to the InstantiatedTypesFinder.
 *
 * @author Johannes Düsing
 */
sealed trait InstantiatedFieldsFinder {
    def collectInstantiatedFields(project: SomeProject): Iterable[(Field, UIDSet[ReferenceType])] = Iterable.empty
}

/**
 * The default instantiated fields finder does not consider any fields to be instantiated.
 */
trait DefaultInstantiatedFieldsFinder extends InstantiatedFieldsFinder {
    override def collectInstantiatedFields(project: SomeProject): Iterable[(Field, UIDSet[ReferenceType])] =
        super.collectInstantiatedFields(project)
}

/**
 * A trait that considers all library fields to be instantiated with their declared type IF library class files are
 * interfaces only. A fully sound over-approximation would have to consider all subtypes of each field to possibly
 * be instantiated as well - this is not done here to somewhat retain precision, especially for RTA.
 */
trait SoundLibraryInstantiatedFieldsFinder extends InstantiatedFieldsFinder {

    override def collectInstantiatedFields(project: SomeProject): Iterable[(Field, UIDSet[ReferenceType])] = {
        if (!project.libraryClassFilesAreInterfacesOnly)
            return super.collectInstantiatedFields(project);

        super.collectInstantiatedFields(project) ++ project
            .allLibraryClassFiles
            .flatMap(_.fields)
            .filter(_.fieldType.isReferenceType)
            .map { field => (field, UIDSet(field.fieldType.asReferenceType)) }
    }

}

/**
 * A trait that considers fields to be instantiated based on the configuration of OPAL. Users can supply custom
 * values to mark fields as instantiated. Per default the field type is considered, but if users provide a
 * "typeHint" property, they can specify exact types and / or subtypes to be considered for the given field.
 */
trait ConfigurationInstantiatedFieldsFinder extends InstantiatedFieldsFinder {

    @inline private[this] def additionalInstantiatedFieldsKey: String = {
        InitialInstantiatedFieldsKey.ConfigKeyPrefix + "instantiatedFields"
    }

    override def collectInstantiatedFields(project: SomeProject): Iterable[(Field, UIDSet[ReferenceType])] = {
        import net.ceedubs.ficus.readers.ArbitraryTypeReader._

        implicit val logContext: LogContext = project.logContext
        var instantiatedFields = Set.empty[(Field, UIDSet[ReferenceType])]

        if (!project.config.hasPath(additionalInstantiatedFieldsKey)) {
            OPALLogger.info(
                "project configuration",
                s"configuration key $additionalInstantiatedFieldsKey is missing; " +
                    "no additional fields are considered instantiated"
            )
            return instantiatedFields
        }

        val fieldDefinitions =
            try {
                project.config.as[List[InstantiatedFieldContainer]](additionalInstantiatedFieldsKey)
            } catch {
                case e: Throwable =>
                    OPALLogger.error(
                        "project configuration - recoverable",
                        s"configuration key $additionalInstantiatedFieldsKey is invalid; " +
                            "see InstantiatedFieldsFinder documentation",
                        e
                    )
                    return instantiatedFields;
            }

        fieldDefinitions foreach { fieldDefinition =>
            project.classFile(ObjectType(fieldDefinition.declaringClass)) match {
                case Some(cf) if cf.findField(fieldDefinition.name).nonEmpty =>
                    cf.findField(fieldDefinition.name).foreach { field =>
                        fieldDefinition.typeHint match {
                            case Some(typeHint) if field.fieldType.isReferenceType =>
                                val considerSubtypes = typeHint.endsWith("+")
                                val fqn = if (considerSubtypes) typeHint.substring(0, typeHint.length - 1) else typeHint
                                val hintType = ObjectType(fqn)

                                if (field.fieldType.isObjectType &&
                                    project.classHierarchy.isASubtypeOf(hintType, field.fieldType.asObjectType).isNo
                                ) {
                                    // If the given type hint is not a subtype of the field type, we warn and
                                    // fall back to default behavior
                                    OPALLogger.warn(
                                        "project configuration - recoverable",
                                        s"type hint $typeHint for instantiated field not valid"
                                    )
                                    instantiatedFields += ((field, UIDSet(field.fieldType.asReferenceType)))
                                } else {
                                    if (considerSubtypes)
                                        instantiatedFields += ((
                                            field,
                                            UIDSet.fromSpecific[ReferenceType](project.classHierarchy.allSubtypes(
                                                hintType,
                                                reflexive = true
                                            ))
                                        ))
                                    else
                                        instantiatedFields += ((field, UIDSet(hintType)))
                                }

                            case None if field.fieldType.isReferenceType =>
                                instantiatedFields += ((field, UIDSet(field.fieldType.asReferenceType)))
                            case _ =>
                            // This is okay, primitive types don't need to be initialized
                        }
                    }

                case Some(_) =>
                    OPALLogger.warn(
                        "project configuration - recoverable",
                        s"configured field named ${fieldDefinition.name} not found on" +
                            s" class ${fieldDefinition.declaringClass}"
                    )
                case None =>
                    OPALLogger.warn(
                        "project configuration - recoverable",
                        s"class ${fieldDefinition.declaringClass} not found for configured instantiated field"
                    )
            }
        }

        super.collectInstantiatedFields(project) ++ instantiatedFields
    }

    private case class InstantiatedFieldContainer(declaringClass: String, name: String, typeHint: Option[String])
}

object ConfigurationInstatiatedFieldsFinder
    extends ConfigurationInstantiatedFieldsFinder

object DefaultInstantiatedFieldsFinder
    extends DefaultInstantiatedFieldsFinder
    with ConfigurationInstantiatedFieldsFinder

object SoundLibraryInstantiatedFieldsFinder
    extends SoundLibraryInstantiatedFieldsFinder
    with ConfigurationInstantiatedFieldsFinder
