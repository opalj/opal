# Overview
This directory contains the projects to generate the "BugDescriptionLanguage"-plug-in for eclipse.
An eclipse installation with Xtext support is required ("http://www.itemis.com/en/xtext/download/") for building the projects and the plug-in.
Additionally the "Eclipse Plug-in Development Environment" is required to run/debug the plug-in within eclipse (it is already included in the Xtext eclipse).


# Generating Grammar
Rightclick on "GenerateBDL.mwe2" in project "org.opalj.bdl" in the same package and choose "Run As -> MWE2 Workflow"

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