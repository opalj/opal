# Java Fixtures Compiler - SBT Plug-in

This sbt plug-in compiles the Java projects that are used as test fixtures when testing/developing OPAL.

## Background

OPAL is a Java Bytecode library/static analysis framework and therefore – for testing purposes – requires stable Java class files with very specific properties. I.e., when the class files are (re-)compiled the generated files should be identical to the very last byte. By compiling specific Java test projects – in the following referred to as test fixtures; or just fixtures – using a well-defined, hardcoded Java compiler the required property can be guaranteed.   
Further documentation can be found in the project markdown file `OPAL/bi/src/test/fixtures-java/Readme.md`

To compile the fixtures OPAL uses the Eclipse JDT compiler which is hardcoded in this plugin as a dependency. The plugin takes this compiler, and offers an SBT task to discover and compile the Java fixtures based on the plugin's configuration

## Configuration of the plugin

The base configuration scope of the plugin is "Compile". The plugin offers the following tasks and settings (sbt code is shown because it indicates the key types and has descriptions):

```
// tasks of the plugin

val javaFixtureCompile = taskKey[Seq[JavaFixtureCompilationResult]]("compilation of java fixture projects against Eclipse JDT compiler specified statically in the plugin's dependencies")

val javaFixturePackage = taskKey[Seq[JavaFixturePackagingResult]]("compilation and packaging of java fixture projects against Eclipse JDT compiler specified statically in the plugin's dependencies")

val javaFixtureDiscovery = taskKey[Seq[JavaFixtureCompilationTask]]("discovery of java compilation tasks")

// will be scoped to the compilation task.

val javaFixtureTaskDefs = settingKey[Seq[JavaFixtureCompilationTask]]("java fixture compilation task definitions for the plugin")

// will be scoped to the discovery task

val javaFixtureProjectsDir = settingKey[File]("Folder containing java project folders to be discovered by the plugin")

val javaFixtureSupportDir = settingKey[File]("Folder containing support libraries for use of discovered tasks of the plugin")

val javaFixtureTargetDir = settingKey[File]("Folder in which subfolders and JAR files for the output of discovered tasks of the plugin will be created")
```

The definitions and implementations of the tasks (compile, package, discovery) reside within the plugin's main object, `org.opalj.javacompilation.javaFixtureCompiler`. The classes that model the settings and results for the tasks are

- `org.opalj.javacompilation.JavaFixtureCompilationTask`
- `JavaFixtureCompilationResult`
- `JavaFixturePackagingResult`

and reside within `org.opalj.javacompilation.FixtureCompileSpec.scala`.

In the following, we discuss how to setup the plugin to find all java fixtures and how to compile them automatically using the discovery task and fixture-specific configuration files. For doing it all manually, the user may set the `javaFixtureTaskDefs` setting to a sequence of `org.opalj.javacompilation.JavaFixtureCompilationTask` task descriptions. The intricacies of this approach are not discussed here; it is straightforward though to instantiate custom compilation tasks with the configuration semantics that are also used with the automated discovery. See `org.opalj.javacompilation.FixtureCompileSpec.scala` for inline documentation of the `JavaFixtureCompilationTask` class.

The version of the Eclipse JDT compiler is set statically in the plugin's `build.sbt` file.

## Setup using automated discovery of compilation tasks

The setup of the plugin in OPAL uses the default settings, quoted verbatim:

```
javaFixtureProjectsDir in javaFixtureDiscovery := sourceDirectory.value / "fixtures-java" / "projects",
javaFixtureSupportDir in javaFixtureDiscovery := sourceDirectory.value / "fixtures-java" / "support",
javaFixtureTargetDir in javaFixtureDiscovery := resourceManaged.value
```

These three settings define where the compilation task discovery looks for java fixtures and their specific configuration. It suffices to say here that the general directory structure as it concerns this plugin, looks like this:

```
  + javaFixtureProjectsDir
    + project1 (ostensibly package "project1")
      + sub1 (ostensibly package "project1.sub1")
        . ClassSub.java
      . Class1.java
      . Class2.java
      . compiler.config    // optional, specifies compiler options and support libraries
    + project2
  + javaFixtureSupportDir
    + lib1
      + pkg1
        . SupportClass1.java (ostensibly package "lib1.pkg1")
        . SupportClass2.java (ostensibly package "lib1.pkg1")
  + javaFixtureTargetDir
    + project1 (possibly with configuration options as part of the folder name)
      . (here the compiled class files will reside)
    + project2
      . (here the compiled class files will reside)
    .project1.jar (compiled, possibly configuration options are part of the name)

```

The compiler.config file is optional. In short, its content are distinct lines for either:
 - _comments_ -- any line starting with a `#`
 - _requires_ specification -- the line `requires=lib1` makes the support library `lib1` from
   the above exemplaric directory structure available on the classpath
 - _command line parameters_ -- any line starting with a dash, e. g. `-1.8 -g -parameters -genericsignature`.
   For a reference of the Eclipse JDT compiler command line options, please refer to `jdtcompiler.txt`.

A more complete specification on the format of fixture-specific .config files (that include command-line arguments and "require" specifications of support libraries) can be found in the file `OPAL/bi/src/test/fixtures-java/Readme.md`.


## Configuring the plugin

In the following code, we manually specify the parameters for the plug-in when used in the "Test"
configuration as opposed to the (enclosing) "Compile" configuration scope. One implication of this is,
that the default settings for the plugin locate the (default) `javaFixtureProjectsDir` inside of the
`sourceDirectory` of the configuration scope as seen above. So, when using "Compile" as scope as per default,
the fixture projects are taken to be located under `src/main/...`, while with the "Test" configuration, the
fixture projects directory is by default located under `src/test`. We will go one step further and change the name of
that projects directory as an example.

First, every project that wants to use the plugin has to do so explicitely:

```
lazy val proj = Project(
		base = file("xyz/here")
).enablePlugins(javaFixtureCompiler)
```

For this to work, import the plugin inside your `project/plugins.sbt`:

```
addSbtPlugin("de.opal-project" % "sbt-java-fixture-compiler" % "1.0")
```

If you left it here, the plugin would assume the default projects resp. supports, target folders:

 - `src/main/fixtures-java/projects`
 - `src/main/fixtures-java/support`
 - `target/scala-2.xx/resource_managed/main`

Now, for changing that as described above, using the "Test" scope:

```
lazy val bi = Project(
		base = file("xyz/here")
).enablePlugins(javaFixtureCompiler)
.settings(
  inConfig(Test)(
    javaFixtureCompiler.baseJavafixtureSettings ++ // first, import the base settings
    Seq(
      javaFixtureProjectsDir in javaFixtureDiscovery := sourceDirectory.value / "fixt" / "projects",
      javaFixtureSupportDir in javaFixtureDiscovery := sourceDirectory.value / "fixt" / "support"
    )
  )
)

```

The directories for the fixture discovery in the "Test" scope should now be:

- `src/test/fixt/projects`
- `src/test/fixt/support`
- `target/scala-2.xx/resource_managed/main`

The `javaFixtureCompile` and `javaFixturePackage` tasks are ready to be used on the sbt command line; to
include them in your build, this following additional project settings are an example of using the package
task for test resource generation, mapping the return value of the package task to a sequence of generated files in the process:

```
resourceGenerators += Def.task {
    (javaFixturePackage in Test).value flatMap (_.generatedFiles)
}
```
