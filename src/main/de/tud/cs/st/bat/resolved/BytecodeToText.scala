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

import de.tud.cs.st.util.UTF8Println

import de.tud.cs.st.bat.resolved.reader.Java6Framework


/**
 * Prints out the object graph using the compiler generated "toString" method.
 * <p> The main purpose of this class is to demonstrate how to use BAT. </p>
 *
 * @author Michael Eichberg
 */
object BytecodeToText extends UTF8Println {


	def main(args : Array[String]) : Unit = {

		println(""" |BytecodeToTxt (c) 2009 
						|Software Engineering, Technische Universität Darmstadt
						|Michael Eichberg (eichberg@informatik.tu-darmstadt.de) """.stripMargin)

		//	var classFile : Java6Framework.ClassFile = null
		//	classFile = Java6Framework.ClassFile(() => new Object().getClass().getResourceAsStream("Object.class") )
		// println(classFile.toString+"\n")

		for (arg <- args) {
			println(Java6Framework.ClassFile(() => new FileInputStream(arg)).toString)
		}	
 	}
}

