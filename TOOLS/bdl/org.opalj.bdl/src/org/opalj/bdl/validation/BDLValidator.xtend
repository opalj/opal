/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
// Author: Thorsten Jacobi

package org.opalj.bdl.validation

import org.opalj.bdl.bDL.ParameterKeyValueElement
import org.opalj.bdl.bDL.BDLPackage

import org.eclipse.xtext.validation.Check
import org.opalj.bdl.bDL.ParameterContainer
import org.opalj.bdl.bDL.ParameterElement
import org.opalj.bdl.bDL.IssueMethodDefinition
import org.opalj.bdl.bDL.IssueCategoryElement
import org.opalj.bdl.bDL.IssueCategories
import org.opalj.bdl.bDL.IssueKindElement
import org.eclipse.emf.common.util.EList
import org.eclipse.emf.ecore.EObject
import org.eclipse.emf.ecore.EStructuralFeature

/**
 * This class contains custom validation rules. 
 *
 * See https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#validation
 */
class BDLValidator extends AbstractBDLValidator {

	public static val INVALID_VALUE = 'invalidValue'
	public static val INVALID_NAME  = 'invalidName'


	// check to ensure a correct value for a parameter ending with "time" or "factor"
	@Check
	def checkKeyValueParameter(ParameterKeyValueElement element){
		if (element.getName().toLowerCase.endsWith("time"))
			if (!element.getValue().toLowerCase.matches("[0-9]+m?s|infinity"))
				error('Parameter value should be a valid time ending with "s" or "ms"!',
					BDLPackage.Literals.PARAMETER_KEY_VALUE_ELEMENT__VALUE,
					INVALID_VALUE
				);
		if (element.getName().toLowerCase.endsWith("factor"))
			if (!element.getValue().toLowerCase.matches("[0-9]+(\\.[0-9]+)?|infinity"))
				error('Parameter value should be a valid factor!',
					BDLPackage.Literals.PARAMETER_KEY_VALUE_ELEMENT__VALUE,
					INVALID_VALUE
				);
	} 
	
	// check to ensure that each parameter name is unique TODO: maybe ignore case?
	@Check
	def check(ParameterContainer container){
		for (ParameterElement e: container.getElements()){
			for (ParameterElement other: container.getElements()){
				if (other != e)
					if (e.name.equals(other.name)) 
						error("Every parameter can only occur once!", other, BDLPackage.Literals.PARAMETER_ELEMENT__NAME, INVALID_NAME);
			}
		}
	}
	
	
	@Check
	def check(IssueCategoryElement element){
		for (IssueCategories cat: element.elements)
			for (IssueCategories other: element.elements)
				if (cat != other)
					if (
						( (cat.bug==other.bug) && (cat.bug != null))||
						( (cat.comprehensibility==other.comprehensibility) && (cat.comprehensibility != null))||
						( (cat.smell==other.smell) && (cat.smell != null))||
						( (cat.performance==other.performance) && (cat.performance != null))
					)
						error("Every category can only occur once!", element, BDLPackage.Literals.ISSUE_CATEGORY_ELEMENT__NAME, INVALID_NAME);
	}
	
	@Check
	def check(IssueKindElement element){
		checkStringList(element.elements,
			element,
			BDLPackage.Literals.ISSUE_KIND_ELEMENT__ELEMENTS,
			"Every kind can only occur once!"
		);
	}
	@Check
	def check(IssueMethodDefinition method){
		checkStringList(method.accessFlags,
			method,
			BDLPackage.Literals.ISSUE_METHOD_DEFINITION__ACCESS_FLAGS,
			"Every access flag can only occur once!"
		);
	}
	
	def checkStringList( EList<String> list, EObject src, EStructuralFeature feature, String message){
		for (var int first = 0; first < list.length; first++){
			for (var second = first +1; second < list.length; second++)
				if (list.get(first).equals(list.get(second)))
					error(message + ' ("'+ list.get(first) +'")', src, feature, INVALID_NAME);
		}
	}
}
