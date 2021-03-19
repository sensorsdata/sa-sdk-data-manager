package com.sensorsdata.manager.db;

public class DBConstant {
    /* 数据库中的表名 */
    public static final String TABLE_EVENTS = "events";
    /* 数据库名称 */
    public static final String DATABASE_NAME = "sensorsdata_manager";
    /* 数据库版本号 */
    public static final int DATABASE_VERSION = 1;
    /* Event 表字段 */
    public static final String KEY_DATA = "data";
    public static final String KEY_CREATED_AT = "created_at";
    /* ContentProvider 的 AUTHORITY */
    public static final String AUTHORITY = "com.sensorsdata.manager.SensorsDataContentProvider";
    public static final int DB_OUT_OF_MEMORY_ERROR = -2;
}
