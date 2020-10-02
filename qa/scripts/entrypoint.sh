#!/bin/bash -e

cat >&2 <<EOF
##############################################
### LOGSTASH PLUGIN METRIC TESTING HARNESS ###
##############################################

Input is expected to come from a file within
the container, which may be provided via a folder mapping.
Format of the input is one IP per line, with line
endings in LF format.

Metrics will be output every 100,000 rows, and
the current record will be output every 20,000
rows as a spot check.

EOF

trap 'exit 0' SIGTERM

export LS_JAVA_OPTS="-Dls.cgroup.cpuacct.path.override=/ -Dls.cgroup.cpu.path.override=/ $LS_JAVA_OPTS"

dataset="${1:-/qa/inputs/demo-found.sh}"
database="${2:-/qa/samples/demo.mmdb}"

>&2 echo "Running dataset \"$dataset\" with MMDB database \"$database\""

function emit_dataset() {
    case "$1" in
        *.log|*.txt)
            cat -- "$1"
            ;;
        *.sh)
            bash "$1"
            ;;
    esac
}

# For the Logstash pipeline config
export MMDB_DATABASE="$database"

emit_dataset "$dataset" \
    | logstash \
    | python3 /qa/scripts/collect-then-die.py

>&2 echo "Done"

