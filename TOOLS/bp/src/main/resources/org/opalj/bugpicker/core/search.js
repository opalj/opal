/* BSD 2-Clause License - see OPAL/LICENSE for details. */
"use strict";

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