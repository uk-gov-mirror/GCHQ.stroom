package stroom.db.util;

import stroom.config.common.HasDbConfig;
import stroom.util.db.ForceLegacyMigration;
import stroom.util.guice.GuiceUtil;
import stroom.util.logging.LambdaLogger;
import stroom.util.logging.LambdaLoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import java.util.HashSet;
import java.util.Set;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.sql.DataSource;

/**
 * @param <T_CONFIG>    A config class that implements {@link HasDbConfig}
 * @param <T_CONN_PROV> A class that extends {@link DataSource}
 */
public abstract class AbstractDataSourceProviderModule<T_CONFIG extends HasDbConfig, T_CONN_PROV extends DataSource>
        extends AbstractModule {

    private static final LambdaLogger LOGGER = LambdaLoggerFactory.getLogger(AbstractDataSourceProviderModule.class);

    protected abstract String getModuleName();

    protected abstract Class<T_CONN_PROV> getConnectionProviderType();

    protected abstract T_CONN_PROV createConnectionProvider(DataSource dataSource);

    private static final ThreadLocal<Set<String>> COMPLETED_MIGRATIONS = new ThreadLocal<>();

    @Override
    protected void configure() {
        super.configure();

        LOGGER.debug("Configure() called on " + this.getClass().getCanonicalName());

        // MultiBind the connection provider so we can see status for all databases.
        GuiceUtil.buildMultiBinder(binder(), DataSource.class).addBinding(getConnectionProviderType());
    }

    /**
     * We inject {@link ForceLegacyMigration} to ensure that the the core DB migration has happened before all
     * other migrations
     */
    @Provides
    @Singleton
    public T_CONN_PROV getConnectionProvider(
            final Provider<T_CONFIG> configProvider,
            final DataSourceFactory dataSourceFactory,
            @SuppressWarnings("unused") final ForceLegacyMigration forceLegacyMigration) {

        LOGGER.debug(() -> "Getting connection provider for " + getModuleName());

        final DataSource dataSource = dataSourceFactory.create(configProvider.get());

        // Prevent migrations from being re-run for each test
        Set<String> set = COMPLETED_MIGRATIONS.get();
        if (set == null) {
            set = new HashSet<>();
            COMPLETED_MIGRATIONS.set(set);
        }
        final boolean required = set.add(getModuleName());

//        final boolean required = COMPLETED_MIGRATIONS
//                .computeIfAbsent(dataSource, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
//                .add(getModuleName());

        if (required) {
            performMigration(dataSource);
        }

        return createConnectionProvider(dataSource);
    }

    protected abstract void performMigration(DataSource dataSource);

    @SuppressWarnings("checkstyle:needbraces")
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        return o != null && getClass() == o.getClass();
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
