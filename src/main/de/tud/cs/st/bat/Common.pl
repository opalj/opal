% License (BSD Style License):
% Copyright (c) 2009
% Software Technology Group,
% Department of Computer Science
% Darmstadt University of Technology
% All rights reserved.
%
% Redistribution and use in source and binary forms, with or without
% modification, are permitted provided that the following conditions are met:
%
% - Redistributions of source code must retain the above copyright notice,
%   this list of conditions and the following disclaimer.
% - Redistributions in binary form must reproduce the above copyright notice,
%   this list of conditions and the following disclaimer in the documentation
%   and/or other materials provided with the distribution.
% - Neither the name of the Software Technology Group or Technische 
%   Universität Darmstadt nor the names of its contributors may be used to 
%   endorse or promote products derived from this software without specific 
%   prior written permission.
%
% THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
% AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
% IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
% ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
% LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
% CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
% SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
% INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
% CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
% ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
% POSSIBILITY OF SUCH DAMAGE.


% This library defines utility predicates that are useful when implementing
% analyses on top of BAT's Prolog representation of Java Bytecode. 
%
% <p><b>
% This library deliberately uses only very basic Prolog language
% constructs since we want to use it for a static analysis engine. This library
% doesn't use any form of extra-logical prediates and doesn't use meta-programming
% techniques. 
% This library should be executable on very primitive Prolog interpreters.
% </b></p>
%
% Version $Date: 2010-03-01 13:12:00 +0100 (Mon, 01 Mar 2010) $ $Rev: 1210 $
% Author Michael Eichberg


% This library uses the following built-in predicates:
%       - throw (in conjunction with an error(resource_error/1) term.)

:- dynamic(uses/2).
:- dynamic(inherits/2).
:- dynamic(subclass/2).
:- dynamic(subtype/2).

% primitive_type(X) :-
%       X is an atom that denotes a primitive type of the Java Programming Language
primitive_type(boolean).
primitive_type(byte).
primitive_type(char).
primitive_type(short).
primitive_type(int).
primitive_type(long).
primitive_type(float).
primitive_type(double).


% subclass(X,Y) :-
%       X and Y are class declarations where X is a direct sub class of Y. 
%       The subclass relation is neither transitive nor reflexive.
subclass(X,Y) :- class_file(_,_,X,Y,_,_,_,_,_,_).
subclass(X,Y) :- class_file(_,_,X,_,Z,_,_,_,_,_),member(Y,Z).


% inherits(X,Y) :-
%       X and Y are class declarations where X inherits from Y. 
%       The inherits relation is transitive, but not reflexive.
inherits(X,Y) :- subclass(X,Y).
inherits(X,Z) :- subclass(X,Y), inherits(Y,Z).


% subtype(X,Y) :-
%       X and Y are class declarations where X is a subtype of Y. 
%       The subtype relation is reflexive and transitive.
%       Example: 
%               subtype(X,class('java/io','Serializable')) 
%               returns all subtypes of java.io.Serializable
subtype(X,X).
subtype(X,Y) :- inherits(X,Y).


% conditional_jump(MID,PC,Branchoffset) :-
%  MID is a method id. 
%  PC is a program counter.
%  Branchoffset is a branch offset.
%       A conditional_jump instruction is an instruction that leads to a jump
%       if a certain condition matches. If the condition does not match, execution
%       continues with the next instruction (fall-through case).
%       This method is particularly useful to implement control-flow analyses, it
%  abstracts over all conditional jump instructions. 
conditional_jump(MID,PC,Branchoffset) :- instr(MID,PC,if(nonnull,Branchoffset)).
conditional_jump(MID,PC,Branchoffset) :- instr(MID,PC,if(null,Branchoffset)).
conditional_jump(MID,PC,Branchoffset) :- instr(MID,PC,if_cmp(reference,eq,Branchoffset)).
conditional_jump(MID,PC,Branchoffset) :- instr(MID,PC,if_cmp(reference,ne,Branchoffset)).
conditional_jump(MID,PC,Branchoffset) :- instr(MID,PC,if_cmp(int,eq,Branchoffset)).
conditional_jump(MID,PC,Branchoffset) :- instr(MID,PC,if_cmp(int,ne,Branchoffset)).
conditional_jump(MID,PC,Branchoffset) :- instr(MID,PC,if_cmp(int,lt,Branchoffset)).
conditional_jump(MID,PC,Branchoffset) :- instr(MID,PC,if_cmp(int,ge,Branchoffset)).
conditional_jump(MID,PC,Branchoffset) :- instr(MID,PC,if_cmp(int,gt,Branchoffset)).
conditional_jump(MID,PC,Branchoffset) :- instr(MID,PC,if_cmp(int,le,Branchoffset)).
conditional_jump(MID,PC,Branchoffset) :- instr(MID,PC,if(eq,Branchoffset)).
conditional_jump(MID,PC,Branchoffset) :- instr(MID,PC,if(ne,Branchoffset)).
conditional_jump(MID,PC,Branchoffset) :- instr(MID,PC,if(lt,Branchoffset)).
conditional_jump(MID,PC,Branchoffset) :- instr(MID,PC,if(ge,Branchoffset)).
conditional_jump(MID,PC,Branchoffset) :- instr(MID,PC,if(gt,Branchoffset)).
conditional_jump(MID,PC,Branchoffset) :- instr(MID,PC,if(le,Branchoffset)).

% jump(MID,PC,Branchoffset) :-
%  A jump instruction is an instruction that always jumps (there is no 
%  "fall-through" case.). However, it is possible that the jump target is
%  determined based on some condition that matches. The athrow instruction
%  is not regarded as a jump instruction since the target could be a handler
%  but could also be outside the current method.
jump(MID,PC,Branchoffset) :- instr(MID,PC,tableswitch(Branchoffset,_,_,_)).
jump(MID,PC,Branchoffset) :- instr(MID,PC,tableswitch(_,_,_,Branchoffsets)),member(Branchoffset,Branchoffsets).
jump(MID,PC,Branchoffset) :- instr(MID,PC,lookupswitch(Branchoffset,_,_)).
jump(MID,PC,Branchoffset) :- instr(MID,PC,lookupswitch(_,_,KVPairs)),member(kv(_,Branchoffset),KVPairs).
jump(MID,PC,Branchoffset) :- instr(MID,PC,goto_w(Branchoffset)).

% Currently, we do not support the "deprecated" jsr and ret instructions.
jump(MID,PC,_) :- instr(MID,PC,jsr_w(_)), throw(error(resource_error('jsr / jsr_w is not supported'))).
jump(MID,PC,_) :- instr(MID,PC,ret(_)), throw(error(resource_error('ret is not supported'))).

% return(?MID,?PC) :-
%       PC is the program counter of a (a,i,l,f,d,<void>) instruction of the method
%       identified by MID.
%return(MID,PC) :-       instr(MID,PC,return). % EVERY RETURN INSTRUCTION HAS A TYPE (void, double, reference,...) AS A PARAMETER
return(MID,PC) :-       instr(MID,PC,return(_)).

% no_control_transfer(MID,PC) :- 
%  true if the instruction does not lead to control transfer; i.e. it is not
%       a (conditional) jump instruction, is not a return instruction and not an
%       athrow instruciton.
no_control_transfer(MID,PC) :- 
        instr(MID,PC,I),
        \+(
                return(MID,PC);
                conditional_jump(MID,PC,_);
                jump(MID,PC,_);
                I = athrow
        ).
        

% class_cid(Class,CID) :-
%       Class specifies a class type (e.g. "class('java/lang','Object')"),
%  CID is the "globally" unique ID for this class id (this id is used by 
%       method declarations to refer to their declaring class.).        
class_cid(Class,CID) :- class_file(CID,_,Class,_,_,_,_,_,_,_).


method_uid(method(DeclaringClass,Name,Signature),MID) :- 
        class_cid(DeclaringClass,CID),
        method(CID,MID,Name,Signature,_,_,_,_,_,_,_,_,_,_,_).


% component_type(A,CT) :-
%       Returns the component type (CT) of an array type (A). If A is not an
%       array type, it is assumed that A is already a component type and A is 
%       unified with CT.
%       Example, if A is "array(array(int))" then CT is "int".
component_type(array(IA),CT) :- component_type(IA,CT), !. % green cut
component_type(A,CT) :- A \= array(_),CT = A, !. % green cut


% class_declaration_uses(S_CID,T_CID) :- 
%       T_CID is the id of a class from which the class S_CID - identified by its CID - 
%       directly inherits.
%class_declaration_uses(S_CID,T_CID) :- 
%       class_file(S_CID,_,_,SC,IIs,_,_,_,_,_),
%       (class_cid(SC,T_CID);(member(II,IIs),class_cid(II,T_CID))).
class_declaration_uses(S_CID,T_CID) :- 
        class_file(S_CID,_,_,SuperClass,_,_,_,_,_,_),class_cid(SuperClass,T_CID).
class_declaration_uses(S_CID,T_CID) :- 
        class_file(S_CID,_,_,_,InheritedInterfaces,_,_,_,_,_),
        member(InheritedInterface,InheritedInterfaces),
        class_cid(InheritedInterface,T_CID).


% field_declaration_uses(S,T) :-
%       T is the uid of the class which is used to declare the type of field S - if
%       any. As in previous cases S is the unique id of a field declaration.
field_declaration_uses(S,T) :- 
        field(_,S,_,Type,_,_,_,_,_,_,_),
        component_type(Type,CT),
        class_cid(CT,T).
        
        
% method_declaration_uses(S,T) :- 
%       S is the uid of a method and T is the uid of a parameter's class type,
%       the return type or the uid of a exception (type) declared to be thrown.
method_declaration_uses(S,T) :- 
        method_exceptions(S,EXs),member(EX,EXs),class_cid(EX,T).
method_declaration_uses(S,T) :-
        method(_,S,_,signature(_,RT),_,_,_,_,_,_,_,_,_,_,_),
        class_cid(RT,T).
method_declaration_uses(S,T) :- 
        method(_,S,_,signature(PTs,_),_,_,_,_,_,_,_,_,_,_,_),
        member(PT,PTs),class_cid(PT,T). 

        

        
% field_accesses(MID,PC,Field) :- 
%  "field_access" abstracts over how a field is accessed.
%       MID is the id of the method, PC is the program counter of this instruction,
%       and Field is a "field(DeclaringClass,Name,Type)" term.
field_accesses(MID,PC,DeclaringClass,Name,Type) :- instr(MID,PC,put(field,DeclaringClass,Name,Type)).
field_accesses(MID,PC,DeclaringClass,Name,Type) :- instr(MID,PC,put(static,DeclaringClass,Name,Type)).
field_accesses(MID,PC,DeclaringClass,Name,Type) :- instr(MID,PC,get(field,DeclaringClass,Name,Type)).
field_accesses(MID,PC,DeclaringClass,Name,Type) :- instr(MID,PC,get(static,DeclaringClass,Name,Type)).
        
        
% method_implementation_uses(S,T) :-
%       S is the unique id of a method (declaration); T is the id of a source 
%       element on which the declaration / implemtation of the method S depends;
%       which is used in the implementation of the method identified by S.
%       T is the unique id of an accessed field, a called method or a catched 
%       exception.
method_implementation_uses(S,T) :- 
        field_accesses(S,_,DeclaringClass,Name,_),
        class_cid(DeclaringClass,CID),
        field(CID,T,Name,_,_,_,_,_,_,_,_).
method_implementation_uses(S,T) :-
        (
                instr(S,_,invoke(virtual,DeclaringClass,Name,Signature));
                instr(S,_,invoke(special,DeclaringClass,Name,Signature));
                instr(S,_,invoke(interface,DeclaringClass,Name,Signature));
                instr(S,_,invoke(static,DeclaringClass,Name,Signature))
        ),
        class_cid(DeclaringClass,CID),
        method(CID,T,Name,Signature,_,_,_,_,_,_,_,_,_,_,_).
method_implementation_uses(S,T) :-
        method_exceptions_table(S,Handlers),
        member(handler(_,_,_,Class),Handlers),class_cid(Class,T).
% Given a valid Java class file it is not necessary to consider the following 
% case, becaus every instantiation of a class has to be followed by a call to
% the instructor and, hence, a corresponding "use" relation.
%method_implementation_uses(S,T) :-
%        instr(S, _, new(Class)),
%       class_cid(Class, T).


% uses (S,T) :-
%       S and T are globally UIDs of source elements (class, field, and 
%       method declarations). 
%       If S and T are instantiated then true is returned if the definition / 
%       declaration of the source element S uses the source element identified by T. 
%       If S and T are not instantiated (variable) then all uses relations with the
%       respective source or target are returned.       
uses(S,T) :- class_declaration_uses(S,T).
uses(S,T) :- field_declaration_uses(S,T).
uses(S,T) :- method_declaration_uses(S,T).
uses(S,T) :- method_implementation_uses(S,T).


% overridden_by((+)SuperMID,?SubMID) :-
%       The method identified by SubMID is a method that directly or indirectly 
%       overrides / implements the method identified by SuperMID. (I.e., if 
%       each method starting from SubMID calls its super method then a call to 
%       SuperMID is finally made.)
%       We assume that the code compiles and no constraints of the JVM Spec. or
%       the Java programming language are violated.
%
%       The second clause handles the special case depicted in the following:
%       package A ... public class X { /*default*/ void foo(){...} }    
%       package A ... public class Y extends X { public void foo(){...} }
%       package B ... public class Z extends Y { public void foo(){...} }
%       In this case Z.foo(). only indirectly overrides A.foo().
%
%       This method is optimized for the case that SuperMID is instantiated. However,
%       it can also be used if SubMID is instantiated, but in this case the 
%       performance will be rather bad. In this case, it is more efficient to use
%       overrides.
overridden_by(SuperMID,SubMID) :-
        method(SuperClassID,SuperMID,Name,Signature,SuperVisibility,_,final(no),static(no),_,_,_,_,_,_,_),
        (       
                        SuperVisibility = public ;
                        SuperVisibility = protected 
        ),
        class_cid(SuperClass,SuperClassID),
        inherits(SubClass,SuperClass),
        class_cid(SubClass,SubClassID),
        method(SubClassID,SubMID,Name,Signature,_,_,_,static(no),_,_,_,_,_,_,_).
overridden_by(SuperMID,SubMID) :-
        method(SuperClassID,SuperMID,Name,Signature,default,_,final(no),static(no),_,_,_,_,_,_,_),
        class_cid(class(SuperClassPN,SuperClassSN),SuperClassID),
        inherits(class(SuperClassPN,SubClassSN),class(SuperClassPN,SuperClassSN)), % we are only interested in classes defined in the same package
        class_cid(class(SuperClassPN,SubClassSN),SubClassID),
        method(SubClassID,CandSubMID,Name,Signature,SubVisibility,_,_,static(no),_,_,_,_,_,_,_),
        (
                % 1. Solution
                SubMID = CandSubMID;
                % 2. there may be more solutions (we have now "lifted" the visibility.)
                (
                        (SubVisibility = protected ; SubVisibility = public),
                        overridden_by(CandSubMID,SubMID)
                )
        ).
% Provides the same functionality as overridden_by/2, but the goal order is 
% changed for performance reasons. (From the logical point of view both
% predicates could be used interchangeably.)
overrides(SubMID,SuperMID) :-
        method(SubClassID,SubMID,Name,Signature,SubVisibility,_,_,static(no),_,_,_,_,_,_,_),
        SubVisibility \= private,
        class_cid(class(SubClassPN,SubClassSN),SubClassID),
        inherits(class(SubClassPN,SubClassSN),class(SuperClassPN,SuperClassSN)), 
        class_cid(class(SuperClassPN,SuperClassSN),SuperClassID),
        method(SuperClassID,CandSuperMID,Name,Signature,SuperVisibility,_,final(no),static(no),_,_,_,_,_,_,_),
        (       
                (       SuperVisibility = default, 
                        SubClassPN = SuperClassPN, 
                        SuperMID = CandSuperMID
                );
                (       
                        (       SuperVisibility = public ;
                                SuperVisibility = protected 
                        ), 
                        (
                                SuperMID = CandSuperMID; % a solution
                                overrides(CandSuperMID,SuperMID) % try to find further solutions
                        )
                )                       
        ).
        

% calls(TrgtMID,SrcMID,PC) :- 
%       The (invoke) instruction with the program counter PC in method SrcMID - at
%  least potentially - directly calls the method TrgtMID.
%       (Super calls are also counted as direct method calls.)
%       This predicate does not calculate the transitive closure!
calls(TrgtMID,SrcMID,PC) :- direct_call(TrgtMID,SrcMID,PC).
calls(TrgtMID,SrcMID,PC) :- calls_of_overridden_Methods(TrgtMID,SrcMID,PC).
% The value of the following rule is "uncertain" - the calls are bound to 
%       other methods.
/*calls(TrgtMID,SrcMID,PC) :- 
        overridden_by(TrgtMID,OverriddenMID),
        method(OverriddenClassID,OverriddenMID,_,_,_,_,_,_,_,_,_,_,_,_,_),
        class_cid(OverriddenClass,OverriddenClassID),
        instantiated(OverriddenClass),
        direct_call(OverriddenMID,SrcMID,PC).
*/
direct_call(TrgtMID,SrcMID,PC) :- 
        method(TrgtClassID,TrgtMID,Name,Signature,_,_,_,static(yes),_,_,_,_,_,_,_),
        Name \= '<clinit>', % the static initializer is only called by the JVM!
        class_cid(TrgtClass,TrgtClassID),
        instr(SrcMID,PC,invoke(static,TrgtClass,Name,Signature)).       
direct_call(TrgtMID,SrcMID,PC) :- 
        method(TrgtClassID,TrgtMID,Name,Signature,_,_,_,static(no),_,_,_,_,_,_,_),
        class_cid(TrgtClass,TrgtClassID),
        instr(SrcMID,PC,invoke(special,TrgtClass,Name,Signature)).      
direct_call(TrgtMID,SrcMID,PC) :- 
        method(TrgtClassID,TrgtMID,Name,Signature,Visibility,_,_,static(no),_,_,_,_,_,_,_),
        Visibility \= private,
        class_cid(TrgtClass,TrgtClassID),
        instr(SrcMID,PC,invoke(virtual,TrgtClass,Name,Signature)).      
direct_call(TrgtMID,SrcMID,PC) :- 
        method(TrgtClassID,TrgtMID,Name,Signature,public,abstract(yes),final(no),static(no),_,_,native(no),_,_,_,_),
        class_cid(TrgtClass,TrgtClassID),
        instr(SrcMID,PC,invoke(interface,TrgtClass,Name,Signature)).
calls_of_overridden_Methods(TrgtMID,SrcMID,PC) :-
        overrides(TrgtMID,SuperMID),direct_call(SuperMID,SrcMID,PC).



% instantiated(?Class) :-
%       Class refers to a class that is instantiated at least once.
instantiated(Class) :- instr(_,_,new_object(Class)).


% makes_super_call(MID) :- 
%       The method MID calls its super method.
makes_super_call(MID) :- 
        instr(MID,_,invoke(special,DeclaringClass,Name,Signature)),
        name \= '<init>',
        class_cid(DeclaringClass,DeclaringClassID),
        method(DeclaringClassID,_,Name,Signature,Visibility,_,_,_,_,_,_,_,_,_,_),
        Visibility \= private.
        
        
% replace(OldList,OldElement,ReplacementList,NewList) :-
%       NewList is a list where all occurences of the element OldElement in the list 
%       OldList is replaced with the list of elements identified by ReplacementList.
replace([],_,_,[]) :- !. % green cut
replace([H|Tail],H,Rs,Ns) :- 
        replace(Tail,H,Rs,TailNs),
        append(Rs,TailNs,Ns).
replace([H|Tail],E,Rs,[H|NewTail]):-
        H \= E,
        replace(Tail,E,Rs,NewTail).

% replace_char(OldString,OldChar,ReplacementString,ResultString)        
%       For example: 
%               replace_char("Dies","D","AAA",R),writef('%s',[R]). 
%               =>
%               AAAies
replace_char(OldString,[C|[]],ReplacementString,ResultString) :-
        replace(OldString,C,ReplacementString,ResultString).    