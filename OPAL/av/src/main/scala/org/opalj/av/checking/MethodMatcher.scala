/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package av
package checking

import scala.collection.immutable
import org.opalj.br.ClassFile
import org.opalj.br.Method
import org.opalj.br.analyses.SomeProject
import org.opalj.br.VirtualSourceElement

/**
 * Matches methods based on their attributes, annotations and class.
 *
 * @author Marco Torsello
 */
case class MethodMatcher(
        classLevelMatcher:    ClassLevelMatcher              = AllClasses,
        annotationsPredicate: AnnotationsPredicate           = AnyAnnotations,
        methodPredicate:      SourceElementPredicate[Method] = AnyMethod
)
    extends SourceElementsMatcher {

    def doesClassFileMatch(classFile: ClassFile)(implicit project: SomeProject): Boolean = {
        classLevelMatcher.doesMatch(classFile)
    }

    def doesMethodMatch(method: Method): Boolean = {
        annotationsPredicate(method.annotations) &&
            methodPredicate(method)
    }

    def extension(implicit project: SomeProject): immutable.Set[VirtualSourceElement] = {
        val allMatchedMethods = project.allClassFiles collect {
            case classFile if doesClassFileMatch(classFile) =>
                classFile.methods collect {
                    case m if doesMethodMatch(m) => m.asVirtualMethod(classFile.thisType)
                }
        }
        allMatchedMethods.flatten.toSet
    }

}

/**
 * Defines several additional factory methods to facilitate the creation of
 * [[MethodMatcher]]s.
 *
 * @author Marco Torsello
 */
object MethodMatcher {

    def apply(methodPredicate: SourceElementPredicate[_ >: Method]): MethodMatcher = {
        new MethodMatcher(methodPredicate = methodPredicate)
    }

    def apply(
        annotationsPredicate: AnnotationsPredicate,
        methodPredicate:      SourceElementPredicate[_ >: Method]
    ): MethodMatcher = {
        new MethodMatcher(annotationsPredicate = annotationsPredicate, methodPredicate = methodPredicate)
    }

    def apply(
        classLevelMatcher: ClassLevelMatcher,
        methodPredicate:   SourceElementPredicate[Method]
    ): MethodMatcher = {
        new MethodMatcher(classLevelMatcher, methodPredicate = methodPredicate)
    }

    def apply(
        annotationsPredicate: AnnotationsPredicate
    ): MethodMatcher = {
        new MethodMatcher(annotationsPredicate = annotationsPredicate)
    }

    /**
     * Creates a MethodMatcher, that relies on an AllAnnotationsPredicate for matching
     * the given AnnotationPredicate.
     */
    def apply(annotationPredicate: AnnotationPredicate): MethodMatcher = {
        apply(HasAtLeastTheAnnotations(annotationPredicate))
    }

    def apply(
        classLevelMatcher:   ClassLevelMatcher,
        annotationPredicate: AnnotationPredicate
    ): MethodMatcher = {
        new MethodMatcher(classLevelMatcher, HasAtLeastTheAnnotations(annotationPredicate))
    }

}
