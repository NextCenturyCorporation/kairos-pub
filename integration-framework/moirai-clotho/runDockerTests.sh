#!/bin/bash
usage="$(basename "$0") [-h] [i] -- Tests for the Docker image.

where:
    -h:  shows this help text
     i:  the Docker image name and tag (e.g. sample_image:1.0)"

# Parse options to the `pip` command
while getopts ":h" opt; do
  case ${opt} in
    h|help )
      echo "$usage"
      exit 0
      ;;
   \? )
     echo "Invalid Option: -$OPTARG" 1>&2
     echo "$usage" >&2
     exit 1
     ;;
  esac
done
shift $((OPTIND -1))

# Checks for
if [ $# -eq 0 ]; then
    echo "$usage"
    exit 0
fi

#OSX's built in date doesn use %N so OSX should brew install coreutils and use gdate instead
if [[ "$OSTYPE" == "darwin18" ]]; then
  date="gdate"
else
  date="date"
fi

# Tracking runtime
start=`$date +%s%N`
# -e if script line fails it fails entire script
# -o will make if any command in the pipeline fails it will make the script fail instantly
set -eo pipefail
# When debugging `DEBUG=true ./runDockerTests.sh` display the actual commands being executed with all variables applied
[ "$DEBUG" ] && set -x
# Enables the script to run from anywhere by setting the current working directory to the directory of the script
cd "$(dirname "$0")"
# "moirai-clotho:1.0"
dockerImage=$1
# Check to see if the image exists
if ! docker inspect "$dockerImage" &> /dev/null; then
    echo $'\timage does not exist!'
    false
fi
# Check if the socket is available
# Create an instance of the container-under-test
cid="$(docker run -d "$dockerImage")"

# Remove container afterwards
trap "docker rm -vf $cid > /dev/null" EXIT

docker exec -i $cid [ -S $socketFile ] || (echo "Socket $socketFile is not found" && exit 1)

end=`$date +%s%N`

runtime=$((end-start))
# -9 is used to mask nanoseconds. Output is to milliseconds
# OSX's bash shell doesn't understand "-9" and needs "${#runtime}-9}" which should work for all bash shells.
echo "All test passed in ${runtime:0:${#runtime}-9}.${runtime:(${#runtime}-9):${#runtime}-6}s"
