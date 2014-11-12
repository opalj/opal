/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
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

import java.io.File

import scala.io.Source
import scala.xml.Node

import org.opalj.bi.AccessFlags
import org.opalj.bi.AccessFlagsContexts

/**
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 */
case class ClassFile(
        constant_pool: Constant_Pool,
        minor_version: Int,
        major_version: Int,
        access_flags: Int,
        this_class: Constant_Pool_Index,
        super_class: Constant_Pool_Index,
        interfaces: IndexedSeq[Constant_Pool_Index],
        fields: Fields,
        methods: Methods,
        attributes: Attributes) {

    def jdkVersion: String = {
        // 52 == 8; ... 50 == 6
        if (major_version >= 49) {
            "Java "+(major_version - 44)
        } else if (major_version > 45) {
            "Java 2 Platform version 1."+(major_version - 44)
        } else {
            "JDK 1.1 (JDK 1.0.2)"
        }
    }

    private[this] implicit val cp = constant_pool

    /**
     * The fully qualified name of the class in Java notation (i.e., using dots
     * to seperate packages.)
     */
    final val fqn = cp(this_class).toString

    final val superTypeFQNs = {
        {
            if (super_class != 0)
                "extends "+cp(super_class).toString+" "
            else
                ""
        } + {
            if (interfaces.nonEmpty)
                interfaces.map(i ⇒ cp(i).toString).mkString("implements ", ", ", "")
            else
                ""
        }
    }

    /**
     * Converts the constant pool to (x)HTML5.
     */
    def cpToXHTML: Node = {
        val cpEntries =
            for {
                cpIndex ← (1 until constant_pool.length)
                if cp(cpIndex) != null /*<= need for constant_double/_long entries*/
            } yield {
                <li value={ cpIndex.toString }>{ cp(cpIndex).toNode }</li>
            }

        <ol class="cp_entries">
            <li value="0"> &lt;UNUSED&gt;</li>
            { cpEntries }
        </ol>
    }

    def attributesToXHTML: Seq[Node] = {
        for (attribute ← attributes) yield attributeToXHTML(attribute)
    }

    def attributeToXHTML(attribute: Attribute): Node = {
        attribute match {
            case ica: InnerClasses_attribute ⇒ ica.toXHTML(fqn)
            case _                           ⇒ attribute.toXHTML(cp)
        }
    }

    def fieldsToXHTML: Seq[Node] =
        <table class="fields">{ for (field ← fields) yield field.toXHTML(fqn) }</table>

    def methodsToXHTML: Seq[Node] =
        for ((method, index) ← methods.zipWithIndex)
            yield method.toXHTML( /*fqn,*/ index)

    protected def accessFlags: Node = {
        <span class="access_flags">{ AccessFlags.classFlagsToJava(access_flags) }</span>
    }

    protected def filter: Node = {
        <details class="filter_settings" open="true">
            <summary>Filter</summary>
            <fieldset>
                <input type="radio" id="access_flag_private" name="visibility" value="private" onclick="toogleFilter();"></input><label for="access_flag_private">private</label>
                <input type="radio" id="access_flag_default" name="visibility" value="default" onclick="toogleFilter();"></input><label for="access_flag_default">&lt;default&gt;</label>
                <input type="radio" id="access_flag_protected" name="visibility" value="protected" onclick="toogleFilter();"></input><label for="access_flag_protected">protected</label>
                <input type="radio" id="access_flag_public" name="visibility" value="public" onclick="toogleFilter();"></input><label for="access_flag_public">public</label>
            </fieldset>
            <fieldset>
                <input type="radio" id="access_flag_final" name="final_or_abstract" value="final" onclick="toogleFilter();"></input><label for="access_flag_final">final</label>
                <input type="radio" id="access_flag_abstract" name="final_or_abstract" value="abstract" onclick="toogleFilter();"></input><label for="access_flag_abstract">abstract</label>
            </fieldset>
            <div class="java_flags">
                <input type="checkbox" id="access_flag_static" name="static" value="static" onclick="toogleFilter();"></input><label for="access_flag_static">static</label>
                <input type="checkbox" id="access_flag_strict" value="strict" onclick="toogleFilter();"></input><label for="access_flag_strict">strict</label>
                <input type="checkbox" id="access_flag_native" value="native" onclick="toogleFilter();"></input><label for="access_flag_native">native</label>
                <input type="checkbox" id="access_flag_synchronized" value="synchronized" onclick="toogleFilter();"></input><label for="access_flag_synchronized">synchronized</label>
            </div>
            <div class="jvm_flags">
                <input type="checkbox" id="jvm_modifier_bridge" value="bridge" onclick="toogleFilter();"></input><label for="jvm_modifier_bridge">bridge</label>
                <input type="checkbox" id="jvm_modifier_varargs" value="varargs" onclick="toogleFilter();"></input><label for="jvm_modifier_varargs">varargs</label>
            </div>
            <div class="name_filter">
                <label for="filter_by_method_name">Name:</label><input id="filter_by_method_name" type="text" title='filter by method name' onkeyup="toogleFilter();"></input>
            </div>
            <button value="clear" onclick="clearFilter();">clear</button>
        </details>
    }

    def toXHTML: Node =
        <html>
            <head>
                <title>Java Bytecode of { fqn }</title>
                <style type="text/css">{ scala.xml.Unparsed(ClassFile.ResetCSS) }</style>
                <style type="text/css">{ scala.xml.Unparsed(ClassFile.TheCSS) }</style>
                <script>{ scala.xml.Unparsed(ClassFile.FilterJS) }</script>
            </head>
            <body>
                <div id="class_file">
                    { accessFlags }
                    &nbsp;<b>{ fqn }</b>
                    &nbsp;{ superTypeFQNs }
                    <div id="class_file_version">
                        Version:&nbsp;{ s"$major_version.$minor_version ($jdkVersion)" }
                    </div>
                </div>
                <div class="constant_pool">
                    <details>
                        <summary>Constant Pool</summary>
                        { cpToXHTML }
                    </details>
                </div>
                <div class="members">
                    <div class="attributes">
                        <details>
                            <summary>Attributes</summary>
                            { attributesToXHTML }
                        </details>
                    </div>
                    <div class="fields">
                        <details>
                            <summary>Fields</summary>
                            { fieldsToXHTML }
                        </details>
                    </div>
                    <div class="methods">
                        <details>
                            <summary>Methods</summary>
                            { filter }
                            { methodsToXHTML }
                        </details>
                    </div>
                </div>
            </body>
        </html>
}

object ClassFile {

    final val ResetCSS: String = {
        process(this.getClass().getResourceAsStream("reset.css"))(
            scala.io.Source.fromInputStream(_).mkString
        )
    }

    final val TheCSS: String = {
        process(this.getClass().getResourceAsStream("style.css"))(
            scala.io.Source.fromInputStream(_).mkString
        )
    }

    final val FilterJS = {
        loadJavaScript("filter.js")
    }

    private def loadJavaScript(js: String): String = {
        process(this.getClass().getResourceAsStream(js))(
            scala.io.Source.fromInputStream(_).mkString
        )
    }

}
