#System and Runtime API

Represents the pure API usage to certain Java API feature that concern the JVM, Permissions, and the
underlying operating system.

<dl>
<dt>Process</dt>
<dd>The <i>Command Execution</i> feature shows if external processes are created. In Java that can be
either achieved by <code>Runtime.exec(...)</code> or via a <code>java.lang.ProcessBuilder</code>.</dd>

<dt>JVM Exit</dt>
<dd>The <i>JVM Exit</i> feature reveals calls that stop the JVM either by a normal exit or a
forced stop.</dd>

<dt>Native Libraries</dt>
<dd>The <i>Native Libraries</i> feature does reflect the usage of native libraries. The count
shows how many native libraries are loaded within the given project.</dd>

<dt>SecurityManager</dt>
<dd>The <i>SecurityManager</i> category keeps track of the usage of a <code>java.lang.SecurityManager</code>
in a project. Those features include getting as well as setting an instance of <code>java.lang.SecurityManager</code>.
</dd>
</dl>
