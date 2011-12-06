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
<!-- This stylesheet was developed using the SAXON 9 XSLT 2.0 processor.  -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
    xmlns:opal="http://www.opal-project.de/BAT/10.2011/JVMInstructions"
	xmlns:myfn="http://www.opal-project.de/BAT/XSLT-functions"
	xmlns:xs="http://www.w3.org/2001/XMLSchema">

	<!--
		Capitalizes a given string. The string must have at least one
		character.
	-->
	<xsl:function name="myfn:capitalize" as="xs:string">
	    <xsl:param name="string"/>
	    <xsl:value-of select="concat(upper-case(substring($string,1,1)),substring($string,2))"/>
  	</xsl:function>

  	<!--
  		Returns true if the current instruction is a base instruction.
	-->
	<xsl:function name="opal:isBaseInstruction" as="xs:boolean">
	    <xsl:param name="instruction"/>
		<xsl:variable name="instrName" select="$instruction/@name"/>
	    <xsl:value-of select="exists($instruction/../opal:instruction/opal:base_instruction[./@name eq $instrName])"/>
  	</xsl:function>


	<xsl:function name="opal:lastSegment" as="xs:string">
	    <xsl:param name="string" as="xs:string"/>
		<xsl:param name="token" as="xs:string"/>
		 <xsl:value-of select="string(tokenize($string,$token)[last()])"/>
  	</xsl:function>

	<!--
		Returns the number of parameters of a bytecode instruction.
		Those parameters that can be recalculated (e.g. "padding_bytes") or
		which have no "further" meaning (e.g. "IGNORE") are not counted.

		typeof(param name=instruction) = opal:Instruction
	-->
	<xsl:function name="opal:numberOfInstructionParameters" as="xs:integer">
	    <xsl:param name="instruction"/>
	    <xsl:value-of select="count(opal:stdInstructionParameters($instruction))"/>
  	</xsl:function>


	<!--
		The parameters of a bytecode instruction.
		Those parameters that have no run-time semantics are not included.
	-->
	<xsl:function name="opal:stdInstructionParameters">
	    <xsl:param name="instruction"/>
	    <xsl:sequence select="$instruction/opal:format/opal:std/opal:el[./@type ne 'mnemonic' and ./@type ne 'padding_bytes' and ./@type ne 'IGNORE'] union $instruction/opal:format/opal:std/opal:list"/>
  	</xsl:function>


	<!--
		All "core" instructions. I.e., instructions without base instructions and not the 'wide' instruction.
		$root is the root of the document.
	-->
	<xsl:function name="opal:coreInstructions">
	    <xsl:param name="root"/>
	    <xsl:sequence select="$root/opal:instructions/opal:instruction[@name ne 'wide' and not(./opal:base_instruction)]"/>
  	</xsl:function>


	<xsl:template name="copyright">
		<xsl:param name="sourceFile" required="yes"/>/*
--------------------------------------------------------------------------

		THIS FILE IS AUTO GENERATED - DO NOT CHANGE MANUALLY!
		Generated:  <xsl:value-of select="current-dateTime()"/>
		Source File: <xsl:value-of select="$sourceFile"/>

--------------------------------------------------------------------------

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
*/
</xsl:template>


</xsl:stylesheet>
