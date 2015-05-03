/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2015
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
var debug_OFF = 0,
	debug_ERROR = -10,
	debug_WARNING = -20,
	debug_INFO = -30,
	debug_TRACE = -40,
	debug_ALL = -2147483648;
var debug = debug_ALL;

function log(something, loglevel) {
	if (loglevel == undefined) {
		loglevel = debug_INFO;
	}
	if (debug > loglevel) return;
	console.log(something);
}

/**
 * Manages the active filters for the displayed issues.
 *
 * @namespace IssueFilter
 * @author Tobias Becker
 */
var IssueFilter = function () {
	/**
	 * The node that displays how many issues are currently displayed.
	 *
	 * @memberof IssueFilter
	 */
	var issuesDisplayNode;

	/**
	 * All issues paired with their containing package.
	 *
	 * @memberof IssueFilter
	 */
	var packagesWithIssues = [];

	/**
	 * The filter functions.
	 *
	 * @memberof IssueFilter
	 */
	var filterFuns = [];

	/**
	 * The initialization functions of the filter. 
	 * Will be called once, when DOMContentLoaded-Event is fired
	 *
	 * @memberof IssueFilter
	 */
	var initFuns = [];

	/**
	 * Indicates if the filters are already initialized.
	 *
	 * @memberof IssueFilter
	 */
	var init = false;

	function initialize() {
		log("[IssueFilter] Initialization started.");
		initFuns.forEach(function(f) {
			f();
		})
		issuesDisplayNode = document.querySelector("#issues_displayed");
		var packages = document.querySelector("#analysis_results").querySelectorAll("details.package_summary");
		var i = 0;
		packages.forEach(function(p) {
			packagesWithIssues.push([p, p.querySelectorAll(".an_issue")]);
		});
		init = true;
		log("[IssueFilter] All Filter initialized.");
		IssueFilter.update();
	}

	document.addEventListener("DOMContentLoaded", initialize, false);

	var object = {
		
		/**
		 * Registers a new Filter.
		 * @param {Function} Initialization function. 
		 *        Will be called when the DOMContentLoaded-Event is fired or instantly if it already fired.
		 * @param {Function} The filter function. 
		 *        Will be passed a single issue and should return a boolean 
		 *        indicating if the issue should be displayed (true) or not (false)
		 *
		 * @memberof IssueFilter
		 * @inner
		 */
		register: function (initFun, displayIssue) {
			if (!(initFun instanceof Function)) {
				log("[IssueFilter] register: Invalid Parameter 'initFun' " + initFun, debug_ERROR);
				return;
			}
			if (!(displayIssue instanceof Function)) {
				log("[IssueFilter] register: Invalid Parameter 'displayIssue '" + displayIssue, debug_ERROR);
				return;
			}
			filterFuns.push(displayIssue);
			if (init)
				initFun();
			 else
				initFuns.push(initFun);
		},
		
		/**
		 * Updates the displayed issues by applying all filters. 
		 * If an issue is considered not to be displayed by a filter, it will not be passed to the remaining filters.
		 *
		 * @memberof IssueFilter
		 * @inner
		 */
		update: function () {
			if (!init) {
				log("[IssueFilter] Update cancelled. IssueFilter not yet initialzed!", debug_WARNING);
				return;
			}
			log("[IssueFilter] Update started.");
			var startTime = performance.now();
			var issue_counter_all = 0;
			packagesWithIssues.forEach(function(packageIssue){
				var thePackage = packageIssue[0];
				var issues = packageIssue[1];

				var issue_counter_package = 0;
				issues.forEach(function(issue){
					var display = true;
					var i = 0;
					while (display && i < filterFuns.length) {
						display = filterFuns[i](issue);
						i++;
					}
					if (display) {
						issue.classList.add("issue_visible");
						issue_counter_package++;
					} else {
						issue.classList.remove("issue_visible");
					}
				});
				issue_counter_package > 0 ?
					thePackage.style.display="block" :
					thePackage.style.display="none";
				issue_counter_all += issue_counter_package;
				// update number of displayed issues in package summary:
				var text = thePackage.querySelector("summary.package_summary");
				var textNode = text.querySelector("span.package_issues");
				if (textNode == undefined) {
					text.innerHTML = text.innerHTML + "<span class=\"package_issues\"> </span>";
					textNode = text.querySelector("span.package_issues");
				}
				textNode.innerHTML = " (Issues: " + issue_counter_package + ")";
			});
			issuesDisplayNode.innerHTML = " [Relevance &ge; "+inputRelevance.value+"] " + issue_counter_all;
			var endTime = performance.now();
			log("[IssueFilter] Update ended. Took " + (endTime - startTime) + " milliseconds.");
			log("[IssueFilter] Applied " + filterFuns.length + " Filter");
		},
		
		/**
		 * Convenience function to add the IssueFilter.update as an EventListener.
		 *
		 * @memberof IssueFilter
		 * @inner
		 */
		addListener: function(listenTarget, listenType) {
			if (!init) {
				log("[IssueFilter] addListener called. IssueFilter not yet initialized.", debug_WARNING);
			}
			if (!(listenTarget instanceof HTMLElement)) {
				log("[IssueFilter] addListener: Invalid Parameter 'listenTarget' " + listenTarget, debug_ERROR);
				return;
			}
			if (!(typeof listenType === "string")) {
				log("[IssueFilter] addListener: Invalid Parameter 'listenType' " + listenType, debug_ERROR);
				return;
			}
			listenTarget.addEventListener(listenType, this.update, false);
		}
	};
	return object;
}();

// Filter: relevance
//
var inputRelevance;
IssueFilter.register(
	function() {
		inputRelevance = document.querySelector("input#relevance");
		inputRelevance.value=75;
		IssueFilter.addListener(inputRelevance, "change");
		log("[RelevanceFilter] Initialized.");
	},
	function (issue) {
		return (issue.dataset.relevance >= inputRelevance.valueAsNumber);
	});

// Filter: data-kind & data-category
//
function initDataFilter(dataType){
	var allValues = [];
	document.querySelectorAll("*[data-"+dataType+"]").forEach(
		function(e){
			var values = e.getAttribute("data-"+dataType).split(" ");
			allValues = allValues.concat(values).filter (function (v, i, a) { return a.indexOf (v) == i });
		}
	)
	document.querySelector("#filter_data-"+dataType).innerHTML =
		ArrayJoin(allValues.sort(),
			function (i, e) {
				var name = "filter-data-" + dataType;
				var id = name + i;
				return "<input type='checkbox' id='"+id+"' name='"+name+"' value='"+e+"' checked>"+
						"<label for='"+id+"'>"+e.replace("_", " ")+"</label>";
			}
		);
	document.querySelectorAll("input[name=filter-data-" + dataType + "]").forEach(function(input) {
		IssueFilter.addListener(input, "change");
	});

}

function commonValue(a, b) {
	var t;
	if (b.length > a.length) t = b, b = a, a = t; // indexOf to loop over shorter
	return a.filter(function (e) {
		return (b.indexOf(e) !== -1);
	}).length > 0;
}

function updateDataFilter(dataType, issue) {
	var actual = e.getAttribute("data-"+dataType).split(" ");
	return commonValue(actual,checked);
}

var inputDataKind;
IssueFilter.register(
	function() {
		initDataFilter("kind");
		inputDataKind = document.querySelectorAll("input[name=filter-data-kind]");
	},
	function (issue) {
		var checked = [];
		inputDataKind.forEach(function(input) {
			if (input.checked)
				checked.push(input.value);
		});
		var actual = issue.getAttribute("data-kind").split(" ");
		return commonValue(actual,checked);
	});

var inputDataCategory;
IssueFilter.register(
	function() {
		initDataFilter("category");
		inputDataCategory = document.querySelectorAll("input[name=filter-data-category]");
	},
	function (issue) {
		var checked = [];
		inputDataCategory.forEach(function(input) {
			if (input.checked)
				checked.push(input.value);
		});
		var actual = issue.getAttribute("data-category").split(" ");
		return commonValue(actual,checked);
	});

// Filter: text search
//
function findTextInIssue(issue, text) {
	var erg = false;
	issue.querySelectorAll("dd:not(.issue_message)").forEach(function(dd) {
		var attributeText = "";
		var attributes = dd.attributes;
		for (var i=0; i < attributes.length; i++) {
			var attr = attributes[i];
			var attrText = attr.value.toLowerCase().replace(/\//g, ".");
			if (attr.name.startsWith("data-") && attrText.indexOf(text.toLowerCase()) >= 0) {
				dd.classList.add("highlight_occurence");
				erg = true;
			}
		}
		if (issue.textContent.toLowerCase().indexOf(text.toLowerCase()) >= 0) {
			var nodeIterator = document.createNodeIterator(dd, NodeFilter.SHOW_TEXT);
			var currentNode;
			while (currentNode = nodeIterator.nextNode()) {
				if (currentNode.textContent.toLowerCase().indexOf(text.toLowerCase()) >= 0) {
					erg = true;
					currentNode.parentNode.classList.add("highlight_occurence");
				}
			}
		}
	});
	return erg;
}

var searchField;
var searchCategories = [];
IssueFilter.register(
	function() {
		searchField = document.querySelector("#search_field");
		// delay the event-listener
		searchField.addEventListener("input", function () {
			var searchString = searchField.value;
			setTimeout(function() {
				if (searchString == searchField.value)
					IssueFilter.update();
			}, 300)
		}, false);
		document.querySelectorAll("dt").forEach(function(dt){
			if (searchCategories.indexOf(dt.textContent) < 0)
				searchCategories.push(dt.textContent);
		});
		searchField.disabled = false;
	},
	function (issue) {
		issue.querySelectorAll(".highlight_occurence").forEach(function(e) {
			e.classList.remove("highlight_occurence");
		});
		var searchString = searchField.value;
		if (searchString.length == 0)
			return true;
		var categoryLength = searchString.indexOf(":") - 1;
		var category = searchString.substring(0,categoryLength);
		if (searchCategories.indexOf(category) !== -1) {
			searchString = searchString.slice(categoryLength + 1);
		} else {
			category = "";
		}
		var found = false;
		if (category != "") {
			var elem;
			issue.querySelectorAll("dt").forEach(function(dt){
				if (dt.textContent == category) {
					elem = dt.nextSibling;
					while (elem.nodeName != "DD")
						elem = dt.nextSibling;
				}
			});
			if (elem != undefined) {
				var textToSearch = elem.textContent;
				found = textToSearch.toLowerCase().indexOf(searchString.toLowerCase()) >= 0;
				if (found)
					elem.classList.add("highlight_occurence");
			}
		} else {
			found = findTextInIssue(issue, searchString);
		}
		return found;
	});

function openAllPackages(){
	document.querySelectorAll('div#analysis_results > details').forEach(
		function(e){e.open=true}
	)
}

function closeAllPackages(){
	document.querySelectorAll('div#analysis_results > details').forEach(
		function(e){e.removeAttribute('open')}
	)
}


/*
  Works similar to the join-method of Array, but uses a function for the join
*/
function ArrayJoin(array, joinFunc) {
	var ArrayJoinIntern = function(internArray, index) {
		var element = internArray.shift();
		return internArray.length > 0 ?
			joinFunc(index, element) + ArrayJoinIntern(internArray, index+1) :
			joinFunc(index, element);
	}
	return ArrayJoinIntern(array, 0)
}
