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
package br
package reader

import scala.reflect.ClassTag

import org.opalj.bi.reader.Module_attributeReader

/**
 * The factory method to create the class level `Module` attribute (Java 9).
 *
 * @author Michael Eichberg
 */
trait Module_attributeBinding
    extends Module_attributeReader
    with ConstantPoolBinding
    with AttributeBinding {

    type Module_attribute = br.Module

    type RequiresEntry = br.Requires
    implicit val RequiresEntryManifest: ClassTag[RequiresEntry] = implicitly

    type ExportsEntry = br.Exports
    implicit val ExportsEntryManifest: ClassTag[ExportsEntry] = implicitly
    type ExportsToIndexEntry = String
    implicit val ExportsToIndexEntryManifest: ClassTag[ExportsToIndexEntry] = implicitly

    type OpensEntry = br.Opens
    implicit val OpensEntryManifest: ClassTag[OpensEntry] = implicitly
    type OpensToIndexEntry = String // module name
    implicit val OpensToIndexEntryManifest: ClassTag[OpensToIndexEntry] = implicitly

    type UsesEntry = ObjectType
    implicit val UsesEntryManifest: ClassTag[UsesEntry] = implicitly

    type ProvidesEntry = br.Provides
    implicit val ProvidesEntryManifest: ClassTag[ProvidesEntry] = implicitly
    type ProvidesWithIndexEntry = ObjectType
    implicit val ProvidesWithIndexEntryManifest: ClassTag[ProvidesWithIndexEntry] = implicitly

    override def Module_attribute(
        cp:                   Constant_Pool,
        attribute_name_index: Constant_Pool_Index,
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
            requires, exports, opens, uses, provides
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
        cp:            Constant_Pool,
        exports_index: Constant_Pool_Index,
        exportsFlags:  Int,
        exportsTo:     ExportsToIndexTable
    ): ExportsEntry = {
        br.Exports(
            cp(exports_index).asPackageIdentifier(cp),
            exportsFlags,
            exportsTo
        )
    }

    override def ExportsToIndexEntry(
        cp:               Constant_Pool,
        exports_to_index: Constant_Pool_Index // CONSTANT_UTF8
    ): ExportsToIndexEntry = {
        cp(exports_to_index).asModuleIdentifier(cp)
    }

    override def OpensToIndexEntry(
        cp:             Constant_Pool,
        opens_to_index: Constant_Pool_Index
    ): OpensToIndexEntry = {
        cp(opens_to_index).asModuleIdentifier(cp)
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
            opens_to_index_table
        )
    }

    override def UsesEntry(
        cp:         Constant_Pool,
        uses_index: Constant_Pool_Index // CONSTANT_Class
    ): UsesEntry = {
        cp(uses_index).asObjectType(cp)
    }

    override def ProvidesWithIndexEntry(
        cp:                  Constant_Pool,
        provides_with_index: Constant_Pool_Index // CONSTANT_Class
    ): ProvidesWithIndexEntry = {
        cp(provides_with_index).asObjectType(cp)
    }

    override def ProvidesEntry(
        cp:                        Constant_Pool,
        provides_index:            Constant_Pool_Index, // CONSTANT_Class
        provides_with_index_table: ProvidesWithIndexTable
    ): ProvidesEntry = {
        br.Provides(cp(provides_index).asObjectType(cp), provides_with_index_table)
    }

}

