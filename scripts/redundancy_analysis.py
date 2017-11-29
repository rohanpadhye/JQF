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

import travioli

# Global constants
REGEXP_BRANCH = re.compile("^\s*BRANCH\((\d+),(-?\d+),(\d+)\)$")
REGEXP_CALL   = re.compile("^\s*CALL\((\d+),(\d+),(.*)\)$")
REGEXP_RET    = re.compile("^\s*RET")
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

	# Create new analysis object
	analysis = DynamicAnalysis()

	# Process trace file
	analysis.process_trace(args.trace_file)

	# Print AEC counts
	# print_aec_counts(aec_counts, line_numbers)

	# print "\n\n"

	# Compute AEC redundancies
	aec_redundancies = analysis.compute_redundancies()

	# Print AEC redundancies
	print_aec_redundancies(aec_redundancies, analysis.line_numbers)

	# Serialize AEC counts
	if args.serialize:
		serialize_data(args.serialize, aec_redundancies, analysis.line_numbers)


class DynamicAnalysis(object):

	def __init__(self):
		self.line_numbers = {}       # INT -> INT     // Maps IIDs to line numbers
		self.aec_counts = defaultdict(int)   # SEQ -> INT // Maps an AEC to counts, 
		                                             # where SEQ is an AEC
		self.aec_mems = defaultdict(lambda: defaultdict(int)) # SEQ -> MEM -> INT // Maps an AEC to a 
		                                                                     # map of memory location counts, where SEQ is an 
		                                                                     # AEC and MEM is INT x STRING

	def process_trace(self, trace_file_name):
		self.call_stack = []         # [(STR, INT)]   // Call stack of (Method, IID)
		with open(trace_file_name) as trace_file:
			while True:
				# Read line from file
				line = trace_file.readline()
				# End-of-file is empty line
				if not line:
					break
        # Ignore comments
				if line.startswith("#"):
					continue

				# Try to match BRANCH(iid, arm, line)
				match_branch = REGEXP_BRANCH.match(line)
				if match_branch:
					self.handle_branch(int(match_branch.group(1)), int(match_branch.group(2)), int(match_branch.group(3)))
					continue

				# Try to match HEAPLOAD(iid, line, objectId, field)
				match_heapload = REGEXP_HEAPLOAD.match(line)
				if match_heapload:
					self.handle_heapload(int(match_heapload.group(1)), int(match_heapload.group(2)),
						int(match_heapload.group(3)), match_heapload.group(4))
					continue

				# Try to match CALL(iid, line)
				match_call = REGEXP_CALL.match(line)
				if match_call:
					self.handle_call(int(match_call.group(1)), int(match_call.group(2)), match_call.group(3))
					continue

				# Try to match RET
				match_ret = REGEXP_RET.match(line)
				if match_ret:
					self.handle_ret()
					continue
				
				# Otherwise, error
				raise Exception("Cannot parse trace line: " + line)


	def handle_branch(self, iid, arm, line): # XXX: ARM ID is ignored ???
		# Set PC of top-of-stack
		self.call_stack[-1] = (self.call_stack[-1][0], iid)
		# Remember line number
		self.line_numbers[iid] = line

	def handle_call(self, iid, line, method):
		# Set PC of top-of-stack
		if len(self.call_stack) > 0:
			self.call_stack[-1] = (self.call_stack[-1][0], iid)
		# Remember line number
		self.line_numbers[iid] = line
		# Push frame on stack
		self.call_stack.append((method, 0))


	def handle_ret(self):
		# Pop stack
		self.call_stack.pop()

	def handle_heapload(self, iid, line, objectId, field):
		# Set PC of top-of-stack
		self.call_stack[-1] = (self.call_stack[-1][0], iid)
		# Remember line number
		self.line_numbers[iid] = line
		# Compute AEC and collect info for redundancy metrics
		self.compute_aec_and_collect(tuple(self.call_stack), (objectId, field))	

	def compute_aec_and_collect(self, ec_seq, mem):
		aec_seq = travioli.compute_aec(ec_seq)
		self.aec_mems[aec_seq][mem] += 1


	def compute_redundancies(self):
		aec_redundancies = defaultdict(lambda: (0.0, []))
		for aec, mem_counts in self.aec_mems.iteritems():
			sorted_counts = sorted(mem_counts.values())
			aec_redundancies[aec] = compute_redundancy_score(sorted_counts), sorted_counts
		return aec_redundancies


def compute_redundancy_score(counts):
	sum_counts = float(sum(counts))	
	if sum_counts < 2:
		return 0.0
	uniq_counts = float(len(counts))
	avg_counts = sum_counts/uniq_counts
	score = (avg_counts - 1)*(uniq_counts - 1)/sum_counts
	return score

def discretize_score(score):
	return int(255*(2**score-1))


def print_aec_counts(aec_counts, line_map):
	for aec, count in sorted(aec_counts.items(), key=operator.itemgetter(1)):
		if count == 1:
			continue
		print "Count = " + str(count)
		travioli.print_aec(aec, line_map)

	print str(len(aec_counts)) + " distinct AECs found."

def print_aec_redundancies(aec_redundancies, line_map):
	for aec, (red, counts) in sorted(aec_redundancies.items(), key=lambda x: x[1][0]):
		if red < 0.0001:
			continue # Ignore non-redundant
		print "Redundancy = " + str(red) + " " + str(counts)
		travioli.print_aec(aec, line_map)


def serialize_data(pickle_file_name, data, line_numbers):
	with open(pickle_file_name, 'wb') as pickle_file:
		pickle.dump((data, line_numbers), pickle_file)

if __name__ == "__main__":
	main()








