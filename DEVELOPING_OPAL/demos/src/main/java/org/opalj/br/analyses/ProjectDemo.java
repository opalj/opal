/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.br.analyses;

import java.io.File;

import scala.jdk.javaapi.CollectionConverters;

import org.opalj.br.ClassFile;
import org.opalj.br.Method;
import org.opalj.ai.AIResult;
import org.opalj.ai.BaseAI;
import org.opalj.ai.Domain;
import org.opalj.ai.common.DomainRegistry;
import org.opalj.ai.common.XHTML;

/**
 * Demonstrates how to create and access a <code>Project</code> using Java.
 *
 * @author Michael Eichberg
 */
public class ProjectDemo {

    public static void main(String[] args) {
        // Load a project
        Project<java.net.URL> project = Project.apply(new File(args[0]));

        // Convert the project into a simple Map (NOT RECOMMENDED)
        // Map<ObjectType, ClassFile> project = projectLike.toJavaMap();

        // Create an abstract interpreter (the same instance can be reused)
        BaseAI ai = new BaseAI(true, false);

        // Alternatively choose between the available domains using the registry
        Iterable<String> domainDescriptions = CollectionConverters
                .asJava(DomainRegistry.domainDescriptions());
        System.out.println("The available domains are: ");
        for (String domainDescription : domainDescriptions)
            System.out.println("\t- " + domainDescription);
        // let's assume the user has chosen the domain he wanted to use
        String chosenDomain = domainDescriptions.iterator().next();

        // Do something with it...
        System.out.println("The project contains:");
        for (ClassFile classFile : CollectionConverters.asJava(project.allClassFiles())) {
            System.out.println(" - " + classFile.thisType().toJava());

            Iterable<Method> methods = CollectionConverters
                    .asJava(classFile.methods());
            for (Method method : methods) {
                if (method.body().isDefined()) {
                    // Use a fixed domain
                    // Domain<?> domain = new BaseDomain();
                    // OR use a user-specified domain
                    Domain domain = DomainRegistry.newDomain(chosenDomain, project, method);

                    AIResult result = ai.apply( method, domain);
                    System.out.println(
                        XHTML.dump(classFile,method,"Abstract Interpretation Succeeded", result)
                    );

                }
            }
        }
    }
}
