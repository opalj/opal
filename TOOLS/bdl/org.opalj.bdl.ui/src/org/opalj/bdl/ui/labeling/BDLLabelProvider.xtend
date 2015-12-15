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

package org.opalj.bdl.ui.labeling

import com.google.inject.Inject
import org.opalj.bdl.bDL.IssueElement
//import org.opalj.bdl.bDL.IssueTypes

/**
 * Provides labels for EObjects.
 * 
 * See https://www.eclipse.org/Xtext/documentation/304_ide_concepts.html#label-provider
 */
class BDLLabelProvider extends org.eclipse.xtext.ui.label.DefaultEObjectLabelProvider {

	@Inject
	new(org.eclipse.emf.edit.ui.provider.AdapterFactoryLabelProvider delegate) {
		super(delegate);
	}

	// Labels and icons can be computed like this:
	
	def text (IssueElement ele){
		var String msg = "";
		
		if ( (ele.message != null) && (ele.message.length > 0) )
			msg = ": "+ ele.message;

		return getFromArray(ele.name,",") + msg;
	}
	
	/*def text(IssueTypes ele){
		return rn(ele.deadEdgesAnalysis) + rn(ele.unusedLocalVariables) + rn(ele.guardedAndUnguardedAccessAnalysis) + rn(ele.unusedMethodsAnalysis) + rn(ele.uselessComputationsAnalysis);
	}*/
	def rn(String txt){
		if (txt == null) return "";
		return txt;
	}
	/*def text(ModelContainer ele){
		ele.analysis.text + getFromArray(ele.analysis.name.titles);
	}*/
	
	/*def text(AnalysisElement ele){
		'name'
	}*/
	
	/*def text(ParameterContainer ele){
		ele.parameters.name;
	}*/
	
	def getFromArray(String[] ele, String seperator){
		var t = '';
		for (var i  = 0; i < ele.length; i++){
			if (t != '') t+=seperator;
			t += ele.get(i);
		}
		return t;
	}
//	def text(Greeting ele) {
//		'A greeting to ' + ele.name
//	}
//
//	def image(Greeting ele) {
//		'Greeting.gif'
//	}
}
