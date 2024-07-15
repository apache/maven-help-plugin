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

def buildLog = new File( basedir, 'build.log' ); 
assert buildLog.exists()

assert 2 == buildLog.text.count( ' - mph79 (source: org.apache.maven.its.help:active-profiles_multimodule:1.0-SNAPSHOT)' )
assert 1 == buildLog.text.count( ' - mph79-child (source: org.apache.maven.its.help:active-profiles_multimodule-module:1.0-SNAPSHOT)' )
assert 0 == buildLog.text.count( ' - mph79-child-inactive' )
assert 2 == buildLog.text.count( ' - it-repo (source: external)' )
