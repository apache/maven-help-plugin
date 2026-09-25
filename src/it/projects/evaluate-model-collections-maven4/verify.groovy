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

def result = new groovy.xml.XmlSlurper().parse(new File(basedir, 'resources.xml'))

assert result.name() == 'resources'
assert result.children().size() == 1

// Maven 4 wraps the resource fields in an immutable model delegate.
def resource = result.children()[0].delegate
assert resource.directory*.text() == [new File(basedir, 'src/main/resources').absolutePath]
assert resource.targetPath.text() == 'config'
assert resource.filtering.text() == 'true'
assert resource.includes.children()*.text() == ['**/*.properties', '**/*.xml']
assert resource.excludes.children()*.text() == ['**/private.properties']

// The map converter must retain the source locations, not just avoid the reflection failure.
assert resource.locations.entry.string*.text().containsAll(
        ['directory', 'targetPath', 'filtering', 'includes', 'excludes'])
