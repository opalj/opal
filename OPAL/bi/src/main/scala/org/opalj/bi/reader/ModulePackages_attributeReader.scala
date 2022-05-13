/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

import org.opalj.control.fillArrayOfInt

/**
 * Generic parser for the ''ModulePackages'' attribute (Java 9).
 */
trait ModulePackages_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type ModulePackages_attribute >: Null <: Attribute

    // CONCEPTUALLY:
    // type PackageIndexTableEntry
    // type PackageIndexTable = <X>Array[PackageIndexTableEntry]
    type PackageIndexTable = Array[Constant_Pool_Index]

    def ModulePackages_attribute(
        cp:                   Constant_Pool,
        ap_name_index:        Constant_Pool_Index,
        ap_descriptor_index:  Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        package_index_table:  PackageIndexTable // CONSTANT_Package_info[]
    ): ModulePackages_attribute

    //
    // IMPLEMENTATION
    //

    private[this] def parserFactory() = (
        cp: Constant_Pool,
        ap: AttributeParent,
        ap_name_index: Constant_Pool_Index,
        ap_descriptor_index: Constant_Pool_Index,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) => {
        /*val attribute_length =*/ in.readInt
        val packageCount = in.readUnsignedShort()
        if (packageCount > 0 || reifyEmptyAttributes) {
            val packageIndexTable = fillArrayOfInt(packageCount) { in.readUnsignedShort() }
            ModulePackages_attribute(
                cp,
                ap_name_index,
                ap_descriptor_index,
                attribute_name_index,
                packageIndexTable
            )
        } else {
            null
        }
    }: ModulePackages_attribute

    registerAttributeReader(ModulePackagesAttribute.Name -> parserFactory())

}
