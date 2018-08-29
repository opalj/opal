/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package bi
package reader

import java.io.DataInputStream

import org.opalj.control.fillAnyRefArray
import org.opalj.control.fillArrayOfInt
import org.opalj.collection.immutable.AnyRefArray

/**
 * Generic parser for the ''Module'' attribute (Java 9).
 */
trait Module_attributeReader extends AttributeReader {

    //
    // TYPE DEFINITIONS AND FACTORY METHODS
    //

    type Module_attribute <: Attribute

    type RequiresEntry <: AnyRef
    type Requires = AnyRefArray[RequiresEntry]

    type ExportsEntry <: AnyRef
    type Exports = AnyRefArray[ExportsEntry]

    // CONCEPTUALLY:
    // type ExportsToIndexEntry => type ExportsToIndexTable = <X>Array[ExportsToIndexEntry]
    type ExportsToIndexTable = Array[Constant_Pool_Index] // CONSTANT_Module

    type OpensEntry <: AnyRef
    type Opens = AnyRefArray[OpensEntry]

    // CONCEPTUALLY:
    // type OpensToIndexEntry => type OpensToIndexTable = <X>Array[OpensToIndexEntry]
    type OpensToIndexTable = Array[Constant_Pool_Index]

    // CONCEPTUALLY:
    // type UsesEntry => type Uses = <X>Array[UsesEntry]
    type Uses = Array[Constant_Pool_Index] // CONSTANT_Class[]

    type ProvidesEntry <: AnyRef
    type Provides = AnyRefArray[ProvidesEntry]

    // CONCEPTUALLY:
    // type ProvidesWithIndexEntry => type ProvidesWithIndexTable = <X>Array[ProvidesWithIndexEntry]
    type ProvidesWithIndexTable = Array[Constant_Pool_Index] // CONSTANT_Class_Index[]

    //
    // IMPLEMENTATION
    //

    /**
     * @param module_version_index 0 or index into the constant pool (i.e., optional).
     * @param module_name_index Reference to the constant pool entry with the name of the module -
     *                          which is NOT in internal form. (I.e., "." are used!)
     */
    def Module_attribute(
        constant_pool:        Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        module_name_index:    Constant_Pool_Index, // CONSTANT_Module_info
        module_flags:         Int,
        module_version_index: Constant_Pool_Index, // Optional: CONSTANT_UTF8
        requires:             Requires,
        exports:              Exports,
        opens:                Opens,
        uses:                 Uses,
        provides:             Provides
    ): Module_attribute

    def RequiresEntry(
        constant_pool:         Constant_Pool,
        module_index:          Constant_Pool_Index, // CONSTANT_Module_info
        requires_flags:        Int,
        require_version_index: Constant_Pool_Index // Optional: CONSTANT_UTF8
    ): RequiresEntry

    def ExportsEntry(
        constant_pool:          Constant_Pool,
        module_index:           Constant_Pool_Index, // CONSTANT_Package_info
        exports_flags:          Int,
        exports_to_index_table: ExportsToIndexTable // CONSTANT_Module_info[]
    ): ExportsEntry

    def OpensEntry(
        constant_pool:        Constant_Pool,
        opens_index:          Constant_Pool_Index, // CONSTANT_Package_info
        opens_flags:          Int,
        opens_to_index_table: OpensToIndexTable // CONSTANT_Module_info[]
    ): OpensEntry

    def ProvidesEntry(
        constant_pool:             Constant_Pool,
        provides_index:            Constant_Pool_Index, // CONSTANT_Class
        provides_with_index_table: ProvidesWithIndexTable // CONSTANT_Class[]
    ): ProvidesEntry

    //
    // IMPLEMENTATION
    //

    /**
     * Parser for the Java 9 Module attribute.
     *
     * Structure:
     * <pre>
     * Module_attribute {
     *     u2 attribute_name_index;
     *     u4 attribute_length;
     *
     *     u2 module_name_index;
     *     u2 module_flags;
     *     u2 module_version_index;
     *
     *     u2 requires_count;
     *     {   u2 requires_index; // CONSTANT_Module_info
     *         u2 requires_flags;
     *         u2 requires_version_index; // Optional: CONSTANT_Utf8_info
     *     } requires[requires_count];
     *
     *     u2 exports_count;
     *     {   u2 exports_index; // CONSTANT_Package_info
     *         u2 exports_flags;
     *         u2 exports_to_count;
     *         u2 exports_to_index[exports_to_count]; // CONSTANT_Module_info[]
     *     } exports[exports_count];
     *
     *      u2 opens_count;
     *      {   u2 opens_index; // CONSTANT_Package_info
     *          u2 opens_flags;
     *          u2 opens_to_count;
     *          u2 opens_to_index[opens_to_count]; // CONSTANT_Module_info[]
     *      } opens[opens_count];
     *
     *     u2 uses_count;
     *     u2 uses_index[uses_count]; // CONSTANT_Class[]
     *
     *     u2 provides_count;
     *     {   u2 provides_index; // CONSTANT_Class
     *         u2 provides_with_count;
     *         u2 provides_with_index[provides_with_count]; // CONSTANT_Class_info[]
     *     } provides[provides_count];
     * }
     * </pre>
     */
    private[this] def parserFactory() = (
        ap: AttributeParent,
        cp: Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        in: DataInputStream
    ) ⇒ {
        /*val attribute_length = */ in.readInt()

        val name_index = in.readUnsignedShort()
        val flags = in.readUnsignedShort()
        val version_index = in.readUnsignedShort()

        val requiresCount = in.readUnsignedShort()
        val requires =
            fillAnyRefArray(requiresCount) {
                RequiresEntry(
                    cp,
                    in.readUnsignedShort(),
                    in.readUnsignedShort(),
                    in.readUnsignedShort()
                )
            }

        val exportsCount = in.readUnsignedShort()
        val exports =
            fillAnyRefArray(exportsCount) {
                ExportsEntry(
                    cp,
                    in.readUnsignedShort(),
                    in.readUnsignedShort(),
                    {
                        val exportsToCount = in.readUnsignedShort()
                        fillArrayOfInt(exportsToCount) { in.readUnsignedShort() }
                    }
                )
            }

        val opensCount = in.readUnsignedShort()
        val opens =
            fillAnyRefArray(opensCount) {
                OpensEntry(
                    cp,
                    in.readUnsignedShort(),
                    in.readUnsignedShort(),
                    {
                        val opensToCount = in.readUnsignedShort()
                        fillArrayOfInt(opensToCount) { in.readUnsignedShort() }
                    }
                )
            }

        val usesCount = in.readUnsignedShort()
        val uses = fillArrayOfInt(usesCount) { in.readUnsignedShort() }

        val providesCount = in.readUnsignedShort()
        val provides =
            fillAnyRefArray(providesCount) {
                ProvidesEntry(
                    cp,
                    in.readUnsignedShort(),
                    {
                        val providesWithCount = in.readUnsignedShort()
                        fillArrayOfInt(providesWithCount) { in.readUnsignedShort() }
                    }
                )
            }

        Module_attribute(
            cp,
            attribute_name_index,
            name_index, flags, version_index, requires, exports, opens, uses, provides
        )
    }: Attribute

    registerAttributeReader(ModuleAttribute.Name → parserFactory())
}
