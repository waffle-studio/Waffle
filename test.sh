#!/bin/sh

touch ok1.txt
sleep 10
touch ok2.txt
echo OK
echo '{"value":123}' > _output.json

exit 0
