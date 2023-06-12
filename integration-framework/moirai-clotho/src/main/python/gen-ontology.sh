#!/bin/bash

# Requirements
#     xlrd package

usage()
{
    echo "usage: ./gen-ontology.sh [ontology-file.xlsx]"
    echo ""
    echo -e "Be sure the Excel file has no smart quotes or other extended ASCII characters."
    echo -e ""
}

xlsx=$1
if [[ -z $xlsx ]]
then
	usage; exit 1
fi

echo -e "Converting event primitives..."
python EventPrimitivesXlsxToJsonConverter.py $xlsx ../resources/ontology/event-primitives.json
echo -e "Converting entity primitives..."
python EntityPrimitivesXlsxToJsonConverter.py $xlsx ../resources/ontology/entities.json
echo -e "Converting relation primitives..."
python RelationPrimitivesXlsxToJsonConverter.py $xlsx ../resources/ontology/relations.json
echo -e "Done."
echo -e "Don't forget to copy ontology files to kairos-pub/data-format/ontology"
