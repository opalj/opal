/* BSD 2-Clause License:
 * Copyright (c) 2009 - 2017
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
package lambdas.methodreferences;

import annotations.target.InvokedMethod;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;

import static annotations.target.TargetResolution.*;


/**
 * This class contains a examples for method references dealing with proxy class receiver inheritance.
 *
 * <!--
 * INTENTIONALLY LEFT EMPTY (THIS AREA CAN BE EXTENDED/REDUCED TO MAKE SURE THAT THE
 * SPECIFIED LINE NUMBERS ARE STABLE.
 * -->
 *
 * @author Andreas Muttscheller
 */
public class ReceiverInheritance {
    public static <T, R> R someBiConsumerParameter(
    		java.util.function.Supplier<R> s,
    		java.util.function.BiConsumer<R, T> bc,
			java.util.function.BiConsumer<R, R> r,
			T t) {
		R state = s.get();
		bc.accept(state, t);
		r.accept(state, state);

		return state;
	}

	public static <T> LinkedHashSet<T> callBiConsumer(T t) {
		LinkedHashSet<T> lhm = ReceiverInheritance.<T, LinkedHashSet<T>>someBiConsumerParameter(
				LinkedHashSet::new,
				LinkedHashSet::add,
				LinkedHashSet::addAll,
				t
		);

		return lhm;
	}

	public static <T> void instanceBiConsumer() {
		LinkedHashSet<T> lhm = new LinkedHashSet<T>();
		java.util.function.Consumer<T> bc = lhm::contains;

		lhm.contains("foo");
	}
}
