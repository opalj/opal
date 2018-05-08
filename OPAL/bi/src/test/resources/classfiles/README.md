In this folder, we have collected some JARs of projects which have *interesting* bytecode. The Jars in the main folder are treated as projects on their own; the subfolders are treated as single projects consisting of multiple Jars which are, e.g., expected to be added to one `org.opalj.br.Project`.

The file Java9-selected-jmod-module-info.classes.zip contains various module-info classes and as such cannot be processed by an `org.opalj.br.Project` (every jar must have at most one `module-info` data structure.)
