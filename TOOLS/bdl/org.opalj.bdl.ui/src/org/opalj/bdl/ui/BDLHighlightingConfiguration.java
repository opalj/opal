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
