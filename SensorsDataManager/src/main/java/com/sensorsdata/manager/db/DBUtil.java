package com.sensorsdata.manager.db;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.SystemClock;
import android.text.TextUtils;

import com.sensorsdata.manager.SensorsDataManagerAPI;
import com.sensorsdata.manager.utils.SALogger;
import com.sensorsdata.manager.utils.TimeUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class DBUtil {
    private static final String TAG = "DBUtil";
    private final File mDatabaseFile;
    private Context mContext;
    private static DBUtil mInstance;
    public static final String GZIP_DATA_EVENT = "1";
    public static final String GZIP_DATA_ENCRYPT = "9";
    /**
     * AES 秘钥加密
     */
    private final String EKEY = "ekey";
    /**
     * RSA 公钥名称
     */
    private final String KEY_VER = "pkv";
    /**
     * 加密后的数据
     */
    private final String PAYLOADS = "payloads";

    private DBUtil(Context context) {
        mContext = context.getApplicationContext();
        mDatabaseFile = context.getDatabasePath(DBConstant.DATABASE_NAME);
    }

    public static DBUtil getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new DBUtil(context);
        }
        return mInstance;
    }

    /**
     * 数据库存满时删除数据
     *
     * @param uri URI
     * @return 正常返回 0
     */
    int deleteDataLowMemory(Uri uri) {
        if (belowMemThreshold()) {
            SALogger.i(TAG, "There is not enough space left on the device to store events, so will delete 100 oldest events");
            String lastId = queryLastIds(uri, 100);
            if (lastId == null) {
                return DBConstant.DB_OUT_OF_MEMORY_ERROR;
            }

            cleanupEvents(uri, lastId);
            if (queryDataCount(uri) < 0) {
                return DBConstant.DB_OUT_OF_MEMORY_ERROR;
            }
        }
        return 0;
    }

    /**
     * 是否超过缓存条数大小
     * @param uri Uri
     * @return 是否超过缓存条数，true：超过，false：未超过
     */
    boolean isOutOfFlushSize(Uri uri) {
        return queryDataCount(uri) >= SensorsDataManagerAPI.sharedInstance().getFlushSize();
    }

    private long getMaxCacheSize() {
        try {
            return SensorsDataManagerAPI.sharedInstance().getMaxCacheSize();
        } catch (Exception e) {
            SALogger.printStackTrace(e);
            return 32 * 1024 * 1024;
        }
    }

    private boolean belowMemThreshold() {
        if (mDatabaseFile.exists()) {
            return mDatabaseFile.length() >= getMaxCacheSize();
        }
        return false;
    }

    /**
     * 查询数据条数
     *
     * @param uri Uri
     * @return 条数
     */
    int queryDataCount(Uri uri) {
        return queryDataCount(uri, null, null, null, null);
    }

    /**
     * 查询数据条数
     */
    private int queryDataCount(Uri uri, String[] projection, String selection,
                               String[] selectionArgs, String sortOrder) {
        Cursor cursor = null;
        try {
            cursor = mContext.getContentResolver().query(uri, projection, selection, selectionArgs, sortOrder);
            if (cursor != null) {
                return cursor.getCount();
            }
        } catch (Exception ex) {
            SALogger.printStackTrace(ex);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return 0;
    }

    /**
     * Removes events with an _id &lt;= last_id from table
     *
     * @param uri Uri
     * @param last_id the last id to delete
     * @return the number of rows in the table
     */
    public int cleanupEvents(Uri uri, String last_id) {
        Cursor c = null;
        int count = -1;

        try {
            mContext.getContentResolver().delete(uri, "_id <= ?", new String[]{last_id});
            c = mContext.getContentResolver().query(uri, null, null, null, null);
            if (c != null) {
                count = c.getCount();
            }
        } catch (Exception e) {
            SALogger.printStackTrace(e);
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (Exception ex) {
                // ignore
            }
        }
        return count;
    }

    /**
     * 删除指定 id 的 event 数据
     *
     * @param uri Uri
     * @param ids 指定的 id 集合
     */
    public int cleanupEvents(Uri uri, JSONArray ids) {
        try {
            String whereCause = "DELETE FROM events WHERE _id in " + buildIds(ids);
            mContext.getContentResolver().delete(uri, whereCause, null);
            return queryDataCount(uri);
        } catch (Exception e) {
            SALogger.printStackTrace(e);
        }
        return 0;
    }

    /**
     * 从 Event 表中读取上报数据
     *
     * @param uri Uri
     * @param limit 条数限制
     * @return 数据
     */
    public String[] queryData(Uri uri, int limit) {
        Cursor c = null;
        String data = null;
        String eventIds = null;
        String gZipType = DBUtil.GZIP_DATA_EVENT;
        try {
            Map<String, JSONArray> dataMap = new HashMap<>();
            c = mContext.getContentResolver().query(uri, null, null, null, DBConstant.KEY_CREATED_AT + " ASC LIMIT " + limit);
            if (c != null) {
                JSONArray idEncryptArray = new JSONArray();
                JSONArray idArray = new JSONArray();
                StringBuilder dataBuilder = new StringBuilder();
                final String flush_time = ",\"_flush_time\":";
                String suffix = ",";
                dataBuilder.append("[");
                String keyData, crc, content;
                JSONObject jsonObject;
                while (c.moveToNext()) {
                    if (c.isLast()) {
                        suffix = "]";
                    }
                    try {
                        String eventId = c.getString(c.getColumnIndex("_id"));
                        keyData = c.getString(c.getColumnIndex(DBConstant.KEY_DATA));
                        if (!TextUtils.isEmpty(keyData)) {
                            int index = keyData.lastIndexOf("\t");
                            if (index > -1) {
                                crc = keyData.substring(index).replaceFirst("\t", "");
                                content = keyData.substring(0, index);
                                if (TextUtils.isEmpty(content) || TextUtils.isEmpty(crc)
                                        || !crc.equals(String.valueOf(content.hashCode()))) {
                                    continue;
                                }
                                keyData = content;
                            }

                            jsonObject = new JSONObject(keyData);
                            if (jsonObject.has(EKEY)) { //如果是加密数据
                                String key = jsonObject.getString(EKEY) + "$" + jsonObject.getInt(KEY_VER);
                                if (dataMap.containsKey(key)) {
                                    dataMap.get(key).put(jsonObject.getString(PAYLOADS));
                                } else {
                                    JSONArray jsonArray = new JSONArray();
                                    jsonArray.put(jsonObject.getString(PAYLOADS));
                                    dataMap.put(key, jsonArray);
                                }
                                idEncryptArray.put(eventId);
                            } else {
                                long trackTime = jsonObject.optLong("time");
                                if (TimeUtils.isDateInvalid(trackTime)) {
                                    long elapsedRealtime = jsonObject.optLong("SAElapsedRealtime");
                                    jsonObject.put("time", System.currentTimeMillis() - (SystemClock.elapsedRealtime() - elapsedRealtime));
                                }
                                jsonObject.remove("SAElapsedRealtime");
                                keyData = jsonObject.toString();
                                jsonObject.put("_flush_time", System.currentTimeMillis());
                                dataBuilder.append(keyData, 0, keyData.length() - 1)
                                        .append(flush_time)
                                        .append(System.currentTimeMillis())
                                        .append("}").append(suffix);
                                idArray.put(eventId);
                            }
                        }
                    } catch (Exception e) {
                        SALogger.printStackTrace(e);
                    }
                }

                if (dataMap.size() > 0) {
                    try {
                        JSONArray arr = new JSONArray();
                        for (String key : dataMap.keySet()) {
                            jsonObject = new JSONObject();
                            jsonObject.put(EKEY, key.substring(0, key.indexOf("$")));
                            jsonObject.put(KEY_VER, Integer.valueOf(key.substring(key.indexOf("$") + 1)));
                            jsonObject.put(PAYLOADS, dataMap.get(key));
                            jsonObject.put("flush_time", System.currentTimeMillis());
                            arr.put(jsonObject);
                        }

                        if (arr.length() > 0) {
                            data = arr.toString();
                            gZipType = DBUtil.GZIP_DATA_ENCRYPT;
                            eventIds = idEncryptArray.toString();
                        }
                    } catch (Exception e) {
                        SALogger.printStackTrace(e);
                    }
                } else {
                    data = dataBuilder.toString();
                    gZipType = DBUtil.GZIP_DATA_EVENT;
                    eventIds = idArray.toString();
                }
            }
        } catch (Exception e) {
            SALogger.i(TAG, "Could not pull records for SensorsData out of database events. Waiting to send.", e);
            eventIds = null;
            data = null;
        } finally {
            if (c != null) {
                c.close();
            }
        }

        if (eventIds != null) {
            return new String[]{eventIds, data, gZipType};
        }
        return null;
    }

    /**
     * 查询删除数据最后的 Id
     * @param uri Uri
     * @param limit 限制
     * @return Id
     */
    public String queryLastIds(Uri uri, int limit) {
        Cursor cursor = null;
        String eventId = null;
        try {
            Map<String, JSONArray> dataMap = new HashMap<>();
            cursor = mContext.getContentResolver().query(uri, null, null, null, DBConstant.KEY_CREATED_AT + " ASC LIMIT " + limit);
            if (cursor != null) {
                cursor.moveToLast();
                eventId = cursor.getString(cursor.getColumnIndex("_id"));
            }
        } catch (Exception exception) {
            SALogger.printStackTrace(exception);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return eventId;
    }

    /**
     * 构造 SQL 中的 id 集合
     *
     * @param idArray id 集合
     * @return SQL 中 id 语句
     */
    private String buildIds(JSONArray idArray) throws JSONException {
        StringBuilder idArgs = new StringBuilder();
        idArgs.append("(");
        if (idArray != null && idArray.length() > 0) {
            for (int index = 0; index < idArray.length(); index++) {
                idArgs.append(idArray.get(index)).append(",");
            }
            idArgs.replace(idArgs.length() - 1, idArgs.length(), "");
        }
        idArgs.append(")");
        return idArgs.toString();
    }
}
