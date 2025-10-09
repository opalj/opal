# Configuration explorer documentation
## How to document your configs
The configuration explorer uses a custom parser to parse flags into a browsable documentation
This makes every element of your config documentable. 
This guide is about to teach you how to utilize these flags to create a comprehensible documentation

### How do I create a browsable configuration?
1. Start up your sbt shell
2. run the doc command
3. Open the generated commentedconfigs.html in your source directory

### Where can I add my documentation?
You can add your documentation in the lines before an element, or directly behind the element within the same line.

```
    // You can either add your comment to the element here
    Value = "one"  //Or you can also add your comment here
    
    // However, you cannot add your comment here 
```

Also keep in mind that HOCON allows for multiple values within one line. 
In this case, the comment will be associated with its closest neighbor that fullfills the criteria:

```
    Object = {Key = "Value", AnotherKey = "SecondValue"} // This comment will be associated with Object, since its closing bracket is closest
```

Sub-values need to be placed in its own lines in order to be documented.

### Usage of flags

The configuration explorer allows for different flags to be utilized for different elements of the documentation.
Each flag will be interpreted differently and be represented in a different way after exporting.

The flags can be grouped in two groups, by the point in time where they are visible when the documentation is exported.

#### @label
@label documents the label of the object.
It will be shown in the documentation as the name of the object and will be visible even when the object is collapsed.
If the configuration element is part of an object, the label will be automatically set as its identifier within the object.
This is overridable by manually setting the @label flag within its documentation.

```
    {
        key = "value" // The label property will be set to "key" automatically.
        
        //@label Custom label
        another_key = "value" // The label property is now overridden to "Custom label"
    }
```

#### @brief
@brief shows a brief description of the element for easier comprehension without the need for expanding the element.
For optimal formatting, try to keep the length of the text behind this flag below 50 characters.

#### @description
@description will set the description of the configuration element when the element is expanded.
Usage of the flag is optional, as unflagged content will be added to the description area too.

```
    {
        // @description You can use this flag to add this text to the elements description.
        // However, without any flags, the text will be added to the description too.
        key = "value"
    }
```

#### @type
@type can be used to indicate the type of a value that will be used.

##### Subclass type
The subclass type is one of two special types currently implemented into Configuration Explorer
When tagged with a subclass type, configuration explorer will search for all subclasses of a given root class.

##### Enum type
The enum type is the second of the special types in Configuration Explorer. Use it if you have a finite amount of allowed values that you all want to list in the constraints. 

##### Other types
You can pick a type that you want to indicate, which logical restraints are, but they will be treated as-is and not be refined further. 

#### @constraint
Use @constraint to define which values are allowed and which are not. 
If there are multiple constraints, use a new line for each constraint using the flag "@constraint" at the beginnning of each line.

There are currently two types implemented where you use a special style to list constraints:
##### Subclass type
If your type is "subclass", then list exactly one constraint. The constraint must be the class where all allowed classes inherit from. 
Configuration Explorer will fetch all valid Subclasses of the listed class and list these in the documentation. You may specify a different value in the value field when generating the documentation.

Example:
```
    {
        // @description Configuration Explorer will list all subclasses of ConfigNode as allowed values. (Which are ConfigObject, ConfigList, ConfigEntry)
        // @type subclass
        // @constraint ConfigNode
        value = ConfigObject 
    }
```

##### Enum type
Create one line with the constraint flag for every allowed value that you want to list.

Example:

```
    {
        // @description Add one row for each allowed value
        // @type enum
        // @constraint two
        // @constraint three
        // @constraint five
        // @constraint seven
        primeNumberBelowTen = three
    }
```

#### Other types
The constraints for other types will be passed as-is and can be in a free text.

Example:

```
    {
        // @type int
        // @constraint Values must be within 0 and 100
        // @constraint Values must be even
        key = 2
    }
```

