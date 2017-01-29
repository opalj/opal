# Major Changes 

##Upcomming
 - the AI now prevents simple, unnecessary joins if a variable is known to be dead when multiple control flow paths join
 - added a simple live variables analysis (`br.Code.liveVariables`) which computes the information for a code's locals (operand stack values are not considered because standard compiler generally don't create "dead operands" and the intended usage are performance and precision improvements)
 
##0.8.10
 - added support for the JSON Serialization of Milliseconds, Nanoseconds and Seconds
 
##0.8.9
 - added a list-like data structure (`Chain`) which is specialized for int values to save memory
 (~ 25%) and to avoid unnecessary boxing operations
 - added preliminary Java 9 support
 - added the fix-point computations framework to facilitate the implementation of concurrent, fix-point based analyses