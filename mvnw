#!/bin/sh
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

if [ -z "$MAVEN_WRAPPER_JAR" ]; then
    MAVEN_WRAPPER_JAR="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 && pwd )/.mvn/wrapper/maven-wrapper.jar"
fi

if [ -z "$MAVEN_WRAPPER_PROPERTIES" ]; then
    MAVEN_WRAPPER_PROPERTIES="$( cd -- "$(dirname "$0")" >/dev/null 2>&1 && pwd )/.mvn/wrapper/maven-wrapper.properties"
fi

MAVEN_PROJECTBASEDIR="${MAVEN_BASEDIR:-$(cd -- "$(dirname "$0")" >/dev/null 2>&1 && pwd)}"
export MAVEN_PROJECTBASEDIR

if [ -r "$MAVEN_WRAPPER_PROPERTIES" ]; then
    distributionUrl=$(grep "distributionUrl" "$MAVEN_WRAPPER_PROPERTIES" | cut -d'=' -f2- | tr -d '\r')
fi

MAVEN_HOME="${MAVEN_HOME:-${HOME}/.m2/wrapper/dists/apache-maven-3.9.6}"

if [ ! -d "$MAVEN_HOME" ]; then
    echo "Downloading Maven from $distributionUrl"
    mkdir -p "$MAVEN_HOME"
    tmpFile=$(mktemp /tmp/maven-wrapper.XXXXXX.zip)
    if command -v curl > /dev/null 2>&1; then
        curl -fsSL -o "$tmpFile" "$distributionUrl"
    elif command -v wget > /dev/null 2>&1; then
        wget -q -O "$tmpFile" "$distributionUrl"
    else
        echo "Error: curl or wget is required to download Maven" >&2
        exit 1
    fi
    unzip -q -o "$tmpFile" -d "${HOME}/.m2/wrapper/dists/" && rm -f "$tmpFile"
    MAVEN_HOME=$(find "${HOME}/.m2/wrapper/dists/" -maxdepth 1 -name "apache-maven-3.9*" -type d | head -1)
fi

exec "$MAVEN_HOME/bin/mvn" "$@"
