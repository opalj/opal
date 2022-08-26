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
- [enjarify](https://github.com/Storyyeller/enjarify) (included in docker container)
- [dex2jar](https://github.com/ThexXTURBOXx/dex2jar) (included in docker container)
- [RetDec](https://github.com/avast/retdec) (included in docker container)
- [apk-parser](https://github.com/hsiafan/apk-parser) (library, included via sbt)

To install the required tools in a docker container, run `build_container.sh`. Without the docker container, the ***APK***
module won't work. The container uses the master/main branches of the respective tools. Run `build_container.sh --no-cache`
for a clean build, which pulls new commits from the repositories.

## Usage
First, build the docker container.

```scala
ApkParser.logOutput = true  // optional, for debugging

val project = ApkParser.createProject(
    "PATH-TO-APK",
    BaseConfig,
    DexParser.Enjarify
)

val components = project.get(ApkComponentsKey)

val llvmProject = project.get(LLVMProjectKey)

val contextRegisteredReceivers = project.get(ApkContextRegisteredReceiversKey)
```
