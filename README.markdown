# Overview
BAT is an extensible library for analyzing Java bytecode that is written in Scala and leverages Scala's advanced language features to provide a new and previously unseen level of customizability and scalability. BAT was designed from the ground up
with extensibility, adaptability and scalability in mind. In general, BAT is thread-safe and writing concurrent analyses is facilitated by BAT's overall design.

BAT in particular provides support for an easily customizable abstract interpretation of Java bytecode. Additionally, it has built-in support for the analysis of static source code dependencies and generally provides extensive support for pattern-matching on Java bytecode. 

# Project Structure
BAT consists of four main projects:

* **Core**: The core project provides functionality necessary for reading and traversing Java class files.  
**Util**: Conceptually belongs to the Core project, but contains source code that needs to be compiled before it is possible to 
compile Core. (Util primarily contains macros.) 
**Core** and **Util** are mature. 

* **AI**: Implementation of an abstract interpretation framework that can be used to easily implement analyses at very different levels of precision. 

* **Dependencies**: Provides support for extracting and analyzing a project's source code dependencies. This project is also used to check architectures and
is in particular used to validate parts of BAT's architecture.

* **Tools**: Programs that can directly be executed and which are useful when analyzing Java bytecode.

* **Demo**: Contains working code samples that demonstrates how to use BAT. The code in the Demo project is primarily meant as a teaching resource. To start the examples, start the `sbt` console (Scala Build Tools) and change the current project to "Demo" (`project Demo`).
	
* **Incubation**: Contains code that is not yet finished, but which already provides some useful functionality. In some cases the code is under active development and in some cases the code was just not finished and no decision has been made what to do with it.
	
# Building BAT

BAT uses SBT as its build tool and working with BAT is particular easy using the SBT console.

0. Make sure that you have Java 7, Scala 2.10.3 and SBT 0.13.0 installed and running. Download a recent snapshot of BAT or clone the repository.
1. Go to BAT's root folder. (The folder in which this document is stored.)
2. Call `sbt copy-resources doc`. This compiles all projects and generates the ScalaDoc documentation.
3. Call `sbt test` to test that everything works as expected.
4. You are ready to go. 

# Example Usage

1. Start the sbt console. (In BAT's root folder call `sbt` on the command line.)
2. Change the project to the Abstract Interpretation framework using `project Ext-AbstractInterpretation`.
3. To get the call graph of some class call `run` and specify (a) a jar file and (b) the name of some class. Afterwards the call graph related to that class is calculated and opened. (Graphviz needs to be installed first, since the visualization of the call graph is done using it.) 