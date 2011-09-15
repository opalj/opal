<?xml version="1.0" encoding="UTF-8"?>
<!--
  License (BSD Style License):
  Copyright (c) 2009, 2011
  Software Technology Group
  Department of Computer Science
  Technische Universität Darmstadt
  All rights reserved.   

  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.
  - Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution. 
  - Neither the name of the Software Technology Group or Technische 
    Universität Darmstadt nor the names of its contributors may be used to 
    endorse or promote products derived from this software without specific 
    prior written permission. 

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
  POSSIBILITY OF SUCH DAMAGE.

  Author: Michael Eichberg (www.michael-eichberg.de)
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
	xmlns:opal="http://www.opal-project.de/BAT/10.2009/JVMInstructions">

	<xsl:output media-type="text/plain" encoding="UTF-8" method="text" />

	<xsl:include href="../Common.xsl"/>

	<xsl:template match="/">
    	<xsl:call-template name="copyright">
			<xsl:with-param name="sourceFile">GenerateInstructionExceptionsClass.xsl</xsl:with-param>
		</xsl:call-template>

package de.tud.cs.st.bat.resolved


/**
 * The exceptions that may be thrown when a Java bytecode instruction is executed.
 *
 * @version [Generator:] 0.9.0
 */
object InstructionExceptions {
 
	<xsl:for-each select="distinct-values(/opal:instructions/opal:instruction/opal:exceptions/opal:exception/@type)">
	val <xsl:value-of select="opal:lastSegment(string(.),'\.')"/> = ObjectType("<xsl:value-of select="."/>")
	</xsl:for-each>

}
	</xsl:template>

</xsl:stylesheet>