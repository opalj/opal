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
import org.opalj.bdl.bDL.IssueMessageElement
import org.opalj.bdl.bDL.IssueCategoryElement
import org.opalj.bdl.bDL.IssuePackageElement
import org.opalj.bdl.bDL.IssueClassElement
import org.opalj.bdl.bDL.IssueKindElement
import org.opalj.bdl.bDL.IssueRelevanceElement
import org.opalj.bdl.bDL.IssueMethodElement
import org.opalj.bdl.ui.outline.BDLOutlineTreeProvider
import org.opalj.bdl.bDL.ModelContainer
import org.opalj.bdl.bDL.IssueSuppressComment
import org.opalj.bdl.bDL.IssueMethodDefinition
import org.opalj.bdl.bDL.MethodTypes

/**
 * Provides labels for EObjects.
 * 
 * See https://www.eclipse.org/Xtext/documentation/304_ide_concepts.html#label-provider
 */
class BDLLabelProvider extends org.eclipse.xtext.ui.label.DefaultEObjectLabelProvider {

	// limits the length of most returned Strings
	private static int defaultMaxLength = 42;

	@Inject
	new(org.eclipse.emf.edit.ui.provider.AdapterFactoryLabelProvider delegate) {
		super(delegate);
	}

	// Labels and icons can be computed like this:
	
	def text (IssueElement ele){
		var String msg = "";
		
		if ( (ele.message != null) && (ele.message.message != null) && (ele.message.message.length > 0) )
			msg = ": "+ getPreview(ele.message.message, defaultMaxLength);

		return getFromArray(ele.name,",") + msg;
	}

	def image(ModelContainer element){
		return "library_obj.png";
	}

	def image(IssueMessageElement msg){
		return "message_info.png"
	}
	def text (IssueMessageElement msg){
		return getPreview(msg.message, defaultMaxLength);
	}

	def image(IssueCategoryElement element) {
		return "debug_persp.png";
	}
	def text(IssueCategoryElement element){
		return getPreview(getFromArray(element.elements, ","), defaultMaxLength);
	}

	def image(IssuePackageElement element){
		return "package.png"
	}
	def text(IssuePackageElement element){
		return getPreview(element.getPackage, defaultMaxLength);
	}
	def image(IssueClassElement element){
		return "class.png"
	}
	def text(IssueClassElement element){
		return getPreview(element.getClass_, defaultMaxLength);
	}

	def image(IssueKindElement element){
		return "det_pane_hide.png";
	}
	def text(IssueKindElement element){
		var String ret ="";
		for (String kind : element.elements){
			if (ret != "") ret +=","
			ret += kind;
		}
		return getPreview(ret, defaultMaxLength);
	}
	
	def image(IssueRelevanceElement element){
		return "message_warning.png";
	}
	def text(IssueRelevanceElement element){
		return element.relevance.toString;
	}
	
	def image(IssueMethodElement element){
		return "public_co.png";
	}
	def text(IssueMethodElement element){
		var IssueMethodDefinition definition = element.getDefinition;
		var String ret = getFromArray( definition.accessFlags, " ");
		
		ret += " "+ text(definition.returnType);
		ret += " "+ definition.name;
		var paras = '';
		for (var i  = 0; i < definition.parameter.length; i++){
			if (paras != '') paras+=",";
			paras += text(definition.parameter.get(i));
		}
		ret += "("+ paras +")";
		
		return getPreview(ret, defaultMaxLength);
	}

	def image(IssueSuppressComment element){
		return "readwrite_obj.png"
	}
	def text(IssueSuppressComment element){
		return "Comment: "+ getPreview(element.value, defaultMaxLength);
	}
	
	def String text(MethodTypes type){
		if ((type.void != null) 	&& (type.void.length > 0)) 
			return type.void;
		if ((type.baseType != null) && (type.baseType.length > 0)) 
			return type.baseType;
		if ((type.object != null) 	&& (type.object.length > 0)) 
			return type.ref.simpleName;
		if ((type.array != null) 	&& (type.array.length > 0)){
			//type.dimensions.length
			var String t = text(type.getSubType) as String;
			for (var int i = 0; i < type.dimensions.length; i++) t+= "[]"
			return t
		}
		return "<unknown>"
	}

	def String getPreview(String in, int maxLength){
		if (in.length <= maxLength) return in;
		
		var String ret = in.substring(0, maxLength-3) + "...";		
		
		return ret;
	}

	def getFromArray(String[] ele, String seperator){
		var t = '';
		for (var i  = 0; i < ele.length; i++){
			if (t != '') t+=seperator;
			t += ele.get(i);
		}
		return t;
	}






	// provides the images for the custom elements in the outline
	def image(String element){
		if (element == BDLOutlineTreeProvider.KEY_FILTERBY_CATEGORY)
			return "debuglast_co.png";
		if (element == BDLOutlineTreeProvider.KEY_FILTERBY_CLASS)
			return "class.png";
		if (element == BDLOutlineTreeProvider.KEY_FILTERBY_KINDS)
			return "det_pane_hide.png";
		if (element == BDLOutlineTreeProvider.KEY_FILTERBY_PACKAGES)
			return "package.png";
		if (element == BDLOutlineTreeProvider.KEY_FILTERBY_RELEVANCE)
			return "message_warning.png";
		if (element == BDLOutlineTreeProvider.KEY_FILTERBY_TYPE)
			return "class.png";
	}
}
