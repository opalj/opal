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

  Author: Thomas Schlosser
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
	xmlns:opal="http://www.opal-project.de/BAT/10.2011/JVMInstructions">

	<xsl:param name="debug" select="'false'" />

	<xsl:output media-type="text/plain" encoding="UTF-8" method="text" />

	<xsl:include href="../../Common.xsl"/>

<xsl:template match="/">
	<xsl:call-template name="copyright">
		<xsl:with-param name="sourceFile">GenerateInstructionDependencyExtractor.xsl</xsl:with-param>
	</xsl:call-template>
package de.tud.cs.st.bat.resolved
package dependency

import annotation.switch
import de.tud.cs.st.util.ControlAbstractions.repeat
import de.tud.cs.st.bat.resolved.reader.CodeBinding
import DependencyType._

/**
 * This class extracts all dependencies out of the given instructions.
 *
 * @version [Generated] <xsl:value-of select="current-dateTime()"/>
 */
trait InstructionDependencyExtractor extends CodeBinding {

    val builder: DependencyBuilder

    /**
     * Factory method that transforms an array of instructions into an array of dependencies.
     */
    def process(methodId: Int, instructions: Code) {
        import builder._

        for (instr ← instructions if instr != null) {
            (instr.opcode: @switch) match {<xsl:for-each select="/opal:instructions/opal:instruction">
                                   <xsl:call-template name="opal:instruction" />
                               </xsl:for-each>
                case _ ⇒
            }
        }
    }
}
</xsl:template>

<xsl:template name="opal:instruction"><!-- current context: an opal:instruction -->
    <xsl:if test="count(opal:stdInstructionParameters(.)[./@type eq 'ushort_cp_index→referenceType' or ./@type eq 'ushort_cp_index→objectType' or ./@type eq 'ushort_cp_index→fieldref' or ./@type eq 'ushort_cp_index→call_site_specifier' or ./@type eq 'ushort_cp_index→methodref' or ./@type eq 'ushort_cp_index→interface_methodref'])>0">
                case <xsl:value-of select="upper-case(string(@opcode))"/> ⇒ {
                    val <xsl:value-of select="upper-case(string(@name))"/>(<xsl:for-each select="opal:stdInstructionParameters(.)">
                            <xsl:call-template name="generate_case_parameters"><xsl:with-param name="fe" select="."/></xsl:call-template><xsl:if test="position() != last()">, </xsl:if>
                        </xsl:for-each>) = instr.asInstanceOf[<xsl:value-of select="upper-case(string(@name))"/>]<xsl:for-each select="opal:stdInstructionParameters(.)">
                        <xsl:call-template name="generate_case_impl"><xsl:with-param name="fe" select="."/></xsl:call-template>
                    </xsl:for-each>
                }
</xsl:if>
</xsl:template>



<xsl:template name="generate_case_parameters">
	<xsl:param name="fe" required="yes"/>
	<xsl:variable name="fet" select="$fe/@type"/>
	<xsl:variable name="id" select="$fe/@id"/>
	<xsl:choose>
		<xsl:when test="$fet eq 'ubyte' or $fet eq 'atype' or $fet eq 'byte' or $fet eq 'ushort' or $fet eq 'short' or $fet eq 'int' or $fet eq 'branchoffset' or $fet eq 'branchoffset_wide'"><xsl:value-of select="$id"/></xsl:when>
		<xsl:when test="$fet eq 'ushort_cp_index→referenceType'"><xsl:value-of select="$id"/></xsl:when>
		<xsl:when test="$fet eq 'ushort_cp_index→objectType'"><xsl:value-of select="$id"/></xsl:when>
		<xsl:when test="$fet eq 'ubyte_cp_index→constant_value' or $fet eq 'ushort_cp_index→constant_value'"><!-- used by ldc, ldc_w, ldc2 --><xsl:value-of select="$id"/></xsl:when>
		<xsl:when test="$fet eq 'ushort_cp_index→fieldref'"><!-- used by get/put field/static -->declaringClass, name, fieldType</xsl:when>
		<xsl:when test="$fet eq 'ushort_cp_index→call_site_specifier'">name, methodDescriptor</xsl:when>
		<xsl:when test="$fet eq 'ushort_cp_index→methodref' or $fet eq 'ushort_cp_index→interface_methodref'">declaringClass, name, methodDescriptor</xsl:when>
		<xsl:when test="name($fe) eq 'list'"><xsl:value-of select="$id"/></xsl:when>

	<!-- If we would be able to use schema validation, then we would not require the following check. -->
		<xsl:otherwise>
			<xsl:message terminate="yes">Unsupported format element: <xsl:value-of select="$fe"/></xsl:message>
		</xsl:otherwise>
	</xsl:choose>
</xsl:template>



<xsl:template name="generate_case_impl">
	<xsl:param name="fe" required="yes"/>
	<xsl:variable name="fet" select="$fe/@type"/>
	<xsl:variable name="id" select="$fe/@id"/>
	<xsl:choose>
		<xsl:when test="$fet eq 'ubyte' or $fet eq 'atype' or $fet eq 'byte' or $fet eq 'ushort' or $fet eq 'short' or $fet eq 'int' or $fet eq 'branchoffset' or $fet eq 'branchoffset_wide'"></xsl:when>
		<xsl:when test="$fet eq 'ushort_cp_index→referenceType'"><!-- used by anewarray, checkcast, instanceof, multianewarray -->
                    addDependency(methodId, getID(<xsl:value-of select="$id"/>), <xsl:choose>
						<xsl:when test="../../../@name eq 'anewarray'">CREATES_ARRAY_OF_TYPE</xsl:when>
						<xsl:when test="../../../@name eq 'checkcast'">CASTS_INTO</xsl:when>
						<xsl:when test="../../../@name eq 'instanceof'">CHECKS_INSTANCEOF</xsl:when>
						<xsl:when test="../../../@name eq 'multianewarray'">CREATES_ARRAY_OF_TYPE</xsl:when>
						<xsl:otherwise>USED_TYPE</xsl:otherwise></xsl:choose>)</xsl:when>
		<xsl:when test="$fet eq 'ushort_cp_index→objectType'"><!-- used by new -->
                    addDependency(methodId, getID(<xsl:value-of select="$id"/>), <xsl:choose>
						<xsl:when test="../../../@name eq 'new'">CREATES</xsl:when>
						<xsl:otherwise>USED_TYPE</xsl:otherwise></xsl:choose>)</xsl:when>
		<xsl:when test="$fet eq 'ubyte_cp_index→constant_value' or $fet eq 'ushort_cp_index→constant_value'"></xsl:when><!-- used by ldc, ldc_w, ldc2 -->
		<xsl:when test="$fet eq 'ushort_cp_index→fieldref'"><!-- used by get/put field/static -->
                    addDependency(methodId, getID(declaringClass), USES_FIELD_DECLARING_TYPE)
                    addDependency(methodId, getID(declaringClass, name), <xsl:choose>
						<xsl:when test="starts-with(../../../@name,'get')">READS_FIELD</xsl:when>
						<xsl:when test="starts-with(../../../@name,'put')">WRITES_FIELD</xsl:when></xsl:choose>)
                    addDependency(methodId, getID(fieldType), <xsl:choose>
						<xsl:when test="starts-with(../../../@name,'get')">USES_FIELD_READ_TYPE</xsl:when>
						<xsl:when test="starts-with(../../../@name,'put')">USES_FIELD_WRITE_TYPE</xsl:when></xsl:choose>)</xsl:when>
		<xsl:when test="$fet eq 'ushort_cp_index→call_site_specifier'"><!-- used by invokedynamic --><!--
		 --><!--TODO: A call dependency to a method without declaring class makes not much sense
            addDependency(methodId, getID(name, methodDescriptor),METHOD_CALL)-->
                    methodDescriptor.parameterTypes foreach { parameterType ⇒ addDependency(methodId, getID(parameterType), USES_PARAMETER_TYPE) }
                    addDependency(methodId, getID(methodDescriptor.returnType), USES_RETURN_TYPE)</xsl:when>
		<xsl:when test="$fet eq 'ushort_cp_index→methodref'"><!-- used by invokespecial, invokestatic, invokevirtual -->
                    addDependency(methodId, getID(declaringClass), USES_METHOD_DECLARING_TYPE)
                    addDependency(methodId, getID(declaringClass, name, methodDescriptor), CALLS_METHOD)
                    methodDescriptor.parameterTypes foreach { parameterType ⇒ addDependency(methodId, getID(parameterType), USES_PARAMETER_TYPE) }
                    addDependency(methodId, getID(methodDescriptor.returnType), USES_RETURN_TYPE)</xsl:when>
        <xsl:when test="$fet eq 'ushort_cp_index→interface_methodref'"><!-- used by invokeinterface -->
                    addDependency(methodId, getID(declaringClass), USES_METHOD_DECLARING_TYPE)
                    addDependency(methodId, getID(declaringClass, name, methodDescriptor), CALLS_INTERFACE_METHOD)
                    methodDescriptor.parameterTypes foreach { parameterType ⇒ addDependency(methodId, getID(parameterType), USES_PARAMETER_TYPE) }
                    addDependency(methodId, getID(methodDescriptor.returnType), USES_RETURN_TYPE)</xsl:when>
		<xsl:when test="name($fe) eq 'list'"></xsl:when>

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

</xsl:stylesheet>