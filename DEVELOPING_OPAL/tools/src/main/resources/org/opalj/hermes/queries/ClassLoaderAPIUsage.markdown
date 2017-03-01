# ClassLoader API Usage

Extracts the usage of methods of `java.lang.ClassLoader` in a project and also checks for
custom class loaders.

- detects custom ClassLoaders that extend the `java.lang.ClassLoader`class.
- counts how often the `SystemClassLoader` is retrieved
- counts how often a class loader is retrieved in general
- counts how often resources or system resources are acquired via a class loader