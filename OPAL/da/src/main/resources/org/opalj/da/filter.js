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
	 flagsFilter.forEach(function(e){filterString += "[flags*='"+e.value+"']"});
	 if(nameFilter.length > 0)
		 filterString += "[name*='"+document.querySelector('input[type="text"]').value+"']";
	 	 
	 // 2. show filtered
	 console.log(filterString);	 
	 document.querySelectorAll(filterString).forEach(function(e){e.style.display = 'block'})
 }
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