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
	xmlns:opal="http://www.opal-project.de/BAT/10.2011/JVMInstructions">

	<xsl:param name="debug" select="'false'" />

	<xsl:output media-type="text/plain" encoding="UTF-8" method="text" />

	<xsl:include href="../../Common.xsl"/>

	<xsl:template match="/">
    	<xsl:call-template name="copyright">
			<xsl:with-param name="sourceFile">GenerateBytecodeReaderAndBindingClass.xsl</xsl:with-param>
	</xsl:call-template>
package de.tud.cs.st.bat.resolved
package reader

import de.tud.cs.st.util.ControlAbstractions.repeat


/**
 * Defines a method to parse an array of bytes (with Java bytecode instructions) and to return an array
 * of &lt;code&gt;Instruction&lt;/code&gt;s.
 *
 * The target array has the same size to make sure that jump offsets etc.
 * point to the correct instruction.
 *
 * @version [Generated] <xsl:value-of select="current-dateTime()"/>
 */
trait BytecodeReaderAndBinding extends ConstantPoolBinding with CodeBinding{

	import java.io.DataInputStream
	import java.io.ByteArrayInputStream

	override type Constant_Pool = Array[Constant_Pool_Entry]

	/**
	 * Transforms an array of bytes into an array of [[de.tud.cs.st.bat.resolved.Instruction]]s.
	 */
	def Code(source : Array[Byte])(implicit cp : Constant_Pool) : Code = {

		val bas = new ByteArrayInputStream(source)
		val in = new DataInputStream(bas)
		val target = new Array[Instruction](source.size)
		var previousInstruction : Instruction = null
		while (in.available > 0){
			val index = source.length - in.available
			previousInstruction = parsers(in.readUnsignedByte)(previousInstruction,index,in,cp)
			target(index) = previousInstruction
		}

		target
	}

	// (previousInstruction: Instruction,
	//  index : Int,
	//  in : DataInputStream,
	//  cp : Constant_Pool
	// ) => Instruction
	private val parsers : Array[(Instruction, Int, DataInputStream, Constant_Pool) => Instruction ] = new Array(256)


	// _____________________________________________________________________________________________
	//
	// INITIALIZE THE PARSERS ARRAY
	// _____________________________________________________________________________________________
	//


	<xsl:for-each select="/opal:instructions/opal:instruction">
    	<xsl:variable name="instr_name" select="@name" />
		<xsl:apply-templates select="." />
    </xsl:for-each>
}
</xsl:template>


<xsl:template match="opal:instruction">
	parsers(<xsl:value-of select="./@opcode"/>) =	(previousInstruction : Instruction, index : Int, in : DataInputStream, cp : Constant_Pool) => {
		<xsl:if test="$debug eq 'true'">
		println("Reading instruction: <xsl:value-of select="./@name"/>")
		</xsl:if>
		<xsl:choose>
			<xsl:when test="count(opal:format/opal:std/opal:el) eq 1"><!-- The simple case that the instruction is identified by its mnemonic and does not have any parameters. -->
		<xsl:value-of select="upper-case(string(@name))"/> // instance of the instruction</xsl:when>
			<xsl:otherwise>
				<xsl:choose>
					<xsl:when test="exists(opal:format/opal:wide)">
		if (WIDE == previousInstruction) {
			<xsl:call-template name="create_instruction">
				<xsl:with-param name="fes" select="opal:format/opal:wide/(opal:el[@type ne 'mnemonic'] | opal:list )"/>
			</xsl:call-template>
		}	else {
			<xsl:call-template name="create_instruction">
				<xsl:with-param name="fes" select="opal:format/opal:std/(opal:el[@type ne 'mnemonic'] | opal:list)"/>
			</xsl:call-template>
		}
					</xsl:when>
					<xsl:otherwise>
		{
						<xsl:call-template name="create_instruction">
							<xsl:with-param name="fes" select="opal:format/opal:std/(opal:el[@type ne 'mnemonic'] | opal:list)"/>
						</xsl:call-template>
		}
					</xsl:otherwise>
				</xsl:choose>
			</xsl:otherwise>
		</xsl:choose>
	}
</xsl:template>


<xsl:template name="process_instruction_parameters">
	<xsl:param name="fes" required="yes"/>
	<xsl:param name="parameterId" required="yes"/>

	<xsl:if test="$fes"> <!-- equivalent to:  "count($fes) > 0"  -->
	<xsl:variable name="fe" select="$fes[1]" />
	<xsl:variable name="fet" select="$fe/@type" />
	<xsl:choose>
		<xsl:when test="$fet eq 'padding_bytes'">
			in.skip(3 - (index % 4)) // skip padding bytes
			<xsl:call-template name="process_instruction_parameters">
				<xsl:with-param name="fes" select="$fes[position() > 1]"/>
				<xsl:with-param name="parameterId" select="$parameterId"/>
			</xsl:call-template>
		</xsl:when>
		<xsl:when test="$fet eq 'IGNORE'">
			in.readByte // ignored; fixed value
			<xsl:call-template name="process_instruction_parameters">
				<xsl:with-param name="fes" select="$fes[position() > 1]"/>
				<xsl:with-param name="parameterId" select="$parameterId"/>
			</xsl:call-template>
		</xsl:when>
		<!--  STANDARD VALUES -->
		<xsl:when test="$fet eq 'atype'">
			<xsl:if test="$fe/@id">// <xsl:value-of select="$fe/@id"/>: </xsl:if>
			val p<xsl:value-of select="$parameterId"/> = in.readByte
			<xsl:call-template name="process_instruction_parameters">
				<xsl:with-param name="fes" select="$fes[position() > 1]"/>
				<xsl:with-param name="parameterId" select="$parameterId + 1"/>
			</xsl:call-template>
		</xsl:when>
		<xsl:when test="$fet eq 'ubyte'">
			<xsl:if test="$fe/@id">// <xsl:value-of select="$fe/@id"/>: </xsl:if>
			val p<xsl:value-of select="$parameterId"/> = in.readUnsignedByte
			<xsl:call-template name="process_instruction_parameters">
				<xsl:with-param name="fes" select="$fes[position() > 1]"/>
				<xsl:with-param name="parameterId" select="$parameterId + 1"/>
			</xsl:call-template>
		</xsl:when>
		<xsl:when test="$fet eq 'byte'">
			<xsl:if test="$fe/@id">// <xsl:value-of select="$fe/@id"/>: </xsl:if>
			val p<xsl:value-of select="$parameterId"/> = in.readByte
			<xsl:call-template name="process_instruction_parameters">
				<xsl:with-param name="fes" select="$fes[position() > 1]"/>
				<xsl:with-param name="parameterId" select="$parameterId + 1"/>
			</xsl:call-template>
		</xsl:when>
		<xsl:when test="$fet eq 'ushort'">
			<xsl:if test="$fe/@id">// <xsl:value-of select="$fe/@id"/>: </xsl:if>
			val p<xsl:value-of select="$parameterId"/> = in.readUnsignedShort
			<xsl:call-template name="process_instruction_parameters">
				<xsl:with-param name="fes" select="$fes[position() > 1]"/>
				<xsl:with-param name="parameterId" select="$parameterId + 1"/>
			</xsl:call-template>
		</xsl:when>
		<xsl:when test="$fet eq 'short' or $fe/@type eq 'branchoffset'">
			<xsl:if test="$fe/@id">// <xsl:value-of select="$fe/@id"/>: </xsl:if>
			val p<xsl:value-of select="$parameterId"/> = in.readShort
			<xsl:call-template name="process_instruction_parameters">
				<xsl:with-param name="fes" select="$fes[position() > 1]"/>
				<xsl:with-param name="parameterId" select="$parameterId + 1"/>
			</xsl:call-template>
		</xsl:when>
		<xsl:when test="$fet eq 'int' or $fe/@type eq 'branchoffset_wide'">
			<xsl:if test="$fe/@id">// <xsl:value-of select="$fe/@id"/>: </xsl:if>
			val p<xsl:value-of select="$parameterId"/> = in.readInt
			<xsl:call-template name="process_instruction_parameters">
				<xsl:with-param name="fes" select="$fes[position() > 1]"/>
				<xsl:with-param name="parameterId" select="$parameterId + 1"/>
			</xsl:call-template>
		</xsl:when>
		<!-- CONSTANT POOL ENTRIES -->
		<xsl:when test="$fet eq 'ushort_cp_index→call_site_specifier'">
			/* TODO [Java 7] "invokedynamic" - resolve index into bootstrap method attribute table. */
			val (name,methodDescriptor) /*: (String, MethodDescriptor)*/  = cp(in.readUnsignedShort).asNameAndMethodDescriptor(cp) <xsl:if test="$fe/@id">// <xsl:value-of select="$fe/@id"/></xsl:if>
			val p<xsl:value-of select="$parameterId"/> : String = name
			val p<xsl:value-of select="$parameterId+1"/> : MethodDescriptor = methodDescriptor
			<xsl:call-template name="process_instruction_parameters">
				<xsl:with-param name="fes" select="$fes[position() > 1]"/>
				<xsl:with-param name="parameterId" select="$parameterId + 2"/>
			</xsl:call-template>
		</xsl:when>
		<xsl:when test="$fet eq 'ushort_cp_index→methodref' or $fe/@type eq 'ushort_cp_index→interface_methodref'">
			val (declaringClass,name,methodDescriptor) /*: (ObjectType,String,MethodDescriptor)*/ = cp(in.readUnsignedShort).asMethodref(cp) <xsl:if test="$fe/@id">// <xsl:value-of select="$fe/@id"/></xsl:if>
			val p<xsl:value-of select="$parameterId"/> = declaringClass
			val p<xsl:value-of select="$parameterId+1"/> = name
			val p<xsl:value-of select="$parameterId+2"/> = methodDescriptor
			<xsl:call-template name="process_instruction_parameters">
				<xsl:with-param name="fes" select="$fes[position() > 1]"/>
				<xsl:with-param name="parameterId" select="$parameterId+3"/>
			</xsl:call-template>
		</xsl:when>
		<xsl:when test="$fet eq 'ushort_cp_index→fieldref'">
			val (declaringClass, name, fieldType) /*: (ObjectType,String,FieldType)*/ = cp(in.readUnsignedShort).asFieldref(cp) <xsl:if test="$fe/@id">// <xsl:value-of select="$fe/@id"/></xsl:if>
			val p<xsl:value-of select="$parameterId"/> = declaringClass
			val p<xsl:value-of select="$parameterId+1"/> = name
			val p<xsl:value-of select="$parameterId+2"/> = fieldType
			<xsl:call-template name="process_instruction_parameters">
				<xsl:with-param name="fes" select="$fes[position() > 1]"/>
				<xsl:with-param name="parameterId" select="$parameterId+3"/>
			</xsl:call-template>
		</xsl:when>
		<xsl:when test="$fet eq 'ubyte_cp_index→constant_value' ">
			<xsl:if test="$fe/@id">// <xsl:value-of select="$fe/@id"/></xsl:if>
			val p<xsl:value-of select="$parameterId"/> : ConstantValue[_] = cp(in.readUnsignedByte).asConstantValue(cp)
			<xsl:call-template name="process_instruction_parameters">
				<xsl:with-param name="fes" select="$fes[position() > 1]"/>
				<xsl:with-param name="parameterId" select="$parameterId+1"/>
			</xsl:call-template>
		</xsl:when>
		<xsl:when test="$fet eq 'ushort_cp_index→constant_value' ">
			<xsl:if test="$fe/@id">// <xsl:value-of select="$fe/@id"/></xsl:if>
			val p<xsl:value-of select="$parameterId"/> : ConstantValue[_] = cp(in.readUnsignedShort).asConstantValue(cp)
			<xsl:call-template name="process_instruction_parameters">
				<xsl:with-param name="fes" select="$fes[position() > 1]"/>
				<xsl:with-param name="parameterId" select="$parameterId+1"/>
			</xsl:call-template>
		</xsl:when>
		<xsl:when test="$fet eq 'ushort_cp_index→referenceType' ">
			<xsl:if test="$fe/@id">// <xsl:value-of select="$fe/@id"/></xsl:if>
			val cv : ConstantValue[_] = cp(in.readUnsignedShort).asConstantValue(cp)
			val p<xsl:value-of select="$parameterId"/> = cv.toClass
			<xsl:call-template name="process_instruction_parameters">
				<xsl:with-param name="fes" select="$fes[position() > 1]"/>
				<xsl:with-param name="parameterId" select="$parameterId+1"/>
			</xsl:call-template>
		</xsl:when>
		<xsl:when test="$fet eq 'ushort_cp_index→objectType' "><!-- required by new... i.e. how to interpret the value referenced by a constant_class info structure depends on the instruction -->
			<xsl:if test="$fe/@id">// <xsl:value-of select="$fe/@id"/></xsl:if>
			val p<xsl:value-of select="$parameterId"/> = cp(in.readUnsignedShort).asObjectType(cp)
			<xsl:call-template name="process_instruction_parameters">
				<xsl:with-param name="fes" select="$fes[position() > 1]"/>
				<xsl:with-param name="parameterId" select="$parameterId+1"/>
			</xsl:call-template>
		</xsl:when>
		<xsl:when test="name($fe) eq 'list' and @name eq 'tableswitch'">
			<xsl:if test="$fe/@id">// <xsl:value-of select="$fe/@id"/></xsl:if>
			val p<xsl:value-of select="$parameterId"/> : IndexedSeq[Int] =
				repeat(p3-p2+1) {
					in.readInt
				}
			<xsl:call-template name="process_instruction_parameters">
				<xsl:with-param name="fes" select="$fes[position() > 1]"/>
				<xsl:with-param name="parameterId" select="$parameterId+1"/>
			</xsl:call-template>
		</xsl:when>
		<xsl:when test="name($fe) eq 'list' and @name eq 'lookupswitch'">
			<xsl:if test="$fe/@id">// <xsl:value-of select="$fe/@id"/></xsl:if>
			val p<xsl:value-of select="$parameterId"/> : IndexedSeq[(Int,Int)] =
				repeat(p2){
					(in.readInt,in.readInt)
				}
			<xsl:call-template name="process_instruction_parameters">
				<xsl:with-param name="fes" select="$fes[position() > 1]"/>
				<xsl:with-param name="parameterId" select="$parameterId+1"/>
			</xsl:call-template>
		</xsl:when>

		<xsl:otherwise>
			<xsl:message terminate="yes">Unsupported format element (type): <xsl:value-of select="$fe"/> (<xsl:value-of select="$fe/@type"/>)</xsl:message>
		</xsl:otherwise>
	</xsl:choose>
	</xsl:if>
</xsl:template>


<xsl:template name="create_instruction">
	<!-- The current context has to be an "opal:instruction" element. -->
	<xsl:param name="fes" required="yes"/>
		<xsl:call-template name="process_instruction_parameters">
			<xsl:with-param name="fes" select="$fes"/>
			<xsl:with-param name="parameterId">1</xsl:with-param>
		</xsl:call-template>
			 <xsl:value-of select="upper-case(string(@name))"/>(<xsl:call-template name="create_constructor_parameters"><xsl:with-param name="fes" select="$fes"/><xsl:with-param name="parameterId">1</xsl:with-param></xsl:call-template>)
</xsl:template>
<xsl:template name="create_constructor_parameters">
	<xsl:param name="fes" required="yes"/>
	<xsl:param name="parameterId" required="yes"/>
	<xsl:comment>
		Creates zero or more parameters for each format element. The number of
		parameters depends on the type of the format element and if the information needs
		to be available or can be recalculated.
	</xsl:comment>
	<xsl:if test="$fes"> <!-- equivalent to:  "count($fes) > 0"  -->
	<xsl:variable name="fe" select="$fes[1]" />
	<xsl:variable name="fet" select="$fe/@type" />
	<xsl:choose>
		<xsl:when test="$fet eq 'padding_bytes' or $fet eq 'IGNORE'">
			<xsl:call-template name="create_constructor_parameters">
				<xsl:with-param name="fes" select="$fes[position() > 1]"/>
				<xsl:with-param name="parameterId" select="$parameterId"/>
			</xsl:call-template>
		</xsl:when>
		<xsl:when test="$fet eq 'ubyte' or $fet eq 'byte' or $fet eq 'atype' or $fet eq 'ushort' or $fet eq 'short' or $fet eq 'int' or $fet eq 'branchoffset' or $fet eq 'branchoffset_wide' or $fet eq 'ushort_cp_index→referenceType' or $fet eq 'ushort_cp_index→objectType' or $fet eq 'ubyte_cp_index→constant_value' or $fet eq 'ushort_cp_index→constant_value' or name($fe) eq 'list'">p<xsl:value-of select="$parameterId"/><xsl:if test="$fes[2] and not($fes[2]/@type eq 'IGNORE')">, </xsl:if><xsl:call-template name="create_constructor_parameters"><xsl:with-param name="fes" select="$fes[position() > 1]"/><xsl:with-param name="parameterId" select="$parameterId + 1"/></xsl:call-template></xsl:when>
		<xsl:when test="$fet eq 'ushort_cp_index→call_site_specifier'">/* TODO [Java 7] "invokedynamic" - resolve valid index into the bootstrap_methods array of the bootstrap method table */ p<xsl:value-of select="$parameterId"/>, p<xsl:value-of select="$parameterId + 1"/><xsl:if test="$fes[2] and not($fes[2]/@type eq 'IGNORE')">, </xsl:if><xsl:call-template name="create_constructor_parameters"><xsl:with-param name="fes" select="$fes[position() > 1]"/><xsl:with-param name="parameterId" select="$parameterId + 2"/></xsl:call-template></xsl:when>
		<xsl:when test="$fet eq 'ushort_cp_index→fieldref' or $fet eq 'ushort_cp_index→methodref' or $fet eq 'ushort_cp_index→interface_methodref'">p<xsl:value-of select="$parameterId"/>, p<xsl:value-of select="$parameterId + 1"/>, p<xsl:value-of select="$parameterId + 2"/><xsl:if test="$fes[2] and not($fes[2]/@type eq 'IGNORE')">, </xsl:if><xsl:call-template name="create_constructor_parameters"><xsl:with-param name="fes" select="$fes[position() > 1]"/><xsl:with-param name="parameterId" select="$parameterId + 3"/></xsl:call-template></xsl:when>
		<xsl:otherwise><xsl:message terminate="yes">Unsupported format type encountered while creating constructor parameters.</xsl:message></xsl:otherwise>
	</xsl:choose>
	</xsl:if>
</xsl:template>


</xsl:stylesheet>