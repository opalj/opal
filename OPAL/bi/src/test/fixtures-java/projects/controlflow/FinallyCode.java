/* BSD 2-Clause License - see OPAL/LICENSE for details. */
package controlflow;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Created to test the computation of control flow graphs.
 *
 * @author Erich Wittenbeck
 */
@SuppressWarnings("all")
public class FinallyCode {

    void tryFinally(int input) {
        int x = 100;

        try {
            x = x / input;
        } finally {
            int a = 1;
            int b = 2;

            if (x * (a + b) > 50) {
                int d = 3;
                int e = 4;
            } else {
                int f = 5;
                int g = 6;
            }
        }

        return;
    }

	int loopInFinally(int[] array, int a) {
        int res = 0;

        try {
            res = array[a];
        } catch (ArrayIndexOutOfBoundsException e) {
            return -1;
        } finally {
            for (int i = 0; i < array.length; i++)
                res += array[i];
        }

        return res;
    }

    int highlyNestedFinally(int[] array, int a) {
        int res = 0;

        try {
            res += array[a];
            try {
                res += array[a + 1];
                try {
                    res += array[a + 2];
                    try {
                        res += array[a + 3];
                    } catch (ArrayIndexOutOfBoundsException e) {
                        res--;
                    } finally {
                        return res;
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    res--;
                } finally {
                    return res;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                res--;
            } finally {
                return res;
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            res--;
        } finally {
            return res;
        }
    }

	int duplicateInCatchblock(int a, int b){
		int res = 0;
		try{
			res = a/b;
		}catch(ArithmeticException ae){
			res = b/a;
		}finally{
			return res++;
		}

	}

	void nestedFinally(int a, int b, int c, int d){

		try{
			int e = a/b;
		}finally{
			try{
				int f = c/d;
			}finally{
				int g = b/c;
			}
		}
	}

	int tryFinallyBranch(int a, int b, int c){
		try{
			if(a/b > 1){
				return 1;
			}
		}finally{
			return a*c;
		}
	}

	String tryWithResources(String path) throws IOException{
		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
			return br.readLine();
		}
	}

	String tryWithResourcesCatch(String path) throws IOException{
		try (BufferedReader br = new BufferedReader(new FileReader(path))) {
			return br.readLine();
		}catch(Exception e){
			return "whoops";
		}
	}

	String tryWithResourcesOldSchool(String path) throws IOException{
		BufferedReader br = new BufferedReader(new FileReader(path));
		try {
			return br.readLine();
		} finally {
			if (br != null) br.close();
		}
	}

}
