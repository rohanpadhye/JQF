ifndef AFL_DIR
$(error AFL_DIR is not set)
endif

AFLDIR = ${AFL_DIR}
CC ?= clang

all: bin/afl-proxy

bin/afl-proxy: fuzz/src/main/c/afl-proxy.c
	$(CC) $(CFLAGS) -I$(AFLDIR) $< -o $@ 

clean:
	rm -f bin/afl-proxy

