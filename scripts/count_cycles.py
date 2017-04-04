#!/usr/bin/env pypy
"""
 Copyright (c) 2016, University of California, Berkeley

 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are
 met:

 1. Redistributions of source code must retain the above copyright
 notice, this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright
 notice, this list of conditions and the following disclaimer in the
 documentation and/or other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
"""
import argparse
from collections import defaultdict
import re

REGEXP_BRANCH = re.compile("^\s*BRANCH\((-?\d+),(\d+)\)")
REGEXP_CALL   = re.compile("^\s*CALL\((\d+),(\d+)\)")
REGEXP_RET    = re.compile("^\s*RET")
REGEXP_BEGIN    = re.compile("^\s*BEGIN (.*)")

def main():	
	# Command-line arguments
	parser = argparse.ArgumentParser(description='Collect AECs from a trace and count them')
	parser.add_argument('--input', type=str, dest='trace_file', default='main.log')


	# Parse arguments
	args = parser.parse_args()

	# Process trace file
	process_trace(args.trace_file)

	# Print top AEC count
	print_top_aec_count()


def process_trace(trace_file_name):
	with open(trace_file_name) as trace_file:
		while True:
			# Read line from file
			line = trace_file.readline()
			# End-of-file is empty line
			if not line:
				break
			# Try to match BRANCH(iid, line)
			match_branch = REGEXP_BRANCH.match(line)
			if match_branch:
				handle_branch(int(match_branch.group(1)), int(match_branch.group(2)))
				continue
			# Try to match CALL(iid, line)
			match_call = REGEXP_CALL.match(line)
			if match_call:
				handle_call(int(match_call.group(1)), int(match_call.group(2)))
				continue

			# Try to match BEGIN method
			match_begin = REGEXP_BEGIN.match(line)
			if match_begin:
				handle_begin(match_begin.group(1))
				continue
			# Try to match RET
			match_ret = REGEXP_RET.match(line)
			if match_ret:
				handle_ret()
				continue
			# Otherwise, error
			raise Exception("Cannot parse trace line: " + line)

# GLOBALS
call_stack = []         # [(STR, INT)]   // Call stack of (Method, IID)
line_numbers = {}       # INT -> INT     // Maps IIDs to line numbers
aec_id_map = {}         # SEQ -> INT   // Maps an AEC tuple to an AEC identifier, where SEQ = ((METHOD, IID)+)
aec_seq_tab = []        # INT -> [SEQ] // Maps an AEC identifier to an AEC tuple, where SEQ = ((METHOD, IID)+)
aec_counts = defaultdict(int)   # SEQ -> INT

def handle_branch(iid, line):
	global call_stack
	# Set PC of top-of-stack
	call_stack[-1] = (call_stack[-1][0], iid)
	# Remember line number
	line_numbers[iid] = line
	# Compute AEC and add to cycle count
	compute_aec_and_count(tuple(call_stack))	

def handle_call(iid, line):
	global call_stack
	# Set PC of top-of-stack
	call_stack[-1] = (call_stack[-1][0], iid)
	# Remember line number
	line_numbers[iid] = line

def handle_begin(method):
	global call_stack
	# Push frame on stack
	call_stack.append((method, 0))


def handle_ret():
	# Pop stack
	call_stack.pop()

def compute_aec_and_count(ec_seq):
	aec_seq = compute_aec(ec_seq)
	aec_counts[aec_seq] += 1

def compute_aec(ec):
	# Store edges in reverse, from a func to its predecessor
	# with the call-site on the edge label and remember the path
	# length from the root.
	redges = {} # FUNC -> FUNC X LOC X INT


	# Start node is the first function in the execution context
	(first_func, pc) = ec[0]


	# Process remaining execution context and add back-edges with shortest-path
	# Note: Start-node should have None func in redges
	last_func = None
	last_pc = None
	for (func, pc) in ec:
		if func not in redges:
			redges[func] = (last_func, last_pc)
		last_func, last_pc = func, pc


	# Trace path backwards to start node in order to get shortest path
	aec_seq = [(last_func, last_pc)]
	while True:
		func, pc = redges[last_func]
		if func is None:
			break
		else:
			aec_seq.append((func, pc))
			last_func, last_pc = func, pc

	# Reverse the sequence to get the correct value
	aec_seq.reverse()

	# Return an immutable sequence
	return tuple(aec_seq)

def str_method_iid(method, iid):
	line_number = line_numbers[iid]
	first_dollar = method.find("$")
	first_hash = method.find("#")
	first_paren = method.find("(")
	assert(first_hash > 0)
	assert(first_paren > 0)
	delimiter = first_hash if first_dollar == -1 or first_dollar > first_hash else first_dollar
	class_part = method[:delimiter]
	class_file_name = class_part + '.java'
	clean_method_name = method[:first_paren].replace('/','.')
	return '  ' + clean_method_name + '(' + class_file_name + ':' + str(line_number) + ')'

def print_aec(aec):
	for (method, iid) in reversed(aec):
		print str_method_iid(method, iid)

def print_top_aec_count():
	print str(len(aec_counts)) + " distinct AECs found."
	if len(aec_counts) == 0:
		return
	best_aec = None
	best_count = 0
	for aec, count in aec_counts.iteritems():
		if count > best_count:
			best_aec = aec
			best_count = count

	print "Count = " + str(best_count)
	print_aec(best_aec)

if __name__ == "__main__":
	main()








