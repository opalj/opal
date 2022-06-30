/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream
import org.opalj.control.fillArraySeq

import scala.collection.immutable.ArraySeq

/**
 * Trait that implements a template method to read in the attributes of
 * a class, method_info, field_info or code_attribute structure.
 */
trait AttributesReader
    extends AttributesAbstractions
    with Constant_PoolAbstractions
    with Unknown_attributeAbstractions {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    /**
     * This factory method is called if an attribute is encountered that is unknown.
     * In general, such unknown attributes are represented by the
     * <code>Unknown_attribute</code> class.
     * However, if no representation of the unknown attribute is needed this method
     * can return `null` - after reading (skipping) all bytes belonging to this attribute.
     * If `null` is returned all information regarding this attribute are thrown away.
     */
    def Unknown_attribute(
        cp:                   Constant_Pool,
        ap:                   AttributeParent,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Int,
        in:                   DataInputStream
    ): Unknown_attribute

    //
    // IMPLEMENTATION
    //

    /**
     * This map associates attribute names with functions to read the corresponding
     * attribute
     *
     * ==Names of the Attributes==
     * <b>Java 1/2</b>Attributes:<br/>
     * <ul>
     * <li>ConstantValue_attribute </li>
     * <li>Exceptions_attribute </li>
     * <li>InnerClasses_attribute </li>
     * <li>EnclosingMethod_attribute </li>
     * <li>Synthetic_attribute </li>
     * <li>SourceFile_attribute </li>
     * <li>LineNumberTable_attribute </li>
     * <li>LocalVariableTable_attribute </li>
     * <li>LocalVariableTypeTable_attribute </li>
     * <li>Deprecated_attribute </li>
     * <li>Code_attribute => (CodeReader) </li>
     * </ul>
     * <b>Java 5</b>Attributes:<br />
     * <ul>
     * <li>Signature_attribute </li>
     * <li>SourceDebugExtension_attribute </li>
     * <li>RuntimeVisibleAnnotations_attribute </li>
     * <li>RuntimeInvisibleAnnotations_attribute </li>
     * <li>RuntimeVisibleParameterAnnotations_attribute </li>
     * <li>RuntimeInvisibleParameterAnnotations_attribute </li>
     * <li>AnnotationDefault_attribute </li>
     * </ul>
     * <b>Java 6</b>Attributes:<br />
     * <ul>
     * <li>StackMapTable_attribute </li>
     * </ul>
     * <b>Java 7</b>Attributes:<br />
     * <ul>
     * <li>BootstrapMethods_attribute </li>
     * </ul>
     * <b>Java 8</b>Attributes:<br />
     * <ul>
     * <li>MethodParameters_attribute </li>
     * <li>RuntimeVisibleTypeAnnotations_attribute </li>
     * <li>RuntimeInvisibleTypeAnnotations_attribute </li>
     * </ul>
     * <b>Java 9</b>Attributes:<br />
     * <ul>
     * <li>Module_attribute</li>
     * <li>MainClass_attribute</li>
     * <li>ModulePackages_attribute</li>
     * </ul>
     * <b>Java 11</b>Attributes:<br />
     * <ul>
     * <li>NestHost_attribute</li>
     * <li>NestMembers_attribute</li>
     * </ul>
     * <b>Java 16</b>Attributes:<br />
     * <ul>
     * <li>Record_attribute</li>
     * </ul>
     * <b>Java 17</b>Attributes:<br />
     * <ul>
     * <li>PermittedSubclasses_attribute</li>
     * </ul>
     *
     * The returned function is allowed to return null; in this case the attribute
     * will be discarded.
     */
    private[this] var attributeReaders: Map[String, (Constant_Pool, AttributeParent, Constant_Pool_Index, Constant_Pool_Index, Constant_Pool_Index, DataInputStream) => Attribute] = Map()

    /**
     * See `AttributeReader.registerAttributeReader` for details.
     */
    def registerAttributeReader(
        reader: (String, (Constant_Pool, AttributeParent, Constant_Pool_Index, Constant_Pool_Index, Constant_Pool_Index, DataInputStream) => Attribute)
    ): Unit = {
        attributeReaders += reader
    }

    private[this] var attributesPostProcessors = ArraySeq.empty[Attributes => Attributes]

    /**
     * Registers a new processor for the list of all attributes of a given class file
     * element (class, field, method, code). This can be used to post-process attributes.
     * E.g., to merge multiple line number tables if they exist or to link
     * attributes that have strong dependencies. E.g., (in Java 8) the
     * `localvar_target` structure of the `Runtime(In)VisibleTypeAnnotations` attribute
     * has a reference in the local variable table attribute.
     */
    def registerAttributesPostProcessor(p: Attributes => Attributes): Unit = {
        attributesPostProcessors :+= p
    }

    def Attributes(
        cp:                  Constant_Pool,
        ap:                  AttributeParent,
        ap_name_index:       Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        in:                  DataInputStream
    ): Attributes = {
        val attributes: Attributes =
            fillArraySeq(in.readUnsignedShort) {
                Attribute(cp, ap, ap_name_index, ap_descriptor_index, in)
            }.filter(attr => attr != null) // lets remove the attributes we don't need or understand

        attributesPostProcessors.foldLeft(attributes)((a, p) => p(a))
    }

    def Attribute(
        cp:                  Constant_Pool,
        ap:                  AttributeParent,
        ap_name_index:       Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        in:                  DataInputStream
    ): Attribute = {
        val attribute_name_index = in.readUnsignedShort()
        val attribute_name = cp(attribute_name_index).asString

        attributeReaders.getOrElse(
            attribute_name,
            Unknown_attribute _ // this is a factory method
        )(cp, ap, ap_name_index, ap_descriptor_index, attribute_name_index, in)
    }
}
