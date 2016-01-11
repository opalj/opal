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

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.xtext.ui.editor.IXtextEditorCallback;
import org.eclipse.xtext.ui.editor.hyperlinking.IHyperlinkHelper;
import org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightingConfiguration;
import org.eclipse.xtext.ui.editor.syntaxcoloring.ISemanticHighlightingCalculator;
import org.opalj.bdl.formatting.BDLFormatter;
import org.opalj.bdl.ui.labeling.BDLLabelProvider;

/**
 * Use this class to register components to be used within the IDE.
 */
public class BDLUiModule extends org.opalj.bdl.ui.AbstractBDLUiModule {
	public BDLUiModule(AbstractUIPlugin plugin) {
		super(plugin);
	}
	
	public Class<? extends IHighlightingConfiguration> bindIHighlightingConfiguration () {
	    return BDLHighlightingConfiguration.class;
	}
	
	public java.lang.Class<? extends ISemanticHighlightingCalculator> bindISemanticHighlightingCalculator(){
		return BDLSemanticHighlightingCalculator.class;
	}
	
	@Override
	public Class<? extends ILabelProvider> bindILabelProvider() {
		return BDLLabelProvider.class;
	}
	
	@Override
	public Class<? extends IHyperlinkHelper> bindIHyperlinkHelper() {
		return BDLHyperLinkHelper.class;
	}
	
	public Class<? extends org.eclipse.xtext.formatting.IFormatter> bindIFormatter() {
		return BDLFormatter.class;
	}
	
	@Override
	public Class<? extends IXtextEditorCallback> bindIXtextEditorCallback() {
		return org.eclipse.xtext.ui.editor.IXtextEditorCallback.NullImpl.class;
	}
}
