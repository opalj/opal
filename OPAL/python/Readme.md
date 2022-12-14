# JavaScript module
This module provides an IFDS taint analysis that is aware of calls to `ScriptEngine` objects that evaluate JavaScript and models them by internally invoking another IFDS taint analysis running on the JavaScript code, handing back the results to the Java IFDS taint analysis.

We identified four challenges:
1. Finding the JavaScript source code which is executed on a call to a `ScriptEngine`.
2. Tracking the taints inside the `ScriptEngine` object.
3. Handing over the taints between two taint analyses across different static analysis frameworks without rewriting an analysis.
4. Scalability in presence unboxing of primitive type wrappers.

## 1. Finding the source code
`org.opal.js.LocalJSSourceFinder` implements a way to find the JavaScript source code. The class uses the Def-Use information provided by *TACAI* to find the source code.

There are two ways in how JavaScript is invoked from Java. First, `eval()` can be used to evaluate code using a `ScriptEngine`:
```java
ScriptEngineManager sem = new ScriptEngineManager();
ScriptEngine se = sem.getEngineByName("JavaScript");
se.eval("*** insert JavaScript code here ***");
```
If the argument to `eval` is a constant string, the source code is already known. The argument could also be a `FileReader` or `File`. In that case, the `LocalJSSourceFinder` looks up to find the definition of the `FileReader`/`File` and tries to get the file name.

The other way is to call `invokeFunction(functionName, ...args)` on the `ScriptEngine` object. That case is a bit more complicated, because the source code of the function was provided with an earlier call to `eval()`. So here `LocalJSSourceFinder` goes up to the definition of the `ScriptEngine` and then looks for `eval()` calls on the definition.

### Limitations
The `LocalJSSourceFinder` is only able to find the source code intraprocedural, inherited from using the def-use chains.

## 2. Tracking tainted JavaScript variables in Java
A `ScriptEngine` also holds an environment called `Binding` that maps variables names to values. With `put(varName, obj)` data can be handed to JavaScript and with `get(varName)` can be retrieved back from JavaScript. Every variable in the `Binding` is available as a global variable in the execution of the JavaScript script. Thus, to precisely model what's happening in JavaScript, we need to know the JavaScript variables that are tainted at a call to `eval` or `invokeFunction`.

In essence, the environment is just a `Map<String, Object>` for Java. To track the tainted keys inside the map, we do manually model the behavior of the `get`, `put`, `remove` and `putAll` methods called on a `ScriptEngine` or `Binding` instance. At each call to first three, the first argument represents the variable name. Again, we use the def-use chains from *TACAI* to find a string constant. We introduce a new taint type called `BindingFact`, which holds a variable, either of type `ScriptEngine` or `Binding` and the tainted key.

### Design Decisions
#### In case of a non-constant argument
```java
String var = getUserInput();
se.put(var, "some value");
```
Whenever the analysis does not know the constant string value of the first argument to `put`, it does derive a wildcard fact. The analysis considers a wildcard fact as if all variables inside the environment are tainted.

#### Killing taints
```java
          String var;
               |
     se.put("v1", secret);
               |
           if (cond)
           /       \
  var = "v1";     var = "v2";
           \       /
         se.remove(var);
```
In the example above, `v1` is tainted inside the environment. Later on, `var` is removed from the environment but the constant string depends on the control-flow so `var` is either `"v1"` or `"v2"`. Herre two analysis could do two things:

1. Kill the `BindingFact(_, "v1")`, because `"v1"` might have been removed.
2. Leave `BindingFact(_, "v1")` alive, because `"v2"` might have been removed.

The analysis implements the second way. Having to decide in this case is just a limitation of our analysis not being path-sensitive.

## 3. Gluing two analyses together
The semantics of `eval` and `invokeFunction` can be modelled by transforming the JavaScript source code. For this, we introduce two functions: `opal_source()` and `opal_last_stmt(...)`. The first one is marked as a source in the JavaScript analysis and used to taint all global variables that were handed over from Java. The latter is, as the name suggest, the last statement and records all taints that reach the end of the execution. We do provide dummy implementations for both functions to work around the fact that WALAs Call Graph generation does eliminate call sites without a callee.

We do exploit that the entry and exit points of JavaScript script are easy to find out: The script is executed from top to bottom. Thus, we do perform the transformation on the source code directly.

We do inject four symbols that may not be already in the script: `opal_source`, `opal_last_stmt`, `opal_fill_arg` and `opal_tainted_arg`. We assume that is the case, there is no check for this.

* `opal_source` is a function that has no parameters and returns a value. This function shall be marked as a source in the JavaScript analysis.
* `opal_last_stmt` is a function with n parameters where n is the number of variables visible in the top-level scope.
* `opal_fill_arg` is an untainted variable used to generate valid function calls.
* `opal_tainted_arg` is a tainted variable used to generate valid function calls.

```javascript
// Begin of OPAL generated code
function opal_source() {
    return "secret";
}
function opal_last_stmt(p0, p1) { }

var secret = opal_source();
// End of OPAL generated code

var xxx = secret;

// Begin of OPAL generated code
opal_last_stmt(secret, xxx);
// End of OPAL generated code
```
Above is the generated code for a simple example where `secret` is handed over from the environment inside a `ScriptEngine`. Before the actual script, `secret` is declared in the top-level scope and tainted. After the script, `opal_last_stmt` takes all variable names as arguments. For each tainted argument, WALA is queried for the variable name and converted back to a `BindingFact`.

One might ask why we need to pass all variables in scope as arguments. Take a look at the following JavaScript snippet:
```javascript
var secret = opal_source();
secret = 42;
opal_last_stmt(secret);
```
and now in SSA form:
```javascript
_1 = opal_source();
_2 = 42;
opal_last_stmt(_2);
```
In the SSA form, `_1` is tainted at the end and if we'd query the name of `_1`, that would be `secret`. To find out for which SSA variables we should query the name, we pass them as arguments. That also makes the Java analysis less dependent on the JavaScript analysis. In theory, to switch the underlying JavaScript analysis, one would only need to implement a way such that the JavaScript analysis returns the original variable names for the tainted arguments of `opal_last_stmt`.

```javascript
// Begin of OPAL generated code
function opal_source() {
    return "secret";
}
function opal_last_stmt(p0) { }
// End of OPAL generated code

function check(str, unused) {
    return str === "1337";
}

// Begin of OPAL generated code
var opal_fill_arg = 42;
var opal_tainted_arg = opal_source();
var opal_tainted_return = check(opal_fill_arg, opal_tainted_arg);
opal_last_stmt(opal_tainted_return);
// End of OPAL generated code
```
Above is another example of a transformation, but this time for tainted arguments to `invokeFunction`. Here, we first generate the two variables, one tainted and one untainted. These are used to generate a valid call to the function that is invoked by `invokeFunction`. The return value is always named `opal_tainted_return` and also flows at the end into `opal_last_stmt`. Back in Java, when converting the variable names back to facts, the variable name `opal_tainted_return` indicates the special case that instead of a `BindingFact`, the return value should be tainted.

### Limitations
In theory, if there are too many variables in the top-level scope, the number of arguments in `opal_last_stmt` could reach the maximum number of arguments of the Rhino JavaScript engine, which WALA uses. The limit seems to be in the thousands, so that should never accidentally happen in practise. 

Also, there is a bug in the WALA JavaScript IFDS analysis I did not fix. Look at the snippet below:
```javascript
var x;
function setX(v) {
    x = v;
}
```
Assume a call to `setX` with a tainted argument. At the end of the execution, `x` should be tainted. But in the WALA IFDS analysis, this is not the case.

## 4. Scalability
When using `invokeFunction` to call JavaScript from Java, the variable parameters are passed as `Object` and the return value is an `Object` as well. In case of primitive arguments or returns, boxing and unboxing is needed. In that case, the analysis needs the JDK to precisely resolve boxings. But the IFDS solver in the current state is too inefficient such that the `JavaScriptAwareTaintProblem` gets slow.

I have decided to implement a reader of the summaries from the *StubDroid* paper. Overriding the `useSummaries` field of `ForwardTaintProblem` enables the use of those summaries. With those, there is also no need to load the JDK, which further brings the analysis up to speed.
> S. Arzt and E. Bodden, "StubDroid: Automatic Inference of Precise Data-Flow Summaries for the Android Framework," 2016 IEEE/ACM 38th International Conference on Software Engineering (ICSE), 2016, pp. 725-735, doi: 10.1145/2884781.2884816.


## Things left open
* While the `LocalJSSourceFinder` is able to find filenames, I did not implement a way to find these files in the filesystem. Thus, the `JavaScriptAnalysisCaller` ignores `JavaScriptFileSource`.
* In various places, the analysis depends on finding a string constant. Plugging in a string analysis before running the JavaScript aware taint analysis might greatly improve the accuracy.
* Pooling of JavaScript analysis calls. Currently, for each taint that gets handed over to the JavaScript, a new analysis run is started. We could save the overhead of constructing a JavaScript AST and exploded supergraph everytime by pooling all taints and only calling the JavaScript analysis when the worklist of the Java IFDS solver is empty.
* Another optimization could be inside the IFDS solver: implementing unbalanced returns and only propagate zero-facts at sources.
