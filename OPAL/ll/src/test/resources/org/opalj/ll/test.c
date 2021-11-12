#include <stdio.h>

int pure_fuction(int a, int b) {
  return a + b;
}

void impure_function(int* foo) {
  *foo = 23;
}

int main() {
  printf("hello world\n");
  int result = pure_fuction(1,2);
  printf("result: %i\n", result);
  int foo = 42;
  impure_function(&foo);
  printf("foo: %i", foo);
  return 0;
}
