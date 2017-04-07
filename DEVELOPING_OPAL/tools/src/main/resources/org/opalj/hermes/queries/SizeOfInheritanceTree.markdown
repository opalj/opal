# Size of Inheritance tree

Computes for each type the size of the inheritance tree and then assigns the type to one of the complexity categories.

The size of inheritance tree is equal to the number of unique classes and interfaces a given class inherits from. For example, for `java.lang.Object` the size is `0`. For the marker interface `java.io.Serializable` the size is `1`. (In Java every type (ex-/implicitly) inherits from `java.lang.Object.`)
