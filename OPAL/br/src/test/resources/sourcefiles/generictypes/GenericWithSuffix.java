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
package classhierarchy;

/**
 * This class models a Generic class with a class suffix. A class suffix is created by
 * refering to an inner class.
 * 
 * @example {{{
 *  GenericWithSuffix<String>.Suffix1_1<String> field = null;
 *  
 *  Suffix1_1<String> would be the Suffix of the GenericWithSuffix<String> signature.
 * }}}
 * 
 * @author Michael Reif
 *
 */
public class GenericWithSuffix<E> {

    public class Suffix1_1<E>{
        
        public class Suffix1_2<E>{
            
        }
        
        class Suffix1_3 extends Suffix1_2<E>{
            
        }
        
        class Suffix1_4 implements Interface<Base>{
            
        }
        
        class Suffix1_5<T>{
            
        }
        
        class Suffix1_6 extends Suffix1_5<Base>{
            
        }
        
        class Suffix1_7<T> extends Base{}
    }
    
    class Suffix2_1<T>{
        
        class Suffix2_2<V, W>{}
        
        class Suffix2_3<V, W> extends Suffix2_2<V, W>{}
        
        class Suffix2_4<S1 extends E, S2 extends T>{}
    }
    
    class Suffix3_1<E>{
        class Suffix3_2{
            
        }
    }
    
    public class Suffix4_1<E> extends Suffix1_1<E>{
        class Suffix4_2{
            
        }
    }
}