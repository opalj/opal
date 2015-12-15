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

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.impl.CompositeNodeWithSemanticElement;
import org.eclipse.xtext.nodemodel.impl.LeafNode;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightedPositionAcceptor;
import org.eclipse.xtext.ui.editor.syntaxcoloring.ISemanticHighlightingCalculator;
import org.opalj.bdl.bDL.IssueCategoryElement;
import org.opalj.bdl.bDL.IssueClassElement;
import org.opalj.bdl.bDL.IssueElement;
import org.opalj.bdl.bDL.IssueKindElement;
import org.opalj.bdl.bDL.IssuePackageElement;
import org.opalj.bdl.bDL.IssueRelevanceElement;
import org.opalj.bdl.bDL.IssuesTitleElement;
import org.opalj.bdl.bDL.ModelContainer;
import org.opalj.bdl.bDL.ParameterContainer;
import org.opalj.bdl.bDL.ParameterKeyElement;
import org.opalj.bdl.bDL.ParameterKeyValueElement;

public class BDLSemanticHighlightingCalculator implements ISemanticHighlightingCalculator {

	@Override
	public void provideHighlightingFor(XtextResource resource, IHighlightedPositionAcceptor acceptor) {
		// TODO Auto-generated method stub
		if (resource == null || resource.getParseResult() == null)
		    return;

		INode root = resource.getParseResult().getRootNode();
		acceptor.addPosition(root.getOffset(), root.getLength(), 
	    		BDLHighlightingConfiguration.DEFAULT_ID);
		for (INode node : root.getAsTreeIterable()) {

			boolean isLeaf = (node instanceof LeafNode);
			EObject semanticElement = node.getSemanticElement();//org.eclipse.xtext.nodemodel.util.NodeModelUtils.findActualSemanticObjectFor(node);

			int length2 = node.getText().replace("\r", "").replace("\n", "").length();
			if ((semanticElement != null) && (node.getLength() > 0) && (length2 > 0))  {
				if (isLeaf){
					if (semanticElement instanceof ModelContainer){
						acceptor.addPosition(node.getOffset(), node.getLength(),
								BDLHighlightingConfiguration.Style_Analysis_ID);
					}else
					if (semanticElement instanceof ParameterContainer)// && isLeaf)
					{
						acceptor.addPosition(node.getOffset(), "parameters".length(),
					    		BDLHighlightingConfiguration.Style_Parameters_ID);
					}
					else if (semanticElement instanceof IssuesTitleElement){
						acceptor.addPosition(node.getOffset(), node.getLength(),
					    		BDLHighlightingConfiguration.Style_Issues_ID);
					}
					else if ((semanticElement instanceof IssueElement) && (length2 > 1))
						acceptor.addPosition(node.getOffset(), node.getLength(),
					    		BDLHighlightingConfiguration.Style_Issues_TYPE_ID);
				}else{
					if (semanticElement instanceof ParameterKeyElement)
						acceptor.addPosition(node.getOffset(), node.getLength(),
				    		BDLHighlightingConfiguration.Style_Parameter_KEY_ID);
					else if (semanticElement instanceof ParameterKeyValueElement){
						if (node instanceof CompositeNodeWithSemanticElement){
							HighlightSeperatedNode(node, BDLHighlightingConfiguration.Style_Parameter_KEY_ID, BDLHighlightingConfiguration.Style_Parameter_VALUE_ID, acceptor);
						}
					}else if (
								(semanticElement instanceof IssueCategoryElement)||
								(semanticElement instanceof IssueKindElement)||
								(semanticElement instanceof IssueRelevanceElement)||
								(semanticElement instanceof IssuePackageElement)||
								(semanticElement instanceof IssueClassElement)
							 )
					{
						if ((node.getParent().getSemanticElement() == null) || (node.getParent().getSemanticElement().eClass() != semanticElement.eClass()))
						HighlightSeperatedNode(node, BDLHighlightingConfiguration.Style_Issues_KEY_ID, BDLHighlightingConfiguration.Style_Issues_VALUE_ID, acceptor);
					}
					/*else
					acceptor.addPosition(node.getOffset(), node.getLength(), 
				    		BDLHighlightingConfiguration.DEFAULT_ID);*/
				}
			}

		}
	}
	
	protected void HighlightSeperatedNode(INode node, String key, String value, IHighlightedPositionAcceptor acceptor){
		int index = node.getText().indexOf(":")-2;
		if (index < 0) index = node.getText().indexOf("=")-2;
		
		if (index <= 0){// assume a parameter without value
			acceptor.addPosition(node.getOffset(), node.getLength(),key);
		}else{
			acceptor.addPosition(node.getOffset(), index,key);
			acceptor.addPosition(node.getOffset()+index, 1, BDLHighlightingConfiguration.DEFAULT_ID);
			acceptor.addPosition(node.getOffset()+index+1, node.getLength()-index-1,value);
		}
	}

}
