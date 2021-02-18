/*
 * This file is generated by jOOQ.
 */
package stroom.data.store.impl.fs.db.jooq.tables.records;


import stroom.data.store.impl.fs.db.jooq.tables.FsVolume;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record10;
import org.jooq.Row10;
import org.jooq.impl.UpdatableRecordImpl;

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
public class FsVolumeRecord extends UpdatableRecordImpl<FsVolumeRecord> implements Record10<Integer, Integer, Long, String, Long, String, String, Byte, Long, Integer> {

    private static final long serialVersionUID = 1340842491;

    /**
     * Setter for <code>stroom.fs_volume.id</code>.
     */
    public void setId(Integer value) {
        set(0, value);
    }

    /**
     * Getter for <code>stroom.fs_volume.id</code>.
     */
    public Integer getId() {
        return (Integer) get(0);
    }

    /**
     * Setter for <code>stroom.fs_volume.version</code>.
     */
    public void setVersion(Integer value) {
        set(1, value);
    }

    /**
     * Getter for <code>stroom.fs_volume.version</code>.
     */
    public Integer getVersion() {
        return (Integer) get(1);
    }

    /**
     * Setter for <code>stroom.fs_volume.create_time_ms</code>.
     */
    public void setCreateTimeMs(Long value) {
        set(2, value);
    }

    /**
     * Getter for <code>stroom.fs_volume.create_time_ms</code>.
     */
    public Long getCreateTimeMs() {
        return (Long) get(2);
    }

    /**
     * Setter for <code>stroom.fs_volume.create_user</code>.
     */
    public void setCreateUser(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>stroom.fs_volume.create_user</code>.
     */
    public String getCreateUser() {
        return (String) get(3);
    }

    /**
     * Setter for <code>stroom.fs_volume.update_time_ms</code>.
     */
    public void setUpdateTimeMs(Long value) {
        set(4, value);
    }

    /**
     * Getter for <code>stroom.fs_volume.update_time_ms</code>.
     */
    public Long getUpdateTimeMs() {
        return (Long) get(4);
    }

    /**
     * Setter for <code>stroom.fs_volume.update_user</code>.
     */
    public void setUpdateUser(String value) {
        set(5, value);
    }

    /**
     * Getter for <code>stroom.fs_volume.update_user</code>.
     */
    public String getUpdateUser() {
        return (String) get(5);
    }

    /**
     * Setter for <code>stroom.fs_volume.path</code>.
     */
    public void setPath(String value) {
        set(6, value);
    }

    /**
     * Getter for <code>stroom.fs_volume.path</code>.
     */
    public String getPath() {
        return (String) get(6);
    }

    /**
     * Setter for <code>stroom.fs_volume.status</code>.
     */
    public void setStatus(Byte value) {
        set(7, value);
    }

    /**
     * Getter for <code>stroom.fs_volume.status</code>.
     */
    public Byte getStatus() {
        return (Byte) get(7);
    }

    /**
     * Setter for <code>stroom.fs_volume.byte_limit</code>.
     */
    public void setByteLimit(Long value) {
        set(8, value);
    }

    /**
     * Getter for <code>stroom.fs_volume.byte_limit</code>.
     */
    public Long getByteLimit() {
        return (Long) get(8);
    }

    /**
     * Setter for <code>stroom.fs_volume.fk_fs_volume_state_id</code>.
     */
    public void setFkFsVolumeStateId(Integer value) {
        set(9, value);
    }

    /**
     * Getter for <code>stroom.fs_volume.fk_fs_volume_state_id</code>.
     */
    public Integer getFkFsVolumeStateId() {
        return (Integer) get(9);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<Integer> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record10 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row10<Integer, Integer, Long, String, Long, String, String, Byte, Long, Integer> fieldsRow() {
        return (Row10) super.fieldsRow();
    }

    @Override
    public Row10<Integer, Integer, Long, String, Long, String, String, Byte, Long, Integer> valuesRow() {
        return (Row10) super.valuesRow();
    }

    @Override
    public Field<Integer> field1() {
        return FsVolume.FS_VOLUME.ID;
    }

    @Override
    public Field<Integer> field2() {
        return FsVolume.FS_VOLUME.VERSION;
    }

    @Override
    public Field<Long> field3() {
        return FsVolume.FS_VOLUME.CREATE_TIME_MS;
    }

    @Override
    public Field<String> field4() {
        return FsVolume.FS_VOLUME.CREATE_USER;
    }

    @Override
    public Field<Long> field5() {
        return FsVolume.FS_VOLUME.UPDATE_TIME_MS;
    }

    @Override
    public Field<String> field6() {
        return FsVolume.FS_VOLUME.UPDATE_USER;
    }

    @Override
    public Field<String> field7() {
        return FsVolume.FS_VOLUME.PATH;
    }

    @Override
    public Field<Byte> field8() {
        return FsVolume.FS_VOLUME.STATUS;
    }

    @Override
    public Field<Long> field9() {
        return FsVolume.FS_VOLUME.BYTE_LIMIT;
    }

    @Override
    public Field<Integer> field10() {
        return FsVolume.FS_VOLUME.FK_FS_VOLUME_STATE_ID;
    }

    @Override
    public Integer component1() {
        return getId();
    }

    @Override
    public Integer component2() {
        return getVersion();
    }

    @Override
    public Long component3() {
        return getCreateTimeMs();
    }

    @Override
    public String component4() {
        return getCreateUser();
    }

    @Override
    public Long component5() {
        return getUpdateTimeMs();
    }

    @Override
    public String component6() {
        return getUpdateUser();
    }

    @Override
    public String component7() {
        return getPath();
    }

    @Override
    public Byte component8() {
        return getStatus();
    }

    @Override
    public Long component9() {
        return getByteLimit();
    }

    @Override
    public Integer component10() {
        return getFkFsVolumeStateId();
    }

    @Override
    public Integer value1() {
        return getId();
    }

    @Override
    public Integer value2() {
        return getVersion();
    }

    @Override
    public Long value3() {
        return getCreateTimeMs();
    }

    @Override
    public String value4() {
        return getCreateUser();
    }

    @Override
    public Long value5() {
        return getUpdateTimeMs();
    }

    @Override
    public String value6() {
        return getUpdateUser();
    }

    @Override
    public String value7() {
        return getPath();
    }

    @Override
    public Byte value8() {
        return getStatus();
    }

    @Override
    public Long value9() {
        return getByteLimit();
    }

    @Override
    public Integer value10() {
        return getFkFsVolumeStateId();
    }

    @Override
    public FsVolumeRecord value1(Integer value) {
        setId(value);
        return this;
    }

    @Override
    public FsVolumeRecord value2(Integer value) {
        setVersion(value);
        return this;
    }

    @Override
    public FsVolumeRecord value3(Long value) {
        setCreateTimeMs(value);
        return this;
    }

    @Override
    public FsVolumeRecord value4(String value) {
        setCreateUser(value);
        return this;
    }

    @Override
    public FsVolumeRecord value5(Long value) {
        setUpdateTimeMs(value);
        return this;
    }

    @Override
    public FsVolumeRecord value6(String value) {
        setUpdateUser(value);
        return this;
    }

    @Override
    public FsVolumeRecord value7(String value) {
        setPath(value);
        return this;
    }

    @Override
    public FsVolumeRecord value8(Byte value) {
        setStatus(value);
        return this;
    }

    @Override
    public FsVolumeRecord value9(Long value) {
        setByteLimit(value);
        return this;
    }

    @Override
    public FsVolumeRecord value10(Integer value) {
        setFkFsVolumeStateId(value);
        return this;
    }

    @Override
    public FsVolumeRecord values(Integer value1, Integer value2, Long value3, String value4, Long value5, String value6, String value7, Byte value8, Long value9, Integer value10) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        value10(value10);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached FsVolumeRecord
     */
    public FsVolumeRecord() {
        super(FsVolume.FS_VOLUME);
    }

    /**
     * Create a detached, initialised FsVolumeRecord
     */
    public FsVolumeRecord(Integer id, Integer version, Long createTimeMs, String createUser, Long updateTimeMs, String updateUser, String path, Byte status, Long byteLimit, Integer fkFsVolumeStateId) {
        super(FsVolume.FS_VOLUME);

        set(0, id);
        set(1, version);
        set(2, createTimeMs);
        set(3, createUser);
        set(4, updateTimeMs);
        set(5, updateUser);
        set(6, path);
        set(7, status);
        set(8, byteLimit);
        set(9, fkFsVolumeStateId);
    }
}
