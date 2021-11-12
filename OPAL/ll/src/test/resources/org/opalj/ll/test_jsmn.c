#include <stdio.h>
#include <string.h>
#include "jsmn.h"

int main() {
    char* s = "{\"test\": \"this is a test\", \"foo\": 42}";

    jsmn_parser p;
    jsmntok_t t[128]; /* We expect no more than 128 JSON tokens */

    jsmn_init(&p);
    int r = jsmn_parse(&p, s, strlen(s), t, 128); // "s" is the char array holding the json content
}
