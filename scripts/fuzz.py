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
import os
import pickle
import random
import string
import subprocess

import travioli
import redundancy_analysis
import diff_cycles

# Directory containing this script and the tracing script
SCRIPT_DIR = os.path.dirname(os.path.realpath(__file__))

# Global constants
DECAY_RATE = 0.05
BOOST_RATE = 0.75
IMRPOVEMENT_THRESHOLD = 0.02

class FuzzSession(object):
	def __init__(self, runner):
		self.runner = runner
		self.max_redundancies = defaultdict(float) # Map of AEC to max redundancy score found so far

	def run_and_analyze(self, input):
		trace_files = self.runner(input)
		analysis = redundancy_analysis.DynamicAnalysis()
		for trace_file in trace_files:
			analysis.process_trace(trace_file)
		return analysis

	def run(self, initial_seeds, max_len):

		self.seeds = {seed: 1.0 for seed in initial_seeds}
		self.max_len = max_len
		self.max_redundancy = 0.0
		for seed in initial_seeds:
			assert(len(seed) <= max_len)
			_, max_redundancy = self.run_input(seed)
			#print "Seed = " + str(seed) + " MaxRedundancy = " + str(max_redundancy)
			if max_redundancy > self.max_redundancy:
				self.max_redundancy = max_redundancy

		while True: 
			seed = self.pick_seed()
			mutants = self.mutate(seed, 4)
			for mutant in mutants:
				improvement, max_redundancy = self.run_input(mutant)				
				print ("Input = %s Improvement = %.5f" % (mutant, improvement))
				if improvement > IMRPOVEMENT_THRESHOLD:
					print "  ...adding to seeds!"
					self.add_seed(mutant)
					self.boost_seed(seed)
				if max_redundancy > self.max_redundancy:
					self.max_redundancy = max_redundancy
					print "  ...best input found so far! (MaxRedundancy = " + str(max_redundancy) + ")"



	def run_input(self, input):
		analysis = self.run_and_analyze(input)
		aec_redundancies = analysis.compute_redundancies()
		max_redundancy = max([red for red, counts in aec_redundancies.itervalues()])
		improvement = self.update_max_redundancies(aec_redundancies)
		return improvement, max_redundancy

	def pick_seed(self):
		total_energy = sum(self.seeds.values())
		r = random.uniform(0, total_energy)
		for seed, energy in self.seeds.iteritems():
			if r <= energy:
				print ("Picked seed: %s (chance=%.2f%%)" % (seed, energy/total_energy))
				return seed
			else:
				r = r - energy
		raise "Unreachable code"

	def add_seed(self, input):
		assert (input not in self.seeds)

		# Decay old seeds
		for seed in self.seeds:
			self.seeds[seed] = self.seeds[seed] * (1 - DECAY_RATE)

		# Add new seed
		self.seeds[input] = 1.0

	def boost_seed(self, seed):
		self.seeds[seed] = self.seeds[seed] * (1 + BOOST_RATE)


	def update_max_redundancies(self, aec_redundancies):
		best_improvement = 0.0
		for aec, (red, counts) in aec_redundancies.iteritems():
			current_max = self.max_redundancies[aec]
			if red > current_max:
				improvement = red - current_max
				self.max_redundancies[aec] = red
				if improvement > best_improvement:
					best_improvement = improvement
		return best_improvement 


	def mutate(self, seed, num_mutations):
		charset = string.lowercase + string.uppercase + string.digits + ' !@#$%^&*()-_=+[]{};\':",.<>/?`~'
		mutants = []
		for i in range(num_mutations):
			pos = random.randrange(0, len(seed))
			x = random.choice(charset)
			mutant = seed[:pos] + x + seed[pos+1:]
			mutants.append(mutant)
		print "Mutations: " + str(seed) + " --> " + str(mutants)
		return mutants

def main():
	main_class = "main.Main"
	def dummy_runner(input):
		driver = os.path.join(SCRIPT_DIR, "datatraces.sh")
		with open("/dev/null", 'w') as null:
			ret = subprocess.call([driver, main_class, input], stdout=null, stderr=subprocess.STDOUT, shell=False)
		return ["main.log"]

	session = FuzzSession(dummy_runner)
	session.run(["ABC", "ABCD", "abCD", "AAAAbcd"], 10)




if __name__ == "__main__":
	main()
