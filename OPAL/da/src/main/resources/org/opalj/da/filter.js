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
"use strict";

NodeList.prototype.forEach = Array.prototype.forEach;

function toogleFilter() {
 var flagsFilter = document.querySelectorAll('input:checked[type="checkbox"],input:checked[type="radio"]');
 var nameFilter = document.querySelector('input[type="text"]').value
 if(flagsFilter.length == 0 && nameFilter.length == 0) {
	 // clear filter property
	 document.querySelectorAll('.method').forEach(function(e){e.style.display = 'block'})
 } else {
	 // 1. hide all
	 document.querySelectorAll(".method").forEach(function(e){e.style.display = 'none'})

	 var filterString = ".method"
	 flagsFilter.forEach(function(e){filterString += "[data-access-flags*='"+e.value+"']"});
	 if(nameFilter.length > 0)
		 filterString += "[data-name*='"+document.querySelector('input[type="text"]').value+"']";

	 // 2. show filtered
	 console.log(filterString);
	 document.querySelectorAll(filterString).forEach(function(e){e.style.display = 'block'})
 }
 toggleUnusedFlags();
}

function clearFilter(){
	// clear flags filter
	var flagsFilter = document.querySelectorAll('input:checked[type="checkbox"],input:checked[type="radio"]');
	flagsFilter.forEach(function(f){f.checked = false});

	// clear name filter
	document.querySelectorAll('input[type="text"]').forEach(function(e){e.value = ""})

	// update "view"
	toogleFilter();
}

function toggleUnusedFlags() {
	// sets the input element on disabled and puts a corresponding color on its label
	var disableInput = function(element, disable) {
		element.disabled = disable;
		var label = document.querySelector('label[for="'+ element.id +'"]');
		if (disable)
			label.style.color = "lightgray";
		else
			label.style.color = "black";
	}
    var possibleFlags = document.querySelectorAll('input:not(:checked)[type="checkbox"],input:not(:checked)[type="radio"]');
	var selectedFlags = document.querySelectorAll('input:checked[type="checkbox"],input:checked[type="radio"]');

	var filterString = ".method"
	selectedFlags.forEach(function(e){filterString += "[data-access-flags*='"+e.value+"']"});

    possibleFlags.forEach(function (e) {
		if (e.type == "radio") {
			// on radio types, we have to filter the currently set radio-button in the same set as they are mutually exclusive
			filterString = ".method"
			selectedFlags.forEach(function(f){
				if (e.name != f.name)
					filterString += "[data-access-flags*='"+f.value+"']";
			});
		}
		var possibleElements = document.querySelectorAll(filterString + "[data-access-flags*='"+ e.value +"']").length;
		if (possibleElements > 0)
			disableInput(e, false);
		else
			disableInput(e, true);
    });
}

/**
 * Trims the package names of classes that are on the same package-path
 * as the disassembled class. Each match is replaced by a dot.
 *
 * Examples with java.awt.event.ActionEvent as disassembled class:
 * 		java.awt.event.InputEvent -> ...InputEvent
 *	 	java.awt.font.GraphicAttribute -> ..font.GraphicAttribute
 * 		org.opalj.SomeClass -> org.opalj.SomeClass
*/
function trimPackageNames() {
	// get defining class name and split for packages
	var definingClassNameFQN = document.querySelector("#defined_class").innerHTML;
	var definingPackages = definingClassNameFQN.split(".");
	// splice removes the class name from the package array and returns it:
	var definingClassName = definingPackages.splice(definingPackages.length-1);

	for(var i=1;i<definingPackages.length;i++) {
		definingPackages[i] = definingPackages[i-1] + "." + definingPackages[i];
	}
	document.querySelectorAll(".object_type").forEach(function(e) {
		for(var i=definingPackages.length-1;i>=0;i--) {
			while (e.innerHTML.indexOf(definingPackages[i]+".") >= 0) {
				e.innerHTML = e.innerHTML.replace(definingPackages[i], new Array(i + 1).join( "." ));
				e.title = definingPackages[i] + ".";
			}
		}
	});
}


/**
 * Removes the exception name from the exceptions overview if the containing element
 * (span) would overlap to the next section of the document.
 */
function removeLongExceptionNames() {

	var exceptions = document.querySelectorAll('td.exception');
	exceptions.forEach(function(e) {
		var span = e.querySelector('span');
		if (span != null) {
			var tbody = span.parentNode.parentNode.parentNode;

			// if the totalOffset+width of the span is greater than totalOffset+height of
			// the table body it would overlap to the next section (exception table)
			if (totalOffset(span).top+span.clientWidth > totalOffset(tbody).top+tbody.clientHeight) {
				span.innerHTML = span.getAttribute("data-exception-index") + ': ...';
			}
			if (totalOffset(span).top+span.clientWidth > totalOffset(tbody).top+tbody.clientHeight) {
				span.innerHTML = span.getAttribute("data-exception-index") + ':';
			}
		}
	});
}

/**
 * Computes the offset of an HTMLElement from the top/left corner of the document
 * in contrast to HTMLElement.offsetLeft/offsetTop which computes the offset relative
 * to their HTMLElement.offsetParent node.
 */
function totalOffset(elem) {
	if(!elem) elem = this;

	var x = elem.offsetLeft;
	var y = elem.offsetTop;

	while (elem = elem.offsetParent) {
		x += elem.offsetLeft;
		y += elem.offsetTop;
	}

	return { left: x, top: y };
}


/**
 * Removes (too) long exception names. This is a callback function that is called when
 * a details.method_body is set to open, i.e., the methods instructions can be seen.
 */
function executeOnMethodBodyOpen() {
	removeLongExceptionNames();
}

document.addEventListener("DOMContentLoaded", function(event) {
    toggleUnusedFlags();
	trimPackageNames();

	// adds a listener to details.method_body that is called when its attributes change
	var targets = document.querySelectorAll('details.method_body');
	var observer = new MutationObserver(function(mutations) {
		executeOnMethodBodyOpen();
	});
	var config = { attributes: true, childList: false, characterData: false};
	targets.forEach(function(target) {
		observer.observe(target, config);
	});
});
