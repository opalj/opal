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

import org.opalj.bi.AccessFlagsContexts
import org.opalj.bi.AccessFlags

/**
 * Definition of a Java 9 module.
 *
 * @author Michael Eichberg
 */
case class Module(
        name:        String,
        moduleFlags: Int,
        versionInfo: Option[String],
        requires:    IndexedSeq[Requires],
        exports:     IndexedSeq[Exports],
        opens:       IndexedSeq[Opens],
        uses:        IndexedSeq[ObjectType],
        provides:    IndexedSeq[Provides]
) extends Attribute {

    final override def kindId: Int = Module.KindId

    override def similar(other: Attribute, config: SimilarityTestConfiguration): Boolean = {
        // TODO make the comparisons order independent...
        this == other
    }
}

object Module {

    final val KindId = 44

}

/**
 * @param requires The name of a required module.
 */
case class Requires(requires: String, flags: Int, version: Option[String]) {

    def toJava: String = {
        var flags = AccessFlags.toString(this.flags, AccessFlagsContexts.MODULE)
        if (flags.nonEmpty) flags += " "
        val version = this.version.map(v ⇒ s" //$v").getOrElse("")
        s"requires $flags$requires;$version"
    }
}

/**
 * @param   exports Name of an exported package in internal form.
 * @param   exportsTo List of names of modules whose code can access the
 *          public types in this exported package (in internal form).
 */
case class Exports(exports: String, flags: Int, exportsTo: IndexedSeq[String]) {

    def toJava: String = {
        var flags = AccessFlags.toString(this.flags, AccessFlagsContexts.MODULE)
        if (flags.nonEmpty) flags += " "
        val exportsTo =
            if (this.exportsTo.isEmpty) "" else this.exportsTo.mkString(" to ", ", ", "")

        s"exports $flags$exports$exportsTo;"
    }
}

/**
 * @param opens The name of the package.
 */
case class Opens(opens: String, flags: Int, toPackages: IndexedSeq[String]) {

    def toJava: String = {
        val toPackages =
            if (this.toPackages.isEmpty) "" else this.toPackages.mkString(" to ", ", ", "")

        s"opens $opens$toPackages;"
    }

}

case class Provides(provides: ObjectType, withInterfaces: IndexedSeq[ObjectType]) {

    def toJava: String = {
        val withInterfaces =
            if (this.withInterfaces.isEmpty)
                ""
            else
                this.withInterfaces.map(_.toJava).mkString(" with ", ", ", "")
        s"provides ${provides.toJava}$withInterfaces;"
    }

}
