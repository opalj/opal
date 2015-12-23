# Overview
This directory contains the projects to generate the "BugDescriptionLanguage"-plug-in for eclipse.
An eclipse installation with Xtext support is required ("http://www.itemis.com/en/xtext/download/") for building the projects and the plug-in.
Additionally the "Eclipse Plug-in Development Environment" is required to run/debug the plug-in within eclipse (it is already included in the Xtext eclipse).


# Plug-in Installation
- add the plug-in directory as an update site
- select the category "BDL" and finish the installation

# Using the Plug-in
- just create or open a file with the extension "bdl" after the plug-in is installed and the correct editor should be started

# Generating the Grammar


# Developing
- Importing the projects
	During the first import of the projects into eclipse some errors will appear. These errors can be ignored because they will disappear after the grammar is generated. These errors will result in an additional dialogue popping up when the build process is started which can be ignored by clicking continue. Also there will be a question in the console which also should be confirmed.
- Generating the Grammar
	Right click on "GenerateBDL.mwe2" in project "org.opalj.bdl" in the same package and choose "Run As -> MWE2 Workflow"
- Project "org.opalj.bdl"
	* Grammar: 
		-> in the file "org.opalj.bdl.BDL.xtext"
		- the grammar needs to be regenerated in order to reflect changes
		- definitions without assigning values to attributes will not generate classes
	* Formatting:
		-> located in "org.opalj.bdl.formatting.BDLFormatter.xtend"
		- needs to be adapted if elements are added to/removed from the grammar
	* Validation:
		-> see "org.opalj.bdl.validation.BDLValidator.xtend"
		- needs only to be changed if you want to ensure certain special cases, like a uniqueness of values in a list

- Project "org.opalj.bdl.ui"
	* Font/Styles aka Highlighting 
		- -> Definition of fonts/styles is found in the file "org.opalj.bdl.ui.BDLHighlightingConfiguration"
		- each defined style can be edited by the user in the preference dialogue
		- -> Usage of fonts/styles is located in the file "org.opalj.bdl.ui.BDLSemanticHighlightingCalculator"
		- traverses the document tree and decides which style has to be used for which document node
	* Hyperlinks
		- -> located in the file "org.opalj.bdl.ui.BDLHyperLinkHelper"
		- is only needed for the linking of our custom separation of package, class and function
	* Proposals
		- -> "org.opalj.bdl.ui.contentassist.BDLProposalProvider.xtend"
		- defines extra proposals for elements, like a list of possible values for an element
		- is not needed for the structure of the document
	* Outline
		- -> The tree definition of the tree structure is located in "org.opalj.bdl.ui.outline.BDLOutlineTreeProvider.xtend"
		- also contains the logic for filtering with in the outline
		- -> Icons and text for most elements are defined in the file "org.opalj.bdl.ui.labeling.BDLLabelProvider.xtend"
		- by default every element provides some text to identify it but most of the time the default text is not very helpful ...

- Project "org.opalj.bdl.sdk"
	* Definition of Plug-in name & features
		- "feature.xml" defines the meta data for the plug-in, like name, copyright, dependencies ...
		- "category.xml" defines the category name which is displayed in the import process
		
# Exporting Plug-in
- Click on File->Export in the new window select "Deployable features".
- check "org.opalj.bdl.sdk"
- enter a target Directory 
- go to Options: make sure that "Categorize repository" is checked and "org.opalj.bdl.sdk\category.xml" is entered
- Click on Finish and wait for the operation to complete