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

import scala.xml.Node
//import org.opalj.br.reader.LineNumberTable_attribute

/**
 * <pre>
 * Code_attribute { 
 * 	u2 attribute_name_index; 
 * 	u4 attribute_length; 
 * 	u2 max_stack; 
 * 	u2 max_locals; 
 * 	u4 code_length; 
 * 	u1 code[code_length]; 
 * 	u2 exception_table_length; 
 * 	{	u2 start_pc; 
 * 		u2 end_pc; 
 * 		u2 handler_pc; 
 * 		u2 catch_type; 
 * 	} exception_table[exception_table_length]; 
 * 	u2 attributes_count; 
 * 	attribute_info attributes[attributes_count]; 
 * } 
 * </pre>
 * @author Michael Eichberg
 * @author Wael Alkhatib
 * @author Isbel Isbel
 * @author Noorulla Sharief
 */
case class Code_attribute (
        attribute_name_index: Constant_Pool_Index,
        attribute_length: Int,
        max_stack: Int,
        max_locals: Int,
        code: Code,
        exception_handlers: IndexedSeq[ExceptionTableEntry],
        attributes: Attributes) extends Attribute{
        //val lineNumberTable = attributes.collectFirst{case lnt: LineNumberTable_attribute=> lnt}
	 override def toXHTML(implicit cp: Constant_Pool): Node = {
	  
	    val exception_handlershtml=exception_handlersToXHTML(cp)
	    val codeattributes=attributesToXHTML(cp)
        <div id="sourcecode">
            <details>
                <summary>code</summary>
	                max_stack :{max_stack}, max_locals:{max_locals}
                    {code.toXHTML(cp,attributes,exception_handlers)}                    
                    {exception_handlershtml}
                    {codeattributes}
            </details>
        </div>
    }
	
	def attributesToXHTML(implicit cp: Constant_Pool) = {
        { for (attribute ← attributes) yield attribute.toXHTML(cp) }
    }
	def exception_handlersToXHTML(implicit cp: Constant_Pool):Node = {
	  if(exception_handlers.length>0)
	    <div>
	      <details>
	      <summary>Exception Table:</summary>
	          { for (exception ← exception_handlers) yield exception.toXHTML(cp,code) }
	      </details>
	    </div>
	  else
	    <div></div>
    }
}
object Code_attribute{
	val name = "Code"
}



