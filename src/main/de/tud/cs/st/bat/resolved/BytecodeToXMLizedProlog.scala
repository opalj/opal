/* License (BSD Style License):
*  Copyright (c) 2009, 2011
*  Software Technology Group
*  Department of Computer Science
*  Technische Universität Darmstadt
*  All rights reserved.
*
*  Redistribution and use in source and binary forms, with or without
*  modification, are permitted provided that the following conditions are met:
*
*  - Redistributions of source code must retain the above copyright notice,
*    this list of conditions and the following disclaimer.
*  - Redistributions in binary form must reproduce the above copyright notice,
*    this list of conditions and the following disclaimer in the documentation
*    and/or other materials provided with the distribution.
*  - Neither the name of the Software Technology Group or Technische
*    Universität Darmstadt nor the names of its contributors may be used to
*    endorse or promote products derived from this software without specific
*    prior written permission.
*
*  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
*  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
*  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
*  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
*  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
*  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
*  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
*  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
*  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
*  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
*  POSSIBILITY OF SUCH DAMAGE.
*/
package de.tud.cs.st.bat.resolved

import java.io.FileInputStream
import java.io.InputStream
import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import java.util.Enumeration

import scala.xml.PrettyPrinter
import de.tud.cs.st.util.UTF8Println

import de.tud.cs.st.bat.resolved.reader.Java6Framework

/**
 * Returns an xml representation of a prolog term of a given class file.
 *
 * @author Michael Eichberg
 */
object BytecodeToXMLizedProlog extends UTF8Println {

    val pp = new PrettyPrinter(160, 4)

    def main(args: Array[String]): Unit = {

        for (arg ← args) {
            if (new File(arg).exists) {
                if (arg.endsWith(".zip") || arg.endsWith(".jar")) {
                    val zipfile = new ZipFile(new File(arg))
                    val zipentries = (zipfile).entries
                    while (zipentries.hasMoreElements) {
                        val zipentry = zipentries.nextElement
                        if (!zipentry.isDirectory && zipentry.getName.endsWith(".class")) {
                            processStream(() ⇒ zipfile.getInputStream(zipentry))
                        }
                    }

                } else {
                    processStream(() ⇒ new FileInputStream(arg))
                }
            } else {
                processStream(() ⇒ Class.forName(arg).getResourceAsStream(arg.substring(arg.lastIndexOf('.') + 1) + ".class"))
            }
        }
    }

    private def processStream(f: () ⇒ InputStream) {
        val classFile = Java6Framework.ClassFile(f)
        val facts = classFile.toProlog(XMLizedPrologFactory)
        println("<?xml version='1.0' encoding='UTF-8'?>")
        println("<?xml-stylesheet type='text/css' href='XMLizedProlog.css'?>")
        println(
            pp.format(
                <representation>
				{ facts }
				</representation>
            )
        )
    }
}

import scala.xml.Elem
import scala.xml.Node
import scala.xml.Null
import scala.xml.Text
import scala.xml.TopScope

/**
 * Creates XML variants of Prolog terms.
 *
 * @author Michael Eichberg
 */
object XMLizedPrologFactory extends PrologTermFactory[Node, Node, Elem] {

    private var factCounter: Int = 0

    def KeyAtom(prefix: String) = {
        factCounter = factCounter + 1
        <atom type="key">{ (prefix + "_" + factCounter).toString }</atom>
    }

    def IntegerAtom(value: Long) = <atom type="literal">{ value.toString }</atom>

    def FloatAtom(value: Double) = <atom type="literal">{ value.toString }</atom>

    def StringAtom(value: String) = <atom type="literal">{ value.toString }</atom>

    def TextAtom(value: String) = <atom type="text">{ value.toString }</atom>

    def Terms[T](xs: Seq[T], f: (T) ⇒ Node) = <list>{ for (x ← xs) yield f(x) }</list>

    def Term(functor: String, terms: Node*) = <term functor={ functor }>{ terms }</term>

    def Fact(functor: String, terms: Node*) = <fact functor={ functor }>{ terms }</fact>

}
