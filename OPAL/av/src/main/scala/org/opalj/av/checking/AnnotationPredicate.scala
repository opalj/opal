/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package av
package checking

import org.opalj.br._
import org.opalj.br.ConcreteSourceElement

/**
 * Matches an annotation of a class, field, method or method parameter.
 *
 * @author Marco Torsello
 */
trait AnnotationPredicate extends SourceElementPredicate[ConcreteSourceElement] {

    def apply(other: Annotation): Boolean

    def toDescription(): String

}

/**
 * An annotation matcher that always returns `true`; it matches any annotation.
 *
 * @author Marco Torsello
 */
case object AnyAnnotation extends AnnotationPredicate {

    override def apply(other: Annotation): Boolean = true

    override def apply(other: ConcreteSourceElement): Boolean = true

    override def toDescription(): String = "/*any annotation*/";

}

/**
 * Matches an annotation of a class, field, method or method parameter that has the
 * specified type. The annotation is matched independent of the annotation's values.
 *
 * ==Example==
 * {{{
 * scala> import org.opalj.br._
 * scala> val foo = ObjectType("java/lang/Foo")
 * scala> val aw = org.opalj.av.checking.AnnotatedWith(foo)
 * aw: org.opalj.av.checking.AnnotatedWith = @java.lang.Foo
 * scala> aw(Annotation(foo,IndexedSeq(ElementValuePair("clazz",StringValue(" ")))))
 * res: Boolean = true
 * }}}
 * @author Marco Torsello
 */
case class HasAnnotation(annotationType: FieldType) extends AnnotationPredicate {

    /**
     * Checks if the type of the given annotation is the same as the type of this
     * predicate.
     */
    def apply(other: Annotation): Boolean = other.annotationType eq this.annotationType

    override def apply(sourceElement: ConcreteSourceElement): Boolean = {
        sourceElement.annotations.exists(this(_))
    }

    override def toDescription(): String = "@"+annotationType.toJava

}
/**
 * Factory methods to create [[AnnotatedWith]] predicates.
 */
object HasAnnotation {

    def apply(annotationType: String): HasAnnotation = {
        new HasAnnotation(ObjectType(annotationType.replace('.', '/')))
    }
}

/**
 * Tests if an annotation of a class, field, method or method parameter is as specified.
 * The test takes the element values into consideration; if you don't want to take
 * them into consideration use a [[HasAnnotation]] predicate.
 *
 * {{{
 * scala> import org.opalj.br._
 * scala> import org.opalj.av.checking._
 * scala> val foo = ObjectType("java/lang/Foo")
 *
 * scala> val am = AnnotationPredicate("java.lang.Foo",Map("clazz" -> StringValue("")))
 * am: org.opalj.av.checking.AnnotationPredicate = @java.lang.Foo(clazz="")
 *
 * scala> am(Annotation(foo,IndexedSeq(ElementValuePair("clazz",StringValue("")))))
 * res: Boolean = true
 *
 * scala> am(Annotation(foo,IndexedSeq(ElementValuePair("clazz",StringValue("-+-")))))
 * res: Boolean = false
 *
 * scala> val am = DefaultAnnotationPredicate("java.lang.Foo",IndexedSeq.empty)
 * am: org.opalj.av.checking.DefaultAnnotationPredicate = @java.lang.Foo()
 *
 * scala> am(Annotation(ObjectType("java/lang/Foo"),IndexedSeq(ElementValuePair("clazz",StringValue(" ")))))
 * res: Boolean = false
 * }}}
 *
 * @author Marco Torsello
 */
case class AnnotatedWith(
        annotationType:    FieldType,
        elementValuePairs: Seq[ElementValuePair]
)
    extends AnnotationPredicate {

    /**
     * Checks if the given annotation is as specified by this predicate.
     * Returns `true` if the given annotation has the type `annotationType`and
     * if all element value pairs are matched independent of their order.
     *
     * ==Example Scenarios==
     *  - If the predicate's annotation type is `A` and the other annotation's type is `B`
     *    then the elements will not match.
     *
     *  - If the predicate defines specific [[org.opalj.br.ElementValuePair]]s such as
     *      `ArrayBuffer(ElementValuePair("target", ClassValue("org.opalj.av.checking.AnnotationPredicate")),
     *              ElementValuePair("name", StringValue("Annotation_Predicate"))`
     *      it will then match annotations where the [[org.opalj.br.ElementValuePair]]s have a different order:
     *      `ArrayBuffer(ElementValuePair("name", StringValue("Annotation_Predicate"),
     *              ElementValuePair("target", ClassValue("org.opalj.av.checking.AnnotationPredicate")))`
     *      But it will not match if one or both of the two [[org.opalj.br.ElementValuePair]]s are missing or there is
     *      another [[org.opalj.br.ElementValuePair]] not defined by this predicate.
     */
    override def apply(that: Annotation): Boolean = {
        (that.annotationType eq this.annotationType) && {
            val thisEVPs = this.elementValuePairs
            val thatEVPs = that.elementValuePairs

            thatEVPs.size == thisEVPs.size &&
                thisEVPs.forall(thisEVP => thatEVPs.exists(_ == thisEVP))
        }
    }

    override def apply(sourceElement: ConcreteSourceElement): Boolean = {
        sourceElement.annotations.exists(this(_))
    }

    override def toDescription(): String = {
        elementValuePairs.
            map(_.toJava).
            mkString("@"+annotationType.toJava+"(", ",", ")")
    }

}

/**
 * Defines several additional factory methods to facilitate the creation of
 * [[AnnotationPredicate]]s.
 *
 * @author Marco Torsello
 */
object AnnotatedWith {

    type SomeClass = Class[_]

    /**
     * @param annotationType The type of the annotation. The given value must not be
     *      `java.lang.Void.TYPE`.
     */
    def apply(
        annotationType:    SomeClass,
        elementValuePairs: ElementValuePairs
    ): AnnotatedWith = {
        new AnnotatedWith(Type(annotationType).asFieldType, elementValuePairs)
    }

    def apply(
        annotationTypeName: BinaryString,
        elementValuePairs:  ElementValuePairs
    ): AnnotatedWith = {

        val annotationType = ObjectType(annotationTypeName.asString)
        new AnnotatedWith(annotationType, elementValuePairs)
    }

    def apply(
        annotationTypeName: BinaryString,
        elementValuePairs:  Map[String, ElementValue]
    ): AnnotatedWith = {
        new AnnotatedWith(
            ObjectType(annotationTypeName.asString),
            elementValuePairs.map(kv => ElementValuePair(kv._1, kv._2)).toSeq
        )
    }
    def apply(
        annotationTypeName: BinaryString,
        elementValuePairs:  (String, ElementValue)*
    ): AnnotatedWith = {
        new AnnotatedWith(
            ObjectType(annotationTypeName.asString),
            elementValuePairs.map(kv => ElementValuePair(kv._1, kv._2)).toSeq
        )
    }
}
