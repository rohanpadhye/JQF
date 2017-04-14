#!/usr/bin/env pypy
"""
 Copyright (c) 2017, University of California, Berkeley

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
import operator
import re
import pickle

REGEXP_BRANCH = re.compile("^\s*BRANCH\((-?\d+),(\d+)\)$")
REGEXP_CALL   = re.compile("^\s*CALL\((\d+),(\d+)\)$")
REGEXP_RET    = re.compile("^\s*RET$")
REGEXP_BEGIN    = re.compile("^\s*BEGIN (.*)$")
REGEXP_HEAPLOAD = re.compile("^\s*HEAPLOAD\((-?\d+),(\d+),(\d+),(.*)\)$")

def main():	
	# Command-line arguments
	parser = argparse.ArgumentParser(description='Collect AECs from a trace and count them')
	parser.add_argument('--input', type=str, dest='trace_file', default='main.log', 
		help='Name of trace file containing event log')
	parser.add_argument('--serialize', type=str, dest='serialize', default=None,
		help='Optional name of pickle file to serialize cycle counts')


	# Parse arguments
	args = parser.parse_args()

	# Process trace file
	process_trace(args.trace_file)

	# Print AEC counts
	# print_aec_counts(aec_counts, line_numbers)

	print "\n\n"

	# Compute AEC redundancies
	aec_redundancies = compute_redundancies(aec_mems)

	# Print AEC redundancies
	print_aec_redundancies(aec_redundancies, line_numbers)

	# Serialize AEC counts
	if args.serialize:
		serialize_data(args.serialize, aec_redundancies)


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

			# Try to match HEAPLOAD(iid, line, objectId, field)
			match_heapload = REGEXP_HEAPLOAD.match(line)
			if match_heapload:
				handle_heapload(int(match_heapload.group(1)), int(match_heapload.group(2)),
					int(match_heapload.group(3)), match_heapload.group(4))
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
aec_counts = defaultdict(int)   # SEQ -> INT // Maps an AEC to counts, 
                                             # where SEQ is an AEC
aec_mems = defaultdict(lambda: defaultdict(int)) # SEQ -> MEM -> INT // Maps an AEC to a 
                                                                     # map of memory location counts, where SEQ is an 
                                                                     # AEC and MEM is INT x STRING

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

def handle_heapload(iid, line, objectId, field):
	global call_stack
	# Set PC of top-of-stack
	call_stack[-1] = (call_stack[-1][0], iid)
	# Remember line number
	line_numbers[iid] = line
	# Compute AEC and collect info for redundancy metrics
	compute_aec_and_collect(tuple(call_stack), (objectId, field))	

def compute_aec_and_count(ec_seq):
	aec_seq = compute_aec(ec_seq)
	aec_counts[aec_seq] += 1

def compute_aec_and_collect(ec_seq, mem):
	aec_seq = compute_aec(ec_seq)
	aec_mems[aec_seq][mem] += 1

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

def str_method_iid(method, iid, line_map):
	line_number = line_map[iid]
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

def print_aec(aec, line_map):
	for (method, iid) in reversed(aec):
		print str_method_iid(method, iid, line_map)

def print_aec_counts(aec_counts, line_map):
	for aec, count in sorted(aec_counts.items(), key=operator.itemgetter(1)):
		if count == 1:
			continue
		print "Count = " + str(count)
		print_aec(aec, line_map)

	print str(len(aec_counts)) + " distinct AECs found."

def serialize_data(pickle_file_name, data):
	with open(pickle_file_name, 'wb') as pickle_file:
		pickle.dump((data, line_numbers), pickle_file)

def compute_redundancy(counts):
	sum_counts = float(sum(counts))	
	if sum_counts < 2:
		return 0.0
	uniq_counts = float(len(counts))
	avg_counts = sum_counts/uniq_counts
	score = (avg_counts - 1)*(uniq_counts - 1)/sum_counts
	return score

def compute_redundancies(aec_mems):
	aec_redundancies = defaultdict(float)
	for aec, mem_counts in aec_mems.iteritems():
		sorted_counts = sorted(mem_counts.values())
		aec_redundancies[aec] = compute_redundancy(sorted_counts), sorted_counts
	return aec_redundancies

def print_aec_redundancies(aec_redundancies, line_map):
	for aec, (red, counts) in sorted(aec_redundancies.items(), key=lambda x: x[1][0]):
		if red < 0.0001:
			continue # Ignore non-redundant
		print "Redundancy = " + str(red) + " " + str(counts)
		print_aec(aec, line_map)

if __name__ == "__main__":
	main()








