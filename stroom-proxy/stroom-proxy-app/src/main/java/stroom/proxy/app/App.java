/*
 * Copyright 2017 Crown Copyright
 *
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

package stroom.proxy.app;

import stroom.dropwizard.common.DelegatingExceptionMapper;
import stroom.dropwizard.common.Filters;
import stroom.dropwizard.common.HealthChecks;
import stroom.dropwizard.common.ManagedServices;
import stroom.dropwizard.common.RestResources;
import stroom.dropwizard.common.Servlets;
import stroom.proxy.app.guice.ProxyModule;
import stroom.util.authentication.DefaultOpenIdCredentials;
import stroom.util.shared.BuildInfo;
import stroom.util.shared.ResourcePaths;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.dropwizard.Application;
import io.dropwizard.configuration.EnvironmentVariableSubstitutor;
import io.dropwizard.configuration.SubstitutingSourceProvider;
import io.dropwizard.servlets.tasks.LogConfigurationTask;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.Objects;
import javax.inject.Inject;
import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;

public class App extends Application<Config> {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    @Inject
    private HealthChecks healthChecks;
    @Inject
    private Filters filters;
    @Inject
    private Servlets servlets;
    @Inject
    private RestResources restResources;
    @Inject
    private ManagedServices managedServices;
    @Inject
    private DelegatingExceptionMapper delegatingExceptionMapper;
    @Inject
    private BuildInfo buildInfo;

    public static void main(final String[] args) throws Exception {
        new App().run(args);
    }

    @Override
    public void initialize(final Bootstrap<Config> bootstrap) {
        // This allows us to use templating in the YAML configuration.
        bootstrap.setConfigurationSourceProvider(new SubstitutingSourceProvider(
                bootstrap.getConfigurationSourceProvider(),
                new EnvironmentVariableSubstitutor(false)));
//        bootstrap.addBundle(new AssetsBundle("/ui", ResourcePaths.ROOT_PATH, "index.html", "ui"));
    }

    @Override
    public void run(final Config configuration, final Environment environment) {
        // Add useful logging setup.
        registerLogConfiguration(environment);

        // We want Stroom to use the root path so we need to move Dropwizard's path.
        environment.jersey().setUrlPattern(ResourcePaths.API_ROOT_PATH + "/*");

        // Set up a session handler for Jetty
        environment.servlets().setSessionHandler(new SessionHandler());

        // Configure Cross-Origin Resource Sharing.
        configureCors(environment);

        LOGGER.info("Starting Stroom Proxy");

        final ProxyModule proxyModule = new ProxyModule(configuration, environment);
        final Injector injector = Guice.createInjector(proxyModule);
        injector.injectMembers(this);

        // Add health checks
        healthChecks.register();

        // Add filters
        filters.register();

        // Add servlets
        servlets.register();

        // Add all injectable rest resources.
        restResources.register();

        // Add jersey exception mappers.
        environment.jersey().register(delegatingExceptionMapper);

        // Listen to the lifecycle of the Dropwizard app.
        managedServices.register();

        warnAboutDefaultOpenIdCreds(configuration, injector);

        showBuildInfo();
    }

    private void warnAboutDefaultOpenIdCreds(Config configuration, Injector injector) {
        if (configuration.getProxyConfig().isUseDefaultOpenIdCredentials()) {
            DefaultOpenIdCredentials defaultOpenIdCredentials = injector.getInstance(DefaultOpenIdCredentials.class);
            LOGGER.warn("" +
                    "\n  ---------------------------------------------------------------------------------------" +
                    "\n  " +
                    "\n                                        WARNING!" +
                    "\n  " +
                    "\n   Using default and publicly available Open ID authentication credentials. " +
                    "\n   These should only be used in test/demo environments. " +
                    "\n   Set useDefaultOpenIdCredentials to false for production environments. " +
                    "The API key in use is:" +
                    "\n" +
                    "\n   " + defaultOpenIdCredentials.getApiKey() +
                    "\n  ---------------------------------------------------------------------------------------" +
                    "");
        }
    }

    private static void configureCors(io.dropwizard.setup.Environment environment) {
        FilterRegistration.Dynamic cors = environment.servlets().addFilter("CORS", CrossOriginFilter.class);
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,PUT,POST,DELETE,OPTIONS,PATCH");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, "*");
    }

    private void registerLogConfiguration(final Environment environment) {
        // Task to allow configuration of log levels at runtime
        String path = environment.getAdminContext().getContextPath();

        // To change the log level do one of:
        // curl -X POST -d "logger=stroom&level=DEBUG" [admin context path]/tasks/log-level
        // http -f POST [admin context path]/tasks/log-level logger=stroom level=DEBUG
        // 'http' requires installing HTTPie

        LOGGER.info("Registering Log Configuration Task on {}/tasks/log-level", path);
        environment.admin().addTask(new LogConfigurationTask());
    }

    private void showBuildInfo() {
        Objects.requireNonNull(buildInfo);
        LOGGER.info("Build version: {}, date: {}",
                buildInfo.getBuildVersion(), buildInfo.getBuildDate());
    }
}
