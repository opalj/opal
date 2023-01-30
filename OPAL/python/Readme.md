# Python module
This module provides an IFDS taint analysis that is aware of calls to `ScriptEngine`
objects that evaluate Python and models them by internally invoking another IFDS taint analysis running on the Python code,
handing back the results to the Java IFDS taint analysis.
This module bases on the module made for analysis of JavaScript.


There were four challenges identified:
1. Finding the Python source code which is executed on a call to a `ScriptEngine`.
2. Tracking the taints inside the `ScriptEngine` object.
3. Executing analysis of Python code from Java
4. Scalability in presence unboxing of primitive type wrappers.

## 1. Finding the source code
`org.opal.python.LocalPythonSourceFinder` implements a way to find the Python source code.
The class uses the Def-Use information provided by *TACAI* to find the source code.

There are two ways in how Python is invoked from Java. First, `eval()` can be used to evaluate code using a `ScriptEngine`:
```java
ScriptEngineManager sem = new ScriptEngineManager();
ScriptEngine se = sem.getEngineByName("Python");
se.eval("*** insert Python code here ***");
```
If the argument to `eval` is a constant string, the source code is already known. The argument could also be a `FileReader`
or `File`. In that case, the `LocalPythonSourceFinder` looks up to find the definition of the `FileReader`/`File` and tries to get the file name.

The other way is to call `invokeFunction(functionName, ...args)` on the `ScriptEngine` object. That case is a bit more complicated,
because the source code of the function was provided with an earlier call to `eval()`.
So here `LocalPythonSourceFinder` goes up to the definition of the `ScriptEngine` and then looks for `eval()` calls on the definition.

### Limitations
The `LocalPythonSourceFinder` is only able to find the source code intra-procedural, inherited from using the def-use chains.

## 2. Tracking tainted Python variables in Java
A `ScriptEngine` also holds an environment called `Binding` that maps variables names to values.
With `put(varName, obj)` data can be handed to Python and with `get(variableName)` can be retrieved back from Python.
Every variable in the `Binding` is available as a global variable in the execution of the Python script.
Thus, to precisely model what's happening in Python, we need to know the Python variables that are tainted at a call to `eval` or `invokeFunction`.

In essence, the environment is just a `Map<String, Object>` for Java. To track the tainted keys inside the map,
we do manually model the behavior of the `get`, `put`, `remove` and `putAll` methods called on a `ScriptEngine` or `Binding` instance.
At each call to first three, the first argument represents the variable name.
Again, we use the def-use chains from *TACAI* to find a string constant.
We introduce a new taint type called `BindingFact`, which holds a variable,
either of type `ScriptEngine` or `Binding` and the tainted key.

### Design Decisions
#### In case of a non-constant argument
```java
String variable = getUserInput();
se.put(variable, "some value");
```
Whenever the analysis does not know the constant string value of the first argument to `put`,
it does derive a wildcard fact. The analysis considers a wildcard fact as if all variables inside the environment are tainted.

## 3. Gluing two analyses together
The semantics of `eval` and `invokeFunction` can be modelled by transforming the Python source code. For this,
we introduce two functions: `opal_source()` and `opal_last_stmt(...)`.
The first one is marked as a source in the Python analysis and used to taint all global variables that were handed over from Java.
The latter is, as the name suggest, the last statement and records all variables that reach the end of the execution.

We do exploit that the entry and exit points of Python script are easy to find out: The script is executed from top to bottom.
Thus, we do perform the transformation on the source code directly.

We do look for Top-Level parameters of the Python code and for parameters of the function, using Abstract Syntax Trees.
We are calling Python function with the analysed code as a parameter in order to get the parameter names of the analysed function.
The code for analysis was based on a solution provided by Ph.D. Rathul Gopinath, the Lecturer at the University of Sydney.
Original code can be found here: https://github.com/vrthra/taints.py. Unfortunately extending the string object and overriding default methods
did not work in Jython. As a result, approach without extending string was used.


```Python
import ast

def get_parameters(src_code, function_name):
    a = ast.parse(src_code)
    variables = []
    for function in ast.iter_child_nodes(a):
        if isinstance(function, ast.FunctionDef) and function.name == function_name:
            for node in ast.walk(function):
                if isinstance(node, ast.arguments):
                    for arg in node.args:
                        for a in ast.walk(arg):
                            if isinstance(a, ast.Name):
                                variables.append(a.id)
    return ",".join(variables)

```

Similar approach is used for determining the top level variables.

```Python
import ast

def get_top_level_variables(src_code):
    a = ast.parse(src_code)
    variables = []
    for node in ast.iter_child_nodes(a):
        if isinstance(node, ast.Assign):
            for arg in node.targets:
                for a in ast.walk(arg):
                    if isinstance(a, ast.Name):
                        variables.append(a.id)
    return ','.join(variables)
```



We do inject four symbols that should not be already present in the script: `opal_source`, `opal_last_stmt`, `opal_fill_arg` and `opal_tainted_arg`.
We assume that it is the case, there is no check for this.

* `opal_source` is a function that has no parameters and returns a value. This function shall be marked as a source in the Python analysis.
* `opal_last_stmt` is a function, which takes as a parameter String with names of all variables visible in the top-level scope, concatenated by `,`.
* `opal_fill_arg` is an untainted variable used to generate valid function calls.
* `opal_tainted_arg` is a tainted variable used to generate valid function calls.

```Python
// Begin of OPAL generated code
class tainted_string:
    def __init__(self, value):
        self.value = value
        
    def __radd__(self, other):
        t = tainted_string(str.__add__(other, self.value))
        t._taint = self._taint
        return t
        
    def __add__(self, other):
        t = tainted_string(str.__add__(self.value, other))
        t._taint = self._taint
        return t
        
    def __repr__(self):
        return self.__class__.__name__ + str.__repr__(self.value) + " " + str(self.tainted())


    def taint(self):
        self._taint = True
        return self

    def tainted(self):
        return hasattr(self, '_taint')


def opal_source():
    s = tainted_string("secret")
    s.taint()
    return s

tainted_arguments = []

def opal_last_stmt(args):
    for a in args.split(', '):
        variable = globals()[a]
        if hasattr(variable, '_taint'):
            tainted_arguments.append(a)

// End of OPAL generated code

xxx = secret;

// Begin of OPAL generated code
opal_last_stmt('secret,xxx')
// End of OPAL generated code
```
Above is the generated code for a simple example where `secret` is handed over from the environment inside a `ScriptEngine`.
Before the actual script, `secret` is declared in the top-level scope and tainted.
The tainted object has overwritten functions for right addition. Currently, the analysis works only for strings.
It also does not taint results of operations made on this taint, for example: `len(tainted_value)`.
After the script, `opal_last_stmt` takes all variable names as arguments.
Then the array of tainted_arguments is returned to Java and converted back to a `BindingFact`.

```Python
// Begin of OPAL generated code
class tainted_string:
    def __init__(self, value):
        self.value = value
        
    def __radd__(self, other):
        t = tainted_string(str.__add__(other, self.value))
        t._taint = self._taint
        return t
        
    def __add__(self, other):
        t = tainted_string(str.__add__(self.value, other))
        t._taint = self._taint
        return t
        
    def __repr__(self):
        return self.__class__.__name__ + str.__repr__(self.value) + " " + str(self.tainted())


    def taint(self):
        self._taint = True
        return self

    def tainted(self):
        return hasattr(self, '_taint')


def opal_source():
    s = tainted_string("secret")
    s.taint()
    return s

tainted_arguments = []

def opal_last_stmt(args):
    for a in args.split(', '):
        variable = globals()[a]
        if hasattr(variable, '_taint'):
            tainted_arguments.append(a)
// End of OPAL generated code

function check(str, unused) {
    return str === "1337";
}

// Begin of OPAL generated code
opal_fill_arg = 42
opal_tainted_arg = opal_source()
opal_tainted_return = check(opal_fill_arg, opal_tainted_arg)
opal_last_stmt('opal_tainted_return')
// End of OPAL generated code
```
Above is another example of a transformation, but this time for tainted arguments to `invokeFunction`.
Here, we first generate the two variables, one tainted and one untainted.
These are used to generate a valid call to the function that is invoked by `invokeFunction`.
The return value is always named `opal_tainted_return` and also flows at the end into `opal_last_stmt`.
Back in Java, when converting the variable names back to facts,
the variable name `opal_tainted_return` indicates the special case that instead of a `BindingFact`,
the return value should be tainted.

### Limitations
In theory, if there are too many variables in the top-level scope,
the length of the string in `opal_last_stmt` could reach the maximum length in Jython.
This limit is big enough, so this should never accidentally happen in practise. 


## 4. Scalability
When using `invokeFunction` to call Python from Java,
the variable parameters are passed as `Object` and the return value is an `Object` as well.
In case of primitive arguments or returns, boxing and unboxing is needed. In that case,
the analysis needs the JDK to precisely resolve boxings.
But the IFDS solver in the current state is too inefficient such that the `PythonAwareTaintProblem` gets slow.

There was implemented a reader of the summaries from the *StubDroid* paper.
Overriding the `useSummaries` field of `ForwardTaintProblem` enables the use of those summaries
With those, there is also no need to load the JDK, which further brings the analysis up to speed.
> S. Arzt and E. Bodden, "StubDroid: Automatic Inference of Precise Data-Flow Summaries for the Android Framework," 2016 IEEE/ACM 38th International Conference on Software Engineering (ICSE), 2016, pp. 725-735, doi: 10.1145/2884781.2884816.


## Things left open
* While the `LocalPythonSourceFinder` is able to find filenames, I did not implement a way to find these files in the filesystem. 
Thus, the `PythonAnalysisCaller` ignores `PythonFileSource`.
* In various places, the analysis depends on finding a string constant.
Plugging in a string analysis before running the Python aware taint analysis might greatly improve the accuracy.
* Another optimization could be inside the IFDS solver: implementing unbalanced returns and only propagate zero-facts at sources.
* The analysis written in Python is very simple and not full. There exist complete libraries for Python taint analysis, like Pyre.
Unfortunately Jython, the Python ScriptEngine for Java, currently does not support Python 3.x, which is a requirement for those libraries.
* Currently, the analysis is done by calling Jython ScriptEngine more than once, but it may be possible to decrease the number of calls,
by integrating retrieval of parameters and top level variables into the call for analysing the code.
