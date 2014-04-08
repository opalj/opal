# Overview
OPAL is an extensible library for analyzing Java bytecode. OPAL is completely written in Scala and leverages Scala's 
advanced language features to provide a new and previously unseen level of customizability and scalability. 
OPAL was designed from the ground up with *extensibility*, *adaptability* and *scalability* in mind. 

# Project Structure
OPAL consists of several projects:

* **Util**: Contains common helper classes.

* **Bytecode Toolkit**: The core project provides functionality necessary for reading and traversing Java class files.  

* **Abstract Interpretation Framework**: Implementation of an abstract interpretation framework that can be used to easily implement analyses at very different levels of precision. 

* **Dependencies Extraction**: Provides support for extracting and analyzing a project's source code dependencies. This project is also used to check architectures and
is in particular used to validate parts of OPAL's architecture.

* **OPAL Developer Tools**: Programs that can directly be executed and which are useful when analyzing Java bytecode.

* **Demos**: Contains working code samples that demonstrate how to use OPAL. The code in the Demo project is primarily meant as a teaching resource. To start the examples, start the `sbt` console (Scala Build Tools) and change the current project to "Demo" (`project Demo`).

* **FindREALBugs**: (This project is in its very early stages!) FindBugs reloaded. For further information go to: [FindREALBugs](https://bitbucket.org/delors/opal/wiki/FindREALBugs)

# Further Information #

* [OPAL Project](http://www.opal-project.de)

* [OPAL's Wiki](https://bitbucket.org/delors/opal/wiki/Home)
 