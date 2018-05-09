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

#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <stdarg.h>
#include <sys/shm.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <fcntl.h>

#include "afl-proxy.h"

#define PID_MAX_LIMIT (1 << 22)  // Portable upper-bound for value defined in linux/threads.h

/* 
* Proxy between AFL and JQF. Communicates to 
* both via pipes -- those launched by AFL and two whose names
* are given as command line args and must be created before 
* launching the proxy. 
*
* This proxy is general and can be used to exchange information
* from any external utility writing in the command-line pipes.
*
* author: Caroline Lemieux
* author: Rohan Padhye
*/

// whether to share an extra map of PERF_SIZE after the trace bits
// (this is unused when compiling with stock AFL)
int use_perf_map = 0;

// whether to log non-fatal (exit(1) causing messages)
// set to != 0 for debugging
int log_non_fatal = 1;

/* 
* Log proxy's progress for debugging. 
* to_exit: whether to exit(1) after logging
* log_file_name: name of file in which to log
* fmt... : format string parameters
*/
void log_to_file(int to_exit, char* log_file_name, char const *fmt, ...) { 
   

    /* If no log file name was provided, do not log */
    if (log_file_name == NULL){
      if (to_exit) exit(1);
      else return;
    } 
    /* if it's not an exit message and we don't log
       non-fatal, do not log */
    if (!to_exit & !log_non_fatal){
      return;
    }

    /* Open the log file for logging if it is not yet open */
    static FILE * log_file = NULL;
    if (log_file == NULL) {
      log_file = fopen(log_file_name, "w");
       if (log_file < 0) {
        printf("Couldn't open log file, %s\n", log_file_name);
        exit(1);
      }
    }

    /* print to log file */
    va_list ap;
    va_start(ap, fmt);
    vfprintf(log_file, fmt, ap);
    va_end(ap);

    /* flush to log file since this program might terminate strangely */
    fflush(log_file);

    /* exit if the exit parameter is given */
    if (to_exit) {
      fclose(log_file);
      exit(1);
    }

}

/* main proxy driver. communication channel between a running instance
   of AFL and Java */
int main(int argc, char** argv) {

  /* usage */
  if (argc < 3 || argc > 4){
    printf("usage: %s afltojavafifo javatoaflfifo [logfile]", argv[0]);
    exit(1);
  }

  /* collect file names from arguments */ 
  char * to_java_str = argv[1];
  char * from_java_str = argv[2];
  char * log_file_name = NULL;
  if (argc == 4) log_file_name = argv[3];

  /* set up buffers */
  u8 helo[4] = {'H', 'E', 'L', 'O'}; // to set up connections
  uint32_t status = 0; // to receive + send status from java
  u8 buf[4]; // to receive signals from AFL
  u32 child_pid = PID_MAX_LIMIT + 1; // A PID that can never exist in practice

  /* Run once? Used by afl-showmap and perhaps other tools to avoid looping. */
  bool run_once = false;

  /* temp variable to store communicated bytes */
  int comm_bytes;

#ifdef PERF_SIZE
  /* decide if perf_bits have to be shared */
  if (getenv("JQF_PERF_MAP") != NULL) {
    use_perf_map = 1;
  }
#endif  

  /* set up FIFOs to talk to Java */
  FILE * to_java_fd = fopen(to_java_str, "w");
  if (to_java_fd == NULL){
    log_to_file(1, log_file_name, "Failed to open to java fifo %s\n", to_java_str);
  }

  log_to_file(0, log_file_name, "opened to java fifo %s\n", to_java_str);

  FILE * from_java_fd = fopen(from_java_str, "r");
  if (from_java_fd == NULL){
    log_to_file(1, log_file_name, "Failed to open from java fifo %s\n", from_java_str);
  }

  log_to_file(0, log_file_name, "opened from java fifo %s\n", from_java_str);

  /* set up the trace bits */
  char * shm_str = getenv(SHM_ENV_VAR);
  if (shm_str == NULL){
    log_to_file(1, log_file_name, 
      "Error getting the address of trace_bits from env var %s\n", shm_str);
  }
  int shm_id = atoi(shm_str);
  u8* trace_bits = shmat(shm_id, NULL, 0);
  if (trace_bits < 0){
    log_to_file(1, log_file_name, "Error shmat()ing trace_bits from id %d\n", shm_id);
  }

  /* perf map is right after coverage bit map (unused with stock AFL) */
  u32* perf_bits = (u32*) &trace_bits[MAP_SIZE];
 
  /* say the first hello to AFL. use write() because we
     have an int file descriptor */
  if (write(FORKSRV_FD + 1, (void*) &helo, 4) < 4) {
    log_to_file(0, log_file_name, "Error saying initial hello to AFL\n");
    run_once = true;
  } else {
    log_to_file(0, log_file_name, "Said hello to AFL (init).\n");
  }

  /* main fuzzing loop. AFL sends ready signals through  
     pipe with file descriptor FORKSRV_FD */
  while (run_once || read(FORKSRV_FD,(void *)&buf, 4) == 4){

    if (!run_once) {
      /* this sends "child pid" to AFL -- we use an impossible PID
       * that AFL cannot kill even in the presence of timeouts. */
      if ((comm_bytes = write(FORKSRV_FD+1, &child_pid, 4)) < 4) {
        log_to_file(1, log_file_name, 
          "Something went wrong saying hello to AFL in loop: wrote %d bytes.\n", comm_bytes);
      }
      log_to_file(0, log_file_name, "Said hello to AFL (in loop).\n");
    }

    /* Say hello to Java */
    if ((comm_bytes = fwrite(&helo, 1, 4, to_java_fd)) < 4) {
      log_to_file(1, log_file_name, 
        "Something went wrong saying hello to Java: wrote %d bytes.\n", comm_bytes);
    } 
    /* need to flush the buffer */
    fflush(to_java_fd);

    log_to_file(0, log_file_name, "Said hello to Java.\n");

    /* Get return code from Java */
    if ((comm_bytes = fread((void *) &status, 1 , 4, from_java_fd)) < 4) {
      log_to_file(1, log_file_name, 
        "Something went wrong getting return status from Java: read %d bytes.\n", comm_bytes);
    }

    log_to_file(0, log_file_name, "Got return status from Java.\n");

    /* Get trace bits from Java */
    if ((comm_bytes = fread( trace_bits, 1, MAP_SIZE, from_java_fd)) < MAP_SIZE) {
      log_to_file(1, log_file_name, 
        "Something went wrong getting trace_bits from Java: read %d bytes.\n", comm_bytes);
    }

    log_to_file(0, log_file_name, "Got trace bits from java.\n");
    
#ifdef PERF_SIZE
    if (use_perf_map) {
      /* Get perf bits from Java */
      if ((comm_bytes = fread( perf_bits, 4, PERF_SIZE, from_java_fd)) < PERF_SIZE) {
        log_to_file(1, log_file_name, 
          "Something went wrong getting perf_bits from Java: read %d bytes.\n", comm_bytes);
      }

      log_to_file(0, log_file_name, "Got perf bits from java.\n");
    }
#endif

    if (!run_once) {
      /* Tell AFL we got the return */
      if((comm_bytes = write(FORKSRV_FD + 1, &status, 4)) < 4) {
        log_to_file(1, log_file_name, 
          "Something went wrong sending return status to AFL: wrote %d bytes.\n", comm_bytes);
      }

      log_to_file(0, log_file_name, "sent return status to AFL.\n");
    } else {
      break; // run_once
    }
  }

  /* teardown. Will probably never be called */
  if (fclose(to_java_fd) != 0) { 
    log_to_file(1, log_file_name, 
        "Something went wrong closing pipe to Java.\n");
  }
  if (fclose(from_java_fd) != 0) { 
    log_to_file(1, log_file_name, 
        "Something went wrong closing pipe from Java.\n");
  }
  exit(status);

}
