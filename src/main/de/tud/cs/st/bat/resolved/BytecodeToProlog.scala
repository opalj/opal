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

import de.tud.cs.st.util.UTF8Println
import de.tud.cs.st.prolog._

import de.tud.cs.st.bat.resolved.reader.Java6Framework


/**
 * Returns a prolog representation of a given class file.
 *
 * @author Michael Eichberg
 */

trait BytecodeToPrologTransformer {
	
	protected def println (s : String) : Unit 
	
	val header = """
		|% FastBytecodeToProlog: $Rev: 399 $ 
		|% FastBytecodeToProlog is meant to be used in conjunction with SWIProlog.
		|
		|% ISO PROLOG DIRECTIVES
		|:- discontiguous(class_file/10).
		|:- discontiguous(class_file_source/2).
		|:- discontiguous(enclosing_method/4).
		|:- discontiguous(annotation/4).
		|:- discontiguous(annotation_default/2).
		|:- discontiguous(parameter_annotations/3).
		|:- discontiguous(field/11).
		|:- discontiguous(field_value/2).
		|:- discontiguous(method/15).
		|:- discontiguous(method_exceptions/2).
		|:- discontiguous(method_line_number_table/2).
		|:- discontiguous(method_local_variable_table/2).
		|:- discontiguous(method_exceptions_table/2).
		|:- discontiguous(instr/3).
		""".stripMargin

	
	protected def processFile(fileName:String) {
		if (fileName.endsWith(".zip") || fileName.endsWith(".jar")) {
			val zipfile = new ZipFile(new File(fileName))
			val zipentries = (zipfile).entries
			while (zipentries.hasMoreElements) {
				val zipentry = zipentries.nextElement
				if (!zipentry.isDirectory && zipentry.getName.endsWith(".class")) {
					processStream(() => zipfile.getInputStream(zipentry))
				}
			}
			
		} else {
			processStream(() => new FileInputStream(fileName))
		}
	}

	protected def processStream(f : () => InputStream) {
		val classFile = Java6Framework.ClassFile(f)
		val facts = classFile.toProlog(WriteThroughPrologFactory)
		for (fact <- facts) {
			println(fact.toISOProlog)
		}
	}
	
}

object BytecodeToPrologProcessor extends BytecodeToPrologTransformer with FileProcessor {

	private var buffer = new StringBuffer;

	protected def println (s : String) {
		buffer append s
	}

	def supportedFileTypes () : Set[String] = Set(".class",".zip",".jar")

	def getPrologRepresentation (fileName : String, f :  => InputStream) : String = {
		processStream(f _)		
		val representation = buffer.toString()
		buffer = new StringBuffer
		representation
	}
}

object BytecodeToProlog extends BytecodeToPrologTransformer with UTF8Println {

	def main(args : Array[String]) : Unit = {
			
		println("% STATISTICS ("+args.mkString+") - Java version: "+System.getProperty("java.version"))
		println(header)
		
		for (arg <- args) {
			if (new File(arg).exists) {
				processFile(arg)
			} else {
				processStream(() => Class.forName(arg).getResourceAsStream(arg.substring(arg.lastIndexOf('.')+1)+".class"))
			}	
		}	
 	}
}


object WriteThroughPrologFactory 
	extends PrologTermFactory[
			de.tud.cs.st.prolog.Fact,
			de.tud.cs.st.prolog.GroundTerm,
			de.tud.cs.st.prolog.Atom]
{

	type Term = de.tud.cs.st.prolog.GroundTerm
	type Atom = de.tud.cs.st.prolog.Atom
	type Fact = de.tud.cs.st.prolog.Fact

	import de.tud.cs.st.prolog.GroundTerms

	private var factCounter : Int = 0
	
	def IntegerAtom(value : Long) : Atom = de.tud.cs.st.prolog.IntegerAtom(value)

	def FloatAtom(value : Double) : Atom = de.tud.cs.st.prolog.FloatAtom(value)
	
	/** String atoms are never quoted. */
	def StringAtom(value : String) : Atom = de.tud.cs.st.prolog.StringAtom(value)
	
	/** Text atoms are always quoted. */
	def TextAtom(value : String) : Atom = de.tud.cs.st.prolog.TextAtom(value)
	
	def KeyAtom(prefix : String) : Atom = {
		factCounter = factCounter + 1
		StringAtom(prefix+factCounter)
	}
		
	def Terms[T](xs : Seq[T], f : (T) => Term) : Term = 
		de.tud.cs.st.prolog.GroundTerms.seqToTerms(xs, f )
	
			
	def Term(functor : String, terms : Term*) : Term = {
		de.tud.cs.st.prolog.GroundTerm(functor,terms.toArray)
	}

	def Fact(functor : String, terms : Term*) : Fact = {
		de.tud.cs.st.prolog.Fact(functor,terms.toArray)
	}
		
}

