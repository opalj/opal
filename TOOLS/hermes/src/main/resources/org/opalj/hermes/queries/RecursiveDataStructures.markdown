# Recursive Data Structures

Identifies recursive data structures. In general, a data structure is recursive if it may contain values of itself.

One of the simplest examples of a recursive data structure is a linked list. Another one are the nodes of a tree, as, e.g., used by the composite pattern. Next, we see a very simple linked list definition.

    case class List[T](value : T, rest: List[T])

Analyzing programs with recursive data-structures is particularly interesting, e.g., when developing field sensitive analyses.

The query finds self and mutually recursive data structures. The query first builds a kind of **data-type dependency graph** and then searches for strongly connected components in that graph. For example, the graph related to the relations for the following code:

    class U(v : U, i : Int)
    class X(v : Y, isDone : Boolean) extends Object
    class Y(v1 : Z, v2 : Z) extends U
    class Z(v : X, mutex : Object) extends Object

would be:

    Graph (
        Object -> { }
        U -> { U } // ⟲
        X -> { Y }
        Y -> { Z }
        Z -> { X, Object }
    )

I.e., though the `mutex` field of `Z` could also reference a Y or Z value, this would not be considered because it is in general not relevant. Overall, the algorithm would identify U and Y as being self-recursive data-structures.
