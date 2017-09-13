/*
 * Copyright (c) 2017, University of California, Berkeley
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <stdarg.h>
#include <sys/shm.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <fcntl.h>

#include "types.h"
#include "config.h"

/* 
* Proxy between AFL and some JavaQuickCheck. Communicates to 
* both via pipes -- those launched by AFL and two whose names
* are given as command line args and must be created before 
* launching the proxy. 
*
* This proxy is general and can be used to exchange information
* from any external utility writing in the command-line pipes.
*
* author: Caroline Lemieux
*/


/* 
* Log proxy's progress for debugging. 
* exit: whether to exit(1) after logging
* log_file_name: name of file to log 
*/
void log(int exit, char* log_file_name, char const *fmt, ...) { 
    /* If no log file name was provided, do not log */
    if (log_file_name == NULL){

    }
    static FILE *log_file_fd = NULL;
    if (f == NULL) {
      log_file_fd = fopen(log_file_name, "w");
       if (log_file_fd < 0) {
        printf("Couldn't open log file, %s\n", log_file_name);
        exit(1);
      }
    }
    va_list ap;
    va_start(ap, fmt);
    vprintf(fmt, ap);
    va_end(ap);
    va_start(ap, fmt);
    vfprintf(log_file_fd, fmt, ap);
    va_end(ap);
    fflush(log_file_fd);
    if (exit) {
      fclose(log_file_fd);
      exit(1);
    };
}

/* main proxy driver. communication channel between a running instance
   of AFL and Java */
int main(int argc, char** argv) {

  if (argc < 4 || argc > 5){
    printf("usage: %s inputfile afltojavafifo javatoaflfifo [logfile]", argv[0]);
  }
  char * log_file_name = 

  FILE * log_file_fd = fopen("afljavafuzzing.log", "w");
  if (log_file_fd < 0) {
    printf("Couldn't open log file\n");
    exit(1);
  }
  fputs("adaa\n", log_file_fd);
  fflush(log_file_fd);

  /* set up FIFOs to talk to java */
  char * to_java_str = "/tmp/AFLtoJavaFIFO";
  char * from_java_str = "/tmp/JavatoAFLFIFO";
  // TODO: which permissons?
  if (mkfifo(to_java_str, 0777) < 0) {
    fputs("Failed to create to java fifo\n", log_file_fd);
    fflush(log_file_fd);
    exit(1);
  }
  if (mkfifo(from_java_str, 0777) < 0) {
    fputs("Failed to create from java fifo\n", log_file_fd);
    fflush(log_file_fd);
    exit(1);
  }
  FILE * to_java_fd = fopen(to_java_str, "w");
  if (to_java_fd == NULL){
    fputs("Failed to open to java fd\n", log_file_fd);
    fflush(log_file_fd);
    exit(1);
  }
  FILE * from_java_fd = fopen(from_java_str, "r");
  if (from_java_fd == NULL){
    fputs("Failed to open from java fd\n", log_file_fd);
    fflush(log_file_fd);
    exit(1);
  }

  /* set up the trace bits */
  char * shm_str = getenv(SHM_ENV_VAR);
  if (shm_str == NULL){
    fputs("Error getting the address of trace_bits\n", log_file_fd);
    fflush(log_file_fd);
    exit(1);
  }
  int shm_id = atoi(shm_str);
  u8* trace_bits = shmat(shm_id, NULL, 0);
  if (trace_bits < 0){
    fputs("Error connecting to trace_bits\n", log_file_fd);
    fflush(log_file_fd);
    exit(1);
  }
  
  /* say the first hello to AFL */
  char helo[4] = {'H', 'E', 'L', 'O'};
  if (write(FORKSRV_FD + 1, (void*) &helo, 4) < 4) {
    fputs("Something went wrong saying hello to AFL.", log_file_fd);
    fflush(log_file_fd);
    exit(1);
  }

  /* buffer to catch AFL's cues */
  char buf[4];
  /* status buffer */
  uint32_t status = 0 ;
  //char status[4] = {'\x00', '\x00', '\x00', '\x00'};
  /* main fuzzing loop */
  while (read(FORKSRV_FD,(void *)&buf, 4) == 4){
    /* this sends "child pid" to AFL -- effectively
       just another hello                         */
    if (write(FORKSRV_FD+1, &helo, 4) < 4) {
      fputs("Something went wrong saying hello to AFL (in loop).\n", log_file_fd);
      fflush(log_file_fd);
      exit(1);
    }

    /* Say hello to Java */
    if (fwrite(&helo, 1, 4, to_java_fd) < 4) {
      fputs("Something went wrong saying hello to Java.\n", log_file_fd);
      fflush(log_file_fd);
      exit(1);
    } 
    
    fflush(to_java_fd);
    fputs("I just said hello to Java\n", log_file_fd);

    fflush(log_file_fd);
    int tmp;
    /* Get return code from Java */
    if ((tmp = fread((void *) &status, 1 , 4, from_java_fd)) < 4) {
      fprintf(log_file_fd, "Something went wrong getting return status from Java (%d bytes).\n", tmp);
      fflush(log_file_fd);
      exit(1);
    }
    fprintf( log_file_fd, "Got return status from Java...\n");
    fflush(log_file_fd);
    /* Get trace bits from Java */
   /*if (read(from_java_fd, &trace_bits, sizeof(u8)*MAP_SIZE) < MAP_SIZE) {
      fputs("Something went wrong getting trace bits from Java.\n", log_file_fd);
      exit(1);
    }*/
    int trace_bits_size;

    if ((trace_bits_size = fread( trace_bits, 1, MAP_SIZE, from_java_fd)) < MAP_SIZE) {
      fprintf(log_file_fd, "trace_bits_size: %d\n", trace_bits_size);
      fputs("Something went wrong getting trace bits from Java.\n", log_file_fd);
      fflush(log_file_fd);
      exit(1);
    }
    fprintf(log_file_fd, "trace_bits_size: %d\n", trace_bits_size);

    //trace_bits[0] = 1;
    //fprintf( log_file_fd, "Got trace bits from Java...\n");
    //fwrite(&trace_bits, sizeof(u8), MAP_SIZE, log_file_fd);
    fflush(log_file_fd);

    /* Tell AFL we got the return */
    if( write(FORKSRV_FD + 1, &status, 4) < 4) {
      fputs("Something went wrong sending return status to AFL.", log_file_fd);
      fflush(log_file_fd);
    }
    fprintf(log_file_fd, "Told AFL we returned\n");
    fflush(log_file_fd);
  }

  /* teardown */
  if (fclose(to_java_fd) == 0) { fputs("Couldn't close pipe to Java\n", log_file_fd);}
  if (fclose(from_java_fd) == 0) { fputs("Couldn't close pipe from Java\n", log_file_fd);}

  exit(0);

}
