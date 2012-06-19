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
%
% Version $Date: 2009-11-20 11:53:42 +0100 (Fri, 20 Nov 2009) $ $Rev: 639 $
% Author Michael Eichberg


%:- multifile(instr/3). % ISO Prolog directive (doesn't work with SWIPL)
:- discontiguous(violations/5).

% violations(?Source,?Package,?LineNumber,?Category,?Description) :- 
%	- Source is the simple name of the source file (in case of Java class files it 
%	is the file name made available using class_file_source facts.) If no
%	source file information is available the result has to be the atom 'N/A'.
%	The source file name never includes path information.
%	- Path is the source file's path. If no path information is available 'N/A'
%	is to be used.
%	- LineNumber is an integer atom that points to the source line where the
%	violation occurs. If no line number information is available 'N/A' is to
%	be used.
%	- Description is an atom describing the reason of the violation and (if
%	possible) gives hints how to fix the violation.

% 1st Checker
violations(Source,Path,'N/A','Warning','Covariant equals defined.') :- 
	method(C,_,'equals',signature([P],boolean),public,_,_,_,_,_,_,_,_,_,_),P \= class('java/lang','Object'),
	class_file_source(C,Source),
	class_file(C,_,class(Path,_),_,_,_,_,_,_,_).
	
% 2nd Checker
violations(Source,Path,'N/A','Warning','Equals is defined, but hashCode is missing.') :- 
	method(C,_,'equals',signature([class('java/lang','Object')],boolean),public,_,_,_,_,_,_,_,_,_,_),
	\+ method(C,_,'hashCode',signature([],int),public,_,_,_,_,_,_,_,_,_,_),
	class_file_source(C,Source),
	class_file(C,_,class(Path,_),_,_,_,_,_,_,_).

% 3rd Checker
violations(Source,Path,'N/A','Warning','A serialVersionUID is not declared though this class is marked as serializable') :- 
	subclass(X,class('java/io','Serializable')),
	class_file(C,class,X,_,_,_,_,_,_,_), 
	\+ field(C,_,'serialVersionUID',_,_,_,_,_,_,_,_),
	class_file_source(C,Source), 
	X = class(Path,_).

% 4th Checker
violations(Source,Path,'N/A','Warning','The field serialVersionUID should have type long and should be private, static and final') :- 
	class_file(C,_,class(Path,_),_,_,_,_,_,_,_),
	field(C,F,'serialVersionUID',_,_,_,_,_,_,_,_),
	\+ field(C,F,_,long,private,final(yes),static(yes),_,_,_,_),
	class_file_source(C,Source).


% 5th Checker
% To work correctly this checker requires that "all classes" of a project - 
% including library classes - are available otherwise a field's type cannot 
% be correctly determined
serializable(T) :- 
	T = class(_,_),						
	subtype(T,class('java/io','Serializable')).
serializable(array(C)) :- 
	serializable(C).
serializable(T) :-
	primitive_type(T). % all primitives are serializable
violations(Source,Path,'N/A','Warning',M) :- 
	subclass(class(Path,Name),class('java/io','Serializable')),
	class_file(C,_,class(Path,Name),_,_,_,_,_,_,_),
	field(C,F,N,T,_,_,_,transient(no),_,_,_),
	\+ field(C,F,_,_,_,final(yes),static(yes),_,_,_,_),% final static fields are never serialized
	\+ serializable(T),
	class_file_source(C,Source), 
	concat_atom(['The field ',N,' is not serializable'],M).

