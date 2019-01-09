"use strict";
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
 *	- Redistributions of source code must retain the above copyright notice,
 *	  this list of conditions and the following disclaimer.
 *	- Redistributions in binary form must reproduce the above copyright notice,
 *	  this list of conditions and the following disclaimer in the documentation
 *	  and/or other materials provided with the distribution.
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
/* options for JSHint code quality: */
/* global performance */

// quotes RegExp special character in a String
RegExp.quote = function (str) {
    return (str + '').replace(/[.?*+^$[\]\\(){}|-]/g, "\\$&");
};

// Filter: text search
function findTextInIssue(issue, text, category) {

    var erg = false;
    var rx = new RegExp(RegExp.quote(text), "gi");
    var processDD = function (dd) {
        var attributes = dd.attributes;
        var i = 0;
        while (i < attributes.length) {
            var attr = attributes[i];
            if (attr.name.startsWith("data-")) {
                var attrText = attr.value.toLowerCase().replace(/\//g, ".");
                if (attrText.indexOf(text.toLowerCase()) >= 0) {
                    erg = true;
                }
            }
            i++;
        }
        var nodeIterator = document.createNodeIterator(dd, NodeFilter.SHOW_TEXT);
        var currentNode;
        while (currentNode = nodeIterator.nextNode()) {
            if (currentNode.textContent.toLowerCase().indexOf(text.toLowerCase()) >= 0) {
                erg = true;
            }
        }
    };
    if (category === undefined) {
        issue.querySelectorAll("dd:not(.issue_message)").forEach(processDD);
    } else {
        processDD(category);
    }
    return erg;
}

var searchField;
var searchCategories = [];
IssueFilter.register(
    function () {
        searchField = document.querySelector("#search_field");
        // delay the event-listener
        searchField.addEventListener("input", function () {
            var searchString = searchField.value;
            setTimeout(function () {
                if (searchString == searchField.value)
                    IssueFilter.update();
            }, 300);
        }, false);
        document.querySelectorAll("dt").forEach(function (dt) {
            if (searchCategories.indexOf(dt.innerText.replace(/\W/g, '')) < 0)
                searchCategories.push(dt.innerText.replace(/\W/g, ''));
        });
        searchField.disabled = false;
        log("[TextSearchFilter] Initialized.");
    },
    function (issue) {
        var searchString = searchField.value;
        if (searchString.length === 0)
            return true;
        var categoryLength = searchString.indexOf(":");
        var category = searchString.substring(0, categoryLength);
        if (searchCategories.indexOf(category) !== -1) {
            searchString = searchString.slice(categoryLength + 1);
        } else {
            category = "";
        }
        var found = false;
        if (category !== "") {
            var elem;
            issue.querySelectorAll("dt").forEach(function (dt) {
                if (dt.innerText.replace(/\W/g, '') == category) {
                    elem = dt.nextSibling;
                    while (elem.nodeName != "DD")
                        elem = elem.nextSibling;
                }
            });
            found = findTextInIssue(issue, searchString, elem);
        } else {
            found = findTextInIssue(issue, searchString);
        }
        return found;
    });