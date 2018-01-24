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
package da

import scala.xml.Node
import org.opalj.bi.AccessFlags
import org.opalj.bi.AccessFlagsContexts
import org.opalj.bi.AccessFlagsContexts.MODULE

/**
 * Representation of the ''Module'' attribute (Java 9).
 *
 * @author Michael Eichberg
 */
case class Module_attribute(
        attribute_name_index: Constant_Pool_Index,
        module_name_index:    Constant_Pool_Index,
        module_flags:         Int,
        module_version_index: Constant_Pool_Index, // can be "0"!
        requires:             IndexedSeq[RequiresEntry],
        exports:              IndexedSeq[ExportsEntry],
        opens:                IndexedSeq[OpensEntry],
        uses:                 IndexedSeq[UsesEntry],
        provides:             IndexedSeq[ProvidesEntry]
) extends Attribute {

    def attribute_length: Int = {
        2 + 2 + 2 + // module meta information
            2 + requires.length * 6 + // <= requires
            2 + exports.iterator.map(_.attribute_length).sum +
            2 + opens.iterator.map(_.attribute_length).sum +
            2 + uses.length * 2 + // <= uses
            2 + provides.iterator.map(_.attribute_length).sum
    }

    override def toXHTML(implicit cp: Constant_Pool): Node = {
        val name = cp(module_name_index).toString(cp)
        val flags =
            AccessFlags.toStrings(module_flags, AccessFlagsContexts.MODULE).mkString("{", " ", "}")
        val version =
            if (module_version_index == 0)
                "N/A"
            else
                cp(module_version_index).toString(cp)
        val module = s"Module(name=$name, flags=$flags, version=$version)"

        <details>
            <summary class="attribute">{ module }</summary>
            <div>{ requires.map(_.toString(cp)).sorted.map(r ⇒ <span>{ r }</span><br/>) }</div>
            <div>{ exports.map(_.toString(cp)).sorted.map(r ⇒ <span>{ r }</span><br/>) }</div>
            <div>{ opens.map(_.toString(cp)).sorted.map(r ⇒ <span>{ r }</span><br/>) }</div>
            <div>{ uses.map(_.toString(cp)).sorted.map(r ⇒ <span>{ r }</span><br/>) }</div>
            <div>{ provides.map(_.toString(cp)).sorted.map(r ⇒ <span>{ r }</span><br/>) }</div>
        </details>
    }

}

case class RequiresEntry(
        requires_index:         Constant_Pool_Index,
        requires_flags:         Int,
        requires_version_index: Constant_Pool_Index // can be "0" (<=> no version...)
) {

    def toString(implicit cp: Constant_Pool): String = {

        val flags = AccessFlags.toString(requires_flags, MODULE)
        val requiredPackage = cp(requires_index).toString(cp)
        val versionInfo =
            if (requires_version_index == 0)
                ""
            else
                "// "+cp(requires_version_index).toString(cp)

        s"requires $flags $requiredPackage;$versionInfo"
    }
}

case class ExportsToEntry(exports_to_index: Constant_Pool_Index) {

    def toString(implicit cp: Constant_Pool): String = cp(exports_to_index).toString(cp)

}

case class ExportsEntry(
        exports_index:          Constant_Pool_Index,
        exports_flags:          Int,
        exports_to_index_table: IndexedSeq[ExportsToEntry]
) {

    def attribute_length: Int = 6 + exports_to_index_table.length * 2

    def toString(implicit cp: Constant_Pool): String = {
        val flags = AccessFlags.toString(exports_flags, MODULE)
        val exports_to = {
            if (exports_to_index_table.isEmpty)
                ";"
            else
                exports_to_index_table.map(_.toString).sorted.mkString(" to ", ", ", ";")
        }

        s"exports $flags ${cp(exports_index).toString(cp)}$exports_to"
    }
}

case class OpensToIndexEntry(opens_to_index: Constant_Pool_Index) {

    def toString(implicit cp: Constant_Pool): String = cp(opens_to_index).toString(cp)

}

case class OpensEntry(
        opens_index:          Constant_Pool_Index,
        opens_flags:          Int,
        opens_to_index_table: IndexedSeq[OpensToIndexEntry]
) {

    def attribute_length: Int = 6 + opens_to_index_table.length * 2

    def toString(implicit cp: Constant_Pool): String = {
        val flags = AccessFlags.toString(opens_flags, MODULE)
        val opens_to = {
            if (opens_to_index_table.isEmpty)
                ";"
            else
                opens_to_index_table.map(_.toString).sorted.mkString(" to ", ", ", ";")
        }

        s"opens $flags ${cp(opens_index).toString(cp)}$opens_to"
    }
}

case class UsesEntry(
        uses_index: Constant_Pool_Index
) {

    def toString(implicit cp: Constant_Pool): String = {
        s"uses ${cp(uses_index).toString(cp)};"
    }

}

case class ProvidesWithIndexEntry(provides_with_index: Constant_Pool_Index) {

    def toString(implicit cp: Constant_Pool): String = cp(provides_with_index).toString(cp)

}

case class ProvidesEntry(
        provides_index:            Constant_Pool_Index,
        provides_with_index_table: IndexedSeq[ProvidesWithIndexEntry]
) {

    def attribute_length: Int = 4 + provides_with_index_table.length * 2

    def toString(implicit cp: Constant_Pool): String = {
        val provides_with =
            provides_with_index_table.map(_.toString).sorted.mkString(" with ", ", ", ";")

        s"provides ${cp(provides_index).toString(cp)}$provides_with"
    }
}
