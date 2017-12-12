CC ?= clang

all: bin/afl-proxy

bin/afl-proxy: fuzz/src/main/c/afl-proxy.c
	$(CC) $(CFLAGS) $< -o $@ 

clean:
	rm -f bin/afl-proxy

