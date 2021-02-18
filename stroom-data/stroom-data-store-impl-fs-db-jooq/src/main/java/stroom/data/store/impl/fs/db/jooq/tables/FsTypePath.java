/*
 * This file is generated by jOOQ.
 */
package stroom.data.store.impl.fs.db.jooq.tables;


import stroom.data.store.impl.fs.db.jooq.Indexes;
import stroom.data.store.impl.fs.db.jooq.Keys;
import stroom.data.store.impl.fs.db.jooq.Stroom;
import stroom.data.store.impl.fs.db.jooq.tables.records.FsTypePathRecord;

import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.Identity;
import org.jooq.Index;
import org.jooq.Name;
import org.jooq.Record;
import org.jooq.Row3;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.TableImpl;

import java.util.Arrays;
import java.util.List;
import javax.annotation.processing.Generated;


/**
 * This class is generated by jOOQ.
 */
@Generated(
    value = {
        "http://www.jooq.org",
        "jOOQ version:3.12.3"
    },
    comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class FsTypePath extends TableImpl<FsTypePathRecord> {

    private static final long serialVersionUID = 1125770802;

    /**
     * The reference instance of <code>stroom.fs_type_path</code>
     */
    public static final FsTypePath FS_TYPE_PATH = new FsTypePath();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<FsTypePathRecord> getRecordType() {
        return FsTypePathRecord.class;
    }

    /**
     * The column <code>stroom.fs_type_path.id</code>.
     */
    public final TableField<FsTypePathRecord, Integer> ID = createField(DSL.name("id"), org.jooq.impl.SQLDataType.INTEGER.nullable(false).identity(true), this, "");

    /**
     * The column <code>stroom.fs_type_path.name</code>.
     */
    public final TableField<FsTypePathRecord, String> NAME = createField(DSL.name("name"), org.jooq.impl.SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * The column <code>stroom.fs_type_path.path</code>.
     */
    public final TableField<FsTypePathRecord, String> PATH = createField(DSL.name("path"), org.jooq.impl.SQLDataType.VARCHAR(255).nullable(false), this, "");

    /**
     * Create a <code>stroom.fs_type_path</code> table reference
     */
    public FsTypePath() {
        this(DSL.name("fs_type_path"), null);
    }

    /**
     * Create an aliased <code>stroom.fs_type_path</code> table reference
     */
    public FsTypePath(String alias) {
        this(DSL.name(alias), FS_TYPE_PATH);
    }

    /**
     * Create an aliased <code>stroom.fs_type_path</code> table reference
     */
    public FsTypePath(Name alias) {
        this(alias, FS_TYPE_PATH);
    }

    private FsTypePath(Name alias, Table<FsTypePathRecord> aliased) {
        this(alias, aliased, null);
    }

    private FsTypePath(Name alias, Table<FsTypePathRecord> aliased, Field<?>[] parameters) {
        super(alias, null, aliased, parameters, DSL.comment(""));
    }

    public <O extends Record> FsTypePath(Table<O> child, ForeignKey<O, FsTypePathRecord> key) {
        super(child, key, FS_TYPE_PATH);
    }

    @Override
    public Schema getSchema() {
        return Stroom.STROOM;
    }

    @Override
    public List<Index> getIndexes() {
        return Arrays.<Index>asList(Indexes.FS_TYPE_PATH_NAME, Indexes.FS_TYPE_PATH_PRIMARY);
    }

    @Override
    public Identity<FsTypePathRecord, Integer> getIdentity() {
        return Keys.IDENTITY_FS_TYPE_PATH;
    }

    @Override
    public UniqueKey<FsTypePathRecord> getPrimaryKey() {
        return Keys.KEY_FS_TYPE_PATH_PRIMARY;
    }

    @Override
    public List<UniqueKey<FsTypePathRecord>> getKeys() {
        return Arrays.<UniqueKey<FsTypePathRecord>>asList(Keys.KEY_FS_TYPE_PATH_PRIMARY, Keys.KEY_FS_TYPE_PATH_NAME);
    }

    @Override
    public FsTypePath as(String alias) {
        return new FsTypePath(DSL.name(alias), this);
    }

    @Override
    public FsTypePath as(Name alias) {
        return new FsTypePath(alias, this);
    }

    /**
     * Rename this table
     */
    @Override
    public FsTypePath rename(String name) {
        return new FsTypePath(DSL.name(name), null);
    }

    /**
     * Rename this table
     */
    @Override
    public FsTypePath rename(Name name) {
        return new FsTypePath(name, null);
    }

    // -------------------------------------------------------------------------
    // Row3 type methods
    // -------------------------------------------------------------------------

    @Override
    public Row3<Integer, String, String> fieldsRow() {
        return (Row3) super.fieldsRow();
    }
}
