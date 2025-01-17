package stroom.config.global.impl.db;

import stroom.config.app.PropertyServiceConfig;
import stroom.config.global.impl.ConfigPropertyDao;
import stroom.config.global.impl.GlobalConfigModule;
import stroom.db.util.AbstractFlyWayDbModule;
import stroom.db.util.DataSourceProxy;

import javax.sql.DataSource;

public class GlobalConfigDbModule extends AbstractFlyWayDbModule<PropertyServiceConfig, GlobalConfigDbConnProvider> {

    private static final String MODULE = "stroom-config";
    private static final String FLYWAY_LOCATIONS = "stroom/config/global/impl/db/migration";
    private static final String FLYWAY_TABLE = "config_schema_history";

    @Override
    protected void configure() {
        super.configure();
        install(new GlobalConfigModule());

        bind(ConfigPropertyDao.class).to(ConfigPropertyDaoImpl.class);
    }

    @Override
    protected String getFlyWayTableName() {
        return FLYWAY_TABLE;
    }

    @Override
    protected String getModuleName() {
        return MODULE;
    }

    @Override
    protected String getFlyWayLocation() {
        return FLYWAY_LOCATIONS;
    }

    @Override
    protected Class<GlobalConfigDbConnProvider> getConnectionProviderType() {
        return GlobalConfigDbConnProvider.class;
    }

    @Override
    protected GlobalConfigDbConnProvider createConnectionProvider(final DataSource dataSource) {
        return new DataSourceImpl(dataSource);
    }

    private static class DataSourceImpl extends DataSourceProxy implements GlobalConfigDbConnProvider {

        private DataSourceImpl(final DataSource dataSource) {
            super(dataSource);
        }
    }
}
