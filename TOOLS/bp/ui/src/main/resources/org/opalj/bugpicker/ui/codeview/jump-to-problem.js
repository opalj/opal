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
// Functions to scroll to points in decompiled bytecode
function openBytecodeMethodsBlock() {
	var methodsBlock = document.querySelector('.methods details');
	if (methodsBlock != undefined) {
		methodsBlock.open = true;
	}
}
function getBytecodeMethodBlock(methodId) {
	var mId = document.getElementById(methodId)
	if (mId != undefined)
		return mId.querySelector('details');
	else
		return mId;
}
function openBytecodeMethod(methodId) {
	var methodBlock = getBytecodeMethodBlock(methodId);
	if (methodBlock != undefined) {
		methodBlock.open = true;
	}
}
function jumpToMethodInBytecode(methodId) {
	openBytecodeMethodsBlock();
	openBytecodeMethod(methodId);
	var mb = getBytecodeMethodBlock(methodId);
	if (mb != undefined) {
		mb.scrollIntoView();
	} else {
		window.scrollTo(0,0);
	}
}
function jumpToProblemInBytecode(methodId, pc) {
	openBytecodeMethodsBlock();
	openBytecodeMethod(methodId);
	
	var method = document.getElementById(methodId);
	var index = method.dataset['methodIndex'];
	var pcId = 'm' + index + '_pc' + pc;
	var pcElement = document.getElementById(pcId);
	if (pcElement != undefined) {
		pcElement.scrollIntoView();
	} else if (getBytecodeMethodBlock(methodId) != undefined) {
		getBytecodeMethodBlock(methodId).scrollIntoView();
	} else {
		window.scrollTo(0,0);
	}
}

// Functions to scroll to points in source code
function jumpToLineInSourceCode(line) {
	var adjustedLine = Math.max(1,line - 3);
	var lineElement = document.getElementById('line' + adjustedLine);
	if (lineElement != undefined) {
		lineElement.scrollIntoView();
	} else {
		window.scrollTo(0,0);
	}
}
