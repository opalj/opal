#include <stdio.h>

int foo = 23;

int pure_function(int a, int b) {
  return a + b;
}

void impure_function() {
  foo = 42;
}

int main() {
  printf("hello world, foo is %i\n", foo);
  int result = pure_function(1,2);
  printf("result: %i\n", result);
  impure_function();
  printf("foo: %i\n", foo);
  return 0;
}
