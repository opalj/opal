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

% This library calculates the Control-flow Graph (CFG) of a method. 
%
% Version $Date: 2010-01-29 10:56:45 +0100 (Fri, 29 Jan 2010) $ $Rev: 1085 $
% Author Michael Eichberg (www.michael-eichberg.de)

% Documentation of the association list implementation:
% http://gollem.science.uva.nl/SWI-Prolog/Manual/assoc.html

% between(Start,End,Value) :-
%	Value is an (integer) value between Start and End (inclusive).
%[Built-in]between(Start,End,Start) :- Start <= End.
%[Built-in]between(Start,End,Value) :- Start < End,NextValue is Start + 1, between(NextValue,End,Value).

% TODO To improve the performance we should inline the calculation of the CFG instead of performing it in three steps. In particular the step where we calculate the successor BBs of all BBs is currently highly inefficient!


% next_instr(+MID,+CurrentPC,NextPC) :- 
%	determines the PC of the next instruction (if any!)
next_pc(MID,CurrentPC,NextPC) :-
 	NextPC is CurrentPC + 1,
	instr(MID,NextPC,_). % to make sure that there is another instruction!
	
	
% Future version, where instruction meta-information is explicitly available.
% E.g. given the following meta information:
% 	control_transfer(if_icmpne,conditionally)).
% Further, let's assume that we have the following instruction:
% 	instr(m_3,3,if_icmpne(12))
% Now, to determine, if an instruction starts a basic block, we can use the 
% following query:
%  bb_start_instr(MID,0) :- instr(MID,0,_). % the first instruction starts a basic block if the method has an implementation at all.
%	bb_start_instr(MID,PC) :- 
% 		instr(MID,CurrentPC,Instr), 
%		Instr =.. [Mnemonic|_],control_transfer(Mnemonic,T), T \= 'no',	
%		(	PC is CurrentPC + 1, instr(MID,PC,_); % to make sure that the next instruction exists
%			(	T = 'conditionally',
%				Instr =.. [_,Branchoffset|_],
%				PC is CurrentPC + BranchOffset
%			)
%		)
	

% bb_start_instr(MID,?PC) :-
% 	PC is the program counter of an instruction that starts a basic block.
bb_start_instr(MID,0) :- instr(MID,0,_). % the very first instruction - if the method has an implementation.
bb_start_instr(MID,PC) :- % every "start", "end" and "handler" instruction of a try - catch block
	method_exceptions_table(MID,Handlers),
	member(handler(StartPC,EndPC,HandlerPC,_),Handlers),
	(
		PC = StartPC;
		next_pc(MID,EndPC,PC);
		PC = HandlerPC
	). 
bb_start_instr(MID,PC) :- % the jump target of a conditional jump instructions and the instruction immediately following a conditional jump instruction
	conditional_jump(MID,CurrentPC,BranchOffset),
	(
		PC is CurrentPC + BranchOffset;
		PC is CurrentPC + 1 % if the code is correct this instruction has to exist
	).
bb_start_instr(MID,PC) :- % the jump target of unconditional jump instructions and the instruction immediately following a jump instruction ...
	jump(MID,CurrentPC,BranchOffset),
	(
		PC is CurrentPC + BranchOffset;
		next_pc(MID,CurrentPC,PC) % ... if it exists.
	).
bb_start_instr(MID,PC) :- % the instruction immediately following an athrow or instruction...
	(
		instr(MID,CurrentPC,athrow);
		return(MID,CurrentPC)
	),
	next_pc(MID,CurrentPC,PC). % ... if it exists.



% bb_end_instr(MID,PC) :-
% 	PC is the program counter of the last instruction of a basic block
%	of the method identified by MID.
bb_end_instr(MID,PC) :- 
	method_exceptions_table(MID,Handlers),
	member(handler(StartPC,EndPC,HandlerPC,_),Handlers),
	(
		(StartPC > 0, PC is StartPC-1);
		PC = EndPC ;
		(HandlerPC > 0, PC is HandlerPC-1)
	). 
bb_end_instr(MID,PC) :- % a control transfer instruction ends a basic block
	conditional_jump(MID,PC,_);
	jump(MID,PC,_);
	instr(MID,PC,athrow);
	return(MID,PC).
bb_end_instr(MID,PC) :- % the instruction before a jump target ends a basic block
	(
		conditional_jump(MID,CurrentPC,BranchOffset);
		jump(MID,CurrentPC,BranchOffset)
	),
	TargetPC is CurrentPC + BranchOffset,
	TargetPC > 0, 
	PC is TargetPC - 1.


% bbs(MID,BBs) :-
%	BBs is the list of all basic blocks of the method MID. A basic block
%	is a pair where the first value is the program counter of the first instruction
%	of the basic block and the second value is the last instruction of the 
%	basic block.
bbs(MID,BBs) :-
	setof(BBStartPC,bb_start_instr(MID,BBStartPC),BBStartPCs),
	setof(BBEndPC,bb_end_instr(MID,BBEndPC),BBEndPCs),
	bbs(BBStartPCs,BBEndPCs,BBs).
bbs([BBStartPC|BBStartPCs],[BBEndPC|BBEndPCs],[(BBStartPC,BBEndPC)|BBs]) :-
	bbs(BBStartPCs,BBEndPCs,BBs).
bbs([],[],[]).	


/* VERY INEFFICIENT - WE NEED TO USE THE SET OF ALL BASIC BLOCKS INSTEAD OF 
	RECALCULATING IT EVERY TIME!
% bb_pred(MID,BB,Pred) :-
% 	- MID the method's unique ID
%  - BB is a basic block
%	- Pred is a basic block that might have been executed before this basic block
bb_pred(MID,(FirstInstrPC,_),Pred) :-
	bb(MID,Pred),Pred = (_,LastInstrPC),
	Offset is (FirstInstrPC - LastInstrPC),
	(
		conditional_jump(MID,LastInstrPC,Offset);
		(
			conditional_jump(MID,LastInstrPC,_),
			FirstInstrPC =:= LastInstrPC + 1
		);
		jump(MID,LastInstrPC,Offset);
		(
			LastInstrPC is FirstInstrPC - 1,
			no_control_transfer(MID,LastInstrPC) % required for exception handling	
		)
	).
bb_pred(MID,(FirstInstrPC,_),Pred) :-
	method_exceptions_table(MID,Handlers),
	member(handler(StartPC,EndPC,FirstInstrPC,_),Handlers),
	bb(MID,Pred),Pred = (AFirstInstrPC,_),
	StartPC =< AFirstInstrPC,
	AFirstInstrPC =< EndPC.	
*/


% bb_succ(MID,BB,BBs,SuccBB) :-
%	MID is the ID of a method
% 	BB is a basic block of the method MID (a basic block is a pair (StartInstrPC,LastInstrPC))
%	BBs is the set of ALL basic blocks of the method MID
%	SuccBB is a basic block that may be executed directly after the given basic block BB
bb_succ(MID,(_,LastInstrPC),BBs,SuccBB) :-
	member(SuccBB,BBs),SuccBB = (FirstInstrPC,_),
	Offset is (FirstInstrPC - LastInstrPC),
	(
		jump(MID,LastInstrPC,Offset);
		conditional_jump(MID,LastInstrPC,Offset);
		( 	% fall-through case
			FirstInstrPC =:= LastInstrPC+1, 
			(
				conditional_jump(MID,LastInstrPC,_); 
				no_control_transfer(MID,LastInstrPC)
			)
		)
	).	
bb_succ_ex(MID,(FirstInstrPC,LastInstrPC),BBs,SuccExBB) :-	
	method_exceptions_table(MID,Handlers),
	member(handler(StartPC,EndPC,HandlerPC,_),Handlers),
	FirstInstrPC >= StartPC,
	LastInstrPC =< EndPC,
	SuccExBB = (HandlerPC,_),
	member(SuccExBB,BBs).

% cfg(MID,CFG) :-
%	CFG encodes the method's CFG. CFG is a list of pairs where the first value
%	of each pair is a basic block BB of the CFG and the second value is the list
%	of all successor basic blocks of BB. The list can contain the basic block BB.
%	E.G. CFG = [ ((0, 21), [ (22, 24)]), ((22, 24), [ (25, 31), (120, 127)]), ((..., ...), [...])|...].
cfg(MID,CFG) :-
	findall(
		(BB,SuccBBs),
		(	bbs(MID,BBs),
			member(BB,BBs),
			findall(
				SuccBB,
				(bb_succ(MID,BB,BBs,SuccBB);bb_succ_ex(MID,BB,BBs,SuccBB)),
				SuccBBs)
		),
		CFG
	).


df_ordering(CFG,BBsInDFO) :-
	RootNode=((0,_),_),
	member(RootNode,CFG),
	df_ordering([RootNode],CFG,[],BBsInDFO).

% df_ordering(CurrentNodes,CFG,BBsInDFOAccu,BBsInDFO) :-
%	- CurrentNodes is the list of nodes that need to be visited. Initially,
%		this is the list containing just the root node of the CFG.
%		A node is a pair (BB,SuccBBs) where the first value is a basic block and the
%		the second value is a list of basic blocks that may be executed after
%		the basic block identified using the first value.
%	- CFG a CFG as calculated by the cfg predicate.
%	- BBsInDFOAccu are the BBs that are already visited. Initially, this has to
%		be the empty set [].
%	- BBsInDFO are all BBs of the given CFG in depth first order (this is the output argument).
df_ordering([(BB,SuccBBs)|OtherNodes],CFG,BBsInDFOAccu,BBsInDFO) :-
	\+member(BB,BBsInDFOAccu),!,%green cut
	append(BBsInDFOAccu,[BB],NewBBsInDFOAccu),
	findall(
		SuccNode,
		(member(SuccBB,SuccBBs),member(SuccNode,CFG),SuccNode=(SuccBB,_)),
		SuccNodes
	),
	append(SuccNodes,OtherNodes,CurrentNodes),
	df_ordering(CurrentNodes,CFG,NewBBsInDFOAccu,BBsInDFO).
df_ordering([(BB,_)|OtherNodes],CFG,BBsInDFOAccu,BBsInDFO) :-
	member(BB,BBsInDFOAccu), !,%green cut
	df_ordering(OtherNodes,CFG,BBsInDFOAccu,BBsInDFO).
df_ordering([],_,BBsInDFO,BBsInDFO).
	
	
/*** 
 A node D dominates another node N (D dom N), if ervery path from the initial
 node of the flow graph to N goes throug D. Hence, every node dominates itself,
 and the entry node of a loop dominates all nodes in the loop.
 The Dominator relationship is usually represented using a dominator tree where
 only the immediate dominator realtionship is explicitly represented. 
***/
%dom(D_BB,N_BB,CFG) :-

	
	

% Some example queries:
%	1. For each basic block BB of a method (m_6) get all successor basic blocks.
% 		bbs(m_6,BBs),member(BB,BBs),findall(SuccBB,bb_succ(m_6,BB,BBs,SuccBB),SuccBBs).	
%	2. Get a set of all basic blocks where each basic block is associated with the 
%		list of successor basic blocks (and time the execution.)
% 		time(
%			findall(
%				(BB,SuccBBs),
%				(	bbs(m_6,BBs),
%					member(BB,BBs),
%					findall(SuccBB,bb_succ(m_6,BB,BBs,SuccBB),SuccBBs)
%				),
%				Nodes
%			)
%		).



/*
	We need abstractions to answer queries such as: 
	"Is <InputStream>.close() called on all paths?"

	Note, some methods do not terminate! E.g.,
	public void endless() {
		while (true) {
			System.out.println("endless!");
		}
	}

	public void endless();
  	Code:
   Stack=2, Locals=1, Args_size=1
   	0:	getstatic	#15; //Field java/lang/System.out:Ljava/io/PrintStream;
   	3:	ldc	#21; //String endless!
		5:	invokevirtual	#23; //Method java/io/PrintStream.println:(Ljava/lang/String;)V
		8:	goto	0
	LineNumberTable: 
		line 9: 0
		line 8: 8
	does not terminate!
*/


	

	