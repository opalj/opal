# Overview
_OGViz_ is a standalone project within OPAL. It visualizes arbitrary trees that follow the structure described in section _File Format_. To do so, open _index.html_ in your browser and select a JSON file. The graph is rendered within the browser as a scalable vector graphic (SVG) and can be downloaded as such.

# File Format
In order to visualize a graph, provide a JSON file which contains an array with objects of the following type:  
{  
"id": "integer",  
"label": "string",  
"level": "integer",  
"nodeAttributes": "object",  
"children": [ "integer" ]  
}  

* _id_: required. Each value is supposed to be unique.
* _label_: required: This value represents the name of the node to display.
* _level_: required. Each value needs to be unique and should be greater equals zero.
* _nodeAttributes_: required. If you don't want to further configure a node, pass an empty object. Otherwise, object keys represent attributes and their values attribute values. See https://www.w3.org/TR/SVG10/shapes.html#RectElement for a documentation of configureable attributes. However, do not set one of the following attributes as they break the layout of the generated graph: x, y, width, height.
* _children_: required. An array of _id_ values and denotes the children of a node. If a node has no children, specify an empty array for such a node. 

# Test Suite
This project comes with an integration test suite. Install required packages by running `npm install` in the root directory of this project. Afterwards, the test suite can be run by opening _testsuite/index.html_ in your browser. New test cases can be specified in the _testsuite/tests.js_ file.