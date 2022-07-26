/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br

/**
 * A class file attribute.
 *
 * ==Note==
 * Some class file attributes are skipped or resolved while loading
 * the class file and hence, are no longer represented at runtime.
 *
 * @author Michael Eichberg
 */
trait Attribute {

    /**
     * Returns the unique ID that identifies this kind of attribute (Signature,
     * LineNumberTable, ...)
     *
     * This id can then be used in a switch statement to efficiently identify the
     * attribute.
     * {{{
     * (attribute.id : @scala.annotation.switch) match {
     *      case Signature.Id => ...
     * }
     * }}}
     *
     * ==Associating Unique Id==
     * The unique ids are manually associated with the attributes.
     * The attributes use the following IDs:
     *  - (-1 '''Unknown Attribute''')
     *  - 1-5  The ConstantValue Attribute
     *  - 6 The Code Attribute
     *  - 7 The StackMapTable Attribute
     *  - 8 The Exceptions Attribute
     *  - 9 The InnerClasses Attribute
     *  - 10 The EnclosingMethod Attribute
     *  - 11 The Synthetic Attribute
     *  - 12-16 The Signature Attribute
     *  - 17 The SourceFile Attribute
     *  - 18 The SourceDebugExtension Attribute
     *  - 19 The LineNumberTable Attribute
     *  - 20 The LocalVariableTable Attribute
     *  - 21 The LocalVariableTypeTable Attribute
     *  - 22 The Deprecated Attribute
     *  - 23 The RuntimeVisibleAnnotations Attribute
     *  - 24 The RuntimeInvisibleAnnotations Attribute
     *  - 25 The RuntimeVisibleParameterAnnotations Attribute
     *  - 26 The RuntimeInvisibleParameterAnnotations Attribute
     *  - 27 The RuntimeVisibleTypeAnnotations Attribute
     *  - 28 The RuntimeInvisibleTypeAnnotations Attribute
     *  - 29-41 The AnnotationDefault Attribute
     *  - 42 The BootstrapMethods Attribute
     *  - 43 The MethodParameters Attribute
     *  - 44 The Module Attribute (Java 9)
     *  - 45 The ModuleMainClass Attribute (Java 9)
     *  - 46 The ModulePackages Attribute (Java 9)
     *  - 47 The NestHost Attribute (Java 11)
     *  - 48 The NestMembers Attribute (Java 11)
     *  - 49 The Record Attribute (Java 16)
     *  - 1001 OPAL's VirtualTypeFlag Attribute
     *  - 1002 OPAL's SynthesizedClassFiles Attribute
     *  - 1003 OPAL's TACode Attribute (the 3-Address Code)
     */
    def kindId: Int

    /**
     * Returns true if this attribute and the given one are guaranteed to be indistinguishable
     * at runtime.
     *
     * @note   If this class is implemented as a proper `case class`, this method can often be
     *         implemented by forwarding to the default `equals` method.
     */
    def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean
}

/** Attributes which are referred to by the code attribute. */
trait CodeAttribute extends Attribute {

    /**
     * A function that provides the new PC for every "old" PC. If an instruction I with the pc X
     * does not exist (anymore), the pc of I is equal or larger than `codeSize`.
     */
    def remapPCs(codeSize: Int, f: PC => PC): CodeAttribute

}
