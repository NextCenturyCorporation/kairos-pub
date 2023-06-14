#! /bin/bash

filename=$1
thekey=$2
newvalue=$3

echo "Filename - $filename: Key - $thekey, Value - $newvalue"
echo
if ! grep -R "^[#]*\s*${thekey}=.*" $filename > /dev/null; then
        echo "APPENDING because '${thekey}' not found"
        echo "$thekey=$newvalue" >> $filename
else
        echo "SETTING because '${thekey}' found already"
        sed -ir "s/^[#]*\s*${thekey}=.*/$thekey=$newvalue/" $filename
fi
