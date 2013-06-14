/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
*
*  - Redistributions of source code must retain the above copyright notice,
*    this list of conditions and the following disclaimer.
*  - Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*  - Neither the name of the Software Technology Group or Technische
*    Universität Darmstadt nor the names of its contributors may be used to
*    endorse or promote products derived from this software without specific
*    prior written permission.
*
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st.bat
package reader

import java.io.DataInputStream

import de.tud.cs.st.util.ControlAbstractions.repeat

/**
 * Trait that implements a template method to read in the attributes of
 * a class, method_info, field_info or code_attribute structure.
 *
 * @author Michael Eichberg
 */
trait AttributesReader
        extends AttributesAbstractions
        with Constant_PoolAbstractions
        with Unknown_attributeAbstractions {

    private type ValueAsString = { def value: String } // a structural type

    //
    // ABSTRACT DEFINITIONS
    //

    type Constant_Pool_Entry

    type CONSTANT_Utf8_info <: Constant_Pool_Entry with ValueAsString

    override type Constant_Pool = Array[Constant_Pool_Entry]

    /**
     * This factory method is called if an attribute is encountered that is unknown. In general,
     * such unknown attributes are represented by the <code>Unknown_attribute</code> class.
     * However, if no representation of the unknown attribute is needed this method can return null -
     * 	after reading (skipping) all bytes belonging to this attribute.
     */
    def Unknown_attribute(ap: AttributeParent,
                          cp: Constant_Pool,
                          attribute_name_index: Int,
                          in: DataInputStream): Unknown_attribute

    //
    // IMPLEMENTATION
    //

    /**
     * This map associates attribute names with functions to read the corresponding attribute.
     * <p>
     * <b>Java 2</b>Attributes:<br/>
     * <ul>
     * <li> ConstantValue_attribute </li>
     * <li> Exceptions_attribute </li>
     * <li> InnerClasses_attribute </li>
     * <li> EnclosingMethod_attribute </li>
     * <li> Synthetic_attribute </li>
     * <li> SourceFile_attribute </li>
     * <li> LineNumberTable_attribute </li>
     * <li> LocalVariableTable_attribute </li>
     * <li> LocalVariableTypeTable_attribute </li>
     * <li> Deprecated_attribute </li>
     * <li> Code_attribute => (CodeReader) </li>
     * </ul>
     * <b>Java 5</b>Attributes:<br />
     * <ul>
     * <li> Signature_attribute </li>
     * <li> SourceDebugExtension_attribute </li>
     * <li> RuntimeVisibleAnnotations_attribute </li>
     * <li> RuntimeInvisibleAnnotations_attribute </li>
     * <li> RuntimeVisibleParameterAnnotations_attribute </li>
     * <li> RuntimeInvisibleParameterAnnotations_attribute </li>
     * <li> AnnotationDefault_attribute </li>
     * </ul>
     * <b>Java 6</b>Attributes:<br />
     * <ul>
     * <li> StackMapTable_attribute </li>
     * </ul>
     * <b>Java 7</b>Attributes:<br />
     * <ul>
     * <li> BootstrapMethods_attribute </li>
     * </ul>
     */
    private var attributeReaders: Map[String, (AttributeParent, Constant_Pool, Constant_Pool_Index, DataInputStream) ⇒ Attribute] = Map()

    def register(r: (String, (AttributeParent, Constant_Pool, Constant_Pool_Index, DataInputStream) ⇒ Attribute)): Unit = {
        attributeReaders += r
    }

    def Attributes(ap: AttributesParent, cp: Constant_Pool, in: DataInputStream): Attributes = {
        val attributes_count = in.readUnsignedShort
        if (attributes_count == 0)
            Nil
        else
            repeat(attributes_count) {
                Attribute(ap, cp, in)
            } filter (_ != null) // We remove the attributes we do not understand or which we do not need.
    }

    def Attribute(ap: AttributeParent, cp: Constant_Pool, in: DataInputStream): Attribute = {
        val attribute_name_index = in.readUnsignedShort()
        val attribute_name = (cp(attribute_name_index).asInstanceOf[CONSTANT_Utf8_info]).value

        attributeReaders.getOrElse(
            attribute_name,
            Unknown_attribute _ // this is a factory method
        )(ap, cp, attribute_name_index, in)
    }
}
