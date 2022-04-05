# Publications

This page lists publications around OPAL, both [core contributions](#core-papers) to the framework as well as [further research](#research-using-opal) that uses OPAL.
There are also links to [related presentations](#related-presentations) below.

## Core Papers

> [**Modular Collaborative Program Analysis in OPAL**](https://2020.esec-fse.org/details/fse-2020-papers/191")  
> ESEC/FSE 2020  
> *Dominik Helm, Florian Kübler, Michael Reif, Michael Eichberg, Mira Mezini*
> <details><summary>Abstract</summary>
> Current approaches combining multiple static analyses deriving different, independent properties focus either on modularity or performance.
> Whereas declarative approaches facilitate modularity and automated, analysis-independent optimizations, imperative approaches foster manual, analysis-specific optimizations.
>
> In this paper, we present a novel approach to static analyses that leverages the modularity of blackboard systems and combines declarative and imperative techniques.
> Our approach allows exchangeability, and pluggable extension of analyses in order to improve sound(i)ness, precision, and scalability and explicitly enables the combination of otherwise incompatible analyses.
> With our approach integrated in the OPAL framework, we were able to implement various dissimilar analyses, including a points-to analysis that outperforms an equivalent analysis from Doop, the state-of-the-art points-to analysis framework.</details>

> [**A Programming Model for Semi-implicit Parallelization of Static Analyses**](https://conf.researchr.org/details/issta-2020/issta-2020-papers/18/A-Programming-Model-for-Semi-implicit-Parallelization-of-Static-Analyses)  
> ISSTA 2020  
> *Dominik Helm, Florian Kübler, Jan Thomas Kölzer, Philipp Haller, Michael Eichberg, Guido Salvaneschi, Mira Mezini*
> <details><summary>Abstract</summary>
> Parallelization of static analyses is necessary to scale to real-world programs, but it is a complex and difficult task and, therefore, often only done manually for selected high-profile analyses.
> In this paper, we propose a programming model for semi-implicit parallelization of static analyses which is inspired by reactive programming.
> Reusing the domain-expert knowledge on how to parallelize analyses encoded in the programming framework, developers do not need to think about parallelization and concurrency issues on their own.
> The programming model supports stateful computations, only requires monotonic computations over lattices, and is independent of specific analyses.
> Our evaluation shows the applicability of the programming model to different analyses and the importance of user-selected scheduling strategies.
> We implemented an IFDS solver that was able to outperform a state-of-the-art, specialized parallel IFDS solver both in absolute performance and scalability.</details>

> [**TACAI: An Intermediate Representation based on Abstract Interpretation**](https://pldi20.sigplan.org/details/SOAP-2020-papers/1)  
> SOAP 2020  
> *Michael Reif, Florian Kübler, Dominik Helm, Ben Hermann, Michael Eichberg, Mira Mezini*
> <details><summary>Abstract</summary>
> To facilitate the easier development of static analyses, most Java static analysis frameworks provide an intermediate representation of Java bytecode.
> While such representations are often based on three-address code, the transformation itself is a great, yet too little used opportunity to apply optimizations to the transformed code, such as constant propagation.
>
> In this paper, we propose TACAl, a refinable intermediate representation that is based on abstract interpretation results of a method’s bytecode.
> Exchanging the underlying abstract interpretation domains enables the creation of various intermediate representations of different precision levels.
> Our evaluation shows that TACAI can be efficiently computed and provides slightly more precise receiver-type information than Soot’s Shimple representation.
> Furthermore, we show how exchanging the underlying abstract domains directly impacts the generated representation.</details>

> [**Lattice Based Modularization of Static Analyses**](https://conf.researchr.org/details/ecoop-issta-2018/SOAP-2018-papers/6/Lattice-Based-Modularization-of-Static-Analyses)  
> SOAP 2018  
> *Michael Eichberg, Florian Kübler, Dominik Helm, Michael Reif, Guido Salvaneschi, Mira Mezini*
> <details><summary>Abstract</summary>
> Today, static analyses for, e.g., class immutability or method purity are developed as standalone analyses.
> Complementary information that could improve the analyses is either ignored by making a sound over-approximation or it is also computed by the analyses but at a rudimentary level.
> For example, an immutability analysis requires field mutability information, alias/escape information, and information about the concurrent behavior of methods to correctly classify classes such as java.lang.String or java.util.BigDecimal.
> As a result, without properly supporting the integration of independently developed, mutually benefiting analysis, many analyses will not correctly classify relevant entities.
>
> In this paper, we propose to use explicitly reified lattices that encode the information about a source code element’s properties (e.g., a method’s purity or a class’ immutability) as the sole interface between mutually dependent analyses enabling composition of multiple analyses.
> Our case study shows that using such an approach enables highly scalable, lightweight implementations of modularized static analyses.</details>

> [**Assessment and Creation of Effective Test Corpora**](/articles/Hermes@SOAP18.pdf)  
> SOAP 2018  
> *Michael Reif, Michael Eichberg, Ben Hermann, Mira Mezini*  
> <details><summary>Abstract</summary>
> An integral part of developing a new analysis is to validate the correctness of its implementation and to demonstrate its usefulness when applied to real-world code.
> As a foundation for addressing both challenges developers typically use custom or well-established collections of Java projects.
> The hope is that the collected projects are representative for the analysis’ target domain and therefore ensure a sound evaluation.
> But, without proper means to understand how and to which degree the features relevant to an analysis are found in the projects, the evaluation necessarily remains inconclusive.
> Additionally,it is likely that the collection contains many projects which are – w.r.t. the developed analysis – basically identical and therefore do not help the overall evaluation/testing of the analysis, but still cost evaluation time.
>
> To overcome these limitations we propose Hermes, a framework that enables the systematic assessment of given corpora and the creation of new corpora of Java projects.
> To show the usefulness ofHermes, we used it to comprehend the nature of the projects belonging to the Qualitas Corpus(QC) and then used it to compute a minimal subset of all QC projects useful for generic data- and control-flow analyses.
> This subset enables effective and efficient integration test suites.</details>

> [**Call Graph Construction for Java Libraries**](https://doi.acm.org/10.1145/2950290.2950312)  
> FSE 2016
> *Michael Reif, Michael Eichberg, Ben Hermann, Johannes Lerch, Mira Mezini*  
> <details><summary>Abstract</summary>
> Today, every application uses software libraries.
> Yet, while a lot of research exists w.r.t. analyzing applications, research that targets the analysis of libraries independent of any application is scarce.
> This is unfortunate, because, for developers of libraries, such as the Java Development Kit (JDK), it is crucial to ensure that the library behaves as intended regardless of how it is used.
> To fill this gap, we discuss the construction of call graphs for libraries that abstract over all potential library usages.
> Call graphs are particularly relevant as they are a precursor of many advanced analyses, such as inter-procedural data-flow analyses.
>
> We show that the current practice of using call graph algorithms designed for applications to analyze libraries leads to call graphs that, at the same time, lack relevant call edges and contain unnecessary edges.
> This motivates the need for call graph construction algorithms dedicated to libraries.
> Unlike algorithms for applications, call graph construction algorithms for libraries must take into consideration the goals of subsequent analyses.
> Specifically, we show that it is essential to distinguish between the scenario of an analysis for potential exploitable vulnerabilities from the scenario of an analysis for general software quality attributes, e.g., dead methods or unused fields.
> This distinction affects the decision about what constitutes the library-private implementation, which therefore, needs special treatment.
> Thus, building one call graph that satisfies all needs is not sensical.
> Overall, we observed that the proposed call graph algorithms reduce the number of call edges up to 30% when compared to existing approaches.</details>

> [**A software product line for static analyses: the OPAL framework**](https://doi.acm.org/10.1145/2614628.2614630)  
> SOAP 2014
> *Michael Eichberg, Ben Hermann*  
> <details><summary>Abstract</summary>
> Implementations of static analyses are usually tailored toward a single goal to be efficient, hampering reusability and adaptability of the components of an analysis.
> To solve these issues, we propose to implement static analyses as highly-configurable software product lines (SPLs).
> Furthermore, we also discuss an implementation of an SPL for static analyses -- called OPAL -- that uses advanced language features offered by the Scala programming language to get an easily adaptable and (type-)safe software product line.
>
> OPAL is a general purpose library for static analysis of Java Bytecode that is already successfully used.
> We present OPAL and show how a design based on software produce line engineering benefits the implementation of static analyses with the framework.</details>

## Research Using OPAL

> [**CiFi: Versatile Analysis of Class and Field Immutability**](/articles/CiFi@ASE21.pdf) 
> ASE 2021  
> *Tobias Roth, Dominik Helm, Michael Reif, Mira Mezini*  
> <details><summary>Abstract</summary>
> Reasoning about immutability is important for pre-venting bugs, e.g., in multi-threaded software.
> So far, static analysis to infer immutability properties has mostly focused on individual objects and references.
> Reasoning about fields and entire classes, while significantly simpler, has gained less attention.
> Even a consistently used terminology is missing, which makes it difficult to implement analyses that rely on immutability information.
> We propose a model for class and field immutability that unifies terminology for immutability flavors considered by previous work and covers new levels of immutability to handle lazy initialization and immutability dependent on generic type parameters.
> Using the OPAL static analysis framework, we implement CiFi, a set of modular, collaborating analyses for different flavors of immutability, inferring the properties defined in our model.
> Additionally, we propose a benchmark of representative test cases for class and field immutability.
> We use the benchmark to showcase CiFi’s precision and recall in comparison to state of the art and use CiFi to study the prevalence of immutability in real-world libraries, showcasing the practical quality and relevance of our model.</details>

> [**Hidden in Plain Sight: Obfuscated Strings Threatening Your Privacy**](https://dl.acm.org/doi/10.1145/3320269.3384745)  
> ASIA-CCS 2020  
> *Leonid Glanz, Patrick Müller, Lars Baumgärtner, Michael Reif, Sven Amann, Pauline Anthonysamy, Mira Mezini*  
> <details><summary>Abstract</summary>
> String obfuscation is an established technique used by proprietary, closed-source applications to protect intellectual property.
> Furthermore, it is also frequently used to hide spyware or malware in applications.
> In both cases, the techniques range from bit-manipulation over XOR operations to AES encryption.
> However, string obfuscation techniques/tools suffer from one shared weakness:
> They generally have to embed the necessary logic to deobfuscate strings into the app code.
> In this paper, we show that most of the string obfuscation techniques found in malicious and benign applications for Android can easily be broken in an automated fashion.
> We developed StringHound, an open-source tool that uses novel techniques that identify obfuscated strings and reconstruct the originals using slicing.
> We evaluated StringHound on both benign and malicious Android apps
> In summary, we deobfuscate almost 30 times more obfuscated strings than other string deobfuscation tools.
> Additionally, we analyzed 100,000 Google Play Store apps and found multiple obfuscated strings that hide vulnerable cryptographic usages,
> insecure internet accesses, API keys, hard-coded passwords, and exploitation of privileges without the awareness of the developer.
> Furthermore, our analysis reveals that not only malware uses string obfuscation but also benign apps make extensive use of string obfuscation.</details>

> [**Judge: Identifying, Understanding, and Evaluating Sources of Unsoundness in Call Graphs**](https://conf.researchr.org/details/issta-2019/issta-2019-Technical-Papers/24/Judge-Identifying-Understanding-and-Evaluating-Sources-of-Unsoundness-in-Call-Grap)  
> ISSTA 2019  
> *Michael Reif, Florian Kübler, Michael Eichberg, Dominik Helm, Mira Mezini*  
> <details><summary>Abstract</summary>
> Call graphs are widely used; in particular for advanced control- and data-flow analyses.
> Even though many call graph algorithms with different precision and scalability properties have been proposed, a comprehensive understanding of sources of unsoundness, their relevance, and the capabilities of existing call graph algorithms in this respect is missing.
>
> To address this problem, we propose Judge, a toolchain that helps with understanding sources of unsoundness and improving the soundness of call graphs.
> In several experiments, we use Judge and an extensive test suite related to sources of unsoundness to (a) compute capability profiles for call graph implementations of Soot, WALA, DOOP, and OPAL, (b) to determine the prevalence language features and APIs that affect soundness in modern Java Bytecode, (c) to compare the call graphs of Soot, WALA, DOOP, and OPAL, highlighting important differences in their implementations, and (d) to evaluate the necessary effort to achieve project-specific reasonable sound call graphs.
>
> We show that soundness-relevant features/APIs are frequently used and that support for them differs vastly, up to the point where comparing call graphs computed by the same base algorithms (e.g., RTA) but different frameworks is bogus.
> We also show that Judge can support users in establishing the soundness of call graphs with reasonable effort.</details>

> [**Algebraic Effects for the Masses**](https://2018.splashcon.org/event/splash-2018-oopsla-algebraic-effects-for-the-masses)  
> OOPSLA 2018  
> *Jonathan Immanuel Brachthäuser, Philipp Schuster, Klaus Ostermann*  
> <details><summary>Abstract</summary>
> Algebraic effects are a program structuring paradigm with rising popularity in the functional programming language community.
> Algebraic effects are less wide-spread in the context of imperative, object oriented languages.
> We present a library to program with algebraic effects in Java.
> Our library consists of three core components:
> A type selective CPS transformation via JVM bytecode transformation, an implementation of delimited continuations on top of the bytecode transformation and finally a library for algebraic effects in terms of delimited continuations.</details>

> [**A Unified Lattice Model and Framework for Purity Analyses**](https://ieeexplore.ieee.org/abstract/document/9000061)  
> ASE 2018  
> *Dominik Helm, Florian Kübler, Michael Eichberg, Michael Reif, Mira Mezini*  
> <details><summary>Abstract</summary>
> Analyzing methods in object-oriented programs whether they are side-effect free and also deterministic, i.e., mathematically pure, has been the target of extensive research.
> Identifying such methods helps to find code smells and security related issues, and also helps analyses detecting concurrency bugs.
> Pure methods are also used by formal verification approaches as the foundations for specifications and proving the pureness is necessary to ensure correct specifications.
> However, so far no common terminology exists which describes the purity of methods.
> Furthermore, some terms (e.g., pure or side-effect free) are also used inconsistently.
> Further, all current approaches only report selected purity information making them only suitable for a smaller subset of the potential use cases.
> In this paper, we present a fine-grained unified lattice model which puts the purity levels found in the literature into relation and which adds a new level that generalizes existing definitions.
> We have also implemented a scalable, modularized purity analysis which produces significantly more precise results for real-world programs than the best-performing related work.
> The analysis shows that all defined levels are found in real-world projects.</details>

> [**Systematic Evaluation of the Unsoundness of Call Graph Construction Algorithms for Java**](http://michael-reif.name/publications/JCG.pdf)  
> SOAP 2018  
> *Michael Reif, Florian Kübler, Michael Eichberg, and Mira Mezini*  
> <details><summary>Abstract</summary>
> Call graphs are at the core of many static analyses ranging from the detection of unused methods to advanced control-and data-flow analyses.
> Therefore, a comprehensive under-standing of the precision and recall of the respective graphs is crucial to enable an assessment which call-graph construction algorithms are suited in which analysis scenario.
> For example, malware is often obfuscated and tries to hide its intent by using Reflection.
> Call graphs that do not represent reflective method calls are, therefore, of limited use when analyzing such apps.
>
> In general, the precision is well understood, but the recall is not, i.e., in which cases a call graph will not contain any call edges.
> In this paper, we discuss the design of a comprehensive test suite that enables us to compute a fingerprint of the unsoundnes sof the respective call-graph construction algorithms.
> This suite also enables us to make a comparative evaluation of static analysis frameworks.
> Comparing Soot and WALA shows that WALA currently has better support for new Java 8 features and also for Java Reflection.
> However, in some cases both fail to include expected edges.</details>

> [**CodeMatch: Obfuscation won't Conceal your Repackaged App**](http://www.st.informatik.tu-darmstadt.de/artifacts/codematch/)  
> ESEC/FSE 2017  
> *Leonid Glanz, Sven Amann, Michael Eichberg, Michael Reif, Ben Hermann, Johannes Lerch, Mira Mezini*  
> <details><summary>Abstract</summary>
> An established way to steal the income of app developers, or to trick users into installing malware, is the creation of repackaged apps.
> These are clones of – typically – successful apps.
> To conceal their nature, they are often obfuscated by their creators.
> But, given that it is a common best practice to obfuscate apps, a trivial identification of repackaged apps is not possible.
> The problem is further intensified by the prevalent usage of libraries.
> In many apps, the size of the overall code base is basically determined by the used libraries.
> Therefore, two apps, where the obfuscated code bases are very similar, do not have to be repackages of each other.
>
> To reliably detect repackaged apps, we propose a two step approach which first focuses on the identification and removal of the library code in obfuscated apps.
> This approach – LibDetect – relies on code representations which abstract over several parts of the underlying bytecode to be resilient against certain obfuscation techniques.
> Using this approach, we are able to identify on average 70% more used libraries per app than previous approaches.
> After the removal of an app’s library code, we then fuzzy hash the most abstract representation of the remaining app code to ensure that we can identify repackaged apps even if very advanced obfuscation techniques are used.
> This makes it possible to identify repackaged apps.
> Using our approach, we found that ≈15% of all apps in Android app stores are repackages.</details>

> [**Hidden Truths in Dead Software Paths**](https://doi.acm.org/10.1145/2786805.2786865)  
> ESEC/FSE 2015  
> *Michael Eichberg, Ben Hermann, Leonid Glanz, Mira Mezini*  
> <details><summary>Abstract</summary>
> Approaches and techniques for statically finding a multitude of issues in source code have been developed in the past.
> A core property of these approaches is that they are usually targeted towards finding only a very specific kind of issue and that the effort to develop such an analysis is significant.
> This strictly limits the number of kinds of issues that can be detected.
> In this paper, we discuss a generic approach based on the detection of infeasible paths in code that can discover a wide range of code smells ranging from useless code that hinders comprehension to real bugs.
> Code issues are identified by calculating the difference between the control-flow graph that contains all technically possible edges and the corresponding graph recorded while performing a more precise analysis using abstract interpretation.
> We have evaluated the approach using the Java Development Kit as well as the Qualitas Corpus (a curated collection of over 100 Java Applications) and were able to find thousands of issues across a wide range of categories.</details>

> [**Getting to know you... Towards a Capability Model for Java**](https://doi.acm.org/10.1145/2786805.2786829)  
> ESEC/FSE 2015  
> *Ben Hermann, Michael Reif, Michael Eichberg, Mira Mezini*  
> <details><summary>Abstract</summary>
> Developing software from reusable libraries lets developers face a security dilemma:
> Either be efficient and reuse libraries as they are or inspect them, know about their resource usage, but possibly miss deadlines as reviews are a time consuming process.
> In this paper, we propose a novel capability inference mechanism for libraries written in Java.
> It uses a coarse-grained capability model for system resources that can be presented to developers.
> We found that the capability inference agrees by 86.81% on expectations towards capabilities that can be derived from project documentation.
> Moreover, our approach can find capabilities that cannot be discovered using project documentation.
> It is thus a helpful tool for developers mitigating the aforementioned dilemma.</details>

## Related Presentations

> [**Your JDK 8**](/articles/Your_JDK-Entwicklertag2015.pdf)  
> *Michael Eichberg*

> [**Lattice Based Modularization of Static Analyses**](/articles/FPCF-Slides@SOAP18.pdf)  
> *Michael Eichberg*