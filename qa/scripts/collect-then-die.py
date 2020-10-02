#!/usr/bin/env python3

import json
import requests
import fileinput
import colorama

def stats_for_plugin_id(doc, id, pipeline='main', type_='filters'):
    for stat in doc['pipelines'][pipeline]['plugins'][type_]:
        if stat['id'] == id:
            return stat
    return None

colorama.init()

this_stats = None
prev_stats = None
nr = 0

for line in fileinput.input():
    nr += 1

    if nr % 20_000 == 0:
        # For a spot check, emit this record also
        print(colorama.Style.DIM + line.strip() + colorama.Style.NORMAL)

    if nr % 100_000 == 0:
        # Time to collect statistics

        resp = requests.get("http://127.0.0.1:9600/_node/stats/pipelines?pretty")
        doc = resp.json()
        this_stats = stats_for_plugin_id(doc, 'mmdb0')
        
        # {'id': 'mmdb0', 'events': {'duration_in_millis': 556, 'in': 100625, 'out': 100625}, 'name': 'mmdb'}

        if prev_stats is not None:
            in_diff = this_stats['events']['in'] - prev_stats['events']['in']
            duration_diff = this_stats['events']['duration_in_millis'] - prev_stats['events']['duration_in_millis']

            usec_per_event = 1000 * float(duration_diff) / in_diff

            print(f"Processed {in_diff} events in {duration_diff}ms: {usec_per_event:.1f}μs / event")

        prev_stats = this_stats

    if nr > 1_000_000:
        break

# Do we still need to kill -TERM -1 ?

colorama.deinit()
