/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package generictypes;

/**
 * This class models a generic class with a class suffix. A class suffix is created by
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
 @SuppressWarnings("hiding")
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
