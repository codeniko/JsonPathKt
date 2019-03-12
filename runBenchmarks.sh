#!/bin/bash

FILE=/tmp/jsonpathlite
FILE2=/tmp/jsonpathlite2
./gradlew benchmark -DreadmeFormat > "$FILE"
if [ $? -eq 0 ]; then
  cat "$FILE" | sed 's/^ *//' | grep '^|' > "$FILE2"

  # print path times
  cat "$FILE2" | grep '^|  \$'
  echo ''
  # print compile times
  cat "$FILE2" | grep -v '^|  \$'
fi
