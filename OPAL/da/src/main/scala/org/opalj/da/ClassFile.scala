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

    private[this] implicit val cp = constant_pool

    /**
     * The fully qualified name of the class in Java notation (i.e., using dots
     * to seperate packages.)
     */
    final val fqn = cp(this_class).toString

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

        <ol>{ cpEntries }</ol>
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

    def fieldsToXHTML: Node = {
        <ul>{ for (field ← fields) yield field.toXHTML(cp) }</ul>
    }

    def methodsToXHTML: Node = {
        <div>{ for ((method, index) ← methods.zipWithIndex) yield method.toXHTML(index) }</div>
    }

    protected def accessFlags: Node = {
        <span class="AccessFlags">{ AccessFlags.classFlagsToJava(access_flags) }</span>
    }

    protected def filter: Node = {
        <details>
            <summary>Filter</summary>
            <table style="min-width:850px" class="code">
                <tr>
                    <td><input type="checkbox" value="HTML" onclick="FlagFilter('private');"> Private </input></td>
                    <td><input type="checkbox" value="HTML" onclick="FlagFilter('public');"> Public </input></td>
                    <td><input type="checkbox" value="HTML" onclick="FlagFilter('protected');"> Protected </input></td>
                    <td><input type="checkbox" value="HTML" onclick="FlagFilter('static');"> Static </input></td>
                    <td><input type="checkbox" value="HTML" onclick="FlagFilter('final');"> Final </input></td>
                    <td><input type="checkbox" value="HTML" onclick="FlagFilter('synchronized');"> Synchronized </input></td>
                </tr>
                <tr>
                    <td><input type="checkbox" value="HTML" onclick="FlagFilter('bridge');"> Bridge </input></td>
                    <td><input type="checkbox" value="HTML" onclick="FlagFilter('varargs');"> Varargs </input></td>
                    <td><input type="checkbox" value="HTML" onclick="FlagFilter('native');"> Native </input></td>
                    <td><input type="checkbox" value="HTML" onclick="FlagFilter('abstract');"> Abstract </input></td>
                    <td><input type="checkbox" value="HTML" onclick="FlagFilter('strict');"> Strict </input></td>
                    <td><input type="text" title='enter methode name' onkeyup="NameFilter(this.value);">  </input></td>
                </tr>
            </table>
        </details>
    }

    def toXHTML: Node =
        <html>
            <head>
                <title>Java Bytecode of { fqn }</title>
                <style type="text/css">{ scala.xml.Unparsed(ClassFile.ResetCSS) }</style>
                <style type="text/css">{ scala.xml.Unparsed(ClassFile.TheCSS) }</style>
                <script>{ scala.xml.Unparsed(ClassFile.FilterJS) }</script>
                <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.7.0/jquery.min.js"></script>
            </head>
            <body>
                <p class="Summary">
                    { accessFlags }
                    &nbsp;<b>{ fqn }</b>
                    Version:&nbsp;{ major_version+"."+minor_version }
                    <span class="constantPoolLink">
                        <a href="#openConstantPool" style="color:black;">ConstantPool</a>
                    </span>
                </p>
                <div id="openConstantPool" class="constantPool">
                    <div>
                        <a href="#close" title="Close" class="close">X</a>
                        { cpToXHTML }
                    </div>
                </div>
                <div id="classFile">
                    <div id="attributes">
                        <details>
                            <summary>Class Attributes</summary>
                            { attributesToXHTML }
                        </details>
                    </div>
                    <div id="fields">
                        <details>
                            <summary>Fields</summary>
                            <ol>
                                { fieldsToXHTML }
                            </ol>
                        </details>
                    </div>
                    <div id="methods">
                        <details>
                            <summary>Methods</summary>
                            <ol>
                                { filter }
                                { methodsToXHTML }
                            </ol>
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
