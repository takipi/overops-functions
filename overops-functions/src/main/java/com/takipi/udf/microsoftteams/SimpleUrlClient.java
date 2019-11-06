package com.takipi.udf.microsoftteams;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.takipi.api.core.consts.ApiConstants;
import com.takipi.api.core.request.intf.ApiPostRequest;
import com.takipi.api.core.result.intf.ApiResult;
import com.takipi.api.core.url.UrlClient;

import java.net.HttpURLConnection;
import java.util.Map;

public class SimpleUrlClient extends UrlClient {
    private SimpleUrlClient(String hostname, int connectTimeout, int readTimeout, LogLevel defaultLogLevel, Map<Integer, LogLevel> responseLogLevels) {
        super(hostname, connectTimeout, readTimeout, defaultLogLevel, responseLogLevels);
    }

    public <T extends ApiResult> Response<String> post(ApiPostRequest<T> request) {
        try {
            String postData = request.postData();
            byte[] data = (Strings.isNullOrEmpty(postData) ? null : postData.getBytes(ApiConstants.UTF8_ENCODING));

            return post(request.urlPath(), data, request.contentType(),
                    request.queryParams());
        } catch (Exception e) {
            logger.error("Api url client POST {} failed.", request.getClass().getName(), e);
            return Response.of(HttpURLConnection.HTTP_INTERNAL_ERROR, null);
        }
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private static final int CONNECT_TIMEOUT = 15000;
        private static final int READ_TIMEOUT = 60000;

        private int connectTimeout;
        private int readTimeout;
        private LogLevel defaultLogLevel;
        private Map<Integer, LogLevel> responseLogLevels;

        Builder() {
            this.connectTimeout = CONNECT_TIMEOUT;
            this.readTimeout = READ_TIMEOUT;

            this.defaultLogLevel = LogLevel.ERROR;
            this.responseLogLevels = Maps.newHashMap();
        }

        public Builder setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;

            return this;
        }

        public Builder setReadTimeout(int readTimeout) {
            this.readTimeout = readTimeout;

            return this;
        }

        public Builder setDefaultLogLevel(LogLevel defaultLogLevel) {
            this.defaultLogLevel = defaultLogLevel;

            return this;
        }

        public Builder setResponseLogLevels(Map<Integer, LogLevel> responseLogLevels) {
            this.responseLogLevels = responseLogLevels;

            return this;
        }

        public SimpleUrlClient build() {
            return new SimpleUrlClient("", connectTimeout, readTimeout, defaultLogLevel,
                    ImmutableMap.copyOf(responseLogLevels));
        }
    }
}