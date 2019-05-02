#!/bin/bash
#$1: bench
#$2: synct
#$3: sem
#$4: runs

echo "--------------$1 results ---------------"
for tech in afl rnd seq jqf; do
  echo $tech
  for i in $(seq $4); do
    if [ ! -f $1-$tech-results-$i/cov-all.log ]; then
      echo ""
    else
      echo `egrep $2 $1-$tech-results-$i/cov-all.log | wc -l` '	' `egrep $3 $1-$tech-results-$i/cov-all.log | wc -l || echo`
    fi
  done
  echo ""
  echo ""
done
