#!/bin/bash
#
# Produce an simulated workload of 1 million IP addresses that are variously NOT
# found in the dataset.
#
# We do this my taking lines of (input,count) pairs, making 'count' repeats of
# 'input', then shuffling all those lines. We then repeat the process
# ad-infinitum.

trap 'exit 0' SIGTERM

while true
do
    awk '{ for (i=0; i<$2; i++) { print $1 } }' \
        /qa/inputs/demo-found.in | shuf
done </dev/null | head -n 1000000
