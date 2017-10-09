package org.opalj.fpcf.properties;


import java.util.Optional;

/**
 * Defines a class that given a specific entity and property validates if the property is
 * as expected. The class implicitly defines the expectation.
 */
public interface PropertyMatcher {

    /**
     *
     * @return 'None' if the property was successfully matched; 'Some(<String>)' if the
     *         property was not successfully matched. The String describes the reason.
     */
    Optional<String> hasProperty(
                       Project<?> p,
                       Object entity,
                       Property property                   );


}
