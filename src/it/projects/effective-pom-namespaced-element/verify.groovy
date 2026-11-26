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

def result = new File(basedir, 'effective-pom.xml')
assert result.exists()

def text = result.text
def LS = System.lineSeparator()
// the XML declaration comes first, so the output was pretty-printed
assert text.startsWith('<?xml version="1.0" encoding="UTF-8"?>')
assert text.count('<?xml') == 1
assert text.contains('<ac:if xmlns:ac="antlib:net.sf.antcontrib">' + LS)
assert text.contains('<ac:then>' + LS)
assert text.contains('</ac:if>' + LS)
assert !text.contains('__MAVEN_HELP_PLUGIN_COLON__')
