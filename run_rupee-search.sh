#!/bin/bash

cd "$(dirname "$0")"

mkdir -p output

cd ./rupee-search/target/

for pdb_file in "$3"/*.pdb; do
    # filename without pdb extension
    pdb_id=$(basename "$pdb_file" .pdb)

    java -jar -Dlog4j.configurationFile=log4j2.xml rupee-search-0.0.1-SNAPSHOT-jar-with-dependencies.jar -u DIR,"$pdb_file",FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,"$1","$2" > "$4/${pdb_id}-output.txt"
done

for pdb in ../../data/upload/*.pdb; do
    rm "$pdb"
done
