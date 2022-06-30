/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package av
package checking

import scala.util.matching.Regex
import org.opalj.br.ClassFile
import org.opalj.br.VirtualSourceElement
import org.opalj.br.VirtualSourceElement.asVirtualSourceElements
import org.opalj.br.analyses.SomeProject
import org.opalj.bi.AccessFlagsMatcher

/**
 * A class matcher matches classes defined by the respective classes.
 *
 * @author Marco Torsello
 */
trait ClassMatcher extends ClassLevelMatcher {

    def matchMethods: Boolean

    def matchFields: Boolean

}

/**
 * Matches all project and library classes including inner elements like methods and fields defined by
 * the respective classes.
 *
 * @author Marco Torsello
 */
case object AllClasses extends ClassMatcher {

    def matchMethods: Boolean = true

    def matchFields: Boolean = true

    def doesMatch(classFile: ClassFile)(implicit project: SomeProject): Boolean = true

    def extension(implicit project: SomeProject): Set[VirtualSourceElement] = {
        asVirtualSourceElements(project.allClassFiles)
    }

}

/**
 * Default class matcher matches classes defined by the respective classes.
 *
 * @author Marco Torsello
 */
case class DefaultClassMatcher(
        accessFlagsMatcher:       AccessFlagsMatcher   = AccessFlagsMatcher.ANY,
        namePredicate:            NamePredicate        = RegexNamePredicate(""".*""".r),
        annotationsPredicate:     AnnotationsPredicate = AnyAnnotations,
        matchSubclasses:          Boolean              = false,
        matchImplementingclasses: Boolean              = false,
        matchMethods:             Boolean              = true,
        matchFields:              Boolean              = true
) extends ClassMatcher {

    def isSubClass(classFile: ClassFile, project: SomeProject): Boolean = {
        var sourceClassFile: ClassFile = classFile
        while (sourceClassFile.superclassType.nonEmpty) {
            if (namePredicate(sourceClassFile.superclassType.get.fqn))
                return true;

            project.classFile(sourceClassFile.superclassType.get) match {
                case Some(cf) => sourceClassFile = cf
                case None =>
                    return false;
            }
        }

        false
    }

    def implementsInterface(classFile: ClassFile, project: SomeProject): Boolean = {
        classFile.interfaceTypes.exists(i => namePredicate(i.fqn))
    }

    def doesAnnotationMatch(classFile: ClassFile): Boolean = {
        annotationsPredicate(classFile.annotations)
    }

    def doesMatch(classFile: ClassFile)(implicit project: SomeProject): Boolean = {
        val classFileName = classFile.thisType.fqn
        (namePredicate(classFileName) ||
            (matchSubclasses && isSubClass(classFile, project)) ||
            (matchImplementingclasses && implementsInterface(classFile, project))) &&
            (doesAnnotationMatch(classFile)) &&
            accessFlagsMatcher.unapply(classFile.accessFlags)
    }

    def extension(implicit project: SomeProject): Set[VirtualSourceElement] = {
        asVirtualSourceElements(
            project.allClassFiles filter { doesMatch },
            matchMethods,
            matchFields
        )
    }

}

/**
 * Defines several additional factory methods to facilitate the creation of
 * [[ClassMatcher]]s.
 *
 * @author Marco Torsello
 */
object ClassMatcher {

    def apply(namePredicate: NamePredicate): ClassMatcher = {
        new DefaultClassMatcher(namePredicate = namePredicate)
    }

    def apply(annotationsPredicate: AnnotationsPredicate): ClassMatcher = {
        new DefaultClassMatcher(annotationsPredicate = annotationsPredicate)
    }

    def apply(className: String): ClassMatcher = {
        require(className.indexOf('*') == -1)
        new DefaultClassMatcher(namePredicate = Equals(className))
    }

    def apply(className: String, matchPrefix: Boolean): ClassMatcher = {
        require(className.indexOf('*') == -1)
        val namePredicate =
            if (matchPrefix)
                StartsWith(className)
            else
                Equals(className)
        new DefaultClassMatcher(namePredicate = namePredicate)
    }

    def apply(
        className:    String,
        matchPrefix:  Boolean,
        matchMethods: Boolean,
        matchFields:  Boolean
    ): ClassMatcher = {
        require(className.indexOf('*') == -1)

        val namePredicate =
            if (matchPrefix)
                StartsWith(className)
            else
                Equals(className)

        new DefaultClassMatcher(
            namePredicate = namePredicate,
            matchMethods = matchMethods,
            matchFields = matchFields
        )
    }

    def apply(className: String, matchPrefix: Boolean, matchSubclasses: Boolean): ClassMatcher = {
        require(className.indexOf('*') == -1)

        val namePredicate =
            if (matchPrefix)
                StartsWith(className)
            else
                Equals(className)
        new DefaultClassMatcher(
            namePredicate = namePredicate,
            matchSubclasses = matchSubclasses
        )
    }

    def apply(regex: Regex): ClassMatcher = {
        new DefaultClassMatcher(namePredicate = RegexNamePredicate(regex))
    }

    def apply(regex: Regex, matchSubclasses: Boolean): ClassMatcher = {
        new DefaultClassMatcher(
            namePredicate = RegexNamePredicate(regex),
            matchSubclasses = matchSubclasses
        )
    }

}
