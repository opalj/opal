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
                if cp(cpIndex) != null
            } yield {
                <li value={ cpIndex.toString }>{ cp(cpIndex).toString() }</li>
            }

        <ol class="constant_pool_entries">{ cpEntries }</ol>
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
        for (field ← fields)
            yield field.toXHTML(fqn)

    def methodsToXHTML: Seq[Node] =
        for ((method, index) ← methods.zipWithIndex)
            yield method.toXHTML( /*fqn,*/ index)

    protected def accessFlags: Node = {
        <span class="access_flags">{ AccessFlags.classFlagsToJava(access_flags) }</span>
    }

    protected def filter: Node = {
        <details class="filter_settings">
            <summary>Filter</summary>
            <fieldset>
                <input type="radio" name="visibility" value="private" onclick="toogleFilter();">private</input>
                <input type="radio" name="visibility" value="default" onclick="toogleFilter();">&lt;default&gt;</input>
                <input type="radio" name="visibility" value="protected" onclick="toogleFilter();">protected</input>
                <input type="radio" name="visibility" value="public" onclick="toogleFilter();">public</input>
            </fieldset>
            <fieldset>
                <input type="radio" name="final_or_abstract" value="final" onclick="toogleFilter();">final</input>
                <input type="radio" name="final_or_abstract" value="abstract" onclick="toogleFilter();">abstract</input>
            </fieldset>
            <div class="java_flags">
                <input type="checkbox" name="static" value="static" onclick="toogleFilter();">static</input>
                <input type="checkbox" value="strict" onclick="toogleFilter();">strict</input>
                <input type="checkbox" value="native" onclick="toogleFilter();">native</input>
                <input type="checkbox" value="synchronized" onclick="toogleFilter();">synchronized</input>
            </div>
            <div class="jvm_flags">
                <input type="checkbox" value="bridge" onclick="toogleFilter();">bridge</input>
                <input type="checkbox" value="varargs" onclick="toogleFilter();">varargs</input>
            </div>
            <div class="name_filter">
                Name:<input type="text" title='filter by method name' onkeyup="toogleFilter();"></input>
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
                    <span class="constant_pool_link">
                        <a href="#constant_pool" style="color:black;">ConstantPool</a>
                    </span>
                    <div>
                        <span class="tooltip">
                            Version:&nbsp;{ major_version+"."+minor_version }
                            <span>{ jdkVersion }</span>
                        </span>
                    </div>
                </div>
                <div id="constant_pool">
                    <div>
                        <a href="#close" title="Close" class="close">X</a>
                        { cpToXHTML }
                    </div>
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
