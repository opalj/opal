# Field Access Statistics

Identifies unused fields and those fields which are not private but which are only accessed by its defining type – in the scope of the project. Fields which define compile-time constants.  *Primitive constants – e.g., `public final static double pi = 3.1d` – and `String` constants are typically inlined by compilers and it is therefore "normal" that these fields have no (more) use and they are therefore ignored.*


Reflective accesses are not considered.
