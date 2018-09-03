/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj
package br
package reader

import org.opalj.bi.reader.Module_attributeReader
import org.opalj.collection.immutable.RefArray

/**
 * The factory method to create the "class level" `Module` attribute (Java 9).
 *
 * @author Michael Eichberg
 */
trait Module_attributeBinding
    extends Module_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type Module_attribute = br.Module

    type RequiresEntry = br.Requires

    type ExportsEntry = br.Exports
    type ExportsToIndexEntry = String

    type OpensEntry = br.Opens

    type OpensToIndexEntry = String // module name

    type UsesEntry = ObjectType

    type ProvidesEntry = br.Provides
    type ProvidesWithIndexEntry = ObjectType

    override def Module_attribute(
        cp:                   Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
        as_name_index:        Constant_Pool_Index,
        as_descriptor_index:  Constant_Pool_Index,
        module_name_index:    Constant_Pool_Index, // CONSTANT_Module_info
        module_flags:         Int,
        module_version_index: Constant_Pool_Index, // CONSTANT_UTF8
        requires:             Requires,
        exports:              Exports,
        opens:                Opens,
        uses:                 Uses,
        provides:             Provides
    ): Module_attribute = {
        Module(
            cp(module_name_index).asModuleIdentifier(cp),
            module_flags,
            if (module_version_index == 0) None else Some(cp(module_version_index).asString),
            requires,
            exports,
            opens,
            RefArray.mapFrom(uses)(cp(_).asObjectType(cp)),
            provides
        )
    }

    override def RequiresEntry(
        cp:                    Constant_Pool,
        requires_index:        Constant_Pool_Index,
        requires_flags:        Int,
        require_version_index: Constant_Pool_Index // Optional: CONSTANT_UTF8
    ): RequiresEntry = {
        br.Requires(
            cp(requires_index).asModuleIdentifier(cp),
            requires_flags,
            if (require_version_index != 0) Some(cp(require_version_index).asString) else None
        )
    }

    override def ExportsEntry(
        cp:                     Constant_Pool,
        exports_index:          Constant_Pool_Index,
        exports_flags:          Int,
        exports_to_index_table: ExportsToIndexTable
    ): ExportsEntry = {
        br.Exports(
            cp(exports_index).asPackageIdentifier(cp),
            exports_flags,
            RefArray.mapFrom(exports_to_index_table)(cp(_).asModuleIdentifier(cp))
        )
    }

    override def OpensEntry(
        cp:                   Constant_Pool,
        opens_index:          Constant_Pool_Index, // CONSTANT_Package_info
        opens_flags:          Int,
        opens_to_index_table: OpensToIndexTable
    ): OpensEntry = {
        br.Opens(
            cp(opens_index).asPackageIdentifier(cp),
            opens_flags,
            RefArray.mapFrom(opens_to_index_table)(cp(_).asModuleIdentifier(cp))
        )
    }

    override def ProvidesEntry(
        cp:                        Constant_Pool,
        provides_index:            Constant_Pool_Index, // CONSTANT_Class
        provides_with_index_table: ProvidesWithIndexTable
    ): ProvidesEntry = {
        br.Provides(
            cp(provides_index).asObjectType(cp),
            RefArray.mapFrom(provides_with_index_table)(cp(_).asObjectType(cp))
        )
    }

}

