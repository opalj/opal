# Micro Patterns
Micro Patterns identify special usages of classes, their fields and methods
(cf. [Micro Patterns in Java Code](https://pdfs.semanticscholar.org/ddb5/037b14518890040a170a0fa5199e1d360e18.pdf)).

### Designator
An interface with absolutely no members.

### Taxonomy
An empty interface extending another interface.

### Joiner
An empty interface joining two or more superinterfaces.

### Pool
A class which declares only static final fields, but no methods.

### Function Pointer
A class with a single public instance method, but with no fields.

### Function Object
A class with a single public instance method, and at least one instance field.

### Cobol Like
A class with a single static method, but no instance members.

### Stateless
A class with no fields, other than static final ones.

### Common State
A class in which all fields are static.

### Immutable
A class with several instance fields, which are assigned exactly once, during instance construction.

### Restricted Creation
A class with no public constructors, and at least one static field of the same type as the class.

### Sampler
A class with one or more public constructors, and at least one static field of the same type as the class.

### Box
A class which has exactly one, mutable, instance field.

### Compound Box
A class with exactly one non primitive instance field.

### Canopy
A class with exactly one instance field that it assigned exactly once, during instance construction.

### Record
A class in which all fields are public, no declared methods.

### Data Manager
A class where all methods are either setters or getters.

### Sink
A class whose methods do not propagate calls to any other class.

### Outline
A class where at least two methods invoke an abstract method on `this`.

### Trait
An abstract class which has no state.

### State Machine
An interface whose methods accept no parameters.

### Pure Type
A class with only abstract methods, and no static members, and no fields.

### Augmented Type
Only abstract methods and three or more static final fields of the same type.

### Pseudo Class
A class which can be rewritten as an interface: no concrete methods, only static fields.

### Implementor
A concrete class, where all the methods override inherited abstract methods.

### Overrider
A class in which all methods override inherited, non-abstract methods.

### Extender
A class which extends the inherited protocol, without overriding any methods.
