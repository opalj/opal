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

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightingConfiguration;
import org.eclipse.xtext.ui.editor.syntaxcoloring.IHighlightingConfigurationAcceptor;
import org.eclipse.xtext.ui.editor.utils.TextStyle;

public class BDLHighlightingConfiguration implements IHighlightingConfiguration {

    public static final String DEFAULT_ID = "default";
    public static final String Style_Analysis_ID = "Style_Analysis";
    public static final String Style_Parameters_ID = "Style_Parameters";
    public static final String Style_Parameter_KEY_ID = "Style_Parameters_KEY";
    public static final String Style_Parameter_VALUE_ID = "Style_Parameters_VALUE";
    public static final String Style_Issues_ID = "Style_Issues";
    public static final String Style_Issues_TYPE_ID = "Style_Issues_TYPE";
    public static final String Style_Issues_KEY_ID = "Style_Issues_KEY";
    public static final String Style_Issues_VALUE_ID = "Style_Issues_VALUE";
    public static final String Style_Method_TYPES_ID = "Style_Method_TYPES";
    public static final String Style_Issues_MESSAGE_ID = "Style_Issues_MESSAGE";
    
	@Override
	public void configure(IHighlightingConfigurationAcceptor acceptor) {
		// TODO Auto-generated method stub
		acceptor.acceptDefaultHighlighting(DEFAULT_ID, "Default", defaultTextStyle());
		acceptor.acceptDefaultHighlighting(Style_Analysis_ID, "Analysis", 
				createTextStyle(20, SWT.BOLD, new RGB(128, 0, 70) ));
		acceptor.acceptDefaultHighlighting(Style_Parameters_ID, "Parameters", 
				createTextStyle(18, 0, new RGB(170, 0, 94) ));
		acceptor.acceptDefaultHighlighting(Style_Issues_ID, "Issues", 
				createTextStyle(18, 0, new RGB(170, 0, 94) ));
		acceptor.acceptDefaultHighlighting(Style_Parameter_KEY_ID, "Parameter name", 
				createTextStyle(10, 0, new RGB(100, 0, 200) ));
		acceptor.acceptDefaultHighlighting(Style_Parameter_VALUE_ID, "Parameter value", 
				createTextStyle(10, SWT.ITALIC, new RGB(1, 1, 1) ));
		acceptor.acceptDefaultHighlighting(Style_Issues_KEY_ID, "Issue attribute", 
				createTextStyle(10, 0, new RGB(100, 0, 200) ));
		acceptor.acceptDefaultHighlighting(Style_Issues_VALUE_ID, "Issue attribtue value", 
				createTextStyle(10, SWT.ITALIC, new RGB(1, 1, 1) ));
		acceptor.acceptDefaultHighlighting(Style_Issues_TYPE_ID, "Issue type", 
				createTextStyle(10, 0, new RGB(100, 0, 200) ));
		acceptor.acceptDefaultHighlighting(Style_Method_TYPES_ID, "Method types", 
				createTextStyle(10, 0, new RGB(255, 0, 127) ));
		acceptor.acceptDefaultHighlighting(Style_Issues_MESSAGE_ID, "Issue message", 
				createTextStyle(10, SWT.ITALIC, new RGB(175, 75, 125) ));
		
	}

    protected TextStyle defaultTextStyle() {
        TextStyle textStyle = new TextStyle();
        textStyle.setColor(new RGB(1, 1, 1));
        
        textStyle.setFontData(getDefaultFont(10));
        
        return textStyle;
    }
    
    private FontData[] getDefaultFont(int adjustSize){
    	FontData[] fd = JFaceResources.getDefaultFont().getFontData();

    	if (adjustSize > 0)
	        for (FontData t : fd){
	        	 t.setHeight(adjustSize);
	        }

        return fd;
    }
    
    protected TextStyle createTextStyle(int size, int style,RGB color){
    	TextStyle ret  = new TextStyle();
    	ret.setFontData(getDefaultFont(size));
    	ret.setColor(color);
    	ret.setStyle(style);
    	return ret;
    }
}
