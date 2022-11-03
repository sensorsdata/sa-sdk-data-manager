package com.sensorsdata.manager;

import static com.sensorsdata.manager.utils.Base64Coder.CHARSET_UTF8;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;

import com.sensorsdata.manager.db.DBConstant;
import com.sensorsdata.manager.db.DBUtil;
import com.sensorsdata.manager.exceptions.ConnectErrorException;
import com.sensorsdata.manager.exceptions.InvalidDataException;
import com.sensorsdata.manager.exceptions.ResponseErrorException;
import com.sensorsdata.manager.utils.Base64Coder;
import com.sensorsdata.manager.utils.JSONUtils;
import com.sensorsdata.manager.utils.NetworkUtils;
import com.sensorsdata.manager.utils.SALogger;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HttpsURLConnection;

class AnalyticsFlushData {
    private final String TAG = "AnalyticsDataFlush";
    private static final int MESSAGE_CODE_FLUSH = 100;
    private final Context mContext;
    private final SMConfigOptions mSMConfigOptions;
    private final Uri mEventUri;
    private final Object mLock = new Object();
    private final Handler mHandler;

    public AnalyticsFlushData(Context context, final SMConfigOptions SMConfigOptions) {
        this.mContext = context;
        this.mSMConfigOptions = SMConfigOptions;
        this.mEventUri = Uri.parse("content://" + DBConstant.AUTHORITY + "/" + DBConstant.TABLE_EVENTS);
        HandlerThread handlerThread = new HandlerThread("SENSORS_DATA_THREAD");
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == MESSAGE_CODE_FLUSH) {
                    sendData();
                    //mHandler.sendEmptyMessageDelayed(MESSAGE_CODE_FLUSH, mSMConfigOptions.mFlushInterval);
                }
            }
        };
        //mHandler.sendEmptyMessageDelayed(MESSAGE_CODE_FLUSH, mSMConfigOptions.mFlushInterval);
    }

    /**
     * flush 上报数据
     */
    public void flushDelay() {
        if (!mHandler.hasMessages(MESSAGE_CODE_FLUSH)) {
            mHandler.sendEmptyMessageDelayed(MESSAGE_CODE_FLUSH, mSMConfigOptions.mFlushInterval);
        }
    }
    /**
     * flush 上报数据
     */
    public void flush() {
        mHandler.sendEmptyMessage(MESSAGE_CODE_FLUSH);
    }

    private void sendData() {
        try {
            if (!SensorsDataManagerAPI.sharedInstance().isNetworkRequestEnable()) {
                SALogger.i(TAG, "NetworkRequest 已关闭，不发送数据！");
                return;
            }

            if (TextUtils.isEmpty(SensorsDataManagerAPI.sharedInstance().getServerUrl())) {
                SALogger.i(TAG, "Server url is null or empty.");
                return;
            }

            //无网络
            if (!NetworkUtils.isNetworkAvailable(mContext)) {
                return;
            }

            //不符合同步数据的网络策略
            String networkType = NetworkUtils.networkType(mContext);
            if (!NetworkUtils.isShouldFlush(networkType, mSMConfigOptions.mNetworkTypePolicy)) {
                SALogger.i(TAG, String.format("您当前网络为 %s，无法发送数据，请确认您的网络发送策略！", networkType));
                return;
            }

            if (!SensorsDataManagerAPI.mIsMainProcess) {//不是主进程
                return;
            }
        } catch (Exception e) {
            SALogger.printStackTrace(e);
            return;
        }
        int count = 100;
        while (count > 0) {
            boolean deleteEvents = true;
            String[] eventsData;
            synchronized (mLock) {
                eventsData = DBUtil.getInstance(mContext).queryData(mEventUri, 50);
            }

            if (eventsData == null) {
                return;
            }

            final String eventIds = eventsData[0];
            final String rawMessage = eventsData[1];
            String gzip = eventsData[2];
            String errorMessage = null;

            try {
                String data = rawMessage;
                if (DBUtil.GZIP_DATA_EVENT.equals(gzip)) {
                    data = encodeData(rawMessage);
                }

                if (!TextUtils.isEmpty(data)) {
                    sendHttpRequest(SensorsDataManagerAPI.sharedInstance().getServerUrl(), data, gzip, rawMessage, false);
                }
            } catch (ConnectErrorException e) {
                deleteEvents = false;
                errorMessage = "Connection error: " + e.getMessage();
            } catch (InvalidDataException e) {
                errorMessage = "Invalid data: " + e.getMessage();
            } catch (ResponseErrorException e) {
                deleteEvents = isDeleteEventsByCode(e.getHttpCode());
                errorMessage = "ResponseErrorException: " + e.getMessage();
            } catch (Exception e) {
                deleteEvents = false;
                errorMessage = "Exception: " + e.getMessage();
            } finally {
                if (!TextUtils.isEmpty(errorMessage)) {
                    if (SALogger.isLogEnabled()) {
                        SALogger.i(TAG, errorMessage);
                    }
                }
                if (deleteEvents) {
                    try {
                        count = DBUtil.getInstance(mContext).cleanupEvents(mEventUri, new JSONArray(eventIds));
                    } catch (JSONException e) {
                        SALogger.printStackTrace(e);
                        count = DBUtil.getInstance(mContext).cleanupEvents(mEventUri, eventIds);
                    }
                    SALogger.i(TAG, String.format(Locale.CHINA, "Events flushed. [left = %d]", count));
                } else {
                    count = 0;
                }
            }
        }
    }

    private void sendHttpRequest(String path, String data, String gzip, String rawMessage, boolean isRedirects) throws ConnectErrorException, ResponseErrorException {
        HttpURLConnection connection = null;
        InputStream in = null;
        OutputStream out = null;
        BufferedOutputStream bout = null;
        try {
            final URL url = new URL(path);
            connection = (HttpURLConnection) url.openConnection();
            if (connection == null) {
                SALogger.i(TAG, String.format("can not connect %s, it shouldn't happen", url.toString()), null);
                return;
            }
            if (mSMConfigOptions.mSSLSocketFactory != null && connection instanceof HttpsURLConnection) {
                ((HttpsURLConnection) connection).setSSLSocketFactory(mSMConfigOptions.mSSLSocketFactory);
            }
            connection.setInstanceFollowRedirects(false);

            Uri.Builder builder = new Uri.Builder();
            //先校验crc
            if (!TextUtils.isEmpty(data)) {
                builder.appendQueryParameter("crc", String.valueOf(data.hashCode()));
            }

            builder.appendQueryParameter("gzip", gzip);
            builder.appendQueryParameter("data_list", data);

            String query = builder.build().getEncodedQuery();
            if (TextUtils.isEmpty(query)) {
                return;
            }

            connection.setFixedLengthStreamingMode(query.getBytes(CHARSET_UTF8).length);
            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            out = connection.getOutputStream();
            bout = new BufferedOutputStream(out);
            bout.write(query.getBytes(CHARSET_UTF8));
            bout.flush();

            int responseCode = connection.getResponseCode();
            SALogger.i(TAG, "responseCode: " + responseCode);
            if (!isRedirects && NetworkUtils.needRedirects(responseCode)) {
                String location = NetworkUtils.getLocation(connection, path);
                if (!TextUtils.isEmpty(location)) {
                    closeStream(bout, out, null, connection);
                    sendHttpRequest(location, data, gzip, rawMessage, true);
                    return;
                }
            }
            try {
                in = connection.getInputStream();
            } catch (FileNotFoundException e) {
                in = connection.getErrorStream();
            }
            byte[] responseBody = slurp(in);
            in.close();
            in = null;

            String response = new String(responseBody, CHARSET_UTF8);
            if (SALogger.isLogEnabled()) {
                String jsonMessage = JSONUtils.formatJson(rawMessage);
                // 状态码 200 - 300 间都认为正确
                if (responseCode >= HttpURLConnection.HTTP_OK &&
                        responseCode < HttpURLConnection.HTTP_MULT_CHOICE) {
                    SALogger.i(TAG, "valid message: \n" + jsonMessage);
                } else {
                    SALogger.i(TAG, "invalid message: \n" + jsonMessage);
                    SALogger.i(TAG, String.format(Locale.CHINA, "ret_code: %d", responseCode));
                    SALogger.i(TAG, String.format(Locale.CHINA, "ret_content: %s", response));
                }
            }
            if (responseCode < HttpURLConnection.HTTP_OK || responseCode >= HttpURLConnection.HTTP_MULT_CHOICE) {
                // 校验错误
                throw new ResponseErrorException(String.format("flush failure with response '%s', the response code is '%d'",
                        response, responseCode), responseCode);
            }
        } catch (IOException e) {
            throw new ConnectErrorException(e);
        } finally {
            closeStream(bout, out, in, connection);
        }
    }

    /**
     * 在服务器正常返回状态码的情况下，目前只有 (>= 500 && < 600) || 404 || 403 才不删数据
     *
     * @param httpCode 状态码
     * @return true: 删除数据，false: 不删数据
     */
    private boolean isDeleteEventsByCode(int httpCode) {
        boolean shouldDelete = true;
        if (httpCode == HttpURLConnection.HTTP_NOT_FOUND ||
                httpCode == HttpURLConnection.HTTP_FORBIDDEN ||
                (httpCode >= HttpURLConnection.HTTP_INTERNAL_ERROR && httpCode < 600)) {
            shouldDelete = false;
        }
        return shouldDelete;
    }

    private void closeStream(BufferedOutputStream bout, OutputStream out, InputStream in, HttpURLConnection connection) {
        if (null != bout) {
            try {
                bout.close();
            } catch (Exception e) {
                SALogger.i(TAG, e.getMessage());
            }
        }

        if (null != out) {
            try {
                out.close();
            } catch (Exception e) {
                SALogger.i(TAG, e.getMessage());
            }
        }

        if (null != in) {
            try {
                in.close();
            } catch (Exception e) {
                SALogger.i(TAG, e.getMessage());
            }
        }

        if (null != connection) {
            try {
                connection.disconnect();
            } catch (Exception e) {
                SALogger.i(TAG, e.getMessage());
            }
        }
    }

    private String encodeData(final String rawMessage) throws InvalidDataException {
        GZIPOutputStream gos = null;
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream(rawMessage.getBytes(CHARSET_UTF8).length);
            gos = new GZIPOutputStream(os);
            gos.write(rawMessage.getBytes(CHARSET_UTF8));
            gos.close();
            byte[] compressed = os.toByteArray();
            os.close();
            return new String(Base64Coder.encode(compressed));
        } catch (IOException exception) {
            // 格式错误，直接将数据删除
            throw new InvalidDataException(exception);
        } finally {
            if (gos != null) {
                try {
                    gos.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private static byte[] slurp(final InputStream inputStream)
            throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[8192];

        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        buffer.flush();
        return buffer.toByteArray();
    }
}
