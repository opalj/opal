/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package org.opalj
package bi
package reader

import scala.reflect.ClassTag

import java.io.DataInputStream

import org.opalj.control.repeat

/**
 * Generic parser for the ''Module'' attribute (Java 9).
 */
trait Module_attributeReader extends AttributeReader {

    type Module_attribute <: Attribute

    type RequiresEntry
    implicit val RequiresEntryManifest: ClassTag[RequiresEntry]

    type ExportsEntry
    implicit val ExportsEntryManifest: ClassTag[ExportsEntry]
    type ExportsToIndexEntry
    implicit val ExportsToIndexEntryManifest: ClassTag[ExportsToIndexEntry]
    type ExportsToIndexTable = IndexedSeq[ExportsToIndexEntry]

    type OpensEntry
    implicit val OpensEntryManifest: ClassTag[OpensEntry]
    type OpensToIndexEntry
    implicit val OpensToIndexEntryManifest: ClassTag[OpensToIndexEntry]
    type OpensToIndexTable = IndexedSeq[OpensToIndexEntry]

    type UsesEntry
    implicit val UsesEntryManifest: ClassTag[UsesEntry]

    type ProvidesEntry
    implicit val ProvidesEntryManifest: ClassTag[ProvidesEntry]
    type ProvidesWithIndexEntry
    implicit val ProvidesWithIndexEntryManifest: ClassTag[ProvidesWithIndexEntry]
    type ProvidesWithIndexTable = IndexedSeq[ProvidesWithIndexEntry]

    type Requires = IndexedSeq[RequiresEntry]
    type Exports = IndexedSeq[ExportsEntry]
    type Opens = IndexedSeq[OpensEntry]
    type Uses = IndexedSeq[UsesEntry]
    type Provides = IndexedSeq[ProvidesEntry]

    /**
     * @param module_version_index 0 or index into the constant pool. (I.e., optional)
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

    def ExportsToIndexEntry(
        constant_pool:    Constant_Pool,
        exports_to_index: Constant_Pool_Index
    ): ExportsToIndexEntry

    def ExportsEntry(
        constant_pool:          Constant_Pool,
        module_index:           Constant_Pool_Index, // CONSTANT_Package_info
        exports_flags:          Int,
        exports_to_index_table: ExportsToIndexTable
    ): ExportsEntry

    def OpensToIndexEntry(
        constant_pool:  Constant_Pool,
        opens_to_index: Constant_Pool_Index
    ): OpensToIndexEntry

    def OpensEntry(
        constant_pool:        Constant_Pool,
        opens_index:          Constant_Pool_Index, // CONSTANT_Package_info
        opens_flags:          Int,
        opens_to_index_table: OpensToIndexTable
    ): OpensEntry

    def UsesEntry(
        constant_pool: Constant_Pool,
        uses_index:    Constant_Pool_Index // CONSTANT_Class
    ): UsesEntry

    def ProvidesWithIndexEntry(
        constant_pool:       Constant_Pool,
        provides_with_index: Constant_Pool_Index // CONSTANT_Class
    ): ProvidesWithIndexEntry

    def ProvidesEntry(
        constant_pool:             Constant_Pool,
        provides_index:            Constant_Pool_Index, // CONSTANT_Class
        provides_with_index_table: ProvidesWithIndexTable
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
        val requires = repeat(requiresCount) {
            RequiresEntry(
                cp,
                in.readUnsignedShort(), in.readUnsignedShort(), in.readUnsignedShort()
            )
        }

        val exportsCount = in.readUnsignedShort()
        val exports = repeat(exportsCount) {
            ExportsEntry(
                cp,
                in.readUnsignedShort(),
                in.readUnsignedShort(),
                {
                    val exportsToCount = in.readUnsignedShort()
                    repeat(exportsToCount) {
                        val cpIndex = in.readUnsignedShort()
                        ExportsToIndexEntry(cp, cpIndex)
                    }
                }
            )
        }

        val opensCount = in.readUnsignedShort()
        val opens = repeat(opensCount) {
            OpensEntry(
                cp,
                in.readUnsignedShort(),
                in.readUnsignedShort(),
                {
                    val opensToCount = in.readUnsignedShort()
                    repeat(opensToCount) {
                        val cpIndex = in.readUnsignedShort()
                        OpensToIndexEntry(cp, cpIndex)
                    }
                }
            )
        }

        val usesCount = in.readUnsignedShort()
        val uses = repeat(usesCount) { UsesEntry(cp, in.readUnsignedShort()) }

        val providesCount = in.readUnsignedShort()
        val provides = repeat(providesCount) {
            ProvidesEntry(
                cp,
                in.readUnsignedShort(),
                {
                    val providesWithCount = in.readUnsignedShort()
                    repeat(providesWithCount) {
                        val cpIndex = in.readUnsignedShort()
                        ProvidesWithIndexEntry(cp, cpIndex)
                    }
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
