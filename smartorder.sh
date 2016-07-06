#!/bin/sh.exe
rm ./target/universal/stage/RUNNING_PID
activator clean stage
cp -r rose ./target/universal/stage/
./target/universal/stage/bin/smartorder -Dplay.crypto.secret=c1cc1o
