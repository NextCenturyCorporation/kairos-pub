#! /bin/bash

pem=~/.ssh/moirai-machine.pem
remote () {
    ssh -i $pem ubuntu@devbox.kairos.nextcentury.com $1 &
}

copy () {
    for file in $@
    do
        scp -i $pem $file ubuntu@devbox.kairos.nextcentury.com:/home/ubuntu/moirai-clotho/$file
    done
}
remote "cd moirai-clotho && && git clean -f && git checkout src"
copy $(git diff --name-only)
copy $(git ls-files --others --exclude-standard)
remote "cd moirai-clotho && ./gradlew -Dskip.tests && docker kill compose_clotho_1 && docker-compose -f compose/neptune.yml up"