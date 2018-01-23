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
import travioli

# Global constants
REGEXP_BRANCH = re.compile("^\s*BRANCH\((\d+),(-?\d+),(\d+)\)$")
REGEXP_CALL   = re.compile("^\s*CALL\((\d+),(\d+),(.*)\)$")
REGEXP_ALLOC   = re.compile("^\s*ALLOC\((\d+),(\d+),(\d+)\)$")
REGEXP_RET    = re.compile("^\s*RET")
REGEXP_HEAPLOAD = re.compile("^\s*HEAPLOAD\((-?\d+),(\d+),(\d+),(.*)\)$")
REGEXP_END = re.compile("^# End (.*)$")

def main():	
	# Command-line arguments
	parser = argparse.ArgumentParser(description='Determine inputs that maximize individual branches')
	parser.add_argument('--input', type=str, dest='trace_file', default='main.log', 
		help='Name of trace file containing event log')


	# Parse arguments
	args = parser.parse_args()

	# Create new analysis object
	analysis = TraceAnalysis()

	# Process trace file
	analysis.process_trace(args.trace_file)

	# Print inputs and maximizing branches
	maximizing_inputs = analysis.get_maximizing_inputs()
	sorted_counts = sorted(maximizing_inputs.items(), key=lambda p: p[1][1], reverse=True)
	for (iid, arm), (input, count) in sorted_counts:
		print count, analysis.src_map[iid] + '.' + str(arm), input


class TraceAnalysis(object):

	def __init__(self):
		self.src_map = {}       # INT -> STR     // Maps IIDs to source locations

	def process_trace(self, trace_file_name):
		self.call_stack = []         # [(STR, INT)]   // Call stack of (Method, IID)
		self.branch_counts = defaultdict(int) # INT x INT -> INT // Map of (IID, Arm) to counts for this input
		self.inputs_to_branch_counts = {}     # STR -> ((INT x INT) -> INT) // Map of inputs to branch counts
		with open(trace_file_name) as trace_file:
			while True:
				# Read line from file
				line = trace_file.readline()
				# End-of-file is empty line
				if not line:
					break

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

				# Try to match CALL(iid, line, method)
				match_call = REGEXP_CALL.match(line)
				if match_call:
					self.handle_call(int(match_call.group(1)), int(match_call.group(2)), match_call.group(3))
					continue

				# Try to match RET
				match_ret = REGEXP_RET.match(line)
				if match_ret:
					self.handle_ret()
					continue
				

				# Try to match ALLOC(iid, line, size)
				match_alloc = REGEXP_ALLOC.match(line)
				if match_alloc:
					self.handle_alloc(int(match_alloc.group(1)), int(match_alloc.group(2)), int(match_alloc.group(3)))
					continue


				# Try to match end-of-input
				match_end = REGEXP_END.match(line)
				if match_end:
					self.handle_end(match_end.group(1))
					continue



				# Otherwise, error
				raise Exception("Cannot parse trace line: " + line)


	def handle_branch(self, iid, arm, line):
		# Set PC of top-of-stack
		self.call_stack[-1] = (self.call_stack[-1][0], iid)
		# Map source location if not exists
		if iid not in self.src_map:
			method = self.call_stack[-1][0]
			self.src_map[iid] = travioli.str_method_line(method, line)

		# Increment branch count
		self.branch_counts[(iid, arm)] += 1

	def handle_call(self, iid, line, method):
		# Set PC of top-of-stack
		if len(self.call_stack) > 0:
			self.call_stack[-1] = (self.call_stack[-1][0], iid)
		# Push frame on stack
		self.call_stack.append((method, 0))


	def handle_ret(self):
		# Pop stack
		self.call_stack.pop()

	def handle_heapload(self, iid, line, objectId, field):
		# Set PC of top-of-stack
		self.call_stack[-1] = (self.call_stack[-1][0], iid)
		# Do not do anything else for now

	def handle_alloc(self, iid, line, size):
		# Set PC of top-of-stack
		self.call_stack[-1] = (self.call_stack[-1][0], iid)
		# Do not do anything else for now

	def handle_end(self, input):
		# Save current counts to map
		self.inputs_to_branch_counts[input] = self.branch_counts
		# Reset branch counts
		self.branch_counts = defaultdict(int)

	def get_maximizing_inputs(self):
		max_branch_counts = defaultdict(int)
		maximizing_inputs = {}
		for input, branch_counts in self.inputs_to_branch_counts.iteritems():
			for branch, count in branch_counts.iteritems():
				if count > max_branch_counts[branch]:
					max_branch_counts[branch] = count
					maximizing_inputs[branch] = (input, count)

		return maximizing_inputs



if __name__ == "__main__":
	main()








