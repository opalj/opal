# Overview

The ***APK*** module provides tools to do cross language (dex bytecode and native code) analyses on Android APKs.

## Working features
- APK's dex can be analyzed after being transformed to jars via [enjarify](https://github.com/Storyyeller/enjarify) or
  [dex2jar](https://github.com/ThexXTURBOXx/dex2jar).
- APK's native code can be analyzed after being lifted to LLVM IR via [RetDec](https://github.com/avast/retdec).
- APK components / entry points - Activities, Services, Broadcast Receivers and Content Providers - are parsed from
  AndroidManifest.xml.
- Occurrences of context-registered Broadcast Receivers are parsed, but the recovered class name is imprecise. 
  Reconstruction of the IntentFilter is only working in trivial cases where the filter is created in the same method as
  where registerReceiver() is called.

## Future work
- Entry points of UI events are missing, e.g. button onClick, gestures, ...
- Reconstruction of precise class names and IntentFilters for context registered Broadcast Receivers in bytecode.
- Parsing of context-registered Broadcast Receivers in native code.

## Dependencies
The ***APK*** module uses following projects and libraries:
- [enjarify](https://github.com/Storyyeller/enjarify)
- [dex2jar](https://github.com/ThexXTURBOXx/dex2jar)
- [RetDec](https://github.com/avast/retdec)
- [apk-parser](https://github.com/hsiafan/apk-parser)

To install the required tools in a docker container, run `build_container.sh`. Without the docker container, the ***APK***
module won't work.

## Usage
First, build the docker container.

```scala
ApkParser.logOutput = true  // optional

val project = ApkParser.createProject(
    "PATH-TO-APK",
    BaseConfig,
    false                   // optional, true = enjarify (default), false = dex2jar
)

val components = project.get(ApkComponentsKey)

val llvmProject = project.get(LLVMProjectKey)

val contextRegisteredReceivers = project.get(ApkContextRegisteredReceiversKey)
```
