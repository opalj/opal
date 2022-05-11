/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * Defines methods to return common attributes from the attributes table of
 * [[ClassFile]], [[Field]] and [[Method]] declarations.
 *
 * @author Michael Eichberg
 */
trait CommonSourceElementAttributes extends CommonAttributes {

    def runtimeVisibleAnnotations: Annotations =
        attributes collectFirst { case RuntimeVisibleAnnotationTable(vas) => vas } match {
            case Some(annotations) => annotations
            case None              => NoAnnotations
        }

    def runtimeInvisibleAnnotations: Annotations =
        attributes collectFirst { case RuntimeInvisibleAnnotationTable(ias) => ias } match {
            case Some(annotations) => annotations
            case None              => NoAnnotations
        }

    /**
     * The list of all annotations. In general, if a specific annotation is searched for
     * the method [[runtimeVisibleAnnotations]] or [[runtimeInvisibleAnnotations]]
     * should be used.
     */
    def annotations: Annotations = runtimeVisibleAnnotations ++ runtimeInvisibleAnnotations

    /**
     * `True` if this element was created by the compiler and the attribute `Synthetic`
     * is present. Compilers are, however, free to use the attribute or the corresponding
     * access flag.
     */
    def isSynthetic: Boolean = attributes contains Synthetic

    /**
     * Returns true if this (field, method, class) declaration is declared
     * as deprecated.
     *
     * ==Note==
     * The deprecated attribute is always set by the Java compiler when either the
     * deprecated annotation or the JavaDoc tag is used.
     */
    def isDeprecated: Boolean = attributes contains Deprecated

}
