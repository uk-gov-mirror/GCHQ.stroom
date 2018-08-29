/*
 * This file is generated by jOOQ.
 */
package stroom.config.impl.db.stroom.tables;


import java.util.Arrays;
import java.util.List;

import javax.annotation.Generated;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;

import stroom.config.impl.db.stroom.Indexes;
import stroom.config.impl.db.stroom.Keys;
import stroom.config.impl.db.stroom.Stroom;
import stroom.config.impl.db.stroom.tables.records.ConfigRecord;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.11.4"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Config extends TableImpl<ConfigRecord> {

    private static final long serialVersionUID = -440826825;

    /**
     * The reference instance of <code>stroom.config</code>
     */
    public static final Config CONFIG = new Config();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<ConfigRecord> getRecordType() {
        return ConfigRecord.class;
    }

    /**
     * The column <code>stroom.config.id</code>.
     */
    public final TableField<ConfigRecord, Integer> ID = createField("id", org.jooq.impl.SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>stroom.config.name</code>.
     */
    public final TableField<ConfigRecord, String> NAME = createField("name", org.jooq.impl.SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.config.val</code>.
     */
    public final TableField<ConfigRecord, String> VAL = createField("val", org.jooq.impl.SQLDataType.CLOB.nullable(false), this, "");

    /**
     * Create a <code>stroom.config</code> table reference
     */
    public Config() {
        this(DSL.name("config"), null);
    }

    /**
     * Create an aliased <code>stroom.config</code> table reference
     */
    public Config(String alias) {
        this(DSL.name(alias), CONFIG);
    }

    /**
     * Create an aliased <code>stroom.config</code> table reference
     */
    public Config(Name alias) {
        this(alias, CONFIG);
    }

    private Config(Name alias, Table<ConfigRecord> aliased) {
        this(alias, aliased, null);
    }

    private Config(Name alias, Table<ConfigRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""));
    }

    public <O extends Record> Config(Table<O> child, ForeignKey<O, ConfigRecord> key) {
        super(child, key, CONFIG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema getSchema() {
        return Stroom.STROOM;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.CONFIG_NAME, Indexes.CONFIG_PRIMARY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Identity<ConfigRecord, Integer> getIdentity() {
        return Keys.IDENTITY_CONFIG;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UniqueKey<ConfigRecord> getPrimaryKey() {
        return Keys.KEY_CONFIG_PRIMARY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UniqueKey<ConfigRecord>> getKeys() {
        return Arrays.<UniqueKey<ConfigRecord>>asList(Keys.KEY_CONFIG_PRIMARY, Keys.KEY_CONFIG_NAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Config as(String alias) {
        return new Config(DSL.name(alias), this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Config as(Name alias) {
        return new Config(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public Config rename(String name) {
        return new Config(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public Config rename(Name name) {
        return new Config(name, null);
    }
}
