#!/bin/bash

cd "$(dirname "$0")"

cd "./rupee-tm"
mvn clean package install

cd "../rupee-core"
mvn clean package install

cd "../rupee-search"
mvn clean package install

cd "./target"
java -jar rupee-search-0.0.1-SNAPSHOT-jar-with-dependencies.jar -i DIR
java -jar rupee-search-0.0.1-SNAPSHOT-jar-with-dependencies.jar -h DIR
