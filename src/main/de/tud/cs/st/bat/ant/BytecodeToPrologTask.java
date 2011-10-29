/* License (BSD Style License):
 * Copyright (c) 2009
 * Software Technology Group,
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the Software Technology Group or Technische
 *   Universität Darmstadt nor the names of its contributors may be used to
 *   endorse or promote products derived from this software without specific
 *   prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package de.tud.cs.st.bat.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Java;
import org.apache.tools.ant.types.Path;
import org.apache.tools.ant.types.Reference;
import org.apache.tools.ant.types.Resource;
import org.apache.tools.ant.types.ResourceCollection;
import org.apache.tools.ant.types.resources.Union;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class implements an ANT task that runs the BytecodeToProlog tool.
 * 
 * @since 20.10.2009 11:47:50
 * @author Sebastian Harrte
 * @author Ralf Mitchke
 */
public class BytecodeToPrologTask extends Task {

	private static final String JVM_MEMORY = "1024m";

	private static final String MAIN_CLASS = "de.tud.cs.st.bat.resolved.BytecodeToProlog";

	private Union resources = new Union();

	private File output;

	private Path classpath;

	private boolean combineOutput = false;

	public File getOutput() {
		return output;
	}

	public void setOutput(File output) {
		this.output = output;
	}

	public boolean isCombineOutput() {
		return combineOutput;
	}

	public void setCombineOutput(boolean combineOutput) {
		this.combineOutput = combineOutput;
	}

	public void add(ResourceCollection resource) {
		resources.add(resource);
	}

	public Path getClasspath() {
		return classpath;
	}

	public void setClasspath(Path classpath) {
		this.classpath = classpath;
	}

	public void setClasspathRef(Reference refId) {
		Path cp = new Path(this.getProject());
		cp.setRefid(refId);
		setClasspath(cp);
	}

	/**
	 * Checks if any of the nested resources is newer than a given threshold.
	 * 
	 * @param threshold
	 *            The threshold. This should be the last modified time of the
	 *            output.
	 * @return True if the output needs to be updated, that is, if any of the
	 *         resources is newer than threshold.
	 */
	public boolean needsUpdate(long threshold) {
		Iterator<?> it = resources.iterator();

		while (it.hasNext()) {
			Resource resource = (Resource) it.next();

			if (resource.isFilesystemOnly()) {
				if (resource.getLastModified() > threshold) {
					log("Resource " + resource + " is newer than output.");
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public void execute() throws BuildException {
		System.out.println("Output: " + output);
		System.out.println("Last Modified: " + output.lastModified());

		for (Iterator<?> it = resources.iterator(); it.hasNext();) {
			Resource resource = (Resource) it.next();
			log("Resource: " + resource);
		}

		if ((!output.exists()) || needsUpdate(output.lastModified())) {
			log("Updating prolog output: " + output);
			updateOutput();
		}
	}

	/**
	 * Removes the output and then appends the prolog code for all resources to
	 * the output file. If an error occurs, the output file is removed.
	 */
	private void updateOutput() {
		if (output.exists() && !output.delete()) {
			log("Unable to delete output file: " + output);
		}

		Iterator<?> it = resources.iterator();

		try {
			if (combineOutput) {
				List<String> args = new ArrayList<String>(resources.size());
				/** not nice, but sufficient **/
				while (it.hasNext()) {
					Resource resource = (Resource) it.next();
					args.add(resource.toString());
				}
				log("generating combined output: " + args.toString());
				generateProlog(args);
			} else {
				while (it.hasNext()) {
					Resource resource = (Resource) it.next();
					generateProlog(resource);
				}
			}
		} catch (BuildException e) {
			if (output.exists() && !output.delete()) {
				log("Unable to delete output after failure: " + output);
			}
			throw e;
		}
	}

	/**
	 * Appends the prolog code for a single resource to the output file.
	 * 
	 * @param resource
	 *            The resource.
	 */
	private void generateProlog(Resource resource) {
		generateProlog(resource.toString());
	}

	private void generateProlog(String arg) {
		List<String> list = new ArrayList<String>(1);
		list.add(arg);
		generateProlog(list);
	}

	private void generateProlog(List<String> args) {
		Java java = new Java();

		java.bindToOwner(this);
		java.init();
		java.setClasspath(classpath);
		java.setFork(true);
		java.setAppend(false);
		java.setFailonerror(true);
		java.setOutput(output);
		java.setFailonerror(true);
		java.setClassname(MAIN_CLASS);
		java.setMaxmemory(JVM_MEMORY);
		java.setLogError(true);
		for (String arg : args) {
			java.createArg().setValue(arg);
		}
		java.execute();
	}
}
