/*
  License (BSD Style License):
  Copyright (c) 2009, 2011
  Software Technology Group
  Department of Computer Science
  Technische Universität Darmstadt
  All rights reserved.   
 
  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
    this list of conditions and the following disclaimer.
  - Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution. 
  - Neither the name of the Software Technology Group or Technische 
    Universität Darmstadt nor the names of its contributors may be used to 
    endorse or promote products derived from this software without specific 
    prior written permission. 

  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
  POSSIBILITY OF SUCH DAMAGE.
*/

% This library prints out the Control-flow Graph (CFG) of a method. 
% This library is tested to work with SWIProlog
%
% Version $Date: 2009-11-05 18:17:56 +0100 (Thu, 05 Nov 2009) $ $Rev: 524 $
% Author Michael Eichberg (www.michael-eichberg.de)

bb_descriptor((FirstInstrPC,LastInstrPC)) :-
	write('   bb_'),
	write(FirstInstrPC),
	write('_'),
	write(LastInstrPC).


bb_to_dot_label(MID,(FirstInstrPC,LastInstrPC)) :- 
	bb_descriptor((FirstInstrPC,LastInstrPC)),
	write(' [label="'),
	write(FirstInstrPC),
	write(' → '),
	write(LastInstrPC),
	write('|'),
	instrs_to_text(MID,FirstInstrPC,LastInstrPC),
	writeln('"];').

	
instrs_to_text(MID,FirstInstrPC,LastInstrPC) :-
	between(FirstInstrPC,LastInstrPC,PC),
	instr(MID,PC,Instr),
	% We have to escape some common characters; e.g. "[" with "&#91;"
	swritef(S,'%w',[Instr]),
		string_to_list(S,SL),
		%replace_chars(SL,[("[","&#91;"),("]","&#93;"),("<","&#60;"),(">","&#62;")],NSL),
		replace_char(SL,"[","&#91;",NS1),
		replace_char(NS1,"]","&#93;",NS2),
		replace_char(NS2,"<","&#60;",NS3),
		replace_char(NS3,">","&#62;",NS4),
		writef('%s',[NS4]),
	write('\\l'),
	fail. % failure driven loop
instrs_to_text(_,_,_).
	

bbs_to_dot(MID,BBs) :-
	member(SomeBB,BBs),
	bb_to_dot_label(MID,SomeBB),
	( 
		(
			bb_succ(MID,SomeBB,BBs,SuccBB),
			bb_descriptor(SomeBB),
			write(' -> '),
			bb_descriptor(SuccBB),
			writeln(';')
		);
		(
			bb_succ_ex(MID,SomeBB,BBs,SuccBB),
			bb_descriptor(SomeBB),
			write(' -> '),
			bb_descriptor(SuccBB),
			writeln(' [style=dotted,color=red];')
		)	
	),
	fail. % failure driven loop
bbs_to_dot(_,_).

% Example query: cfg_to_dot(m_6).

cfg_to_dot(MID) :- 
	writeln('digraph G {'),
	writeln('   node[shape=record,fontsize=10,fontname="Arial"];'),
	bbs(MID,BBs),
	bbs_to_dot(MID,BBs),
	writeln('}').
	
	
	