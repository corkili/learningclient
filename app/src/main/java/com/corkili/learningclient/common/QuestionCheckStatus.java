package com.corkili.learningclient.common;

public class QuestionCheckStatus {

    public static final int NOT_CHECK = -1;
    public static final int CHECK_FALSE = 0;
    public static final int CHECK_TRUE = 1;
    public static final int CHECK_HALF_TRUE = 2;

    public static String getStatusUI(int checkStatus) {
        StringBuilder sb = new StringBuilder();
        if (checkStatus == QuestionCheckStatus.CHECK_FALSE) {
            sb.append("[错误]");
        } else if (checkStatus == QuestionCheckStatus.CHECK_TRUE) {
            sb.append("[正确]");
        } else if (checkStatus == QuestionCheckStatus.CHECK_HALF_TRUE) {
            sb.append("[半对]");
        } else {
            sb.append("[未批改]");
        }
        return sb.toString();
    }

}
