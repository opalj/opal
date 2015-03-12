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
function updateRelevance(minimumRelevance){
	document.querySelectorAll("*[data-relevance]").forEach(
		function(e){
			if (e.dataset.relevance < minimumRelevance)
				e.classList.add("hide-relevance");
			else
				e.classList.remove("hide-relevance");
		}
	)
	hideEmptyPackages();
	updateNumberOfIssues(minimumRelevance);
}

function hideEmptyPackages() {
	document.querySelector("#analysis_results").querySelectorAll("details.package_summary").forEach(
		function(p){
			var package_counter = 0;
			p.querySelectorAll(".an_issue").forEach(
				function(e){
					if (e.classList.contains("issue_visible") && !e.classList.contains("hide-relevance"))
						package_counter++;
				}
			)
			package_counter > 0 ? 
				p.style.display="block" :
				p.style.display="none";
		}
	)
}

function updateNumberOfIssues(minimumRelevance){
	if (minimumRelevance === undefined)
		minimumRelevance = document.getElementById('relevance').value
	var current = 0;
	document.querySelectorAll(".an_issue").forEach(
		function(e){
			if (e.classList.contains("issue_visible") && !e.classList.contains("hide-relevance"))
				current++;
		}
	)
	document.querySelector("#issues_displayed").innerHTML = 
		" [Relevance &ge; "+minimumRelevance+"] " + current;
}

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

document.addEventListener("DOMContentLoaded", function () {
	initFilter("kind");
	initFilter("category");
}, false);

var filterCount = 0;

function initFilter(dataType){
	var allValues = [];
	document.querySelectorAll("*[data-"+dataType+"]").forEach(
		function(e){
			var values = e.getAttribute("data-"+dataType).split(" ");
			allValues = allValues.concat(values).filter (function (v, i, a) { return a.indexOf (v) == i });
		}
	)
	var i = 0;
	document.querySelector("#filter_data-"+dataType).innerHTML = 
		ArrayJoin(allValues.sort(), 
			function (i, e) { 
				var name = "filter-data-" + dataType;
				var id = name + i;
				return "<input type='checkbox' id='"+id+"' name='"+name+"' value='"+e+"' onchange='updateFilter(\""+dataType+"\")' checked>"+
						"<label for='"+id+"'>"+e.replace("_", " ")+"</label>"; 
			}
		);
	filterCount++;
	updateFilter(dataType);
}

function updateFilter(dataType){
	document.querySelectorAll(".an_issue").forEach(
		function(e) { 
			e.classList.remove("show-"+dataType);
			e.classList.remove("issue_visible");
		});
	document.querySelectorAll("input[name=filter-data-"+dataType+"]:checked").forEach(
		function(f){
			document.querySelectorAll(".an_issue[data-" + dataType + "~=" + f.getAttribute("value") + "]")
				.forEach(function(e) { e.classList.add("show-"+dataType) } );
		})
		
	// there has to be at least as much "show-" in the className of an issue as there are filter for the issue to be shown
	document.querySelectorAll(".an_issue").forEach(
		function(e) { 
			if (e.className.count("show-") >= filterCount)
				e.classList.add("issue_visible");
		});
	hideEmptyPackages();
	updateNumberOfIssues();
}

String.prototype.count = function(substring) {
	return (this.valueOf().length - (this.valueOf().replace(new RegExp(substring, "g"), "").length)) / substring.length;
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
