#!/bin/bash

cd `dirname $0`


cd java
javac CMA_Analyze_map24.java
cd ..

cd ..
tar xf cyc.tar.gz
cd exe
