/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.jetoile.hadoopunit.component;

import fr.jetoile.hadoopunit.ComponentMetadata;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ConfluentSchemaRegistryMetadata extends ComponentMetadata {

    @Override
    public String getName() {
        return "CONFLUENT_SCHEMAREGISTRY";
    }

    @Override
    public String getBootstrapClass() {
        return "fr.jetoile.hadoopunit.component.ConfluentSchemaRegistryBootstrap";
    }

    @Override
    public String getArtifactKey() {
        return "confluent.schemaregistry.artifact";
    }

    @Override
    public List<String> getDependencies() {
        return Arrays.asList("CONFLUENT_KAFKA");
    }

}