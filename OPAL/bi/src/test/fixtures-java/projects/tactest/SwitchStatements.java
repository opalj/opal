/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package tactest;

/**
 * Class with simple methods containing switch statements.
 * 
 * @author Roberts Kolosovs
 */
public class SwitchStatements {

    int tableSwitch(int a) {
        switch (a) {
        case 1:
            return 1;
        case 2:
            return 2;
        case 3:
            return 3;
        default:
            return 0;
        }
    }

    int lookupSwitch(int a) {
        switch (a) {
        case 1:
            return 10;
        case 10:
            return 200;
        default:
            return 0;
        }
    }

}
