/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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

/*
 The minified graphs in the test cases correspond to the ones described in the files mentioned in
 the test cases. Make sure that the contents of the minified strings match the file contents, i. e.,
 they represent the same graph. 
 */

// Variables and constants used by the test cases
const CELL_MIN_WIDTH = 50;
const CELL_HEIGHT = 30;
const EDGE_PADDING = 10;

/**
 * Helper function which transforms a DOM node into its textual representation, i. e., the string
 * a browser would convert this node to when appending it to the DOM.
 *
 * @param node The node to convert into HTML.
 * @return {String} The stringified representation of the node.
 */
function domNodeToString(node) {
    let el = document.createElement('div');
    el.appendChild(node);
    return el.innerHTML;
}

/**
 * This test case checks if a graph with several parent-children relations is computed correctly.
 * The test graph corresponds to the file parent-children.json.
 * It applies the following
 * node optimizations: moveChildrenUp
 * edge optimizations: separateOutgoingEdges
 */
QUnit.test('Optimized parent-children test', function (assert) {
    let minifiedGraph = '[{"id":1,"label":"1","level":0,"nodeAttributes":{},"children":[2,3,4]},{"id":2,"label":"2","level":1,"nodeAttributes":{},"children":[5]},{"id":3,"label":"3","level":2,"nodeAttributes":{},"children":[6]},{"id":4,"label":"4","level":3,"nodeAttributes":{},"children":[6]},{"id":5,"label":"5","level":4,"nodeAttributes":{},"children":[7]},{"id":6,"label":"6","level":5,"nodeAttributes":{},"children":[7]},{"id":7,"label":"7","level":6,"nodeAttributes":{},"children":[]}]';
    let jsonGraph = JSON.parse(minifiedGraph);
    let expectedResult = '<svg><defs><marker id="arrowMarker" markerWidth="13" markerHeight="13" refX="10" refY="6" orient="90"><path d="M2,2 L2,11 L10,6 L2,2" style="fill: #000000;"></path></marker></defs><svg x="5" y="65" height="28.2" width="50"><rect height="28.2" width="50" stroke="black" stroke-width="2" fill="white"></rect><text x="10px" y="20px" font-size="14pt" font-family="Arial,sans-serif">1</text></svg><svg x="5" y="185" height="28.2" width="50"><rect height="28.2" width="50" stroke="black" stroke-width="2" fill="white"></rect><text x="10px" y="20px" font-size="14pt" font-family="Arial,sans-serif">2</text></svg><svg x="155" y="185" height="28.2" width="50"><rect height="28.2" width="50" stroke="black" stroke-width="2" fill="white"></rect><text x="10px" y="20px" font-size="14pt" font-family="Arial,sans-serif">3</text></svg><svg x="305" y="185" height="28.2" width="50"><rect height="28.2" width="50" stroke="black" stroke-width="2" fill="white"></rect><text x="10px" y="20px" font-size="14pt" font-family="Arial,sans-serif">4</text></svg><svg x="5" y="305" height="28.2" width="50"><rect height="28.2" width="50" stroke="black" stroke-width="2" fill="white"></rect><text x="10px" y="20px" font-size="14pt" font-family="Arial,sans-serif">5</text></svg><svg x="305" y="305" height="28.2" width="50"><rect height="28.2" width="50" stroke="black" stroke-width="2" fill="white"></rect><text x="10px" y="20px" font-size="14pt" font-family="Arial,sans-serif">6</text></svg><svg x="155" y="425" height="28.2" width="50"><rect height="28.2" width="50" stroke="black" stroke-width="2" fill="white"></rect><text x="10px" y="20px" font-size="14pt" font-family="Arial,sans-serif">7</text></svg><line x1="5" x2="55" y1="125" y2="125" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="5" x2="5" y1="95" y2="125" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="55" x2="105" y1="125" y2="125" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="105" x2="155" y1="125" y2="125" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="155" x2="205" y1="125" y2="125" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="205" x2="255" y1="125" y2="125" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="255" x2="305" y1="125" y2="125" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="5" x2="5" y1="125" y2="155" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="155" x2="155" y1="125" y2="155" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="305" x2="305" y1="125" y2="155" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="5" x2="5" y1="155" y2="185" stroke="black" stroke-width="1" stroke-dasharray="5, 2" marker-end="url(#arrowMarker)"></line><line x1="155" x2="155" y1="155" y2="185" stroke="black" stroke-width="1" stroke-dasharray="5, 2" marker-end="url(#arrowMarker)"></line><line x1="305" x2="305" y1="155" y2="185" stroke="black" stroke-width="1" stroke-dasharray="5, 2" marker-end="url(#arrowMarker)"></line><line x1="5" x2="5" y1="215" y2="245" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="155" x2="205" y1="245" y2="245" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="155" x2="155" y1="215" y2="245" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="205" x2="255" y1="245" y2="245" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="255" x2="305" y1="245" y2="245" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="305" x2="305" y1="215" y2="245" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="5" x2="5" y1="245" y2="275" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="305" x2="305" y1="245" y2="275" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="5" x2="5" y1="275" y2="305" stroke="black" stroke-width="1" stroke-dasharray="5, 2" marker-end="url(#arrowMarker)"></line><line x1="305" x2="305" y1="275" y2="305" stroke="black" stroke-width="1" stroke-dasharray="5, 2" marker-end="url(#arrowMarker)"></line><line x1="5" x2="55" y1="365" y2="365" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="5" x2="5" y1="335" y2="365" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="55" x2="105" y1="365" y2="365" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="105" x2="155" y1="365" y2="365" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="155" x2="205" y1="365" y2="365" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="205" x2="255" y1="365" y2="365" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="255" x2="305" y1="365" y2="365" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="305" x2="305" y1="335" y2="365" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="155" x2="155" y1="365" y2="395" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="155" x2="155" y1="395" y2="425" stroke="black" stroke-width="1" stroke-dasharray="5, 2" marker-end="url(#arrowMarker)"></line></svg>';

    let optimizations = {
        nodeOptimizations: [moveChildrenUp],
        edgeOptimizations: [separateOutgoingEdges]
    };

    let computedGraph =
        graphToSvg(jsonGraph, optimizations, CELL_MIN_WIDTH, CELL_HEIGHT, EDGE_PADDING);
    let actualResult = domNodeToString(computedGraph.svg);

    assert.equal(actualResult, expectedResult);
});

/**
 * This test case checks if a graph with one backedge is computed correctly. The test graph
 * corresponds to the file graph-backedge.json.
 * It applies the following
 * node optimizations: moveChildrenUp
 * edge optimizations: separateOutgoingEdges
 */
QUnit.test('Optimized graph with backedge test', function (assert) {
    let minifiedGraph = '[{"id":1,"label":"1","level":0,"nodeAttributes":{},"children":[2,3,4]},{"id":2,"label":"2","level":1,"nodeAttributes":{},"children":[]},{"id":3,"label":"3","level":2,"nodeAttributes":{},"children":[]},{"id":4,"label":"4","level":3,"nodeAttributes":{},"children":[5]},{"id":5,"label":"5","level":4,"nodeAttributes":{},"children":[1]}]';
    let jsonGraph = JSON.parse(minifiedGraph);
    let expectedResult = '<svg><defs><marker id="arrowMarker" markerWidth="13" markerHeight="13" refX="10" refY="6" orient="90"><path d="M2,2 L2,11 L10,6 L2,2" style="fill: #000000;"></path></marker></defs><svg x="5" y="65" height="28.2" width="50"><rect height="28.2" width="50" stroke="black" stroke-width="2" fill="white"></rect><text x="10px" y="20px" font-size="14pt" font-family="Arial,sans-serif">1</text></svg><svg x="5" y="185" height="28.2" width="50"><rect height="28.2" width="50" stroke="black" stroke-width="2" fill="white"></rect><text x="10px" y="20px" font-size="14pt" font-family="Arial,sans-serif">2</text></svg><svg x="155" y="185" height="28.2" width="50"><rect height="28.2" width="50" stroke="black" stroke-width="2" fill="white"></rect><text x="10px" y="20px" font-size="14pt" font-family="Arial,sans-serif">3</text></svg><svg x="305" y="185" height="28.2" width="50"><rect height="28.2" width="50" stroke="black" stroke-width="2" fill="white"></rect><text x="10px" y="20px" font-size="14pt" font-family="Arial,sans-serif">4</text></svg><svg x="5" y="305" height="28.2" width="50"><rect height="28.2" width="50" stroke="black" stroke-width="2" fill="white"></rect><text x="10px" y="20px" font-size="14pt" font-family="Arial,sans-serif">5</text></svg><line x1="5" x2="55" y1="35" y2="35" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="55" x2="105" y1="35" y2="35" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="5" x2="5" y1="35" y2="65" stroke="black" stroke-width="1" stroke-dasharray="5, 2" marker-end="url(#arrowMarker)"></line><line x1="105" x2="105" y1="35" y2="65" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="105" x2="105" y1="65" y2="95" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="5" x2="55" y1="125" y2="125" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="5" x2="5" y1="95" y2="125" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="55" x2="105" y1="125" y2="125" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="105" x2="155" y1="125" y2="125" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="105" x2="105" y1="95" y2="125" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="155" x2="205" y1="125" y2="125" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="205" x2="255" y1="125" y2="125" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="255" x2="305" y1="125" y2="125" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="5" x2="5" y1="125" y2="155" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="105" x2="105" y1="125" y2="155" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="155" x2="155" y1="125" y2="155" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="305" x2="305" y1="125" y2="155" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="5" x2="5" y1="155" y2="185" stroke="black" stroke-width="1" stroke-dasharray="5, 2" marker-end="url(#arrowMarker)"></line><line x1="105" x2="105" y1="155" y2="185" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="155" x2="155" y1="155" y2="185" stroke="black" stroke-width="1" stroke-dasharray="5, 2" marker-end="url(#arrowMarker)"></line><line x1="305" x2="305" y1="155" y2="185" stroke="black" stroke-width="1" stroke-dasharray="5, 2" marker-end="url(#arrowMarker)"></line><line x1="105" x2="105" y1="185" y2="215" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="5" x2="55" y1="245" y2="245" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="55" x2="105" y1="245" y2="245" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="105" x2="155" y1="245" y2="245" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="105" x2="105" y1="215" y2="245" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="155" x2="205" y1="245" y2="245" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="205" x2="255" y1="245" y2="245" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="255" x2="305" y1="245" y2="245" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="305" x2="305" y1="215" y2="245" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="5" x2="5" y1="245" y2="275" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="105" x2="105" y1="245" y2="275" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="5" x2="5" y1="275" y2="305" stroke="black" stroke-width="1" stroke-dasharray="5, 2" marker-end="url(#arrowMarker)"></line><line x1="105" x2="105" y1="275" y2="305" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="105" x2="105" y1="305" y2="335" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="5" x2="55" y1="365" y2="365" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="5" x2="5" y1="335" y2="365" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="55" x2="105" y1="365" y2="365" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="105" x2="105" y1="335" y2="365" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line></svg>';

    let optimizations = {
        nodeOptimizations: [moveChildrenUp],
        edgeOptimizations: [separateOutgoingEdges]
    };

    let computedGraph =
        graphToSvg(jsonGraph, optimizations, CELL_MIN_WIDTH, CELL_HEIGHT, EDGE_PADDING);
    let actualResult = domNodeToString(computedGraph.svg);

    assert.equal(actualResult, expectedResult);
});

/**
 * This test case checks if a graph is computed correctly when it has long and / or multi line
 * labels. The test graph corresponds to the file graph-textual-labels.json.
 * It applies the following
 * node optimizations: moveChildrenUp
 * edge optimizations: separateOutgoingEdges
 */
QUnit.test('Optimized graph label test', function (assert) {
    let minifiedGraph = '[{"id":1,"label":"This is some rather long text, right?","level":0,"nodeAttributes":{},"children":[2,3,4]},{"id":2,"label":"Lorem Ipsum","level":1,"nodeAttributes":{},"children":[5]},{"id":3,"label":"We can do line\\nbreaks, too","level":2,"nodeAttributes":{},"children":[5]},{"id":4,"label":"Dolor sit amet","level":3,"nodeAttributes":{},"children":[]},{"id":5,"label":"The\\nEnd","level":4,"nodeAttributes":{},"children":[]}]';
    let jsonGraph = JSON.parse(minifiedGraph);
    let expectedResult = '<svg><defs><marker id="arrowMarker" markerWidth="13" markerHeight="13" refX="10" refY="6" orient="90"><path d="M2,2 L2,11 L10,6 L2,2" style="fill: #000000;"></path></marker></defs><svg x="5" y="65" height="28.2" width="320"><rect height="28.2" width="320" stroke="black" stroke-width="2" fill="white"></rect><text x="10px" y="20px" font-size="14pt" font-family="Arial,sans-serif">This is some rather long text, right?</text></svg><svg x="5" y="185" height="28.2" width="140"><rect height="28.2" width="140" stroke="black" stroke-width="2" fill="white"></rect><text x="10px" y="20px" font-size="14pt" font-family="Arial,sans-serif">Lorem Ipsum</text></svg><svg x="425" y="185" height="46.4" width="150"><rect height="46.4" width="150" stroke="black" stroke-width="2" fill="white"></rect><text x="10px" y="20px" font-size="14pt" font-family="Arial,sans-serif">We can do line</text><text x="10px" y="38.2px" font-size="14pt" font-family="Arial,sans-serif">breaks, too</text></svg><svg x="675" y="185" height="28.2" width="140"><rect height="28.2" width="140" stroke="black" stroke-width="2" fill="white"></rect><text x="10px" y="20px" font-size="14pt" font-family="Arial,sans-serif">Dolor sit amet</text></svg><svg x="425" y="321.4" height="46.4" width="60"><rect height="46.4" width="60" stroke="black" stroke-width="2" fill="white"></rect><text x="10px" y="20px" font-size="14pt" font-family="Arial,sans-serif">The</text><text x="10px" y="38.2px" font-size="14pt" font-family="Arial,sans-serif">End</text></svg><line x1="5" x2="325" y1="125" y2="125" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="5" x2="5" y1="95" y2="125" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="325" x2="375" y1="125" y2="125" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="375" x2="425" y1="125" y2="125" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="425" x2="575" y1="125" y2="125" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="575" x2="625" y1="125" y2="125" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="625" x2="675" y1="125" y2="125" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="5" x2="5" y1="125" y2="155" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="425" x2="425" y1="125" y2="155" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="675" x2="675" y1="125" y2="155" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="5" x2="5" y1="155" y2="185" stroke="black" stroke-width="1" stroke-dasharray="5, 2" marker-end="url(#arrowMarker)"></line><line x1="425" x2="425" y1="155" y2="185" stroke="black" stroke-width="1" stroke-dasharray="5, 2" marker-end="url(#arrowMarker)"></line><line x1="675" x2="675" y1="155" y2="185" stroke="black" stroke-width="1" stroke-dasharray="5, 2" marker-end="url(#arrowMarker)"></line><line x1="5" x2="325" y1="245" y2="245" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="5" x2="5" y1="215" y2="245" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="325" x2="375" y1="245" y2="245" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="375" x2="425" y1="245" y2="245" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="425" x2="425" y1="231.4" y2="261.4" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="425" x2="425" y1="261.4" y2="291.4" stroke="black" stroke-width="1" stroke-dasharray="5, 2"></line><line x1="425" x2="425" y1="291.4" y2="321.4" stroke="black" stroke-width="1" stroke-dasharray="5, 2" marker-end="url(#arrowMarker)"></line></svg>';

    let optimizations = {
        nodeOptimizations: [moveChildrenUp],
        edgeOptimizations: [separateOutgoingEdges]
    };

    let computedGraph =
        graphToSvg(jsonGraph, optimizations, CELL_MIN_WIDTH, CELL_HEIGHT, EDGE_PADDING);
    let actualResult = domNodeToString(computedGraph.svg);

    assert.equal(actualResult, expectedResult);
});