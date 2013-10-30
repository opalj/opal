# Overview
BAT is an extensible library written in Scala for analyzing Java Bytecode that leverages Scala's advanced language features to provide a new and yet unseen level of customizability and scalability. BAT was designed from the ground up
with extensibility, adaptability and scalability in mind. In general, BAT is thread-safe and writing concurrent analyses is facilitated by BAT's overall design.

BAT in particular provides support for an easily customizable abstract interpretation of Java bytecode. 

Additionally, it has built-in support the analysis of static source code dependencies and generally provides extensive support for pattern-matching on Java bytecode. 

# Project Structure
BAT consists of four main projects:

* **Core**: This project defines the core functionality necessary for reading and traversing Java class files.  
**Util**: Technically belongs to the Core project, but contains source code that needs to be compiled before it is possible to 
compile core. It primarily contains macros.  
**Core** and **Util** can are mature and significant API changes that affect
users are very unlikely.

* **AI**: This project implements an abstract interpretation framework. This project is useable, but API changes are still likely.

* **Dependencies**: This project implements support for extracting and analyzing a project's source code dependencies.

* **Demo**: Depends on all the other projects and contains fully working code that demonstrates how to use BAT. To start the examples, start the `sbt` console (Scala Build Tools) and change the project to "Demo" (`project Demo`). After that, you can call `run <PATH TO SOME-JAR-FILE or CLASS-FILE>` to run the demo app.
	
* **Incubation**: Contains code that already provides some functionality and may be useful, but which is not yet completely finished. In some cases the code is
under active development and in some cases the code was just not finished and
no decision has been made what to do with it.
	
# Building BAT
0. Download and install Scala and SBT
1. call `sbt copy-resources doc` in BAT's root folder.
2. You are ready to go.