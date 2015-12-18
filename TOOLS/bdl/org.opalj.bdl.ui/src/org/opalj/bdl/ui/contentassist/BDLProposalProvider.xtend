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
package org.opalj.bdl.ui.contentassist

import org.opalj.bdl.ui.contentassist.AbstractBDLProposalProvider
import org.eclipse.emf.ecore.EObject
import org.eclipse.xtext.RuleCall
import org.eclipse.xtext.ui.editor.contentassist.ContentAssistContext
import org.eclipse.xtext.ui.editor.contentassist.ICompletionProposalAcceptor
import org.opalj.bdl.services.BDLGrammarAccess
import com.google.inject.Inject
import org.eclipse.xtext.Keyword

/**
 * See https://www.eclipse.org/Xtext/documentation/304_ide_concepts.html#content-assist
 * on how to customize the content assistant.
 */
class BDLProposalProvider extends AbstractBDLProposalProvider {
	@Inject package extension BDLGrammarAccess grammarAccess
	
	override complete_AnalysisElement(EObject model, RuleCall ruleCall, ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		super.complete_AnalysisElement(model, ruleCall, context, acceptor)

  		acceptor.accept(createCompletionProposal("Analysis of ",context));
	}
	
	override complete_ParametersElement(EObject model, RuleCall ruleCall, ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		super.complete_ParametersElement(model, ruleCall, context, acceptor)
		
		acceptor.accept(createCompletionProposal("Parameters",context));
	}
	
	override complete_IssuesTitleElement(EObject model, RuleCall ruleCall, ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		super.complete_IssuesTitleElement(model, ruleCall, context, acceptor)
		
		acceptor.accept(createCompletionProposal("Issues",context));
	}

	override complete_ParameterElement(EObject model, RuleCall ruleCall, ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		//super.complete_ParameterElement(model, ruleCall, context, acceptor)
		
		acceptor.accept(createCompletionProposal("name=value",context));
		acceptor.accept(createCompletionProposal("name;",context));
	}
	
	override complete_IssueTypes(EObject model, RuleCall ruleCall, ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		super.complete_IssueTypes(model, ruleCall, context, acceptor)
		var gram = grammarAccess as BDLGrammarAccess 
		
		for (EObject ele: gram.issueTypesRule.alternatives.eContents)
			if (ele instanceof Keyword)
				acceptor.accept(createCompletionProposal( (ele as Keyword).value,context));		
	}
	
	override complete_IssueKinds(EObject model, RuleCall ruleCall, ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		super.complete_IssueKinds(model, ruleCall, context, acceptor)
		var gram = grammarAccess as BDLGrammarAccess 
		
		for (EObject ele: gram.issueKindsRule.alternatives.eContents)
			if (ele instanceof Keyword)
				acceptor.accept(createCompletionProposal( (ele as Keyword).value,context));				
	}
	
	override complete_AccessFlags(EObject model, RuleCall ruleCall, ContentAssistContext context, ICompletionProposalAcceptor acceptor) {
		super.complete_AccessFlags(model, ruleCall, context, acceptor)
		var gram = grammarAccess as BDLGrammarAccess 

		for (EObject ele: gram.accessFlagsRule.alternatives.eContents)
			if (ele instanceof Keyword)
				acceptor.accept(createCompletionProposal( (ele as Keyword).value,context));		
	}
	
}
