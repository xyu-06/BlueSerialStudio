#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if command -v javac >/dev/null 2>&1; then
  compiler=(javac)
else
  compiler=(java com.sun.tools.javac.Main)
fi
python3 qa/check_source.py
mkdir -p build/protocol-tests build/mixer-tests build/transport-tests
sources=app/src/main/java/com/embedded/blueserial
"${compiler[@]}" -d build/protocol-tests "$sources"/{SerialProtocol,DriveControlMath,AtCommandBuilder,OutboundQueue,LineFramer,StreamDecoder}.java tests/{ProtocolSelfTest,SerialWorkflowSelfTest}.java
java -cp build/protocol-tests ProtocolSelfTest
java -cp build/protocol-tests SerialWorkflowSelfTest
"${compiler[@]}" -d build/transport-tests tests/android-stubs/android/{annotation,bluetooth,os}/*.java "$sources"/{BluetoothSppClient,OutboundQueue}.java tests/BluetoothClientSelfTest.java
java -cp build/transport-tests BluetoothClientSelfTest
cc -std=c99 -Wall -Wextra tests/drive_mixer_test.c -o build/mixer-tests/test
./build/mixer-tests/test
