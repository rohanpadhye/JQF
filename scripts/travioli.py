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

def str_method_line(method, line_number):
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
		line_number = line_map[iid]
		print str_method_line(method, line_number)
