package com.corkili.learningclient.common;

import com.corkili.learningclient.generate.protobuf.Info.Answer;
import com.corkili.learningclient.generate.protobuf.Info.CourseWorkInfo;
import com.corkili.learningclient.generate.protobuf.Info.CourseWorkSimpleInfo;
import com.corkili.learningclient.generate.protobuf.Info.QuestionInfo;
import com.corkili.learningclient.generate.protobuf.Info.QuestionSimpleInfo;
import com.corkili.learningclient.generate.protobuf.Info.QuestionType;
import com.corkili.learningclient.generate.protobuf.Info.SingleFillingAnswer;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public class ProtoUtils {

    public static String getQuestionTypeUIName(QuestionType questionType) {
        switch (questionType) {
            case SingleChoice:
                return "单选题";
            case MultipleChoice:
                return "多选题";
            case SingleFilling:
                return "填空题（单空）";
            case MultipleFilling:
                return "填空题（多空）";
            case Essay:
                return "问答题";
            default:
                return "未知题型";
        }
    }

    public static QuestionSimpleInfo simplifyQuestionInfo(QuestionInfo questionInfo) {
        if (questionInfo == null) {
            return null;
        }
        return QuestionSimpleInfo.newBuilder()
                .setQuestionId(questionInfo.getQuestionId())
                .setQuestion(questionInfo.getQuestion())
                .setQuestionType(questionInfo.getQuestionType())
                .setAutoCheck(questionInfo.getAutoCheck())
                .setAuthorId(questionInfo.getAuthorId())
                .build();
    }

    public static CourseWorkSimpleInfo simplifyCourseWorkInfo(CourseWorkInfo courseWorkInfo) {
        if (courseWorkInfo == null) {
            return null;
        }
        return CourseWorkSimpleInfo.newBuilder()
                .setCourseWorkId(courseWorkInfo.getCourseWorkId())
                .setOpen(courseWorkInfo.getOpen())
                .setCourseWorkName(courseWorkInfo.getCourseWorkName())
                .setBelongCourseId(courseWorkInfo.getBelongCourseId())
                .setHasDeadline(courseWorkInfo.getHasDeadline())
                .setDeadline(courseWorkInfo.getDeadline())
                .build();
    }

    public static boolean checkChoices(Map<Integer, String> choices) {
        if (choices == null || choices.isEmpty()) {
            return false;
        }
        for (String s : choices.values()) {
            if (StringUtils.isBlank(s)) {
                return false;
            }
        }
        return true;
    }

    public static boolean checkAnswer(Answer answer, QuestionType questionType, Map<Integer, String> choices) {
        switch (questionType) {
            case SingleChoice:
                return checkSingleChoiceAnswer(answer, choices);
            case MultipleChoice:
                return checkMultipleChoice(answer, choices);
            case SingleFilling:
                return checkSingleFillingAnswer(answer);
            case MultipleFilling:
                return checkMultipleFillingAnswer(answer);
            case Essay:
                return checkEssayAnswer(answer);
        }
        return false;
    }

    private static boolean checkSingleChoiceAnswer(Answer answer, Map<Integer, String> choices) {
        if (!answer.hasSingleChoiceAnswer()) {
            return false;
        }
        return choices.keySet().contains(answer.getSingleChoiceAnswer().getChoice());
    }

    private static boolean checkMultipleChoice(Answer answer, Map<Integer, String> choices) {
        if (!answer.hasMultipleChoiceAnswer()) {
            return false;
        }
        for (Integer index : answer.getMultipleChoiceAnswer().getChoiceList()) {
            if (!choices.keySet().contains(index)) {
                return false;
            }
        }
        return checkChoices(choices);
    }

    private static boolean checkSingleFillingAnswer(Answer answer) {
        if (!answer.hasSingleFillingAnswer()) {
            return false;
        }
        for (String ans : answer.getSingleFillingAnswer().getAnswerList()) {
            if (StringUtils.isBlank(ans)) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkMultipleFillingAnswer(Answer answer) {
        if (!answer.hasMultipleFillingAnswer()) {
            return false;
        }
        for (SingleFillingAnswer singleFillingAnswer : answer.getMultipleFillingAnswer().getAnswerMap().values()) {
            for (String ans : singleFillingAnswer.getAnswerList()) {
                if (StringUtils.isBlank(ans)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean checkEssayAnswer(Answer answer) {
        if (!answer.hasEssayAnswer()) {
            return false;
        }
        return !StringUtils.isBlank(answer.getEssayAnswer().getText());
    }

}
