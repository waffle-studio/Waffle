#!/bin/bash

sleep 5
echo "{\"a1\":\"$1\",\"a2\":\"$2\",\"a3\":\"`pwd`\"}" > _output.json

