/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package org.opalj.ifds

object IFDS {

    /**
     * Merges two maps that have sets as values.
     *
     * @param map1 The first map.
     * @param map2 The second map.
     * @return A map containing the keys of both maps. Each key is mapped to the union of both maps'
     *         values.
     */
    def mergeMaps[S, T](map1: Map[S, Set[T]], map2: Map[S, Set[T]]): Map[S, Set[T]] = {
        var result = map1
        for ((key, values) ← map2) {
            result.get(key) match {
                case Some(resultValues) ⇒
                    if (resultValues.size > values.size)
                        result = result.updated(key, resultValues ++ values)
                    else
                        result = result.updated(key, values ++ resultValues)
                case None ⇒
                    result = result.updated(key, values)
            }
        }
        result
    }
}
