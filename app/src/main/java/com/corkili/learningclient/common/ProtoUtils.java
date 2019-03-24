package com.corkili.learningclient.common;

import com.corkili.learningclient.generate.protobuf.Info.Answer;
import com.corkili.learningclient.generate.protobuf.Info.CourseCommentType;
import com.corkili.learningclient.generate.protobuf.Info.CourseWorkInfo;
import com.corkili.learningclient.generate.protobuf.Info.CourseWorkQuestionInfo;
import com.corkili.learningclient.generate.protobuf.Info.CourseWorkSimpleInfo;
import com.corkili.learningclient.generate.protobuf.Info.CourseWorkSubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Info.EssaySubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Info.ExamInfo;
import com.corkili.learningclient.generate.protobuf.Info.ExamQuestionInfo;
import com.corkili.learningclient.generate.protobuf.Info.ExamSimpleInfo;
import com.corkili.learningclient.generate.protobuf.Info.ExamSubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Info.MultipleChoiceSubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Info.MultipleFillingAnswer;
import com.corkili.learningclient.generate.protobuf.Info.MultipleFillingSubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Info.MultipleFillingSubmittedAnswer.Pair;
import com.corkili.learningclient.generate.protobuf.Info.QuestionInfo;
import com.corkili.learningclient.generate.protobuf.Info.QuestionSimpleInfo;
import com.corkili.learningclient.generate.protobuf.Info.QuestionType;
import com.corkili.learningclient.generate.protobuf.Info.Score;
import com.corkili.learningclient.generate.protobuf.Info.Score.MultipleScore;
import com.corkili.learningclient.generate.protobuf.Info.SingleChoiceSubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Info.SingleFillingAnswer;
import com.corkili.learningclient.generate.protobuf.Info.SingleFillingSubmittedAnswer;
import com.corkili.learningclient.generate.protobuf.Info.SubmittedAnswer;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ProtoUtils {

    public static Map<Integer, ExamSubmittedAnswer> generateSubmittedExamAnswerMap(
            ExamInfo examInfo, List<QuestionInfo> questionInfoList,
            Map<Integer, ExamSubmittedAnswer> submittedAnswerMap) {
        Map<Integer, ExamSubmittedAnswer> map = new HashMap<>();
        if (examInfo == null || questionInfoList == null) {
            return map;
        }
        Map<Long, QuestionInfo> questionInfoMap = new HashMap<>();
        for (QuestionInfo questionInfo : questionInfoList) {
            questionInfoMap.put(questionInfo.getQuestionId(), questionInfo);
        }
        if (submittedAnswerMap == null) {
            submittedAnswerMap = new HashMap<>();
        }
        for (ExamQuestionInfo examQuestionInfo : examInfo.getExamQuestionInfoList()) {
            QuestionInfo questionInfo = questionInfoMap.get(examQuestionInfo.getQuestionId());
            if (questionInfo != null) {
                ExamSubmittedAnswer rawExamSubmittedAnswer = submittedAnswerMap.get(examQuestionInfo.getIndex());
                ExamSubmittedAnswer examSubmittedAnswer = ExamSubmittedAnswer.newBuilder()
                        .setQuestionIndex(examQuestionInfo.getIndex())
                        .setScore(rawExamSubmittedAnswer != null ? rawExamSubmittedAnswer.getScore() : -1)
                        .setSubmittedAnswer((rawExamSubmittedAnswer == null
                                || rawExamSubmittedAnswer.getSubmittedAnswer() == null)
                                ? generateDefaultSubmittedAnswer(questionInfo)
                                : rawExamSubmittedAnswer.getSubmittedAnswer())
                        .build();
                map.put(examQuestionInfo.getIndex(), examSubmittedAnswer);
            }
        }
        return map;
    }
    
    public static Map<Integer, CourseWorkSubmittedAnswer> generateSubmittedCourseWorkAnswerMap(
            CourseWorkInfo courseWorkInfo, List<QuestionInfo> questionInfoList, 
            Map<Integer, CourseWorkSubmittedAnswer> submittedAnswerMap) {
        Map<Integer, CourseWorkSubmittedAnswer> map = new HashMap<>();
        if (courseWorkInfo == null || questionInfoList == null) {
            return map;
        }
        Map<Long, QuestionInfo> questionInfoMap = new HashMap<>();
        for (QuestionInfo questionInfo : questionInfoList) {
            questionInfoMap.put(questionInfo.getQuestionId(), questionInfo);
        }
        if (submittedAnswerMap == null) {
            submittedAnswerMap = new HashMap<>();
        }
        for (CourseWorkQuestionInfo courseWorkQuestionInfo : courseWorkInfo.getCourseWorkQuestionInfoList()) {
            QuestionInfo questionInfo = questionInfoMap.get(courseWorkQuestionInfo.getQuestionId());
            if (questionInfo != null) {
                CourseWorkSubmittedAnswer rawCourseWorkSubmittedAnswer = submittedAnswerMap.get(courseWorkQuestionInfo.getIndex());
                CourseWorkSubmittedAnswer courseWorkSubmittedAnswer = CourseWorkSubmittedAnswer.newBuilder()
                        .setQuestionIndex(courseWorkQuestionInfo.getIndex())
                        .setCheckStatus(rawCourseWorkSubmittedAnswer != null ? rawCourseWorkSubmittedAnswer.getCheckStatus() : -1)
                        .setSubmittedAnswer((rawCourseWorkSubmittedAnswer == null 
                                || rawCourseWorkSubmittedAnswer.getSubmittedAnswer() == null) 
                                ? generateDefaultSubmittedAnswer(questionInfo)
                                : rawCourseWorkSubmittedAnswer.getSubmittedAnswer())
                        .build();
                map.put(courseWorkQuestionInfo.getIndex(), courseWorkSubmittedAnswer);
            }
        }
        return map;
    }
    
    private static SubmittedAnswer generateDefaultSubmittedAnswer(QuestionInfo questionInfo) {
        QuestionType questionType = questionInfo.getQuestionType();
        if (questionType == QuestionType.SingleFilling) {
            return SubmittedAnswer.newBuilder()
                    .setSingleFillingSubmittedAnswer(SingleFillingSubmittedAnswer.getDefaultInstance())
                    .build();
        } else if (questionType == QuestionType.MultipleFilling) {
            Map<Integer, Pair> ans = new HashMap<>();
            if (questionInfo.getAnswer().hasMultipleFillingAnswer()) {
                MultipleFillingAnswer multipleFillingAnswer = questionInfo.getAnswer().getMultipleFillingAnswer();
                for (Entry<Integer, SingleFillingAnswer> entry : multipleFillingAnswer.getAnswerMap().entrySet()) {
                    Pair pair = Pair.newBuilder()
                            .setIndex(entry.getKey())
                            .setAnswer("")
                            .setScoreOrCheckStatus(-1)
                            .build();
                    ans.put(pair.getIndex(), pair);
                }
            }
            return SubmittedAnswer.newBuilder()
                    .setMultipleFillingSubmittedAnswer(MultipleFillingSubmittedAnswer.newBuilder().putAllAnswer(ans))
                    .build();
        } else if (questionType == QuestionType.SingleChoice) {
            return SubmittedAnswer.newBuilder()
                    .setSingleChoiceSubmittedAnswer(SingleChoiceSubmittedAnswer.getDefaultInstance())
                    .build();
        } else if (questionType == QuestionType.MultipleChoice) {
            return SubmittedAnswer.newBuilder()
                    .setMultipleChoiceSubmittedAnswer(MultipleChoiceSubmittedAnswer.getDefaultInstance())
                    .build();
        } else if (questionType == QuestionType.Essay) {
            return SubmittedAnswer.newBuilder()
                    .setEssaySubmittedAnswer(EssaySubmittedAnswer.getDefaultInstance())
                    .build();
        } else {
            return SubmittedAnswer.getDefaultInstance();
        }
    }

    public static int getCommentTypeRating(CourseCommentType courseCommentType) {
        if (courseCommentType == null) {
            return 0;
        }
        switch (courseCommentType) {
            case BAD: return 1;
            case JUST_MID: return 2;
            case MID: return 3;
            case GOOD: return 4;
            case VERY_GOOD: return 5;
        }
        return 0;
    }

    public static CourseCommentType generateCommentTypeFromRating(int rating) {
        switch (rating) {
            case 1: return CourseCommentType.BAD;
            case 2: return CourseCommentType.JUST_MID;
            case 3: return CourseCommentType.MID;
            case 4: return CourseCommentType.GOOD;
            case 5: return CourseCommentType.VERY_GOOD;
        }
        return CourseCommentType.MID;
    }

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

    public static Score generateScore(QuestionInfo questionInfo, double... score) {
        if (questionInfo.getQuestionType() != QuestionType.MultipleFilling) {
            if (score == null || score.length == 0 || score[0] < 0) {
                return Score.newBuilder().setSingleScore(0.0).build();
            } else {
                return Score.newBuilder().setSingleScore(score[0]).build();
            }
        }
        Map<Integer, Double> scoreMap = new HashMap<>();
        List<Integer> indexList = new ArrayList<>(questionInfo.getAnswer().getMultipleFillingAnswer().getAnswerMap().keySet());
        Collections.sort(indexList, (o1, o2) -> o1 - o2);
        for (int i = 0; i < indexList.size(); i++) {
            Integer index = indexList.get(i);
            if (i < score.length) {
                scoreMap.put(index, score[i]);
            } else {
                scoreMap.put(index, 0.0);
            }
        }
        return Score.newBuilder()
                .setMultipleScore(MultipleScore.newBuilder().putAllScore(scoreMap).build())
                .build();
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

    public static ExamSimpleInfo simplifyExamInfo(ExamInfo examInfo) {
        if (examInfo == null) {
            return null;
        }
        return ExamSimpleInfo.newBuilder()
                .setExamId(examInfo.getExamId())
                .setExamName(examInfo.getExamName())
                .setBelongCourseId(examInfo.getBelongCourseId())
                .setStartTime(examInfo.getStartTime())
                .setEndTime(examInfo.getEndTime())
                .setDuration(examInfo.getDuration())
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
