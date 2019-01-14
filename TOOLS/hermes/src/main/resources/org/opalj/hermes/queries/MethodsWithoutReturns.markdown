# Methods Without Returns

This analysis derives two features:

 1. Those methods that neither return normally nor abnormally.
    E.g.,

        while(true) {
            try { ...}
            catch {
                case t : Throwable => /* ignored */
            }
        }

    or

        while (true){;}

 1. Those methods that – if at all – only return abnormally.
