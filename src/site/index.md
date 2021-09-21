# OPAL

OPAL is a next-generation, highly configurable and scalable static analysis platform that supports developers in systematically chosing the best tradeoffs between precision, soundness and performance of static analyses.
It does so by hosting a wide and extensible collection of modular analyses modules that can be automatically composed in a case-by-case manner to collaboratively reason about a particular software at hand.
OPAL manages the execution of analysis modules and adjusts it as needed for scalability.<center><iframe width="560" height="315" src="https://www.youtube-nocookie.com/embed/-V25yQDXPqg" title="YouTube video player" frameborder="0" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe></center>

OPAL can be used for Java bytecode processing, engineering, manipulation and analysis.
It is written in Scala 2.12.x and supports Java 16 Bytecode; OPAL in particular provides support to facilitate the analysis of Java 8 lambda expressions (*Invokedynamic* instructions) and Java 15 dynamic constants. 

The latest release is *4.0.0*, the latest snapshot version is *4.0.1-SNAPSHOT*.
Both versions are found on [Maven Central](https://search.maven.org/#search%7Cga%7C1%7Cde.opal-project).
Look [here](/UsingOPAL.html) for information on how to use OPAL in your project.

In-depth tutorials on developing static analyses with OPAL can be found in the navigation menu on the left, in particular starting with an introduction to [writing fixed-point analyses](/tutorial/FixedPointAnalyses.html).

Publications about OPAL's core concepts and about uses of OPAL in scientific research can be found [here](/Publications.html)

[comment]: ## "OPAL based Tools"

[comment]: # "### BugPicker"

[comment]: # "Find bugs in your Java project using [BugPicker](tools/bugpicker/index.php)."

### OPAL Java Bytecode Disassembler

OPAL's [Java Bytecode Disassembler](DeveloperTools.html) disassembles your Java bytecode.
OPAL can produce the raw bytecode or a more readable three-address code that optionally contains additional information derived from abstract interpretation.
The Bytecode Disassembler is available as a standalone tool and as plugin for ATOM, IntelliJ IDEA and Visual Studio Code.