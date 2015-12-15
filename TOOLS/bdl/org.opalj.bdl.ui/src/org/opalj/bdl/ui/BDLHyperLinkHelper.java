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

package org.opalj.bdl.ui;

import org.eclipse.xtext.common.types.JvmType;
import org.eclipse.xtext.common.types.access.IJvmTypeProvider;
import org.eclipse.xtext.common.types.xtext.ui.TypeAwareHyperlinkHelper;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.hyperlinking.IHyperlinkAcceptor;
import org.opalj.bdl.bDL.IssueClassElement;
import org.opalj.bdl.bDL.IssueElement;
import org.opalj.bdl.bDL.IssuePackageElement;

import com.google.inject.Inject;

@SuppressWarnings("restriction")
public class BDLHyperLinkHelper extends TypeAwareHyperlinkHelper {
	@Inject
    private IJvmTypeProvider.Factory      jvmTypeProviderFactory;
	
	@Override
    public void createHyperlinksByOffset(XtextResource resource, int offset, IHyperlinkAcceptor acceptor) {
		INode node = NodeModelUtils.findLeafNodeAtOffset(resource.getParseResult().getRootNode(), offset);
		
		if (
				(node != null) && 
				(node.getSemanticElement() != null) &&
				(
						(node.getSemanticElement() instanceof IssuePackageElement)||
						(node.getSemanticElement() instanceof IssueClassElement)
				)
		   )
		{
			INode parent = node.getParent();
			while ((parent != null) && (!(parent.getSemanticElement() instanceof IssueElement)))
					parent = parent.getParent();
			
			if (parent != null){
				IssueElement issue = (IssueElement)parent.getSemanticElement();
			
				IssuePackageElement pPackage 	= issue.getPackage();
				IssueClassElement   pClass  	= issue.getClass_();

				if ( (pPackage != null) && (pClass != null) ){
					
					String fullName = pPackage.getPackage().replace("/", ".") +"."+ pClass.getClass_();
					IJvmTypeProvider typeProvider = jvmTypeProviderFactory.findOrCreateTypeProvider(resource.getResourceSet());
					JvmType pType = typeProvider.findTypeByName(fullName);
					if (pType != null)
						createHyperlinksTo(resource, node.getParent(), pType, acceptor);
					
					return;
				}
			}
		}
		
		
		super.createHyperlinksByOffset(resource, offset, acceptor);
	}
}
