/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package java11nests;

public class NestHost {

    private void nestHostMethod() {}

    class NestMember1 {
        private void nestMember1Method() {}
    }

    class NestMember2 {}

}

class NoNestMember {
    private void noNestMemberMethod() {}
}