import java.util.NoSuchElementException;

public class ExceptionCode{
	
	int simpleException(int index){
		int[] array = {1, 2, 3, 4};
		int res = 0;
		
		try{
			res = array[index];
		}catch(ArrayIndexOutOfBoundsException e){
			return -1;
		}
		
		return res;
	}
	
	int multipleCatchAndFinally(int index){
		int[] array = {1, 2, 3, 4};
		int res = 0;
		
		try{
			res = 5/array[index];
		}catch(ArrayIndexOutOfBoundsException e){
			res = -1;
		}catch(ArithmeticException e){
			res = -1;
		}finally{
			return res;
		}
	}
	
	
	
	
	int nestedExceptions(int index){
		int[] array = {0, 1, 2, 3, 4, 5};
		int res = 0;
		
		try{
			int divisor = array[index];
			try{
				res = 6 / divisor;
			}catch(ArithmeticException e){
				return -1;
			}
		}catch(ArrayIndexOutOfBoundsException e){
			try{
				int newDivisor = array[index+1];
				res = 6/newDivisor;
			}catch(ArithmeticException e2){
				return -1;
			}
		}
		
		
		return res;
	}
	
	
	int loopExceptionWithFinallyReturn(int[] array, int a){
		
		int res = 0;
		
		try{
			res = array[a];
		}catch(ArrayIndexOutOfBoundsException e){
			for(int i = 0; i<array.length; i++)
				res += array[i];
		}finally{
			return res;
		}
	}
	
	int loopExceptionWithCatchReturn(int[] array, int a){
		int res = 0;
		
		try{
			res = array[a];
		}catch(ArrayIndexOutOfBoundsException e){
			return -1;
		}finally{
			for(int i = 0; i<array.length; i++)
				res += array[i];
		}
		
		return res;
	}
	
	
	int highlyNestedFinally(int[] array, int a){
		int res = 0;
		
		try{
			res += array[a];
			try{
				res += array[a+1];
				try{
					res += array[a+2];
					try{
						res += array[a+3];
					}catch(ArrayIndexOutOfBoundsException e){
						res--;
					}finally{
						return res;
					}
				}catch(ArrayIndexOutOfBoundsException e){
					res--;
				}finally{
					return res;
				}
			}catch(ArrayIndexOutOfBoundsException e){
				res--;
			}finally{
				return res;
			}
		}catch(ArrayIndexOutOfBoundsException e){
			res--;
		}finally{
			return res;
		}
	}
	
	void tryFinally(int input){
		int x = 100;
		
		try{
			x = x/input;
		}finally{
			int a = 1;
			int b = 2;
			
			if(x*(a + b) > 50){
				int d = 3;
				int e = 4;
			}else{
				int f = 5;
				int g = 6;
			}
		}
		
		return;
	}
}