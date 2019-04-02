package com.corkili.learningclient.service;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.corkili.learningclient.generate.protobuf.Info.NavigationEventType;
import com.corkili.learningclient.generate.protobuf.Request.CourseCatalogQueryRequest;
import com.corkili.learningclient.generate.protobuf.Request.CoursewareUpdateRequest;
import com.corkili.learningclient.generate.protobuf.Request.NavigationProcessRequest;
import com.corkili.learningclient.generate.protobuf.Response.CourseCatalogQueryResponse;
import com.corkili.learningclient.generate.protobuf.Response.CoursewareUpdateResponse;
import com.corkili.learningclient.generate.protobuf.Response.NavigationProcessResponse;
import com.corkili.learningclient.network.HttpUtils;
import com.google.protobuf.ByteString;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

public class ScormService {

    public static final int UPDATE_SCORM_MSG = 0xB1;
    public static final int QUERY_CATALOG_MSG = 0xB2;
    public static final int PROCESS_NAVIGATION_MSG = 0xB3;

    private static final String TAG = "ScormService";
    private static ScormService instance;

    private ScormService() {

    }

    public static ScormService getInstance() {
        if (instance == null) {
            synchronized (ScormService.class) {
                if (instance == null) {
                    instance = new ScormService();
                }
            }
        }
        return instance;
    }

    public void updateScorm(final Handler handler, long courseId, boolean isDelete, File scormZipFile) {
        Message msg = new Message();
        msg.what = UPDATE_SCORM_MSG;
        ServiceResult result = null;
        if (!isDelete && (scormZipFile == null || !scormZipFile.exists() || !scormZipFile.isFile() || !scormZipFile.canRead())) {
            result = ServiceResult.failResultWithMessage("未选择文件");
        }
        if (result != null) {
            msg.obj = result;
            handler.sendMessage(msg);
            return;
        }
        AsyncTaskExecutor.execute(() -> {
            byte[] data = new byte[0];
            if (scormZipFile != null) {
                try {
                    data = FileUtils.readFileToByteArray(scormZipFile);
                } catch (IOException e) {
                    msg.obj = ServiceResult.failResultWithMessage("无法读取文件");
                    handler.sendMessage(msg);
                    return;
                }
            }
            CoursewareUpdateRequest request = CoursewareUpdateRequest.newBuilder()
                    .setCourseId(courseId)
                    .setIsDelete(isDelete)
                    .setFilename(scormZipFile != null ? scormZipFile.getName() : "")
                    .setData(ByteString.copyFrom(data))
                    .build();
            CoursewareUpdateResponse response = HttpUtils.request(request,
                    CoursewareUpdateRequest.class, CoursewareUpdateResponse.class, "/course/updateCourseware");
            Log.i(TAG, "updateCourseware: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            CoursewareUpdateResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            CoursewareUpdateResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void queryCatalog(final Handler handler, long scormId) {
        Message msg = new Message();
        msg.what = QUERY_CATALOG_MSG;
        final CourseCatalogQueryRequest request = CourseCatalogQueryRequest.newBuilder()
                .setScormId(scormId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            CourseCatalogQueryResponse response = HttpUtils.request(request,
                    CourseCatalogQueryRequest.class, CourseCatalogQueryResponse.class, "/scorm/queryCatalog");
            Log.i(TAG, "queryCatalog: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            CoursewareUpdateResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            CoursewareUpdateResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }

    public void processNavigation(final Handler handler, NavigationEventType eventType,
                                  String targetItemId, long scormId, String level1CatalogItemId) {
        Message msg = new Message();
        msg.what = PROCESS_NAVIGATION_MSG;
        final NavigationProcessRequest request = NavigationProcessRequest.newBuilder()
                .setNavigationEventType(eventType)
                .setTargetItemId(targetItemId)
                .setScormId(scormId)
                .setLevel1CatalogItemId(level1CatalogItemId)
                .build();
        AsyncTaskExecutor.execute(() -> {
            NavigationProcessResponse response = HttpUtils.request(request,
                    NavigationProcessRequest.class, NavigationProcessResponse.class, "/scorm/processNavigation");
            Log.i(TAG, "processNavigation: " + response);
            if (response == null) {
                msg.obj = ServiceResult.failResultWithMessage("网络请求错误");
            } else {
                if (response.getResponse().getResult()) {
                    msg.obj = ServiceResult.successResult(response.getResponse().getMsg(),
                            CoursewareUpdateResponse.class, response);
                } else {
                    msg.obj = ServiceResult.failResult(response.getResponse().getMsg(),
                            CoursewareUpdateResponse.class, response);
                }
            }
            handler.sendMessage(msg);
        });
    }
}
