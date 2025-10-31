/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
def buildLog = new File(basedir, 'build.log'); 
assert buildLog.exists()

def LS = System.getProperty("line.separator")
if (mavenVersion.startsWith('4.')) {
    // we have defined properties maven.compiler.source/target to 8 as user properties for execution
    // Maven 4 take precedence over the pom properties
    // also order of properties is changed in Maven 4
    assert buildLog.text.find(
            '(?s)' +
                    '  <properties>' + LS +
                    '    <maven\\.compiler\\.target>8</maven\\.compiler\\.target>' + LS +
                    '    <maven\\.compiler\\.source>8</maven\\.compiler\\.source>' + LS +
                    '.*' +
                    '  </properties>') != null

} else {
    assert buildLog.text.find(
            '(?s)' +
                    '  <properties>' + LS +
                    '    <maven\\.compiler\\.source>1\\.6</maven\\.compiler\\.source>' + LS +
                    '    <maven\\.compiler\\.target>1\\.6</maven\\.compiler\\.target>' + LS +
                    '.*' +
                    '  </properties>') != null
}
