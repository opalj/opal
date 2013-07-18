# Overview
BAT is a highly-adaptable, native Scala library for analyzing Java bytecode. It implements the complete 
infrastructure necessary for reading Java 7 class files. Additionally, BAT offers a default
representation of Java bytecode that facilitates writing analyses.

BAT in particular provides support for the analysis of static source code dependencies. Furthermore, it 
provides extensive support for pattern-matching on Java Bytecode to facilitate the development of custom analyses.

# Project Structure
BAT consists of four main projects:

* **Core**: This project defines the core functionality necessary for reading and analyzing Java class files.  
**Util**: Technically belongs to the Core project, but contains some sources that need to be compiled before we can 
compile core. It contains in particular Scala Macros.  
**Core** and **Util** are mature.

* **AI**: This project will implement a small abstract interpretation framework (This is work in progress.)

* **Dependencies**: This project is concerned with checking a project's source code dependencies (This project is currently under revision.)

* **Demo**: Depends on all the other projects and contains fully working code that demonstrates how to use BAT. To start the examples, start the `sbt` console (Scala Build Tools) and change the project to "Demo" (`project Demo`). After that, you can call `run <PATH TO SOME-JAR-FILE or CLASS-FILE>` to run the demo app.
	
# Building BAT
0. Download and install Scala and SBT
1. call `sbt copy-resources doc` in BAT's root folder.
2. You are ready to go.