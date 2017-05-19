# java.lang.System and java.lang.Runtime API

Represents the usage of core methods of `java.lang.System` or `java.lang.Runtime` that are related to the state of the JVM, Permissions, or to accessing the underlying operating system.

## Command Execution
Counts method calls that create external processes. In Java that can be either achieved using `Runtime.exec(...)` or using `java.lang.ProcessBuilder`.

## JVM Exit
Counts calls to JVM methods (`Runtime.halt`/`Runtime.exit`) that stop the JVM.

## Native Libraries
The *Native Libraries* feature reflects the usage of native libraries loaded using `java.lang.Runtime`. The count shows how many load library calls are found in the project.

## SecurityManager
Counts usages of the `java.lang.SecurityManager` in a project. This includes getting as well as setting an instance of `java.lang.SecurityManager`.

## Sound
The Sound group counts how often the sound API to play songs is used. This group only considers the classes in the two packages `javax.sound.sampled` and `javax.scene.media`

## Environment
Any access or query to the system's environment variables is captured by the `Information` group.
