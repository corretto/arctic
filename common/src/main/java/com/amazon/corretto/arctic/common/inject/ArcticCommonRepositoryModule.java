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

package com.amazon.corretto.arctic.common.inject;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Named;
import javax.inject.Singleton;

import com.amazon.corretto.arctic.common.exception.ArcticConfigurationException;
import com.amazon.corretto.arctic.common.repository.TestLoadRepository;
import com.amazon.corretto.arctic.common.repository.TestRepository;
import com.amazon.corretto.arctic.common.repository.TestSaveRepository;
import com.amazon.corretto.arctic.common.repository.impl.CompositeLoadRepositoryImpl;
import com.amazon.corretto.arctic.common.repository.impl.JsonFileTestLoadRepositoryImpl;
import com.amazon.corretto.arctic.common.repository.impl.JsonFileTestSaveRepositoryImpl;
import com.amazon.corretto.arctic.common.repository.impl.TestRepositoryImpl;
import com.amazon.corretto.arctic.common.serialization.GsonPathAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import org.apache.commons.configuration2.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.inject.name.Names.named;

/**
 * This module handles the configuration for the repository that loads and saves the tests.
 */
public final class ArcticCommonRepositoryModule extends ArcticModule {
    private static final Logger log = LoggerFactory.getLogger(ArcticCommonRepositoryModule.class);
    private final boolean loadRepository;

    /**
     * Constructor of the module to inject the repositories. Boolean flags control whether we want to inject a support
     * for loading test or just saving
     *
     * @param config Copy of the configuration
     * @param loadRepository True if we want to bind a TestLoadRepository and TestRepository
     */
    public ArcticCommonRepositoryModule(final Configuration config, final boolean loadRepository) {
        super(config);
        this.loadRepository = loadRepository;
    }

    @Override
    public void configure() {
        log.info("Arctic scope: {}", getConfig().getString(CommonInjectionKeys.REPOSITORY_SCOPE));
        bindFromConfig(String.class, CommonInjectionKeys.REPOSITORY_JSON_PATH, "any valid system path");
        bindFromConfig(String.class, CommonInjectionKeys.REPOSITORY_JSON_NAME, "valid file name");
        bindFromConfig(String.class, CommonInjectionKeys.REPOSITORY_SCOPE, "an arbitrary scope");
        bindFromConfig(Boolean.class, CommonInjectionKeys.REPOSITORY_WINDOWS_LEGACY_MODE, Arrays.asList(true, false));
        String scopeMode = getConfig().getString(CommonInjectionKeys.SCOPE_MODE);
        String scope = getConfig().getString(CommonInjectionKeys.REPOSITORY_SCOPE);
        bind(TestSaveRepository.class).to(JsonFileTestSaveRepositoryImpl.class);
        if (loadRepository) {
            bindLoadScopes(scopeMode, scope);
            bind(TestRepository.class).to(TestRepositoryImpl.class);
        } else {
            bind(TestRepository.Mode.class).toInstance(TestRepository.Mode.SINGLE);
            bind(new TypeLiteral<List<String>>(){}).annotatedWith(named(CommonInjectionKeys.SCOPES))
                    .toInstance(Collections.emptyList());
        }
    }

    private void bindLoadScopes(final String scopeMode, final String scope) {
        TestRepository.Mode mode;
        try {
            mode = TestRepository.Mode.valueOf(scopeMode.toUpperCase());
        } catch (Exception e) {
            throw new ArcticConfigurationException(CommonInjectionKeys.SCOPE_MODE,
                    Arrays.asList(TestRepository.Mode.SINGLE.toString(), TestRepository.Mode.DEFAULT.toString(),
                            TestRepository.Mode.INCREMENTAL.toString(), TestRepository.Mode.CUSTOM.toString()));
        }
        bind(TestRepository.Mode.class).toInstance(mode);
        switch (mode) {
            case SINGLE:
                bind(new TypeLiteral<List<String>>(){}).annotatedWith(named(CommonInjectionKeys.SCOPES))
                        .toInstance(List.of(scope));
                break;
            case DEFAULT:
                bind(new TypeLiteral<List<String>>(){}).annotatedWith(named(CommonInjectionKeys.SCOPES))
                        .toInstance(List.of(scope, TestRepository.DEFAULT_SCOPE));
                break;
            case INCREMENTAL:
                bindIncrementalScopes(scope);
                break;
            case CUSTOM:
                List<String> scopeList = getConfig().getList(String.class, CommonInjectionKeys.SCOPE_CUSTOM + scope);
                bind(new TypeLiteral<List<String>>(){}).annotatedWith(named(CommonInjectionKeys.SCOPES))
                        .toInstance(scopeList);
                break;
            default:
                throw new ArcticConfigurationException(CommonInjectionKeys.SCOPE_MODE,
                        Arrays.asList(TestRepository.Mode.SINGLE.toString(), TestRepository.Mode.DEFAULT.toString(),
                                TestRepository.Mode.INCREMENTAL.toString(), TestRepository.Mode.CUSTOM.toString()));
        }
    }

    private void bindIncrementalScopes(final String scope) {
        Integer scopeInt = safeParse(scope).orElseThrow(() -> new ArcticConfigurationException(
                CommonInjectionKeys.REPOSITORY_SCOPE, "Scope needs to be an integer when running in incremental mode"));
        String repositoryRoot = getConfig().getString(CommonInjectionKeys.REPOSITORY_JSON_PATH);
        List<String> scopes = Stream.concat(Arrays.stream(new File(repositoryRoot).listFiles())
                .filter(File::isDirectory)
                .map(File::getName)
                .map(this::safeParse)
                .flatMap(Optional::stream)
                .filter(it -> it <= scopeInt)
                .sorted(Comparator.reverseOrder())
                .map(String::valueOf),
                Stream.of(TestRepository.DEFAULT_SCOPE))
                .collect(Collectors.toList());

        bind(new TypeLiteral<List<String>>(){}).annotatedWith(named(CommonInjectionKeys.SCOPES))
                .toInstance(scopes);
    }

    private Optional<Integer> safeParse(final String value) {
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (Exception e) {
            return Optional.empty();
        }
    }


    /**
     * Provides method for a TestLoadRepository. The method will create one CompositeLoadRepository that can read from
     * all the scopes that are bound. This method is not called directly, it's part of the dependency injection.
     * @param repositoryPath Root path for the repositories
     * @param repositoryFileName Name of the file for te test in the repository
     * @param scopes List of scopes that we need to check. List order determines priority
     * @param gson Instance of gson for deserialization
     * @return An instance of TestLoadRepository that can read from all the scopes required.
     */
    @Provides
    @Singleton
    public TestLoadRepository getCompositeRepository(
            final @Named(CommonInjectionKeys.REPOSITORY_JSON_PATH) String repositoryPath,
            final @Named(CommonInjectionKeys.REPOSITORY_JSON_NAME) String repositoryFileName,
            final @Named(CommonInjectionKeys.SCOPES) List<String> scopes,
            final Gson gson) {
        log.info("Scope load order: {}", String.join(" ", scopes));
        LinkedHashMap<String, TestLoadRepository> repos = scopes.stream().collect(Collectors.toMap(
                Function.identity(),
                scope -> new JsonFileTestLoadRepositoryImpl(repositoryPath, repositoryFileName, scope, gson),
                (x, y) -> y,
                LinkedHashMap::new));

        return new CompositeLoadRepositoryImpl(repos);
    }

    /**
     * Provides method for the instance of gson used when serializing and deserializing tests. This is a standard gson
     * instance, although some fields may define special adapters to control serialization. This method is not called
     * directly, and it is part of the dependency injection.
     * @param windowsLegacy Save paths using \\ instead of /
     * @return An instance of gson.
     */
    @Provides
    @Singleton
    public Gson getGson(final @Named(CommonInjectionKeys.REPOSITORY_WINDOWS_LEGACY_MODE) boolean windowsLegacy) {
        return new GsonBuilder().setPrettyPrinting()
                .registerTypeHierarchyAdapter(Path.class, new GsonPathAdapter(windowsLegacy))
                .create();
    }
}
