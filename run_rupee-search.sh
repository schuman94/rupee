#!/bin/bash

# Cambiar al directorio del script
cd "$(dirname "$0")"

# Crear el directorio de salida si no existe
mkdir -p output

# Cambiar al directorio de rupee-search/target/
cd ./rupee-search/target/

# Iterar sobre todos los archivos .pdb en la carpeta "../../pdb/"
for pdb_file in ../../upload_dir/upload/*.pdb; do
    # Obtener el nombre del archivo sin la extensión .pdb
    pdb=$(basename "$pdb_file" .pdb)

    # Ejecutar el algoritmo de búsqueda con los parámetros proporcionados y guardar el resultado en la carpeta output
    java -jar -Dlog4j.configurationFile=log4j2.xml rupee-search-0.0.1-SNAPSHOT-jar-with-dependencies.jar -s DIR,"$pdb",FALSE,FALSE,FALSE,FALSE,FALSE,FALSE,"$1","$2" > "../../output/${pdb}-output.txt"
done
