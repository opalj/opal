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
		<xsl:with-param name="sourceFile">GenerateInstructionDepExtractor.xsl</xsl:with-param>
	</xsl:call-template>
		
package de.tud.cs.st.bat.resolved
package dependency

import de.tud.cs.st.util.ControlAbstractions.repeat
import de.tud.cs.st.bat.resolved.reader.CodeBinding
import DependencyType._

/**
 * This class extracts all (non-primitive) dependencies out of the given instructions.
 *
 * @version [Generated] <xsl:value-of select="current-dateTime()"/>
 */
trait InstructionDepExtractor extends DependencyBinding with CodeBinding{

  val FIELD_AND_METHOD_SEPARATOR : String

  val builder: DepBuilder
  
  /**
   * Factory method that transforms an array of instructions into an array of dependencies.
   */
  def process(methodId: Option[Int], instructions : Code) {
    if(methodId == None){
      return
    }
    for (instr &lt;- instructions){
      if(instr != null){
        instr match{
        <xsl:for-each select="/opal:instructions/opal:instruction">
          <xsl:call-template name="opal:instruction" />
        </xsl:for-each>
          case _ => Nil
        }
      }
    }
  }

  protected def getID(className: String, field: Field_Info): Option[Int] =
    getID(className, field.name)
  protected def getID(targetType: { def toJava: String }, fieldName: String): Option[Int] =
    getID(getName(targetType), fieldName)
  protected def getID(className: String, fieldName: String): Option[Int] =
    getID(className + FIELD_AND_METHOD_SEPARATOR + fieldName)

  protected def getID(className: String, method: Method_Info): Option[Int] =
    getID(className, method.name, method.descriptor)
  protected def getID(targetType: { def toJava: String }, name: String, methodDescriptor: MethodDescriptor): Option[Int] =
    getID(getName(targetType), name, methodDescriptor)
  protected def getID(className: String, methodName: String, methodDescriptor: MethodDescriptor): Option[Int] =
    getID(className + FIELD_AND_METHOD_SEPARATOR + getMethodAsName(methodName, methodDescriptor))
  protected def getID(methodName: String, methodDescriptor: MethodDescriptor): Option[Int] =
    getID(getMethodAsName(methodName, methodDescriptor))
  protected def getMethodAsName(methodName: String, methodDescriptor: MethodDescriptor): String = {
    methodName + "(" + methodDescriptor.parameterTypes.map(pT => getName(pT)).mkString(", ") + ")"
  }

  protected def getID(obj: { def toJava: String }): Option[Int] =
    getID(getName(obj))
  protected def getID(name: String): Option[Int] =
    if (filter(name)) None else Some(builder.getID(name))

  protected def getName(obj: { def toJava: String }): String =
    obj.toJava
  
  protected def filter(name: String): Boolean
} 
</xsl:template>

<xsl:template name="opal:instruction">
	<!-- current context: an opal:instruction -->
	<xsl:if test="opal:numberOfInstructionParameters(.)>0">
	case <xsl:value-of select="upper-case(string(@name))"/>(<xsl:for-each select="opal:stdInstructionParameters(.)">
		<xsl:call-template name="generate_case_parameters"><xsl:with-param name="fe" select="."/></xsl:call-template><xsl:if test="position() != last()">, </xsl:if>
	</xsl:for-each>) =&gt; {
	<xsl:for-each select="opal:stdInstructionParameters(.)">
		<xsl:call-template name="generate_case_impl"><xsl:with-param name="fe" select="."/></xsl:call-template>
	</xsl:for-each>
	}</xsl:if>
</xsl:template>

<!-- 
            for (instr <- ca.code) {
              if (instr != null) {
                for (dep <- instr.dependencies) {
                  // (cType, name, methodDescriptor, eType)
                  dep match {
                    case ttd: ToTypeDependency =>
                      builder.addEdge(methodID, getID(ttd.targetType), ttd.dependencyType)
                    case tfd: ToFieldDependency =>
                      builder.addEdge(methodID, getID(tfd.targetType, tfd.fieldName), tfd.dependencyType)
                    case tmd: ToMethodDependency =>
                      tmd.targetType match {
                        case Some(targetType) =>
                          builder.addEdge(methodID, getID(targetType, tmd.methodName, tmd.methodDescriptor), tmd.dependencyType)
                          builder.addEdge(methodID, getID(tmd.methodDescriptor.returnType), USED_TYPE)
                          for (paramType <- tmd.methodDescriptor.parameterTypes) {
                            builder.addEdge(methodID, getID(paramType), USED_TYPE)
                          }
                        case None =>

                      }
                  }
                }
              }
            }

 -->

<xsl:template name="generate_case_parameters">
	<xsl:param name="fe" required="yes"/>
	<xsl:variable name="fet" select="$fe/@type"/>
	<xsl:variable name="id" select="$fe/@id"/>
	<xsl:choose>
		<xsl:when test="$fet eq 'ubyte' or $fet eq 'atype' or $fet eq 'byte' or $fet eq 'ushort' or $fet eq 'short' or $fet eq 'int' or $fet eq 'branchoffset' or $fet eq 'branchoffset_wide'">
	<xsl:value-of select="$id"/></xsl:when> <!-- val <xsl:value-of select="$id"/> : Int -->
		<xsl:when test="$fet eq 'ushort_cp_index→referenceType'">
	<xsl:value-of select="$id"/></xsl:when><!-- val <xsl:value-of select="$id"/> : ReferenceType -->
		<xsl:when test="$fet eq 'ushort_cp_index→objectType'">
	<xsl:value-of select="$id"/></xsl:when><!-- val <xsl:value-of select="$id"/> : ObjectType -->
		<xsl:when test="$fet eq 'ubyte_cp_index→constant_value' or $fet eq 'ushort_cp_index→constant_value'"><!-- used by ldc, ldc_w, ldc2 -->
	<xsl:value-of select="$id"/></xsl:when><!-- val <xsl:value-of select="$id"/> : ConstantValue[_] -->
		<xsl:when test="$fet eq 'ushort_cp_index→fieldref'"><!-- used by get/put field/static -->
	declaringClass, name, fieldType</xsl:when><!-- val declaringClass : ObjectType, // Recall, if we have "Object[] os = ...; os.length" then os.length is translated to the special arraylength instruction 
	val name : String,
	val fieldType : FieldType -->
		<xsl:when test="$fet eq 'ushort_cp_index→call_site_specifier'">
	name, methodDescriptor</xsl:when><!-- // TODO.... valid index into the bootstrap_methods array of the bootstrap method table 
	val name : String,
	val methodDescriptor : MethodDescriptor // an interface or class type to be precise -->
		<xsl:when test="$fet eq 'ushort_cp_index→methodref' or $fet eq 'ushort_cp_index→interface_methodref'">
	declaringClass, name, methodDescriptor</xsl:when><!-- val declaringClass : ObjectType, // an interface or class type to be precise
	val name : String, // an interface or class type to be precise
	val methodDescriptor : MethodDescriptor -->
		<xsl:when test="name($fe) eq 'list'">
	<xsl:value-of select="$id"/>
		</xsl:when><!-- val <xsl:value-of select="$id"/> : IndexedSeq[<xsl:call-template name="toVariableTypes"><xsl:with-param name="fets" select="$fe/opal:el/@type"/></xsl:call-template>] -->
	
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
		<xsl:when test="$fet eq 'ubyte' or $fet eq 'atype' or $fet eq 'byte' or $fet eq 'ushort' or $fet eq 'short' or $fet eq 'int' or $fet eq 'branchoffset' or $fet eq 'branchoffset_wide'"></xsl:when> <!-- val <xsl:value-of select="$id"/> : Int -->
		<xsl:when test="$fet eq 'ushort_cp_index→referenceType'">
	builder.addDep(methodId, getID(<xsl:value-of select="$id"/>),USED_TYPE)</xsl:when><!-- val <xsl:value-of select="$id"/> : ReferenceType -->
		<xsl:when test="$fet eq 'ushort_cp_index→objectType'">
	builder.addDep(methodId, getID(<xsl:value-of select="$id"/>),USED_TYPE)</xsl:when><!-- val <xsl:value-of select="$id"/> : ObjectType -->
		<xsl:when test="$fet eq 'ubyte_cp_index→constant_value' or $fet eq 'ushort_cp_index→constant_value'"><!-- used by ldc, ldc_w, ldc2 -->
	builder.addDep(methodId, getID(<xsl:value-of select="$id"/>.valueType),USED_TYPE)</xsl:when><!-- val <xsl:value-of select="$id"/> : ConstantValue[_] -->
		<xsl:when test="$fet eq 'ushort_cp_index→fieldref'"><!-- used by get/put field/static -->
	builder.addDep(methodId, getID(declaringClass, name), <xsl:choose>
	<xsl:when test="starts-with(../../../@name,'get')">FIELD_READ</xsl:when>
	<xsl:when test="starts-with(../../../@name,'put')">FIELD_WRITE</xsl:when></xsl:choose>)
	builder.addDep(methodId, getID(fieldType),USED_TYPE)</xsl:when><!-- val declaringClass : ObjectType, // Recall, if we have "Object[] os = ...; os.length" then os.length is translated to the special arraylength instruction 
	val name : String,
	val fieldType : FieldType -->
		<xsl:when test="$fet eq 'ushort_cp_index→call_site_specifier'">
	builder.addDep(methodId, getID(name, methodDescriptor),METHOD_CALL)
	for (paramType &lt;- methodDescriptor.parameterTypes) {
		builder.addDep(methodId, getID(paramType), USED_TYPE)
	}
	builder.addDep(methodId, getID(methodDescriptor.returnType),USED_TYPE)</xsl:when><!-- // TODO.... valid index into the bootstrap_methods array of the bootstrap method table 
	val name : String,
	val methodDescriptor : MethodDescriptor // an interface or class type to be precise -->
		<xsl:when test="$fet eq 'ushort_cp_index→methodref' or $fet eq 'ushort_cp_index→interface_methodref'">
	builder.addDep(methodId, getID(declaringClass, name, methodDescriptor),METHOD_CALL)
	builder.addDep(methodId, getID(declaringClass),USED_TYPE)
	for (paramType &lt;- methodDescriptor.parameterTypes) {
		builder.addDep(methodId, getID(paramType), USED_TYPE)
	}
	builder.addDep(methodId, getID(methodDescriptor.returnType),USED_TYPE)</xsl:when><!-- val declaringClass : ObjectType, // an interface or class type to be precise
	val name : String, // an interface or class type to be precise
	val methodDescriptor : MethodDescriptor -->
		<xsl:when test="name($fe) eq 'list'">
	//<xsl:value-of select="$id"/>
		</xsl:when><!-- val <xsl:value-of select="$id"/> : IndexedSeq[<xsl:call-template name="toVariableTypes"><xsl:with-param name="fets" select="$fe/opal:el/@type"/></xsl:call-template>] -->
	
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