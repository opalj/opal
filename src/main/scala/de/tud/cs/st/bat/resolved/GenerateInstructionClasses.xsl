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
    xmlns:opal="http://www.opal-project.de/BAT/06.2012/JVMInstructions">


    <xsl:output media-type="text/plain" encoding="UTF-8" method="text"/>


	<xsl:include href="../Common.xsl"/>


	<xsl:template match="/">
    	<xsl:for-each select="/opal:instructions/opal:instruction">
        	<xsl:result-document href="{upper-case(string(@name))}.scala">
            	<xsl:apply-templates select="." />
       		</xsl:result-document>
    	</xsl:for-each>
	</xsl:template>


	<xsl:template match="opal:instruction">
    	<xsl:call-template name="copyright">
			<xsl:with-param name="sourceFile">GenerateInstructionClasses.xsl</xsl:with-param>
		</xsl:call-template>
    	<xsl:call-template name="class_definition"/>
	</xsl:template>


	<xsl:template name="class_definition">
<!-- current context: an opal:instruction -->
package de.tud.cs.st.bat.resolved

import de.tud.cs.st.util.ControlAbstractions.repeat

import de.tud.cs.st.bat.resolved.InstructionExceptions._

/**
 * <xsl:value-of select="opal:operation/text()" />.
 *
 * @version Generator: 0.10.0 (Last change: June, 16 2012)
 */
<xsl:choose>
	<xsl:when test="count(opal:format/opal:std/opal:el) eq 1">case object <xsl:value-of select="upper-case(string(@name))"/><!-- We have to use uppercase names to avoid name clashes with keywords (e.g. "return") and to make sure the names look meaningful: "FLOAD" looks better than "Fload".  --></xsl:when>
	<xsl:otherwise>case class <xsl:value-of select="upper-case(string(@name))"/> (<!-- delegate the generation of the parameters -->
	<xsl:call-template name="generate_parameters"/>
)</xsl:otherwise></xsl:choose>
extends Instruction {

	def opcode : Int = <xsl:value-of select="@opcode"/>

	def mnemonic : String = "<xsl:value-of select="@name"/>"

	lazy val exceptions : List[ObjectType] = <xsl:call-template name="toList">
		<xsl:with-param name="elems" select="opal:exceptions/opal:ex/@type"/>
	</xsl:call-template>

	def toXML(pc : Int) =
		&lt;<xsl:value-of select="@name"/> pc={ pc.toString }&gt;<xsl:call-template name="generate_xml_elements"/>
		&lt;/<xsl:value-of select="@name"/>&gt;


	def toProlog[F,T,A &lt;: T](
		factory : PrologTermFactory[F,T,A],
		declaringEntityKey : A,
		pc : Int,
		pc_to_seqNo : Array[Int]
	) : F = {

		import factory._

		Fact(
			"instr",
			declaringEntityKey,
			IntegerAtom(pc_to_seqNo(pc)),<!--
 --><xsl:choose>
		<!-- A parameterized instruction does not always have additional
			 format elements / instruction arguments.
		-->
		<xsl:when test="./opal:parameterized">
			Term(
				"<xsl:value-of select="./opal:parameterized/@base"/>"<xsl:if test="count(./opal:parameterized/opal:*) gt 0">,</xsl:if><!--
			 --><xsl:call-template name="generate_prolog_parameterization_parameters"><xsl:with-param name="instruction" select="."/></xsl:call-template><xsl:if test="opal:numberOfInstructionParameters(.) gt 0">,</xsl:if><!--
			 --><xsl:call-template name="generate_prolog_arguments_for_format_elements"/>
			)
		</xsl:when>
		<!-- The format of a specialized instruction is determined by its base
			 instruction (which can be parameterized itself)
		-->
		<xsl:when test="./opal:specialized">
			<xsl:variable name="baseInstructionName" select="./opal:specialized/@base"/>
			Term(
				<xsl:call-template name="generate_prolog_arguments_for_specialized_instruction"/>
			)
		</xsl:when>
		<xsl:when test="opal:numberOfInstructionParameters(.) eq 0"> <!-- e.g. arraylength -->
			StringAtom("<xsl:value-of select="string(@name)"/>")
		</xsl:when>
		<xsl:otherwise> <!-- e.g. checkcast-->
			Term(
				"<xsl:value-of select="string(@name)"/>",<!--
			 --><xsl:call-template name="generate_prolog_arguments_for_format_elements"/>
			)
		</xsl:otherwise>
	</xsl:choose>
		)
	}
}
</xsl:template>


<xsl:template name="generate_parameters">
	<!-- current context: an opal:instruction -->
	<xsl:for-each select="opal:stdInstructionParameters(.)">
		<xsl:call-template name="formatElement_to_VariableDeclaration"><xsl:with-param name="fe" select="."/></xsl:call-template><xsl:if test="position() != last()">, </xsl:if>
	</xsl:for-each>
 </xsl:template>


<xsl:template name="generate_xml_elements">
	<!-- current context: an opal:instruction -->
	<xsl:for-each select="opal:stdInstructionParameters(.)">
		<xsl:call-template name="formatElement_to_XMLElement"><xsl:with-param name="fe" select="."/></xsl:call-template>
	</xsl:for-each>
</xsl:template>


<xsl:template name="generate_prolog_parameterization_parameters">
	<xsl:param name="instruction" required="yes"/>
	<xsl:for-each select="$instruction/opal:parameterized/opal:*">
		<xsl:apply-templates select="."/><xsl:if test="position() != last()">, </xsl:if>
	</xsl:for-each>
</xsl:template>
<xsl:template match="opal:type">
	<xsl:choose>
		<xsl:when test="./@value">
				Term(
					"<xsl:value-of select="./@name"/>",
					<!-- FIXME Create the correct type of atom depending on the type of the value. -->
					StringAtom("<xsl:value-of select="./@value"/>"))</xsl:when>
		<xsl:otherwise>
				StringAtom("<xsl:value-of select="./@name"/>")</xsl:otherwise>
	</xsl:choose>
</xsl:template>
<xsl:template match="opal:classifier">
				StringAtom("<xsl:value-of select="./@name"/>")</xsl:template>
<xsl:template match="opal:operator">
				StringAtom("<xsl:value-of select="./@name"/>")</xsl:template>


<xsl:template name="generate_prolog_arguments_for_specialized_instruction">
	<!-- current context: an opal:instruction -->
	<xsl:variable name="baseInstructionName" select="./opal:specialized/@base"/>
	<xsl:variable name="baseInstruction" select="./../opal:instruction[./@name eq $baseInstructionName]"/>
	<xsl:choose>
		<xsl:when test="$baseInstruction/opal:parameterized">
			<!-- the base instruction is further parameterized -->
				"<xsl:value-of select="$baseInstruction/opal:parameterized/@base"/>",<xsl:if test="count($baseInstruction/opal:parameterized/opal:*) gt 0"><!--
			 --><xsl:call-template name="generate_prolog_parameterization_parameters"><xsl:with-param name="instruction" select="$baseInstruction"/></xsl:call-template>,</xsl:if>
			 <xsl:choose>
				<xsl:when test="./@name eq 'ldc'">
				constantValue.valueToProlog(factory)</xsl:when>
				<xsl:otherwise>
				IntegerAtom(<xsl:value-of select="./opal:specialized/opal:parameter/@value"/>)</xsl:otherwise>
			</xsl:choose>
		</xsl:when>
		<xsl:otherwise>
			<!-- The base instruction is not further parameterized
				 (e.g. goto - goto_w) and has exactly one parameter
		 -->	"<xsl:value-of select="$baseInstructionName"/>",
			<xsl:choose>
				<xsl:when test="./@name eq 'goto' or ./@name eq 'jsr'">
				IntegerAtom(pc_to_seqNo(pc+branchoffset) - pc_to_seqNo(pc))</xsl:when>
				<xsl:otherwise>
					<xsl:message terminate="yes">Error unsupported:<xsl:value-of select="./@name"/></xsl:message>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>


<xsl:template name="generate_prolog_arguments_for_format_elements">
	<xsl:for-each select="opal:stdInstructionParameters(.)">
		<xsl:variable name="id" select="./@id"/>
		<xsl:variable name="fet" select="./@type"/>
		<xsl:choose>
			<xsl:when test="$fet eq 'ubyte' or $fet eq 'byte' or $fet eq 'atype' or $fet eq 'ushort' or $fet eq 'short' or $fet eq 'int'">
				IntegerAtom(<xsl:value-of select="$id"/>)</xsl:when>

			<xsl:when test="$fet eq 'branchoffset' or $fet eq 'branchoffset_wide'">
				IntegerAtom(pc_to_seqNo(pc+<xsl:value-of select="$id"/>) - pc_to_seqNo(pc))</xsl:when>

			<xsl:when test="$fet eq 'ushort_cp_index→referenceType' or $fet eq 'ushort_cp_index→objectType'">
				<xsl:value-of select="$id"/>.toProlog(factory)</xsl:when>

			<xsl:when test="$fet eq 'ubyte_cp_index→constant_value' or $fet eq 'ushort_cp_index→constant_value'">
				<xsl:value-of select="$id"/>.valueToProlog(factory)</xsl:when>

			<xsl:when test="$fet eq 'ushort_cp_index→fieldref'">
				declaringClass.toProlog(factory),
				TextAtom(name),
				fieldType.toProlog(factory)</xsl:when>


			<xsl:when test="$fet eq 'ushort_cp_index→call_site_specifier'">
				<!-- TODO [Java 7] "invokedynamic" - resolve valid index into the bootstrap_methods array of the bootstrap method table  -->
				TextAtom(name),
				methodDescriptor.toProlog(factory)</xsl:when>

			<xsl:when test="$fet eq 'ushort_cp_index→methodref' or $fet eq 'ushort_cp_index→interface_methodref'">
				declaringClass.toProlog(factory),
				TextAtom(name),
				methodDescriptor.toProlog(factory)</xsl:when>

			<xsl:when test="name(.) eq 'list' and  ../../../@name eq 'lookupswitch'">
				Terms(
					npairs,
					(pair:Tuple2[Int,Int]) => {
						val (value,offset) = pair
						Term(
							"kv",
							Term("value",IntegerAtom(value)),
							IntegerAtom(pc_to_seqNo(pc+offset) - pc_to_seqNo(pc))
						)
					}
				)</xsl:when>

			<xsl:when test="name(.) eq 'list' and  ../../../@name eq 'tableswitch'">
				Terms(jumpOffsets,(offset : Int) => IntegerAtom(pc_to_seqNo(pc+offset) - pc_to_seqNo(pc)))</xsl:when>


			<!-- If we would be able to use schema validation, then we would not require the following check. -->
			<xsl:otherwise>
				<xsl:message terminate="yes">Unsupported format element: <xsl:value-of select="."/></xsl:message>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:if test="position() != last()">, </xsl:if>
	</xsl:for-each>
</xsl:template>

<!--


    H E L P E R   F U N C T I O N S


-->
<xsl:template name="formatElement_to_XMLElement">
	<xsl:param name="fe" required="yes"/>
	<xsl:variable name="id" select="$fe/@id"/>
	<xsl:variable name="fet" select="$fe/@type"/>
	<xsl:choose>
			<xsl:when test="$fet eq 'ubyte' or $fet eq 'byte' or $fet eq 'atype' or $fet eq 'ushort' or $fet eq 'short' or $fet eq 'int' or $fet eq 'branchoffset' or $fet eq 'branchoffset_wide'">
			&lt;<xsl:value-of select="$id"/> value={ <xsl:value-of select="$id"/>.toString }/&gt;</xsl:when>

			<xsl:when test="$fet eq 'ushort_cp_index→referenceType' or $fet eq 'ushort_cp_index→objectType'">
			&lt;<xsl:value-of select="$id"/> type={ <xsl:value-of select="$id"/>.toJava }/&gt;</xsl:when>

			<xsl:when test="$fet eq 'ubyte_cp_index→constant_value' or $fet eq 'ushort_cp_index→constant_value'"><!-- used by ldc, ldc_w, ldc2 -->
			&lt;<xsl:value-of select="$id"/> type={ <xsl:value-of select="$id"/>.valueType.toJava }&gt;
 				{ <xsl:value-of select="$id"/>.valueToString }
			&lt;/<xsl:value-of select="$id"/>&gt;</xsl:when>

			<xsl:when test="$fet eq 'ushort_cp_index→fieldref'"><!-- used by get/put field/static -->
			&lt;fieldref declaring_class={ declaringClass.toJava } name={ name } type={ fieldType.toJava }/&gt;</xsl:when>

			<xsl:when test="$fet eq 'ushort_cp_index→call_site_specifier'">
			/* TODO [Java 7] "invokedynamic" - resolve valid index into the bootstrap_methods array of the bootstrap method table  */
			&lt;methodref name={ name } &gt;
				{ methodDescriptor.toXML }
			&lt;/methodref&gt;</xsl:when>

			<xsl:when test="$fet eq 'ushort_cp_index→methodref' or $fet eq 'ushort_cp_index→interface_methodref'">
			&lt;methodref name={ name } declaring_class={ declaringClass.toJava } &gt;
				{ methodDescriptor.toXML }
			&lt;/methodref&gt;</xsl:when>

			<xsl:when test="name($fe) eq 'list' and  ../../../@name eq 'lookupswitch'">
			{ for (npair &lt;- npairs) yield { val (value,jumpOffset) = npair ; &lt;case value={ value.toString } jump_offset={ jumpOffset.toString }/&gt; } }</xsl:when>

			<xsl:when test="name($fe) eq 'list' and  ../../../@name eq 'tableswitch'">
			{ for (jumpOffset &lt;- jumpOffsets) yield { &lt;case jump_offset={ jumpOffset.toString }/&gt; } }</xsl:when>


		<!-- If we would be able to use schema validation, then we would not require the following check. -->
		<xsl:otherwise>
			<xsl:message terminate="yes">Unsupported format element: <xsl:value-of select="$fet"/></xsl:message>
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>



<xsl:template name="formatElement_to_VariableDeclaration">
	<xsl:param name="fe" required="yes"/>
	<xsl:variable name="fet" select="$fe/@type"/>
	<xsl:variable name="id" select="$fe/@id"/>
	<xsl:choose>
		<xsl:when test="$fet eq 'ubyte' or $fet eq 'atype' or $fet eq 'byte' or $fet eq 'ushort' or $fet eq 'short' or $fet eq 'int' or $fet eq 'branchoffset' or $fet eq 'branchoffset_wide'">
	val <xsl:value-of select="$id"/> : Int</xsl:when>
		<xsl:when test="$fet eq 'ushort_cp_index→referenceType'">
	val <xsl:value-of select="$id"/> : ReferenceType</xsl:when>
		<xsl:when test="$fet eq 'ushort_cp_index→objectType'">
	val <xsl:value-of select="$id"/> : ObjectType</xsl:when>
		<xsl:when test="$fet eq 'ubyte_cp_index→constant_value' or $fet eq 'ushort_cp_index→constant_value'"><!-- used by ldc, ldc_w, ldc2 -->
	val <xsl:value-of select="$id"/> : ConstantValue[_]</xsl:when>
		<xsl:when test="$fet eq 'ushort_cp_index→fieldref'"><!-- used by get/put field/static -->
	val declaringClass : ObjectType, // Recall, if we have "Object[] os = ...; os.length" then os.length is translated to the special arraylength instruction
	val name : String,
	val fieldType : FieldType</xsl:when>
		<xsl:when test="$fet eq 'ushort_cp_index→call_site_specifier'">
	// TODO [Java7] "invokedynamic" - resolve valid index into the bootstrap_methods array of the bootstrap method table
	val name : String,
	val methodDescriptor : MethodDescriptor // an interface or class type to be precise</xsl:when>
		<xsl:when test="$fet eq 'ushort_cp_index→methodref' or $fet eq 'ushort_cp_index→interface_methodref'">
	val declaringClass : ReferenceType, // an interface or class type to be precise
	val name : String, // an interface or class type to be precise
	val methodDescriptor : MethodDescriptor</xsl:when>
		<xsl:when test="name($fe) eq 'list'">
	val <xsl:value-of select="$id"/> : IndexedSeq[<xsl:call-template name="toVariableTypes"><xsl:with-param name="fets" select="$fe/opal:el/@type"/></xsl:call-template>]
		</xsl:when>

	<!-- If we would be able to use schema validation, then we would not require the following check. -->
		<xsl:otherwise>
			<xsl:message terminate="yes">Unsupported format element: <xsl:value-of select="$fe"/></xsl:message>
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>



<xsl:template name="toVariableTypes">
	<xsl:param name="fets" required="yes"/>
	<xsl:choose>
		<xsl:when test="count($fets) eq 1"><xsl:call-template name="formatElementType_to_VariableType"><xsl:with-param name="fet" select="$fets[position() eq 1]"/></xsl:call-template></xsl:when>
		<xsl:otherwise>(<xsl:for-each select="$fets"><xsl:call-template name="formatElementType_to_VariableType"><xsl:with-param name="fet" select="." /></xsl:call-template><xsl:if test="position() != last()">, </xsl:if></xsl:for-each>)</xsl:otherwise>
	</xsl:choose>
</xsl:template>



<xsl:template name="formatElementType_to_VariableType">
	<!-- Currently, only usable in combination with processing "list" format elements
		 where the primitive types are int or branchoffset_wide
	-->
	<xsl:param name="fet" required="yes"/>
	<xsl:choose>
		<xsl:when test="$fet eq 'int' or $fet eq 'branchoffset_wide'">Int</xsl:when>
		<!-- If we would be able to use schema validation, then we would not require the following check. -->
		<xsl:otherwise>
			<xsl:message terminate="yes">Unsupported format type: <xsl:value-of select="$fet"/></xsl:message>
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>



<xsl:template name="toList">
    <xsl:param name="elems" required="yes"/>
    <xsl:for-each select="$elems"><xsl:value-of select="."/> :: </xsl:for-each> Nil
</xsl:template>

</xsl:stylesheet>
