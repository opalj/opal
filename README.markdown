# Overview
BAT is a Scala library written in Scala 2.9.x for analyzing Java Bytecode.

BAT provides different representations of Java Bytecode to support a project's special needs.
BAT generally supports Java 6 class files and contains preliminary support for Java 7 as of Jan. 2012.

Currently, four representations are provided/are supported:

1. Native/Canonical - Basically, a one to one representation of the class file; Java Bytecode's constant pool is not resolved and instructions are not made explicit.

2. Resolved - An object-oriented representation that facilitates writing analysis against the bytecode. 
            If you want to write bytecode analyses in Scala then we recommend to use this representation. 
            The most important property of this representation is that the constant pool is completely resolved and all instructions are explicitly represented.
            This representation also provides some functionality to extract a class file's dependencies.

3. Prolog - A representation of Java Bytecode as a set of Prolog facts. This representation builds upon the resolved representation and basically represents all information that are represented by the resolved representation.

4. XML - A representation of Java Bytecode as XML. Currently, instructions are not represented.

If you need "your own" representation or if you miss a certain feature, just write us an email. Adding further representations is straightforward, because BAT was engineered for this particular use case.

*Currently, BAT does not support manipulating existing Java class files and we have no intentions to add this feature in the near future.*

**Using BAT**

If you just want to use, but not otherwise extend BAT, it is recommended that you call "ant package" and use the generated jar file. At run time no further packages are required. 

To compile BAT just call the corresponding ANT target. Please note, that BAT makes heavy use of code generation and some parts of the hand-written code depends on the generated code. The generated code is found under "build/src/" and if you want to edit BAT using your favorite IDE, make sure that you include the folder with the generated source files.

