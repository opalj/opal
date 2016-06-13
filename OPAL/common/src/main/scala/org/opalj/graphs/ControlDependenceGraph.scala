///* BSD 2-Clause License:
// * Copyright (c) 2009 - 2014
// * Software Technology Group
// * Department of Computer Science
// * Technische Universität Darmstadt
// * All rights reserved.
// *
// * Redistribution and use in source and binary forms, with or without
// * modification, are permitted provided that the following conditions are met:
// *
// *  - Redistributions of source code must retain the above copyright notice,
// *    this list of conditions and the following disclaimer.
// *  - Redistributions in binary form must reproduce the above copyright notice,
// *    this list of conditions and the following disclaimer in the documentation
// *    and/or other materials provided with the distribution.
// *
// * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
// * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// * POSSIBILITY OF SUCH DAMAGE.
// */
//package org.opalj
//package graphs
//
///**
// * Represents the control-dependence information.
// * 
// * An instruction/statement is control dependent on a predicate (here: `if`, `switch` or any 
// * instruction that may throw an exception) if the value of the predicate 
// * controls the execution of the instruction.
// * Let G be a control flow graph; Let X and Y be nodes in G; Y is control dependent on X iff
// * there exists a directed path P from X to Y with any Z in P \ X is not post-dominated by Y.
// * 
// * Note that in the context of static analysis an invokation instruction that may throw an 
// * exception, which may result in a different control-flow, is also a `predicate` additionally to 
// * all ifs and switches.
// * 
// * @param controlDependentOn Contains for each node of the graph (represented by its positive, 
// * 			unique int id) the information on which node it is directly dependent on or -1 if
// *			the instruction is not control dependent on any other node.  
// * @author Michael Eichberg
// */
//class ControlDependenceGraph private(   private val controlDependentOn: Array[Int]    ) {
//    
//    /**
//     * @return `true` if `y` is directly or indirectly control dependent on `x`, `false` otherwise.
//     */
//    @scala.annotation.tailrec def isYControlDependentOnX(y : Int, x : Int) : Boolean = {
//        controlDependentOn(y) match {
//            case `x` => true
//            case -1 => false
//            case v => isYControlDependentOnX(y,v) 
//        }
//    }
//    
//    /**
//     * @param y A node of the underlying graph.
//     * @param x A node of the underlying graph.
//     * 
//     * @return `true` if `y` is directly control dependent on `x`, `false` otherwise.
//     */
//    def isYDirectlyControlDependentOnX(y : Int, x : Int) : Boolean =   {
//        controlDependentOn(y) == x
//    }
//        
//    
//}
//
///**
// * Factory to compute the control-dependence graph (based on the [[PostDominatorTree]]).
// *
// * @author Michael Eichberg
// */
//object ControlDependenceGraph {
//
//    def fornone(g: Int ⇒ Unit): Unit = { (f: (Int ⇒ Unit)) ⇒ { /*nothing to to*/ } }
//
//    /**
//     * Computes the control-dependence graph. The artificial start node of
//     * the internally used post dominator tree will have the id = (maxNodeId+1).
//     * 
//     * A node (basic block) Y is control-dependent on another X iff X determines whether Y 
//     * executes, i.e.
//	 * 	-	there exists a path from X to Y such that every node in the path other than X & Y is 
//	 * 		post-dominated by Y
//	 * 	-	X is not post-dominated by Y
//     *
//     * @example
//     * {{{
//     * scala>//Graph: 0 -> 1->E;  1 -> 2->E
//     * scala>def isExitNode(i: Int) = i == 1 || i == 2
//     * isExitNode: (i: Int)Boolean
//     *
//     * scala>def foreachExitNode(f: Int ⇒ Unit) = { f(1); f(2) }
//     * foreachExitNode: (f: Int => Unit)Unit
//     *
//     * scala>def foreachPredecessorOf(i: Int)(f: Int ⇒ Unit) = i match {
//     *      |    case 0 ⇒
//     *      |    case 1 ⇒ f(0)
//     *      |    case 2 ⇒ f(1)
//     *      |}
//     * foreachPredecessorOf: (i: Int)(f: Int => Unit)Unit
//     * scala>def foreachSuccessorOf(i: Int)(f: Int ⇒ Unit) = i match {
//     *      |    case 0 ⇒ f(1)
//     *      |    case 1 ⇒ f(2)
//     *      |    case 2 ⇒
//     *      |}
//     * foreachSuccessorOf: (i: Int)(f: Int => Unit)Unit
//     * scala>val cdg = org.opalj.graphs.ControlDependenceGraph.apply(
//     *      |    isExitNode,
//     *      |    foreachExitNode,
//     *      |    foreachSuccessorOf,
//     *      |    foreachPredecessorOf,
//     *      |    2
//     *      |)
//     * cdg: org.opalj.graphs.ControlDependenceGraph = ...
//     * scala>cdg.toDot()
//     * }}}
//     */
//    def apply(
//        isExitNode: Int ⇒ Boolean,
//        foreachExitNode: (Int ⇒ Unit) ⇒ Unit,
//        foreachSuccessorOf: Int ⇒ ((Int ⇒ Unit) ⇒ Unit),
//        foreachPredecessorOf: Int ⇒ ((Int ⇒ Unit) ⇒ Unit),
//        maxNode: Int): (DominatorTree,ControlDependenceGraph)  = {
//
//        
//        val pdt =
//            PostDominatorTree(
//                isExitNode, foreachExitNode,
//                foreachSuccessorOf,
//                foreachPredecessorOf,
//                maxNode
//            )
//
//        val rdf = // reverse dominance frontier    
//            
//       val controlDependentOn = new Array[Int]( maxNode + 1)
//            
//        (pdt,new ControlDependenceGraph(controlDependentOn))
//    }
//
//}
