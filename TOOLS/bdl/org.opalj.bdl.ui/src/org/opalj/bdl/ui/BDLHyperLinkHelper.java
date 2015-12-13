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
		System.out.println("createHyperlinksByOffset");
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
			//System.out.println("inner");
			
			//System.out.println("parent: "+ node.getParent() +"\t"+ node.getSemanticElement());
			INode parent = node.getParent();
			while ((parent != null) && (!(parent.getSemanticElement() instanceof IssueElement)))
					parent = parent.getParent();
			
			if (parent != null){
				IssueElement issue = (IssueElement)parent.getSemanticElement();
			
				IssuePackageElement pPackage 	= issue.getPackage();
				IssueClassElement   pClass  	= issue.getClass_();
				
				//System.out.println("found\t"+ pPackage +"\t"+ pClass);
				if ( (pPackage != null) && (pClass != null) ){
					
					String fullName = pPackage.getPackage().replace("/", ".") +"."+ pClass.getClass_();
					//System.out.println("full: "+ fullName);
					IJvmTypeProvider typeProvider = jvmTypeProviderFactory.findOrCreateTypeProvider(resource.getResourceSet());
					//System.out.println("provider: "+ typeProvider);
					//System.out.println("typeProvider : "+ typeProvider );
					JvmType pType = typeProvider.findTypeByName(fullName);
					//System.out.println("type: "+ pType);
					if (pType != null)
						createHyperlinksTo(resource, node.getParent(), pType, acceptor);
					
					return;
				}
			}
		}
		
		
		super.createHyperlinksByOffset(resource, offset, acceptor);
	}
}
