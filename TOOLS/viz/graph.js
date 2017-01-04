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

/**
 * @author Patrick Mell
 */

// Constants
const API_NOT_SUPPORTED = 'Your browser does not support the necessary APIs (File, FileReader, FileList)';
const COULD_NOT_LOAD_FILE = 'Failed to load file';
const FONT_SIZE = '10pt';
const FONT_FAMILY = 'Helvetica,sans-serif';
const LINE_HEIGHT_FACTOR = 1.3;

// We need this representation for faster accesses
let hashmapGraph = [];

// This array stores the widths of columns. Columns may have different widths in case of long
// labels. The array indices represent the column indices and the values the width in pixels.
let columnWidths = [];
// This object stores the heights of cells. Rows may have different heights in case of multi line
// labels. The object has numerical column indices as keys and an array of numbers as value. This
// array represents the cell heights in pixels
let rowHeights = {};

/**
 * Checks if the necessary APIs (File, FileReader, FileList) are available.
 * @return Returns true if they are available, else false.
 */
let isApiAvailable = function () {
    return window.File && window.FileReader && window.FileList;
};

/**
 * Converts a given rawGraph into an SVG object.
 *
 * @param rawGraph The graph as a JSON object. See the Readme.md for a structural description of
 * the graph.
 * @param optimizations An object with two properties: optimizations and edgeOptimizations.
 * Both are arrays of functions where each item refers to a node optimization
 * step.
 * For the nodeOptimizations pass functions that take one argument: node positions. For this
 * structure, see getPreliminaryNodePositions for instance. The steps are applied in ascending
 * order.
 * For edgeOptimizations pass functions that take two arguments: A matrix and node positions.
 * @param minCellWidth The width in pixels a cell is guaranteed to have. Depending on the label of
 * a node the width might be larger.
 * @param cellHeight The height in pixels of a cell.
 * @param edgePadding In some cases, edges may be placed either above or next to each other. This
 * value in pixels determine the distance between such lines.
 * @return {Object} Returns an object consisting of three properties:
 * - 'svg' stores the SVG which represents the graph and can be attached to the DOM, for instance
 * - 'canvasWidth' stores the minimum width in pixels a canvas should have in order to display the
 *   graph properly
 * - 'canvasHeight' stores the minimum height in pixels a canvas should have in order to display the
 *   graph properly
 */
let graphToSvg = function (rawGraph, optimizations, minCellWidth, cellHeight, edgePadding) {
    // These constants denote the number of free vertical and horizontal cells between node cells
    // (note that a value of four means three free cells, five means four and so on)
    let VERTICAL_PADDING = 4;
    let HORIZONTAL_PADDING = 3;

    // Reset objects
    columnWidths = [];
    rowHeights = {};

    // Convert into a hashmap for constant access times of nodes
    hashmapGraph = graphToHashmap(rawGraph);

    let nodePositions = getPreliminaryNodePositions(rawGraph);
    // Apply node optimizations
    optimizations.nodeOptimizations.forEach(f => nodePositions = f(nodePositions));

    // Map the nodes and edges to a matrix
    let matrixNodePositions = getMatrixNodePositions(nodePositions, VERTICAL_PADDING, HORIZONTAL_PADDING);
    let matrix = addNodePositionsToMatrix(matrixNodePositions);
    matrix = addEdgesToMatrix(matrix, matrixNodePositions, VERTICAL_PADDING);
    // Apply edge optimizations
    optimizations.edgeOptimizations.forEach(f => matrix = f(matrix, matrixNodePositions));

    // Convert the matrix to SVG
    console.log(matrix);
    return matrixToSVG(matrix, cellHeight, minCellWidth, edgePadding);
};

/**
 * Converts the given graph into a (very simple) hashmap.
 *
 * @param graph The graph structure as it was provided by the JSON file.
 * @return {Array} Returns an array where the (stringified) IDs of nodes are the keys and the
 * whole object the value.
 */
function graphToHashmap(graph) {
    let hashmap = [];
    for (let i = 0; i < graph.length; i++) {
        hashmap[String(graph[i].id)] = graph[i];
    }
    return hashmap;
}

/**
 * This function simply converts the given graph to initial node positions. At this stage no
 * optimizations whatsoever are applied.
 *
 * @param graph The graph as JSON.
 * @return {Array} Returns an array where there is an object for each node (access it by the node's
 * ID) with two properties: x, y. These indicate the preliminary positions of the nodes in the
 * matrix.
 */
let getPreliminaryNodePositions = function (graph) {
    // "Constructor" for a node position (serves the purpose of avoiding redundancies).
    function createNodePosition(x, y) {
        return {x: x, y: y};
    }

    if (graph.length == 0) {
        return [];
    }

    let nodePositions = [];
    let queue = [graph[0].id];

    while (queue.length > 0) {
        let nextNode = hashmapGraph[queue[0]];
        queue.shift();

        // Position the current node if necessary (will only be executed for root nodes)
        if (nodePositions[nextNode.id] === undefined) {
            nodePositions[nextNode.id] = createNodePosition(0, nextNode.level);
        }
        // Position the children
        let pushToQueue = [];
        let horizontalIndex = 0;
        nextNode.children.forEach(function (element) {
            let child = hashmapGraph[element];
            if (nodePositions[child.id] === undefined) {
                nodePositions[child.id] = createNodePosition(horizontalIndex, child.level);
                horizontalIndex++;
                pushToQueue.unshift(child.id);
            }
        });
        // Add unseen children to queue
        pushToQueue.forEach(function (element) {
            queue.unshift(element);
        });
    }

    return nodePositions;
};

/**
 * Applies the optimization step which moves all children of a node on the same level.
 *
 * @param nodePositions An array of objects that were created by the 'createNodePosition' function.
 * @return {Array} Returns the modified nodePositions array.
 */
let moveChildrenUp = function (nodePositions) {
    // The indices represent row numbers and the value the number of elements in that row
    let numberElementsInRow = [];
    // Stores all positions children => children will be positioned exactly once!
    let processedChildren = [];

    // Change the node positions in place
    nodePositions.forEach(function (position, index) {
        let nextNode = hashmapGraph[index];
        let childRow = position.y + 1;
        if (numberElementsInRow[childRow] === undefined) {
            numberElementsInRow[childRow] = 0;
        }
        // Move all children to the same level
        nextNode.children.forEach(function (childElement) {
            if (processedChildren.indexOf(childElement) == -1) {
                nodePositions[childElement].x = numberElementsInRow[childRow];
                nodePositions[childElement].y = childRow;
                numberElementsInRow[childRow]++;
            }
        });
        // Avoid that nodes that have incoming back edges are re-positioned
        if (processedChildren.indexOf(index) == -1) {
            processedChildren.push(index);
        }
    });

    return nodePositions;
};

/**
 * It can lead to ambiguities when a node has an outgoing forward and backward edge. This function
 * thus serves as an edge optimization step which does the following: Outgoing lines to backward
 * edges are set one padding unit to the right of outgoing forward edges.
 *
 * @param matrix The matrix to make the changes to. The matrix already contains nodes and edges.
 * @param nodePositions An array where node IDs are the key and the node positions the values.
 * @return {Array} Returns the modified matrix.
 */
let separateOutgoingEdges = function (matrix, nodePositions) {
    hashmapGraph.forEach(function (value) {
        let thisPosition = nodePositions[value.id];
        // Gather outgoing forward and backward edge of this node
        let forwardEdgeNodeIds = [], backwardEdgeNodeIds = [];
        value.children.forEach(function (childId) {
            let childPosition = nodePositions[childId];
            if (childPosition.y > thisPosition.y) {
                forwardEdgeNodeIds.push(childId);
            } else if (childPosition.y < thisPosition.y) {
                backwardEdgeNodeIds.push(childId);
            }
        });
        // Reset outgoing backward line if necessary
        if (forwardEdgeNodeIds.length > 0 && backwardEdgeNodeIds.length > 0) {
            thisPosition.y++;
            let borderLeft = matrix[thisPosition.y][thisPosition.x].borderLeft;
            for (let i = 0; i < borderLeft.targetNodes.length; i++) {
                if (backwardEdgeNodeIds.indexOf(borderLeft.targetNodes[i]) > -1) {
                    borderLeft.linePaddings[i]++;
                }
            }
        }
    });
    return matrix;
};

/**
 * @param nodePositions The positions of the nodes to map to the matrix.
 * @param verticalPadding The number of vertical empty cells between nodes + 1.
 * @param horizontalPadding The number of horizontal empty cells between nodes + 1.
 * @return {Array} Returns the modified node positions. These can be directed mapped to the matrix.
 */
function getMatrixNodePositions(nodePositions, verticalPadding, horizontalPadding) {
    nodePositions.forEach(function (element, index) {
        nodePositions[index].x *= horizontalPadding;
        nodePositions[index].y = nodePositions[index].y * verticalPadding + 2;
    });
    return nodePositions;
}

// "Constructor" for a matrix cell element
function createMatrixCell() {
    return {
        nodeId: -1,
        cellContent: '',
        borderBottom: {linePaddings: [], sourceNodes: [], targetNodes: []},
        borderLeft: {linePaddings: [], sourceNodes: [], targetNodes: []}
    };
}

/**
 * As nodes of different columns can have different widths, the offset of a column can not be
 * computed by column_width * column_index. This function computes the offset for columns.
 *
 * @param column The column to get the offset for.
 * @param minCellWidth The minimum width of a cell.
 * @return {number} Returns the x-axis offset value.
 */
function getXOffsetForColumn(column, minCellWidth) {
    let sum = 0;
    for (let i = 0; i < column; i++) {
        sum += (columnWidths[i] !== undefined || columnWidths[i] < minCellWidth)
            ? columnWidths[i] : minCellWidth;
    }
    return sum;
}

/**
 * As nodes of different rows can have different heights, the vertical offset of a cell can not be
 * computed by row_height * row_index. This function computes the offset for cells.
 *
 * @param row The row to get the offset for.
 * @param column The column which specifies the cell to get the offset for.
 * @param minCellHeight The minimum height of a cell.
 * @return {number} Returns the y-axis offset value for the specified cell in pixels.
 */
function getYOffsetForRow(row, column, minCellHeight) {
    let sum = 0;
    for (let i = 0; i < row; i++) {
        if (rowHeights[column] === undefined || rowHeights[column][i] === undefined ||
            rowHeights[column][i] < minCellHeight) {
            sum += minCellHeight;
        } else {
            sum += rowHeights[column][i];
        }
    }
    return sum;
}

/**
 * This function maps the computed node positions to a matrix. Using padding, the vertical and
 * horizontal distance between cells can be set.
 *
 * @param matrixNodePositions The positions of the nodes to map to the matrix.
 * @return {Array} Returns an array of arrays where each item of the inner arrays represents a
 * matrix cell. The outer array denote the rows, the inner the columns of the matrix.
 */
function addNodePositionsToMatrix(matrixNodePositions) {

    // Computes the preliminary size of the matrix (it might be that the size has to be increased
    // later on). An object with the following properties is returned: height, width.
    function getInitialMatrixDim() {
        let width = 0;
        let height = 0;
        matrixNodePositions.forEach(function (element) {
            width = (width > element.x) ? width : element.x;
            height = (height > element.y) ? height : element.y;
        });

        return {
            width: width,
            // +1 because we want an additional cell for outgoing edges of the lowest nodes
            height: height + 1
        };
    }

    /**
     * @param dimension An object that has the properties: height, width.
     * @return {Array} Returns an array of arrays where the inner elements represent matrix cells
     * created by createMatrixCell. The outer array denote the rows, the inner the columns of the
     * matrix.
     */
    function initializeMatrix(dimension) {
        let matrix = [];
        for (let i = 0; i <= dimension.height; i++) {
            matrix.push([]);
            for (let j = 0; j <= dimension.width; j++) {
                matrix[i].push(createMatrixCell());
            }
        }
        return matrix;
    }

    function addNodesToMatrix(matrix) {
        // Compute the node positions and add them to the matrix (we keep the first row unused for
        // incoming edges)
        matrixNodePositions.forEach(function (element, index) {
            matrix[element.y][element.x].nodeId = hashmapGraph[index].id;
            matrix[element.y][element.x].cellContent = hashmapGraph[index].label;
        });
        return matrix;
    }

    let matrix = initializeMatrix(getInitialMatrixDim());
    return addNodesToMatrix(matrix);
}

/**
 * @param matrix The matrix to modify, i.e., add edges.
 * @param matrixNodePositions An array with node IDs as keys and their positions in the matrix as
 * value.
 * @param verticalPadding
 * @return {Array} The extended matrix including forward and backward edges.
 */
function addEdgesToMatrix(matrix, matrixNodePositions, verticalPadding) {

    // Append a column to the matrix (on the right) 
    function appendColumnToMatrix() {
        matrix.forEach(function (row) {
            row.push(createMatrixCell());
        });
        return matrix;
    }

    function markMatrixCellBorder(border, offset, sourceNodeId, targetNodeId) {
        border.linePaddings.push(offset);
        border.sourceNodes.push(sourceNodeId);
        border.targetNodes.push(targetNodeId);
    }

    // This function checks if an edge is simply a vertical line
    function canInsertVerticalEdge(fromPosition, toPosition) {
        if (fromPosition.x != toPosition.x) {
            return false;
        } else {
            let y1 = fromPosition.y, y2 = toPosition.y;
            // Make sure fromPosition.y is smaller than toPosition.y
            if (y1 > y2) {
                let tmp = y1;
                y2 = y1;
                y1 = tmp;
            }
            // Check and return
            return (y1 + verticalPadding == y2);
        }
    }

    // Inserts a vertical edge
    function addVerticalEdge(fromPosition, toPosition, sourceNodeId, targetNodeId) {
        // Make sure fromPosition.y is smaller than toPosition.y
        if (fromPosition.y > toPosition.y) {
            let tmp = fromPosition;
            fromPosition = toPosition;
            toPosition = tmp;
        }

        // Mark the path in the matrix
        for (let i = fromPosition.y + 1; i < toPosition.y; i++) {
            markMatrixCellBorder(matrix[i][fromPosition.x].borderLeft, 0, sourceNodeId, targetNodeId);
        }

        return matrix;
    }

    // Add a vertical forward edge (fromPosition.x = toPosition.x) between two nodes; it is assumed
    // that between the two nodes there exist other nodes
    function addVerticalEdgeAcrossLevels(fromPosition, toPosition, sourceNodeId, targetNodeId) {
        // First, we add columns to the matrix if necessary
        while (fromPosition.x + 2 >= matrix[0].length) {
            matrix = appendColumnToMatrix();
        }

        // Add the outgoing edge of the source node
        let cellBorder = matrix[fromPosition.y + 1][fromPosition.x].borderLeft;
        markMatrixCellBorder(cellBorder, 0, sourceNodeId, targetNodeId);

        // Mark the horizontal line which will lead to the vertical one
        let offset = getHorizontalOffset(fromPosition.y + 1, fromPosition.x, fromPosition.x + 1, sourceNodeId);
        cellBorder = matrix[fromPosition.y + 1][fromPosition.x].borderBottom;
        markMatrixCellBorder(cellBorder, offset, sourceNodeId, targetNodeId);
        cellBorder = matrix[fromPosition.y + 1][fromPosition.x + 1].borderBottom;
        markMatrixCellBorder(cellBorder, offset, sourceNodeId, targetNodeId);

        // Mark the vertical line
        let fromY = fromPosition.y, toY = toPosition.y;
        if (fromY < toY) {
            fromY += 2;
            toY -= 2;
        } else {
            fromY = toY - 2;
            toY = fromPosition.y + 1;
        }
        offset = getVerticalOffset(fromPosition.x + 2, fromY, toY, sourceNodeId);
        for (let i = fromY; i <= toY; i++) {
            cellBorder = matrix[i][fromPosition.x + 2].borderLeft;
            markMatrixCellBorder(cellBorder, offset, sourceNodeId, targetNodeId)
        }

        // Mark the border right above the target node
        offset = getHorizontalOffset(toPosition.y - 2, toPosition.x, toPosition.x + 1, sourceNodeId);
        cellBorder = matrix[toPosition.y - 2][toPosition.x].borderBottom;
        markMatrixCellBorder(cellBorder, offset, sourceNodeId, targetNodeId);
        cellBorder = matrix[toPosition.y - 2][toPosition.x + 1].borderBottom;
        markMatrixCellBorder(cellBorder, offset, sourceNodeId, targetNodeId);

        // Mark the incoming line
        cellBorder = matrix[toPosition.y - 1][toPosition.x].borderLeft;
        markMatrixCellBorder(cellBorder, 0, sourceNodeId, targetNodeId);

        return matrix;
    }

    // This function assumes that x1 <= x2
    function getHorizontalOffset(row, x1, x2, sourceNodeId) {
        let offset = 0;
        for (let i = x1; i <= x2; i++) {
            let indexSourceId = matrix[row][i].borderBottom.sourceNodes.indexOf(sourceNodeId);
            if (indexSourceId > -1) {
                offset = indexSourceId;
            } else {
                let length = matrix[row][i].borderBottom.linePaddings.length;
                offset = (length > offset) ? length : offset;
            }
        }
        return offset;
    }

    // This function assumes that y1 < y2
    function getVerticalOffset(column, y1, y2, sourceNodeId) {
        let offset = 0;
        for (let i = y1; i <= y2; i++) {
            let indexSourceId = matrix[i][column].borderLeft.sourceNodes.indexOf(sourceNodeId);
            if (indexSourceId > -1) {
                offset = indexSourceId;
            } else {
                let length = matrix[i][column].borderLeft.linePaddings.length;
                offset = (length > offset) ? length : offset;
            }
        }
        return offset;
    }

    // Adds a forward edge to a node on the next level; it is assumed that
    // fromPosition.x != toPosition.x
    function addEdgeToNextLevelNode(fromPosition, toPosition, sourceNodeId, targetNodeId) {
        let x1 = fromPosition.x, x2 = toPosition.x;
        let y1 = fromPosition.y, y2 = toPosition.y;

        // Mark the edge directly below the outgoing node
        let cellBorder = matrix[y1 + 1][x1].borderLeft;
        markMatrixCellBorder(cellBorder, 0, sourceNodeId, targetNodeId);

        // Mark the line
        let fromColumn = (x1 < x2) ? x1 : x2;
        let toColumn = (x1 > x2) ? x1 : x2;
        for (let i = fromColumn; i < toColumn; i++) {
            cellBorder = matrix[y1 + 1][i].borderBottom;
            markMatrixCellBorder(cellBorder, 0, sourceNodeId, targetNodeId);
        }

        // Vertical line into the target node; here, it is not necessary to determine
        for (let i = y1 + 2; i < y2; i++) {
            cellBorder = matrix[i][x2].borderLeft;
            markMatrixCellBorder(cellBorder, 0, sourceNodeId, targetNodeId);
        }

        return matrix;
    }

    // Adds a forward edge to a node that is not on the same level and not on the same vertical line
    function addForwardEdge(fromPosition, toPosition, sourceNodeId, targetNodeId) {
        let x1 = fromPosition.x, x2 = toPosition.x;
        let y1 = fromPosition.y, y2 = toPosition.y;

        // Mark the edge directly below the outgoing node
        let cellBorder = matrix[y1 + 1][x1].borderLeft;
        markMatrixCellBorder(cellBorder, 0, sourceNodeId, targetNodeId);

        // Find and mark the horizontal line just below the source node
        let startX = x1, endX = x2;
        if (x1 < x2) {
            x2 -= 2;
        } else {
            startX = x2 + 2;
            endX = x1 - 1;
        }
        let offset = getHorizontalOffset(y1 + 1, startX, endX, sourceNodeId);
        for (let i = startX; i <= endX; i++) {
            cellBorder = matrix[y1 + 1][i].borderBottom;
            markMatrixCellBorder(cellBorder, offset, sourceNodeId, targetNodeId);
        }

        // Find and mark the vertical line of the edge
        let column = (x2 > x1) ? x2 - 1 : x2 + 2;
        let startY = y1 + 2, endY = y2 - 2;
        // (Add columns to the matrix if necessary)
        while (column + 1 >= matrix[0].length) {
            matrix = appendColumnToMatrix();
        }
        offset = getVerticalOffset(column, startY, endY, sourceNodeId);
        for (let i = startY; i <= endY; i++) {
            cellBorder = matrix[i][column].borderLeft;
            markMatrixCellBorder(cellBorder, offset, sourceNodeId, targetNodeId);
        }

        // Find and mark the horizontal line just above the target node
        startX = (x2 > x1) ? column : toPosition.x;
        endX = (x2 > x1) ? toPosition.x : column - 1;
        offset = getHorizontalOffset(endY, startX, endX, sourceNodeId);
        for (let i = startX; i <= endX; i++) {
            cellBorder = matrix[endY][i].borderBottom;
            markMatrixCellBorder(cellBorder, offset, sourceNodeId, targetNodeId);
        }

        // Mark the incoming edge of the target node, then we are done
        let matrixCell = matrix[y2 - 1][x2];
        markMatrixCellBorder(matrixCell.borderLeft, 0, sourceNodeId, targetNodeId);

        return matrix;
    }

    // Adds an arbitrary backedge
    function addBackedge(fromPosition, toPosition, sourceNodeId, targetNodeId) {
        let x1 = fromPosition.x, x2 = toPosition.x;
        let y1 = fromPosition.y, y2 = toPosition.y;

        // Mark the edge directly below the outgoing node
        let cellBorder = matrix[y1 + 1][x1].borderLeft;
        markMatrixCellBorder(cellBorder, 0, sourceNodeId, targetNodeId);

        // Mark the horizontal line
        let offset = getHorizontalOffset(y1 + 1, x1, x1 + 2, sourceNodeId);
        for (let i = x1; i <= x1 + 1; i++) {
            cellBorder = matrix[y1 + 1][i].borderBottom;
            markMatrixCellBorder(cellBorder, offset, sourceNodeId, targetNodeId);
        }

        // Mark the vertical line (from bottom to top)
        offset = getVerticalOffset(x1 + 2, y2 + 1, y1 - 1, sourceNodeId);
        for (let i = y1 + 1; i >= y2 - 1; i--) {
            cellBorder = matrix[i][x1 + 2].borderLeft;
            markMatrixCellBorder(cellBorder, offset, sourceNodeId, targetNodeId);
        }

        // Mark the horizontal line to the target node
        let fromX = x1 + 2, toX = x2 - 1;
        if (fromX > toX) {
            fromX = x1;
            toX = x1 + 1;
        }
        offset = getHorizontalOffset(y2 - 2, fromX, toX, sourceNodeId);
        for (let i = fromX; i <= toX; i++) {
            cellBorder = matrix[y2 - 2][i].borderBottom;
            markMatrixCellBorder(cellBorder, offset, sourceNodeId, targetNodeId);
        }

        // Mark the two lines right above the target node
        let matrixCell = matrix[y2 - 1][x2];
        markMatrixCellBorder(matrixCell.borderLeft, 0, sourceNodeId, targetNodeId);

        return matrix;
    }

    // Loop through all 'From' nodes
    hashmapGraph.forEach(function (currentNode, parentId) {
        let currentPositions = matrixNodePositions[parentId];
        // Loop through all 'To' nodes
        currentNode.children.forEach(function (childrenId) {
            let childPosition = matrixNodePositions[childrenId];
            // Check if we can just draw a vertical line (the easy case)
            if (canInsertVerticalEdge(currentPositions, childPosition)) {
                matrix = addVerticalEdge(currentPositions, childPosition, parentId, childrenId);
            }
            // Nodes are on the same vertical line with other nodes being in between and this is a
            // forward edge
            else if (currentPositions.x == childPosition.x && currentPositions.y < childPosition.y) {
                matrix = addVerticalEdgeAcrossLevels(currentPositions, childPosition, parentId, childrenId);
            }
            // Add edge to node on the next level
            else if (currentPositions.y + verticalPadding == childPosition.y) {
                matrix = addEdgeToNextLevelNode(currentPositions, childPosition, parentId, childrenId);
            }
            // Add a forward edge to a node that is not on the next level
            else if (currentPositions.y + verticalPadding < childPosition.y) {
                matrix = addForwardEdge(currentPositions, childPosition, parentId, childrenId)
            }
            // Add backedge (any sort of backedge)
            else if (currentPositions.y > childPosition.y) {
                matrix = addBackedge(currentPositions, childPosition, parentId, childrenId);
            }
        });
    });

    return matrix;
}

/**
 * This function converts the matrix to an SVG element.
 *
 * @param matrix The matrix to convert into an SVG graphic.
 * @param cellHeight The height of a cell in the SVG graphic.
 * @param cellMinWidth The minimum width of a cell in the SVG graphic.
 * @param linePadding The distance in pixels between edges that shall not overlap.
 * @return {Object} Returns an object consisting of three properties:
 * - 'svg' stores the SVG which represents the graph
 * - 'canvasWidth' stores the minimum width a canvas should have in order to display the graph
 *   properly
 * - 'canvasHeight' stores the minimum height a canvas should have in order to display the graph
 *   properly
 */
let matrixToSVG = function (matrix, cellHeight, cellMinWidth, linePadding) {
    // Constants
    const NAMESPACE_SVG = 'http://www.w3.org/2000/svg';
    const BORDER_BOTTOM = 0;
    const BORDER_LEFT = 1;
    const SVG_PADDING = 5;

    /**
     * @param row The row in the matrix.
     * @param column The column in the matrix.
     * @param text Text to put in the rectangle.
     * @param nodeAttributes An object that specifies further node styling attributes. Object keys
     * represent attributes names and object values their corresponding values
     * @return {Element} Returns an SVG element that represents a rectangle.
     */
    function createRect(row, column, text, nodeAttributes) {

        /**
         * Uses canvas.measureText to compute and return the width of the given text of given font.
         * The code has been taken from:
         * http://stackoverflow.com/questions/118241/calculate-text-width-with-javascript/21015393#21015393
         * as of 2016-12-16.
         *
         * @param {String} text The text to be rendered.
         * @return {Number} Returns the width of the text in pixel.
         */
        function getTextWidth(text) {
            let canvas = getTextWidth.canvas ||
                (getTextWidth.canvas = document.createElement("canvas"));
            let context = canvas.getContext("2d");
            context.font = FONT_SIZE + ' ' + FONT_FAMILY;
            let metrics = context.measureText(text);
            // We need the following additional computation with metrics.width for the following
            // reason: Firefox and Chrome/Chromium can compute different metrics.width for the same
            // text; there might be a deviation of some pixels. Thus, we round up to the next
            // number that is dividable by 10. I am aware that there are cases where the result is
            // different, however, for the test cases this is enough and it has no practical impact
            return Math.ceil(metrics.width / 10) * 10;
        }

        // Get the width for the node
        let labels = text.split(/\n/);
        let rectWidth = labels.reduce((maxWidth, nextLabel) => {
            let currentWidth = getTextWidth(nextLabel);
            return (maxWidth > currentWidth) ? maxWidth : currentWidth;
        }, 0);
        rectWidth += 20;
        // Get the height for the node and check if to update the row heights array if necessary
        let rectHeight = labels.length * parseInt(FONT_SIZE) * LINE_HEIGHT_FACTOR + 10;
        if (rowHeights[column] === undefined) {
            rowHeights[column] = [];
            rowHeights[column][row] = rectHeight;
        } else if (rowHeights[column][row] === undefined || rowHeights[column][row] < rectHeight) {
            rowHeights[column][row] = rectHeight;
        }

        rectWidth = rectWidth < cellMinWidth ? cellMinWidth : rectWidth;
        // Check if to update the array which stores all column widths
        if (columnWidths[column] === undefined || columnWidths[column] < rectWidth) {
            columnWidths[column] = rectWidth;
        }
        // Create the element that will hold the rectangle and the text
        let svgGroup = document.createElementNS(NAMESPACE_SVG, 'svg');
        svgGroup.setAttributeNS('', 'x', String(getXOffsetForColumn(column, cellMinWidth) + SVG_PADDING));
        svgGroup.setAttributeNS('', 'y', String(getYOffsetForRow(row, column, cellHeight) + SVG_PADDING));
        svgGroup.setAttributeNS('', 'height', String(rectHeight));
        svgGroup.setAttributeNS('', 'width', String(rectWidth));

        let rect = document.createElementNS(NAMESPACE_SVG, 'rect');
        rect.setAttributeNS('', 'height', String(rectHeight));
        rect.setAttributeNS('', 'width', String(rectWidth));
        rect.setAttributeNS('', 'stroke', 'black');
        rect.setAttributeNS('', 'stroke-width', String(2));
        rect.setAttributeNS('', 'fill', 'white');

        // Apply custom styles
        if (typeof(nodeAttributes) === 'object') {
            for (let key in nodeAttributes) {
                rect.setAttributeNS('', key, nodeAttributes[key]);
            }
        }

        svgGroup.appendChild(rect);

        if (text !== undefined) {
            labels.forEach((nextLine, index) => {
                let y = (20 + parseInt(FONT_SIZE) * LINE_HEIGHT_FACTOR * index) + 'px';
                let textElement = document.createElementNS(NAMESPACE_SVG, 'text');
                textElement.setAttributeNS('', 'x', '10px');
                textElement.setAttributeNS('', 'y', y);
                textElement.setAttributeNS('', 'font-size', FONT_SIZE);
                textElement.setAttributeNS('', 'font-family', FONT_FAMILY);
                let textNode = document.createTextNode(nextLine);
                textElement.appendChild(textNode);
                svgGroup.appendChild(textElement);
            });
        }

        return svgGroup;
    }

    /**
     * @param row The row in the matrix.
     * @param column The column in the matrix.
     * @param border A value x in {0, 1} where x=0 indicates to draw a border at the bottom of the
     * cell and x=1 to draw a border on the left of the cell.
     * 0: border top; 1: border right; 2: border bottom; 3: border left.
     * @param linePadding It might be that lines are drawn in a way where ambiguities arise. This
     * value in pixels ensures that there is some padding between such lines.
     * @param lineOffset This numerical value in pixels is added to the x1 value of the line to
     * draw. The is relevant, e. g., when a node has an outgoing forward and backward ege.
     * @param extendHorLine Due to the line padding it might be that there is some empty gap between
     * a horizontal and vertical line. This value in pixels "fills" the gap.
     * @param insertArrowEnd If set to true, an arrow marker will be appended to the end of the
     * line.
     * @return {Element} Returns an SVG element that represents the a line as specified. If an
     * invalid border was passed, undefined will be returned.
     */
    function createLine(row, column, border, linePadding, lineOffset, extendHorLine, insertArrowEnd) {
        // Work only with valid borders
        if (border < 0 || border > 1) {
            return undefined;
        }

        // Compute the points that specify the line
        let x1, x2, y1, y2;
        switch (border) {
            case BORDER_BOTTOM:
                x1 = getXOffsetForColumn(column, cellMinWidth) + SVG_PADDING + lineOffset;
                x2 = getXOffsetForColumn(column + 1, cellMinWidth) + SVG_PADDING + extendHorLine;
                y1 = getYOffsetForRow(row + 1, column, cellHeight) + SVG_PADDING + linePadding;
                y2 = getYOffsetForRow(row + 1, column, cellHeight) + SVG_PADDING + linePadding;
                break;
            case BORDER_LEFT:
                x1 = getXOffsetForColumn(column, cellMinWidth) + SVG_PADDING + linePadding;
                x2 = getXOffsetForColumn(column, cellMinWidth) + SVG_PADDING + linePadding;
                y1 = getYOffsetForRow(row, column, cellHeight) + SVG_PADDING;
                y2 = getYOffsetForRow(row + 1, column, cellHeight) + SVG_PADDING;
                break;
        }

        // Create and return the line
        let svgLine = document.createElementNS(NAMESPACE_SVG, 'line');
        svgLine.setAttributeNS('', 'x1', x1);
        svgLine.setAttributeNS('', 'x2', x2);
        svgLine.setAttributeNS('', 'y1', y1);
        svgLine.setAttributeNS('', 'y2', y2);
        svgLine.setAttributeNS('', 'stroke', 'black');
        svgLine.setAttributeNS('', 'stroke-width', String(1));
        svgLine.setAttributeNS('', 'stroke-dasharray', '5, 2');
        if (insertArrowEnd) {
            svgLine.setAttributeNS('', 'marker-end', 'url(#arrowMarker)');
        }
        return svgLine;
    }

    /**
     * For determining the extendHorLine parameter of the createLine function it is necessary to
     * know the maximum padding of the neighbors. This function computes this value.
     *
     * @param row The row to get the maximum padding of the neighbors for.
     * @param column The column to get the maximum padding of the neighbors for.
     * @param sourceNodeId The ID of the source node.
     * @return {number} Returns the maximum padding value. Note that this is a relative value and
     * has to be multiplied with the value that determines the width between lines.
     */
    function getMaxPaddingOfNeighbors(row, column, sourceNodeId) {
        let nextRow = row + 1, nextColumn = column + 1;
        // Check bottom right neighbor
        let max = 0;
        if (nextRow < matrix.length && nextColumn < matrix[nextRow].length) {
            let sourceNodes = matrix[row + 1][column + 1].borderLeft.sourceNodes;
            for (let i = 0; i < sourceNodes.length; i++) {
                if (sourceNodes[i] == sourceNodeId) {
                    max = matrix[row + 1][column + 1].borderLeft.linePaddings[i];
                }
            }
        }
        // Check right neighbor
        if (nextColumn < matrix[row].length) {
            let sourceNodes = matrix[row][column + 1].borderLeft.sourceNodes;
            for (let i = 0; i < sourceNodes.length; i++) {
                if (sourceNodes[i] == sourceNodeId) {
                    max = matrix[row][column + 1].borderLeft.linePaddings[i];
                }
            }
        }

        return max;
    }

    /**
     *
     * @param row
     * @param column
     * @return {number}
     */
    function getLineOffset(row, column) {

        // If the left border contains at least one target node ID that is also contained in the
        // bottom border, there is definitely no padding. Otherwise, if the left border has two
        // paddings, we return an offset not equals zero
        let matrixCell = matrix[row][column];
        // Only one outgoing edge => no offset
        if (matrixCell.borderLeft.linePaddings.length < 2) {
            return 0;
        }
        // At least two outgoing edges
        else {
            let leftPaddings = matrixCell.borderLeft.linePaddings;
            let uniqueLeftPaddings =
                leftPaddings.filter((val, index, arr) => arr.indexOf(val) === index);
            // No forward / backward combination => no offset
            if (uniqueLeftPaddings.length < 2) {
                return 0;
            }
            // Otherwise, check if the source takes the same route
            else {
                let leftTargetNodes = matrixCell.borderLeft.targetNodes;
                let bottomTargetNodes = matrixCell.borderBottom.targetNodes;
                let intersection = leftTargetNodes.filter(function (val) {
                    return bottomTargetNodes.indexOf(val) != -1;
                });
                return (intersection.length == 0) ? 0 : 1;
            }
        }
    }

    /**
     * @return {Element} Returns an SVG arrow marker definition for the end of a line.
     */
    function getArrowMarker() {
        let svgPath = document.createElementNS(NAMESPACE_SVG, 'path');
        svgPath.setAttributeNS('', 'd', 'M2,2 L2,11 L10,6 L2,2');
        svgPath.setAttributeNS('', 'style', 'fill: #000000;');

        let svgMarker = document.createElementNS(NAMESPACE_SVG, 'marker');
        svgMarker.setAttributeNS('', 'id', 'arrowMarker');
        svgMarker.setAttributeNS('', 'markerWidth', String(13));
        svgMarker.setAttributeNS('', 'markerHeight', String(13));
        svgMarker.setAttributeNS('', 'refX', String(10));
        svgMarker.setAttributeNS('', 'refY', String(6));
        svgMarker.setAttributeNS('', 'orient', String(90));
        svgMarker.appendChild(svgPath);

        let svgDef = document.createElementNS(NAMESPACE_SVG, 'defs');
        svgDef.appendChild(svgMarker);
        return svgDef;
    }

    /**
     * @param oldCanvasDim An object consisting of two numeric properties - minWidth, minHeight -
     * that determine the minimum dimension a canvas should have to properly display the graph.
     * @param svgElement An SVG object such as generated by createRect or createLine. Currently,
     * this method supports only SVG rectangles and lines.
     */
    function getCanvasDim(oldCanvasDim, svgElement) {
        let width = 0, height = 0;
        // SVG element is a rectangle
        if (svgElement.x !== undefined) {
            width = svgElement.x.baseVal.value + svgElement.width.baseVal.value;
            height = svgElement.y.baseVal.value + svgElement.height.baseVal.value;
        }
        // SVG element is a line (all other elements are not handled)
        else if (svgElement.x1 !== undefined) {
            width = (svgElement.x1.baseVal.value > svgElement.x2.baseVal.value) ?
                svgElement.x1.baseVal.value : svgElement.x2.baseVal.value;
            height = (svgElement.y1.baseVal.value > svgElement.y2.baseVal.value) ?
                svgElement.y1.baseVal.value : svgElement.y2.baseVal.value;
        }
        // Update the old canvas if necessary
        oldCanvasDim.minWidth = (width > oldCanvasDim.minWidth) ? width : oldCanvasDim.minWidth;
        oldCanvasDim.minHeight = (height > oldCanvasDim.minHeight) ? height : oldCanvasDim.minHeight;

        return oldCanvasDim;
    }

    // This object determines the minimum dimension the underlying canvas should have
    let canvasDim = {minWidth: 0, minHeight: 0};

    // The parent SVG element that holds the graph
    let svg = document.createElementNS(NAMESPACE_SVG, 'svg');
    svg.appendChild(getArrowMarker());

    // Loop through the matrix and draw everything. We need to loop twice: In the first step we
    // draw nodes and in the second the edges. This can not be done in one loop because for
    // computing the x values of the lines the width of the nodes have to be determined which is
    // done in the first step 
    for (let row = 0; row < matrix.length; row++) {
        for (let column = 0; column < matrix[row].length; column++) {
            // Create node if necessary
            if (matrix[row][column].cellContent !== '') {
                let label = matrix[row][column].cellContent;
                let attributes = hashmapGraph[matrix[row][column].nodeId].nodeAttributes;
                let rect = createRect(row, column, label, attributes);
                svg.appendChild(rect);
                canvasDim = getCanvasDim(canvasDim, rect);
            }
        }
    }
    for (let row = 0; row < matrix.length; row++) {
        for (let column = 0; column < matrix[row].length; column++) {
            // Draw edges
            let borderBottom = matrix[row][column].borderBottom;
            let borderLeft = matrix[row][column].borderLeft;
            let seenPaddings = [];
            borderBottom.linePaddings.forEach(function (element, index) {
                if (seenPaddings.indexOf(element) == -1) {
                    let additionalLineWidth = linePadding *
                        getMaxPaddingOfNeighbors(row, column, borderBottom.sourceNodes[index]);
                    let lineOffset = getLineOffset(row, column);
                    let line = createLine(row, column, BORDER_BOTTOM, element * linePadding, lineOffset * linePadding, additionalLineWidth, false);
                    svg.appendChild(line);
                    seenPaddings.push(element);
                    canvasDim = getCanvasDim(canvasDim, line);
                }
            });

            seenPaddings = [];
            borderLeft.linePaddings.forEach(function (element) {
                if (seenPaddings.indexOf(element) == -1) {
                    let setArrow =
                        matrix[row + 1] !== undefined && matrix[row + 1][column].cellContent !== '';
                    let lineOffset = getLineOffset(row, column);
                    let line = createLine(row, column, BORDER_LEFT, element * linePadding, lineOffset * linePadding, 0, setArrow);
                    svg.appendChild(line);
                    seenPaddings.push(element);
                    canvasDim = getCanvasDim(canvasDim, line);
                }
            });
        }
    }

    return {
        svg: svg,
        canvasWidth: canvasDim.minWidth + SVG_PADDING,
        canvasHeight: canvasDim.minHeight + SVG_PADDING
    };
};
