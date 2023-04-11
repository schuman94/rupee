#!/bin/bash

cd "$(dirname "$0")/target"

output_directory="../../output"
mkdir -p "$output_directory"

java -jar -Dlog4j.configurationFile=log4j2.xml rupee-search-0.0.1-SNAPSHOT-jar-with-dependencies.jar -s DIR,$1,FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,$2,$3 > "${output_directory}/${1}-output.txt"
