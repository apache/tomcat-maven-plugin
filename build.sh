# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

export MAVEN_OPTS="-javaagent:$HOME/.m2/repository/org/jacoco/org.jacoco.agent/0.8.14/org.jacoco.agent-0.8.14-runtime.jar=destfile=/tmp/jacoco/jacoco-it.exec --add-opens=java.base/java.util.zip=ALL-UNNAMED --add-opens=java.base/java.util.jar=ALL-UNNAMED --add-opens=java.base/sun.net.www.protocol.http=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED"

# Run the main build lifecycle + integration tests
mvn clean install failsafe:integration-test invoker:integration-test

# Generate JaCoCo report from integration test coverage data
mvn jacoco:report -Djacoco.dataFile=/tmp/jacoco/jacoco-it.exec
