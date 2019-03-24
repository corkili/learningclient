package com.corkili.learningclient.network;

import android.util.Log;

import com.corkili.learningclient.generate.protobuf.Request.BaseRequest;
import com.corkili.learningclient.generate.protobuf.Response.BaseResponse;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessageV3;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public abstract class HttpUtils {

    private static String token = "";

    private static final String scheme = "http";
    private static final String host = "192.168.155.2";
    private static final int port = 8080;

    public static <Req extends GeneratedMessageV3, Res extends GeneratedMessageV3> Res request(
            Req req, Class<Req> requestClass, Class<Res> responseClass, String path) {
        try {
            BaseRequest baseRequest = BaseRequest.newBuilder().setToken(token).build();
            Descriptor descriptor = (Descriptor) requestClass.getDeclaredMethod("getDescriptor").invoke(null);
            FieldDescriptor brfd = null;
            for (FieldDescriptor fieldDescriptor : descriptor.getFields()) {
                if (fieldDescriptor.getName().equals("request")) {
                    brfd = fieldDescriptor;
                    break;
                }
            }
            Req request = requestClass.cast(req.toBuilder().setField(brfd, baseRequest).build());

            URI uri = new URI(scheme, null, host, port, path, "", null);
            byte[] data = post(uri.toURL(), request);
            Res response = responseClass.cast(responseClass
                    .getDeclaredMethod("parseFrom", byte[].class)
                    .invoke(null, (Object) data));
            descriptor = (Descriptor) responseClass.getDeclaredMethod("getDescriptor").invoke(null);
            brfd = null;
            for (FieldDescriptor fieldDescriptor : descriptor.getFields()) {
                if (fieldDescriptor.getName().equals("response")) {
                    brfd = fieldDescriptor;
                    break;
                }
            }
            BaseResponse baseResponse= (BaseResponse) response.getField(brfd);
            token = baseResponse.getToken();
            return response;
        } catch (Exception e) {
            Log.e("HttpUtils", e.toString());
        }
        return null;
    }

    private static byte[] post(URL url, GeneratedMessageV3 message) throws IOException {
        if (message == null) {
            return new byte[0];
        }
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        // conn.setConnectTimeout(10000);//连接超时 单位毫秒
        // conn.setReadTimeout(2000);//读取超时 单位毫秒
        // 发送POST请求必须设置如下两行
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("Content-Type", "application/x-protobuf");
        connection.connect();
        // 获取URLConnection对象对应的输出流
        OutputStream os = connection.getOutputStream();
        // 发送请求参数
        os.write(message.toByteArray());
        // flush输出流的缓冲
        os.flush();
        os.close();
        //开始获取数据
        BufferedInputStream bis = new BufferedInputStream(connection.getInputStream());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int len;
        byte[] data = new byte[1024];
        while ((len = bis.read(data)) != -1) {
            baos.write(data, 0, len);
        }
        connection.disconnect();
        return baos.toByteArray();
    }

}
