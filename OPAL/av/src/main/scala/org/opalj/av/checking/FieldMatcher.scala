/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package av
package checking

import scala.collection.immutable
import org.opalj.br.ClassFile
import org.opalj.br.FieldType
import org.opalj.br.Field
import org.opalj.br.analyses.SomeProject
import org.opalj.br.VirtualSourceElement

/**
 * Matches fields based on their name, type, annotations and declaring class.
 *
 * @author Marco Torsello
 * @author Michael Eichberg
 */
case class FieldMatcher(
        declaringClass: ClassLevelMatcher,
        annotations:    AnnotationsPredicate,
        theType:        Option[FieldType],
        theName:        Option[NamePredicate]
)
    extends SourceElementsMatcher {

    def doesClassFileMatch(classFile: ClassFile)(implicit project: SomeProject): Boolean = {
        declaringClass.doesMatch(classFile)
    }

    def doesFieldMatch(field: Field): Boolean = {
        (theType.isEmpty || (theType.get eq field.fieldType)) && (
            (theName.isEmpty || theName.get(field.name))
        ) &&
            annotations(field.annotations)
    }

    def extension(implicit project: SomeProject): immutable.Set[VirtualSourceElement] = {
        val allMatchedFields = project.allClassFiles collect {
            case classFile if doesClassFileMatch(classFile) => {
                classFile.fields collect {
                    case field if doesFieldMatch(field) => field.asVirtualField(classFile)
                }
            }
        }
        allMatchedFields.flatten.toSet
    }

}

/**
 * Defines several additional factory methods to facilitate the creation of
 * [[FieldMatcher]]s.
 *
 * @author Marco Torsello
 */
object FieldMatcher {

    def apply(
        declaringClass:       ClassLevelMatcher    = AllClasses,
        annotationsPredicate: AnnotationsPredicate = AnyAnnotations,
        theType:              Option[String]       = None,
        theName:              Option[String]       = None,
        matchPrefix:          Boolean              = false
    ): FieldMatcher = {

        assert(theName.isDefined || !matchPrefix)

        val nameMatcher: Option[NamePredicate] =
            theName match {
                case Some(f) =>
                    if (matchPrefix)
                        Some(StartsWith(f))
                    else
                        Some(Equals(f))
                case _ =>
                    None
            }

        new FieldMatcher(
            declaringClass,
            annotationsPredicate,
            theType.map(fqn => FieldType(fqn.replace('.', '/'))),
            nameMatcher
        )
    }

}
