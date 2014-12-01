// The OPAL Project
"use strict";

NodeList.prototype.forEach = Array.prototype.forEach; 

function toogleFilter() {
 var flagsFilter = document.querySelectorAll('input:checked[type="checkbox"],input:checked[type="radio"]');
 var nameFilter = document.querySelector('input[type="text"]').value
 if(flagsFilter.length == 0 && nameFilter.length == 0) {
	 // clear filter property
	 document.querySelectorAll('div[class="method"]').forEach(function(e){e.style.display = 'block'}) 	
 } else {
	 // 1. hide all
	 document.querySelectorAll("div[class='method']").forEach(function(e){e.style.display = 'none'})
	 
	 var filterString = "div[class='method']"
	 flagsFilter.forEach(function(e){filterString += "[data-method-flags*='"+e.value+"']"});
	 if(nameFilter.length > 0)
		 filterString += "[name*='"+document.querySelector('input[type="text"]').value+"']";
	 	 
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
	
	var filterString = "div[class='method']"
	selectedFlags.forEach(function(e){filterString += "[data-method-flags*='"+e.value+"']"});
	
    possibleFlags.forEach(function (e) {
		if (e.type == "radio") {
			// on radio types, we have to filter the currently set radio-button in the same set as they are mutually exclusive
			filterString = "div[class='method']"
			selectedFlags.forEach(function(f){
				if (e.name != f.name)
					filterString += "[data-method-flags*='"+f.value+"']";
			});
		}
		var possibleElements = document.querySelectorAll(filterString + "[data-method-flags*='"+ e.value +"']").length;
		if (possibleElements > 0)
			disableInput(e, false);
		else
			disableInput(e, true);
    });
}



document.addEventListener("DOMContentLoaded", function(event) {
    toggleUnusedFlags();
});
