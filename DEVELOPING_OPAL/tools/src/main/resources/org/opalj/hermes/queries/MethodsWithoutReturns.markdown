#Methods Without Returns

This analysis derives two features:

 1. Those methods that neither return normally nor abnormally.

     while(true) {try { ...} catch {case t : Throwable => /*just forget about it*/ }

 1. Those methods that – if at all – only return abnormally. 