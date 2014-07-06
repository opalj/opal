/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2014
 * Software Technology Group
 * Department of Computer Science
 * Technische Universität Darmstadt
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  - Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
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
package org.opalj
package ai

/**
 * Definition of common domains that can be used for the implementation of analyses.
 *
 * ==Types of Domains==
 * In general, we distinguish two types of domains. First, domains that define a
 * general interface (on top of the one defined by [[Domain]]), but do not directly
 * provide an implementation. Hence, whenever you develop a new `Domain` you should
 * consider implementing/using these domains to maximize reusability. Second,
 * `Domain`s that implement a specific interface (trait).  In this case, we further
 * distinguish between domains that provide a default implementation (per ''interface''
 * only one of these `Domain`s can be used to create a '''final `Domain`''') and
 * those that can be stacked and basically refine the overall functionality.
 *
 * '''Examples'''
 *  - Domains That Define a General Interface
 *      - [[Origin]] defines two types which domains that provide information abou the
 *      origin of a value should consider to implement.
 *      - [[TheProject]] defines a standard mechanism how a domain can access the
 *      ''current'' project.
 *      - [[ClassHierarchy]] defines a standard mechanism how to get the project's
 *      class hierarchy.
 *      - ...
 *
 *  - Domains That Provide a Default Implementation
 *      - [[Origin]] defines the functionality to return a value's origin if the value
 *      supports that.
 *      - [[ProjectBasedClassHierarchy]] default implementation of the [[ClassHierarchy]]
 *      trait that uses the project's class hierarchy. Requires that the project is
 *      made available using the standard functionality as defined by [[TheProject]].
 *      - [[DefaultHandlingOfMethodResults]] basically implements a Domain's methods
 *      related to return instructions an uncaught exceptions.
 *      - ...
 *
 *  - Domains That Implement Stackable Functionality
 *      - [[RecordThrownExceptions]] records information about all uncaught exceptions
 *      by intercepting a `Domain`'s respective methods. However, it does provide a
 *      default implementation. Hence, a typical pattern is:
 *      {{{
 *      class MyDomain extends Domain with ...
 *          with DefaultHandlingOfMethodResults with RecordThrownExceptions
 *      }}}
 *
 * ==Thread Safety==
 * Unless explicitly documented, a domain is never thread-safe. The general programming
 * model is to use one `Domain` object per code block/method and therefore, thread-safety
 * is not required for `Domain`s.
 *
 * @author Michael Eichberg
 */
package object domain {

    // currently empty

}
