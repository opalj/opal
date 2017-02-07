# Overview
Hermes is a small, extensible project that helps to assess the quality of a given corpus of applications. It extracts a wide range of properties from the given project to make it possible to asses which language features and API features are used by the respective project(s). This helps you in designing small, efficient test fixtures/corpuses.


##Categories 
 - Completeness - static - Feature of Java Bytecode (a method with multiple line number tables); field resolution is required; all types of constants are used,...
 - Completeness - dynamic - we have a virtual method call, ...
 - Corner Cases - (e.g., a method without a return statement, a switch with just a default case, ...)
 - Landmark APIs - Java Reflection; finalize, Serialization
 


  ...       Project
Feature     P1  P2  P3
X           10  100
Y
Z           


Each entry contains the number of instances of the respective feature... when we click on it we get detailed information where we find instances of the feature ( ClassFile -> Field | (Method -> PC) )

For the evaluation, we are only considered if the feature is contained at least once.

##Extending Hermes
...