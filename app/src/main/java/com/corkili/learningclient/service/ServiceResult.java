package com.corkili.learningclient.service;

import java.util.HashMap;
import java.util.Map;

public class ServiceResult {

    private static final ServiceResult EMPTY_SUCCESS_RESULT = new ServiceResult(true, "success", null);
    private static final ServiceResult EMPTY_FAIL_RESULT = new ServiceResult(false, "fail", null);

    private final boolean result;
    private final String msg;
    private final Map<Object, Object> extraMap;

    private ServiceResult(boolean result, String msg, Map<Object, Object> extraMap) {
        this.result = result;
        this.msg = msg;
        this.extraMap = extraMap;
    }

    public static ServiceResult emptySuccessResult() {
        return EMPTY_SUCCESS_RESULT;
    }

    public static ServiceResult emptyFailResult() {
        return EMPTY_FAIL_RESULT;
    }

    public static ServiceResult successResultWithMessage(String msg) {
        return new ServiceResult(true, msg, null);
    }

    public static ServiceResult failResultWithMessage(String msg) {
        return new ServiceResult(false, msg, null);
    }

    public static ServiceResult successResultWithExtra(Object... keyAndValues) {
        return new ServiceResult(true, "success", generateExtraMap(keyAndValues));
    }

    public static ServiceResult failResultWithExtra(Object... keyAndValues) {
        return new ServiceResult(false, "fail", generateExtraMap(keyAndValues));
    }

    public static ServiceResult successResult(String msg, Object... keyAndValues) {
        return new ServiceResult(true, msg, generateExtraMap(keyAndValues));
    }

    public static ServiceResult failResult(String msg, Object... keyAndValues) {
        return new ServiceResult(false, msg, generateExtraMap(keyAndValues));
    }

    public ServiceResult merge(ServiceResult serviceResult, boolean useBaseResultMessage) {
        boolean mergeResult = result & serviceResult.result;
        String mergeMsg = useBaseResultMessage ? msg : serviceResult.msg;
        Map<Object, Object> mergeExtraMap = new HashMap<>();
        if (extraMap != null) {
            mergeExtraMap.putAll(extraMap);
        }
        if (serviceResult.extraMap != null) {
            mergeExtraMap.putAll(serviceResult.extraMap);
        }
        return new ServiceResult(mergeResult, mergeMsg, mergeExtraMap);
    }

    private static Map<Object, Object> generateExtraMap(Object... keyAndValues) {
        if (keyAndValues.length == 0) {
            return null;
        }
        Map<Object, Object> extraMap = new HashMap<>();
        int n = keyAndValues.length % 2 == 0 ? keyAndValues.length : keyAndValues.length - 1;
        for (int i = 0; i < n; i += 2) {
            extraMap.put(keyAndValues[i], keyAndValues[i + 1]);
        }
        return extraMap;
    }

    public boolean isSuccess() {
        return result;
    }

    public boolean isFail() {
        return !result;
    }

    public boolean result() {
        return result;
    }

    public String msg() {
        return msg != null ? msg : (result ? "success" : "fail");
    }

    public Object extra(Object key) {
        if (extraMap == null) {
            return null;
        }
        if (extraMap.containsKey(key)) {
            return extraMap.get(key);
        } else {
            return null;
        }
    }

    public Object extra(String key) {
        if (extraMap == null) {
            return null;
        }
        if (extraMap.containsKey(key)) {
            return extraMap.get(key);
        } else {
            return null;
        }
    }

    public <T> T extra(Class<T> key) {
        if (extraMap == null) {
            return null;
        }
        Object value = null;
        if (extraMap.containsKey(key)) {
            value = extraMap.get(key);
        }
        if (key.isInstance(value)) {
            return key.cast(value);
        } else {
            return null;
        }
    }

}
