/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import scala.reflect.ClassTag

import java.io.DataInputStream

import org.opalj.control.repeat

/**
 * Generic parser for the ''ModulePackages'' attribute (Java 9).
 */
trait ModulePackages_attributeReader extends AttributeReader {

    type ModulePackages_attribute >: Null <: Attribute

    type PackageIndexTableEntry
    implicit val PackageIndexTableEntryManifest: ClassTag[PackageIndexTableEntry]

    type PackageIndexTable = IndexedSeq[PackageIndexTableEntry]

    def PackageIndexTableEntry(
        constant_pool: Constant_Pool,
        package_index: Constant_Pool_Index // CONSTANT_Package_info
    ): PackageIndexTableEntry

    def ModulePackages_attribute(
        constant_pool:        Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        package_index_table:  PackageIndexTable,
        // The scope in which the attribute is defined
        as_name_index:       Constant_Pool_Index,
        as_descriptor_index: Constant_Pool_Index
    ): ModulePackages_attribute

    //
    // IMPLEMENTATION
    //

    private[this] def parserFactory() = (
        ap: AttributeParent,
        as_name_index: Constant_Pool_Index,
        as_descriptor_index: Constant_Pool_Index,
        cp: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) ⇒ {
        /*val attribute_length =*/ in.readInt
        val packageCount = in.readUnsignedShort()
        if (packageCount > 0 || reifyEmptyAttributes) {
            val packageIndexTable = repeat(packageCount) {
                PackageIndexTableEntry(cp, in.readUnsignedShort())
            }
            ModulePackages_attribute(
                cp,
                attribute_name_index,
                packageIndexTable,
                as_name_index,
                as_descriptor_index
            )
        } else {
            null
        }
    }: ModulePackages_attribute

    registerAttributeReader(ModulePackagesAttribute.Name → parserFactory())

}
