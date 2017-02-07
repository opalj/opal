#Class File Version
Extracts the class file version of each class file belonging to the project.

##Note
Class files using Java 1.0 and 1.1. are grouped together.

##Major Features of Selected Class File Versions

###Java 6 and newer
Don't have *jsr*/*ret* instructions anymore.

###Java 7 and newer
May have *invokedynamic* instructions; however Java only started using *invokedynamic* with Java 8.

###Java 8 and newer
 - usually have *invokedynamic* instructions because Java started using it
 - added default methods (this changed the way how method resolution works)
