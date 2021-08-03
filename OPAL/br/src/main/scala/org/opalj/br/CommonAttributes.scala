/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Defines methods to return common attributes from the attributes table of
 * [[ClassFile]], [[Field]], [[Method]] and [[Code]] declarations.
 *
 * @author Michael Eichberg
 */
trait CommonAttributes {

    def attributes: Attributes

    def runtimeVisibleTypeAnnotations: TypeAnnotations = {
        attributes collectFirst { case RuntimeVisibleTypeAnnotationTable(vas) => vas } match {
            case Some(typeAnnotations) => typeAnnotations
            case None                  => NoTypeAnnotations
        }
    }

    def runtimeInvisibleTypeAnnotations: TypeAnnotations = {
        attributes collectFirst { case RuntimeInvisibleTypeAnnotationTable(ias) => ias } match {
            case Some(typeAnnotations) => typeAnnotations
            case None                  => NoTypeAnnotations
        }
    }

    final def foreachTypeAnnotation[U](f: TypeAnnotation => U): Unit = {
        runtimeVisibleTypeAnnotations.foreach(f)
        runtimeInvisibleTypeAnnotations.foreach(f)
    }

    /**
     * Compares this element's attributes with the given one.
     *
     * @return None, if both attribute lists are similar;
     *         Some(&lt;description of the difference&gt;) otherwise.
     */
    protected[this] def compareAttributes(
        other:  Attributes,
        config: SimilarityTestConfiguration
    ): Option[AnyRef] = {
        val (thisAttributes, otherAttributes) = config.compareAttributes(this, this.attributes, other)
        if (thisAttributes.size != otherAttributes.size) {
            val message =
                "number of (filtered) attributes differ: "+
                    thisAttributes.toSet.diff(otherAttributes.toSet).mkString("{", ",", "}")
            return Some((message, thisAttributes.size, otherAttributes.size));
        }
        // Recall that some attributes may be defined multiple times and therefore an easy
        // approach to get a stable sorting is not available.
        // (We have not seen any case of multiple occurences of an attribute in practice so far.)
        thisAttributes.find { a => !otherAttributes.exists(o => a.similar(o, config)) } map { missingAttribute =>
            val message = "missing attribute: "+missingAttribute
            return Some((message, thisAttributes, otherAttributes));
        }

        None
    }
}
