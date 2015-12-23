# Overview
This directory contains the projects to generate the "BugDescriptionLanguage"-plug-in for eclipse.
An eclipse installation with Xtext support is required ("http://www.itemis.com/en/xtext/download/") for building the projects and the plug-in.
Additionally the "Eclipse Plug-in Development Environment" is required to run/debug the plug-in within eclipse (it is already included in the Xtext eclipse).


# Generating Grammar
Right click on "GenerateBDL.mwe2" in project "org.opalj.bdl" in the same package and choose "Run As -> MWE2 Workflow"

# Exporting Plug-in
Click on File->Export in the new window select "Deployable features".
- check "org.opalj.bdl.sdk"
- enter a target Directory 
- go to Options: make sure that "Categorize repository" is checked and "org.opalj.bdl.sdk\category.xml" is entered
- Finish

# Plug-in Installation
- add the plug-in directory as an update site
- select the category "BDL" and finish the installation

# Using the Plug-in
Create or open a file with the extension "bdl".
Then the IDE will ask if the "Xtext nature" should be added to the project which does not need to be confirmed (TODO: remove this question)

# Developing
-> Project "org.opalj.bdl"
	1) Grammar: 
		-> in the file "org.opalj.bdl.BDL.xtext"
		- the grammar needs to be regenerated in order to reflect changes
		- definitions without assigning values to attributes will not generate classes
	2) Formatting:
		-> located in "org.opalj.bdl.formatting.BDLFormatter.xtend"
		- needs to be adapted if elements are added to/removed from the grammar
	3) Validation:
		-> see "org.opalj.bdl.validation.BDLValidator.xtend"
		- needs only to be changed if you want to ensure certain special cases, like a uniqueness of values in a list

-> Project "org.opalj.bdl.ui"
	1) Font/Styles aka Highlighting 
		-> Definition of fonts/styles is found in the file "org.opalj.bdl.ui.BDLHighlightingConfiguration"
			- each defined style can be edited by the user in the preference dialogue
		-> Usage of fonts/styles is located in the file "org.opalj.bdl.ui.BDLSemanticHighlightingCalculator"
			-> traverses the document tree and decides which style has to be used for which document node
	2) Hyper-links
		-> located in the file "org.opalj.bdl.ui.BDLHyperLinkHelper"
		- is only needed for the linking of our custom separation of package, class and function
	3) Proposals
		-> "org.opalj.bdl.ui.contentassist.BDLProposalProvider.xtend"
		- defines extra proposals for elements, like a list of possible values for an element
		- is not needed for the structure of the document
	4) Outline
		-> The tree definition of the tree structure is located in "org.opalj.bdl.ui.outline.BDLOutlineTreeProvider.xtend"
		- also contains the logic for filtering with in the outline
		-> Icons and text for most elements are defined in the file "org.opalj.bdl.ui.labeling.BDLLabelProvider.xtend"
		- by default every element provides some text to identify it but most of the time the default text is not very helpful ...

-> Project "org.opalj.bdl.sdk"
	1) Definition of Plug-in name & features
		-> "feature.xml" defines the meta data for the plug-in, like name, copyright, dependencies ...
		-> "category.xml" defines the category name which is displayed in the import process