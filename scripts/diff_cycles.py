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
import pickle

import count_cycles


def main():	
	# Command-line arguments
	parser = argparse.ArgumentParser(description='Compute difference between two cycle count maps')
	parser.add_argument('file_name_1', type=str, help='First pickle file containing cycle counts')
	parser.add_argument('file_name_2', type=str, help='Second pickle file containing cycle counts')
	args = parser.parse_args()

	# Load cycle counts
	aec_count_map_1, line_map_1 = load_data(args.file_name_1)
	aec_count_map_2, line_map_2 = load_data(args.file_name_2)

	# Compute diff and print
	aec_count_diff = diff_map(aec_count_map_1, aec_count_map_2)
	line_map = union_map(line_map_1, line_map_2)
	print_diff(aec_count_diff, line_map)


def load_data(file_name):
	with open(file_name, 'rb') as file:
		return pickle.load(file)

def diff_map(map_1, map_2):
	keys = set()
	keys.update(map_1.keys())
	keys.update(map_2.keys())
	result = dict()

	for k in keys:
		count_1 = map_1[k] 
		count_2 = map_2[k]
		result[k] = count_2 - count_1

	return result

def union_map(map_1, map_2):
	keys = set()
	keys.update(map_1.keys())
	keys.update(map_2.keys())
	result = dict()

	for k in keys:
		if k in map_1 and k in map_2:
			assert map_1[k] == map_2[k]
		result[k] = map_2[k] if k in map_2 else map_1[k]

	return result


def print_diff(diff_map, line_map):
	for aec, count in sorted(diff_map.items(), key=operator.itemgetter(1)):
		print "Diff = " + str(count)
		count_cycles.print_aec(aec, line_map)

if __name__ == "__main__":
	main()
