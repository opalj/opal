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

package org.opalj.bdl.formatting

import org.eclipse.xtext.formatting.impl.AbstractDeclarativeFormatter
import org.eclipse.xtext.formatting.impl.FormattingConfig
import com.google.inject.Inject
import org.opalj.bdl.services.BDLGrammarAccess

/** 
 * This class contains custom formatting declarations.
 * See https://www.eclipse.org/Xtext/documentation/303_runtime_concepts.html#formatting
 * on how and when to use it.
 * Also see {@link org.eclipse.xtext.xtext.XtextFormattingTokenSerializer} as an example
 */
public class BDLFormatter extends AbstractDeclarativeFormatter {
	@Inject package extension BDLGrammarAccess

	// It's usually a good idea to activate the following three statements.
	// They will add and preserve newlines around comments
	override protected void configureFormatting(FormattingConfig c) {
		var gram = grammarAccess as BDLGrammarAccess 

// It's usually a good idea to activate the following three statements.
// They will add and preserve newlines around comments
		c.setLinewrap(0, 1, 2).before(SL_COMMENTRule)
		c.setLinewrap(0, 1, 2).before(ML_COMMENTRule)
		c.setLinewrap(0, 1, 1).after(ML_COMMENTRule)
		
		c.setLinewrap(1, 1, 1).after(gram.analysisElementRule)
		c.setLinewrap(1, 1, 1).after(gram.parametersElementRule)
			c.setLinewrap(1, 1, 1).after(gram.parameterElementRule)
			c.setIndentationIncrement.before(gram.parameterElementRule)
			c.setIndentationDecrement.after(gram.parameterElementRule)
		c.setLinewrap(1, 1, 1).after(gram.issuesTitleElementRule)
		c.setLinewrap(1, 1, 1).around(gram.issueElementRule)
			c.setIndentationIncrement.before(gram.issueCategoryElementRule)
			c.setLinewrap(1, 1, 1).before(gram.issueCategoryElementRule);
			c.setLinewrap(1, 1, 1).before(gram.issueKindElementRule);
			c.setLinewrap(1, 1, 1).before(gram.issueRelevanceElementRule);
			c.setLinewrap(1, 1, 1).before(gram.issuePackageElementRule);
			c.setLinewrap(1, 1, 1).around(gram.issueClassElementRule);
			c.setIndentationDecrement.after(gram.issueClassElementRule)
	}
	
}
