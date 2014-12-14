/* stack.h - simple stacking */

#ifndef HOEDOWN_STACK_H
#define HOEDOWN_STACK_H

#include <stdlib.h>

#ifdef __cplusplus
extern "C" {
#endif

struct hoedown_stack {
	void **item;
	size_t size;
	size_t asize;
};

typedef struct hoedown_stack hoedown_stack;

int hoedown_stack_new(hoedown_stack *, size_t);
void hoedown_stack_free(hoedown_stack *);
int hoedown_stack_grow(hoedown_stack *, size_t);
int hoedown_stack_push(hoedown_stack *, void *);
void *hoedown_stack_pop(hoedown_stack *);
void *hoedown_stack_top(hoedown_stack *);

#ifdef __cplusplus
}
#endif

#endif /** HOEDOWN_STACK_H **/
