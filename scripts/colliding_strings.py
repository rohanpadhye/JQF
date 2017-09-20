#!/usr/bin/env pypy
import sys

def main():
	if (len(sys.argv) < 2):
		print "Usage: " + sys.argv[0] + " LENGTH"
		return
	len_str = int(sys.argv[1])
	strings = generate_colliding_strings(len_str)
	for string in strings:
		print string,

def generate_colliding_strings(len_str):
	pairs = ['An', 'BO', 'C0'];

	result = []

	def gen(prefix, k):
		if k == 0:
			result.append(prefix)
		else:
			for pair in pairs:
				gen(prefix + pair, k - 1)

	gen('', len_str)
	return result



if __name__ == '__main__':
	main()