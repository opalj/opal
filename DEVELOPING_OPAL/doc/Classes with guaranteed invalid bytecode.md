#Classes with invalid bytecode 

Here, ___invalid___ means that the code cannot successfully be executed by the JVM or will already be rejected by the bytecode verifier.

An abstract interpretation of the the following methods will lead to various "problems/exceptions":
 
 - `com.drahtwerk.drahtkern.q{ void onCreate(android.os.Bundle) }`  
 NullPointerException due to the load of an empty register: __a_load(7)@49__
 
 - 
