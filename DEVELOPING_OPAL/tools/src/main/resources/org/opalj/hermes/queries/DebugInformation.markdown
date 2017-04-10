# Debug Information

Identifies those class file elements which have debug information. In case of the class file itself, the only potential debug information is the source (file) of the class file. In case of a method with a body, the potential debug information is the `local variable table`, the `local variable type table` and the `line number table`.

This query simply checks for the presence of the attributes.
