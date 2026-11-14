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

assert !new File(basedir, 'build.log').readLines().any { it.startsWith('[ERROR]') }

def parse = { name -> new groovy.xml.XmlSlurper().parse(new File(basedir, name)) }
def checkResource = { resource ->
    assert resource.name() == 'resource'
    assert resource.directory.text() == new File(basedir, 'src/main/resources').absolutePath
    assert resource.targetPath.text() == 'config'
    assert resource.filtering.text() == 'true'
    assert resource.includes.children()*.text() == ['**/*.properties', '**/*.xml']
    assert resource.excludes.children()*.text() == ['**/private.properties']
}
def checkResources = { resources ->
    assert resources.children().size() == 2
    checkResource(resources.children()[0])
    def extra = resources.children()[1]
    assert extra.name() == 'resource'
    assert extra.directory.text() == new File(basedir, 'src/extra/resources').absolutePath
    assert extra.includes.children().isEmpty()
    assert extra.excludes.children().isEmpty()
}

def resources = parse('resources.xml')
assert resources.name() == 'resources'
checkResources(resources)
checkResource(parse('resource.xml'))
def build = parse('build.xml')
assert build.name() == 'build'
checkResources(build.resources)
assert parse('empty.xml').children().isEmpty()

def plugins = parse('plugins.xml').entry.plugin
assert plugins.size() == build.plugins.children().size()
assert plugins.artifactId*.text().contains('maven-compiler-plugin')
assert plugins.artifactId*.text().sort() == build.plugins.children().artifactId*.text().sort()

def profiles = parse('profiles.xml')
assert profiles.name() == 'profiles'
assert profiles.profile.size() == 1
assert profiles.profile.id.text() == 'it-profile'
// String maps are written like Maven 3 Properties: one element per key, sorted
assert profiles.profile.properties.children()*.name() == ['another.key', 'profile.key']
assert profiles.profile.properties.'profile.key'.text() == 'profile value'

['resources.xml', 'resource.xml', 'build.xml', 'plugins.xml', 'profiles.xml'].each { name ->
    def nodes = parse(name).depthFirst().toList()
    // no wrapper, source location or cache fields of the Maven 4 model
    assert nodes.findAll {
        it.name() in ['delegate', 'locations', 'importedFrom', 'pluginMap', 'reportSetMap', 'managementKey']
    }.isEmpty()
    // no implementation classes of the immutable collections and no XStream back references
    assert nodes.findAll { it.@class.size() > 0 || it.@reference.size() > 0 }.isEmpty()
}

return true
