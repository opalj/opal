# OPIUM - OPAL Purity Inference based on a Unified lattice Model

## Overview
OPIUM performs inference of method purity (i.e. deterministic behavior and absence of side effects)
on Java (bytecode) projects.
Its results can be used for bug detection and program comprehension.

OPIUM was described in _A Unified Lattice Model and Framework for Purity Analyses_ published at
[ASE 2018](http://www.ase2018.com).
Purity results for the JOlden benchmark are available
[here](https://bitbucket.org/delors/opal/downloads/JOlden-Purity-Results.zip).

## Obtaining OPIUM
A version of OPIUM ready to to be executed can be downloaded 
[here](https://bitbucket.org/delors/opal/downloads/OPIUM.jar).

A current state of OPIUM can be obtained from 
[OPAL's repository](https://bitbucket.org/delors/opal) where OPIUM is implemented by class
`org.opalj.support.info.Purity`.

## Running OPIUM
OPIUM uses a command line interface. To simply analyse an application, use  
`java -jar OPIUM.jar -cp <path to your application`.

Use
`java -jar OPIUM.jar`
to print a list of additional parameters that OPIUM understands.

OPIUM can be used to analyze Java bytecode up to and including Java 10.

## Latest improvements
The following changes have been made to OPIUM and its purity model since the paper was published:
 - A new purity level, Compile Time Purity, was added to identify methods that do not use global
 state (i.e. static fields) with values that are constant during one execution of a program but may
 differ between different program executions.
 - External Purity was removed in favor for a more fine-grained representation of Contextual Purity:
 Contextual Purity now includes the set of all parameters that may be modified by the method.
 External purity (as still reported separately by OPIUM) comprises all contextually pure methods
 where that set contains just the implicit _this_ reference (i.e. instance methods for which the
 set of modified parameters contains just the value 0).
 - OPIUM now uses parallel execution on as many cores as available, significantly speeding up 
 purity inference.