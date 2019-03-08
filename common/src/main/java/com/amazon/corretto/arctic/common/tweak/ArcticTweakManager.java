/*
 *   Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.amazon.corretto.arctic.common.tweak;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;

/**
 * A central tweak control class that acts as a bridge between the
 * {@link com.amazon.corretto.arctic.common.command.impl.TweakCommand} and the different
 * {@link ArcticTweakableComponent}. The command will send queries and updates to this class ant this class will
 * send them to the registered components that support that key.
 */
public final class ArcticTweakManager {
    private final Set<ArcticTweakableComponent> components;
    private final Set<String> validKeys;

    /**
     * Create a new instance of this class. Called by the dependency injection framework.
     * @param components A collection of all the tweakable components in arctic.
     */
    @Inject
    public ArcticTweakManager(final Set<ArcticTweakableComponent> components) {
        this.components = components;
        validKeys = components.stream()
                .map(ArcticTweakableComponent::getTweakKeys)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    /**
     * Changes a specific tweak value for all the tweakable components that support it.
     * @param key the specific tweak key to set.
     * @param value the specific value for the tweak key.
     * @return true if at least one component accepted that key
     */
    public boolean setTweak(final String key, final String value) {
        if (validKeys.contains(key)) {
            components.stream()
                    .filter(it -> it.getTweakKeys().contains(key))
                    .forEach(it -> it.setTweak(key, value));
            return true;
        }
        return false;
    }

    /**
     * Retrieves all the accepted keys.
     * @return A set with all the valid config keys.
     */
    public Set<String> getTweakKeys() {
        return validKeys;
    }

    /**
     * Gets additional information for a specific key. This information is queried from the different tweakable
     * components.
     * @param key tweak key to change
     * @return additional information about the tweak.
     */
    public String getTweakKeyDescription(final String key) {
        if (validKeys.contains(key)) {
            StringBuilder sb = new StringBuilder();
            components.stream()
                    .filter(it -> it.getTweakKeys().contains(key))
                    .forEach(it -> {
                        sb.append("  ").append(it.getClass().getSimpleName()).append(": ");
                        sb.append(it.getTweakKeyDescription(key));
                        sb.append(System.lineSeparator());
                    });
            return sb.toString();
        } else {
            return null;
        }
    }
}
