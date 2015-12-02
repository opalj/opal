# Project Setup
1. Install Eclipse (4.5/Mars or later)
2. Install the Eclipse plug-in development environment
    1. Go to Help -> Install New Software ...
    2. Select `--All Available Sites--` in the upper dropdown menu
    3. Search and install `Eclipse plug-in development environment`
    4. Follow the installation process
3. Install Scala IDE for Eclipse
    1. Go to Help -> Install New Software ...
    2. Enter `http://download.scala-ide.org/sdk/lithium/e44/scala211/stable/site` where you can select an update site
    3. Search and install `Scala IDE for Eclipse`
    4. Follow the installation process
4. Compile OPAL Framework
    1. Checkout `eclipse` branch from OPAL Repository https://bitbucket.org/bpgr4/
    2. Build OPAL Project and setup Plugin dependencies using SBT:  
    2.1 If OPAL has already been built, run this command in OPAL directory: `sbt copyToEclipsePlugin`  
    2.2 If OPAL has not been built, run:  
    `sbt clean clean-files cleanCache cleanCacheFiles eclipse copyResources it:compile test:compile unidoc publishLocal           copyToEclipsePlugin`  
5. Import Plugin Project into Eclipse
    1. Right-click in package explorer -> Import
    2. Select "General -> Existing Projects into Workspace"
    3. Browse to OPAL Folder and select "OPALEclipse"
#