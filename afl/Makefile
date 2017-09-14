ifndef AFL_DIR
$(error AFL_DIR is not set)
endif

AFLDIR = ${AFL_DIR}
CC = clang

all: proxy.c
	$(CC) $(CFLAGS) -o proxy proxy.c -I$(AFLDIR)

clean:
	rm -f proxy

