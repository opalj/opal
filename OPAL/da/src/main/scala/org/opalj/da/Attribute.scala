/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package da

import scala.xml.Node

/**
 * The attribute class defines the common elements of all attributes; i.e., basically
 * the first two attribute_info elements.
 * <pre>
 * attribute_info {
 *  u2 attribute_name_index;
 *  u4 attribute_length;
 *  u1 info[attribute_length];
 *  ...
 * }
 * </pre>
 *
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 * @author Andre Pacak
 */
trait Attribute {

    /**
     * The number of bytes required to store this attribute; including the index into the constant
     * pool for the name (2 bytes) and the length of the attribute (4 bytes).
     */
    def size: Int = 2 /* attribute_name_index */ + 4 /* attribute_length */ + attribute_length

    /**
     * The number of bytes to store the attribute; excluding the index into the constant
     * pool for the name (2 bytes) and the length of the attribute (4 bytes).
     */
    def attribute_length: Int

    def attribute_name_index: Constant_Pool_Index

    def attribute_name(implicit cp: Constant_Pool) = cp(attribute_name_index).asString

    def toXHTML(implicit cp: Constant_Pool): Node

    // TODO we need to add a method to create "inline representations" for some attributes.
}
