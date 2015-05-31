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
package tactest;

/**
 * Class with simple methods containing control sequences like if-then.
 * 
 * @author Roberts Kolosovs
 *
 */
public class ControlSequences {

	int icmpne(int a, int b) {
		if (a == b) {
			return a;
		}
		return b;
	}

	int icmpeq(int a, int b) {
		if (a != b) {
			return a;
		}
		return b;
	}

	int icmpge(int a, int b) {
		if (a < b) {
			return a;
		}
		return b;
	}

	int icmplt(int a, int b) {
		if (a >= b) {
			return a;
		}
		return b;
	}

	int icmple(int a, int b) {
		if (a > b) {
			return a;
		}
		return b;
	}

	int icmpgt(int a, int b) {
		if (a <= b) {
			return a;
		}
		return b;
	}

	int ifne(int a) {
		if (a == 0) {
			return a;
		}
		return 0;
	}

	int ifeq(int a) {
		if (a != 0) {
			return a;
		}
		return 0;
	}

	int ifge(int a) {
		if (a < 0) {
			return a;
		}
		return 0;
	}

	int iflt(int a) {
		if (a >= 0) {
			return a;
		}
		return 0;
	}

	int ifle(int a) {
		if (a > 0) {
			return a;
		}
		return 0;
	}

	int ifgt(int a) {
		if (a <= 0) {
			return a;
		}
		return 0;
	}

	int ifTest(int a, int b) {
		int result = 0;
		if (a == b) {
			result = a;

			if (a != b) {
				result = a;
			} else if (a < b) {
				result = a;
			} else {
				result = 1;
			}
		} else if (a >= b) {
			result = a;
		} else if (a > b) {
			result = a;
		} else if (a <= b) {
			result = a;
		} else if (a == 0) {
			result = a;
		} else if (a != 0) {
			result = a;
		} else if (a < 0) {
			result = a;
		} else if (a >= 0) {
			result = a;
		} else if (a > 0) {
			result = a;
		} else {
			result = a;
		}
		return result;
	}
}
