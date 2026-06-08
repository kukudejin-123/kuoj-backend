package com.kkdj.judge.service.impl;

import cn.hutool.json.JSONUtil;
import com.kkdj.common.ErrorCode;
import com.kkdj.exception.BusinessException;
import com.kkdj.judge.codeSandbox.CodeSandbox;
import com.kkdj.judge.codeSandbox.CodeSandboxFactory;
import com.kkdj.judge.codeSandbox.CodeSandboxProxy;
import com.kkdj.judge.codeSandbox.model.ExecuteCodeRequest;
import com.kkdj.judge.codeSandbox.model.ExecuteCodeResponse;
import com.kkdj.judge.service.JudgeService;
import com.kkdj.judge.strategy.JudgeContext;
import com.kkdj.judge.strategy.JudgeManager;
import com.kkdj.model.dto.question.JudgeCase;
import com.kkdj.judge.codeSandbox.model.JudgeInfo;
import com.kkdj.model.entity.Question;
import com.kkdj.model.entity.QuestionSubmit;
import com.kkdj.model.enums.QuestionSubmitStatusEnum;
import com.kkdj.service.QuestionService;
import com.kkdj.service.QuestionSubmitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JudgeServiceImpl implements JudgeService {

    @Resource
    private QuestionService questionService;

    @Resource
    private QuestionSubmitService questionSubmitService;

    @Value("${codesandbox.type:example}")
    private String type;

    @Resource
    private JudgeManager judgeManager;


    @Override
    public QuestionSubmit doJudge(long questionSubmitId) {
        // 1)传入题目的提交id，获取到对应的题目，提交信息
        QuestionSubmit questionSubmit = questionSubmitService.getById(questionSubmitId);
        if (questionSubmit == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "提交记录不存在");
        }

        Long questionId = questionSubmit.getQuestionId();
        Question question = questionService.getById(questionId);

        if (question == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "题目不存在");
        }

        // 2) 如果题目提交状态不为等待中，就不用重复执行了
        if (!questionSubmit.getStatus().equals(QuestionSubmitStatusEnum.WAITING.getValue())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目正在判题中");
        }

        // 3）更改判题状态（题目提交表单）的状态为判题中，防止重复执行
        QuestionSubmit questionSubmitUpdate = new QuestionSubmit();
        questionSubmitUpdate.setId(questionSubmitId);
        questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.RUNNING.getValue());
        boolean update = questionSubmitService.updateById(questionSubmitUpdate);
        if (!update) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目状态更新失败");
        }

        JudgeInfo judgeInfo = null;
        try {
            // 4）调用沙箱，获取到执行结果
            CodeSandbox codeSandbox = CodeSandboxFactory.newInstance(type);
            String code = questionSubmit.getCode();
            String language = questionSubmit.getLanguage();
            // 获取输入用例
            String judgeCaseStr = question.getJudgeCase();
            List<JudgeCase> judgeCaseList = JSONUtil.toList(judgeCaseStr, JudgeCase.class);
            List<String> inputList = judgeCaseList.stream().map(JudgeCase::getInput).collect(Collectors.toList());
            codeSandbox = new CodeSandboxProxy(codeSandbox);
            ExecuteCodeRequest excuteCodeRequest = ExecuteCodeRequest.builder()
                    .code(code)
                    .language(language)
                    .inputList(inputList)
                    .build();
            ExecuteCodeResponse excuteCodeResponse = codeSandbox.executeCode(excuteCodeRequest);
            List<String> outputList = excuteCodeResponse.getOutputList();
            // 5）根据代码沙箱执行结果，设置题目的判题状态和信息
            JudgeContext judgeContext = new JudgeContext();
            judgeContext.setJudgeInfo(excuteCodeResponse.getJudgeInfo());
            judgeContext.setInputList(inputList);
            judgeContext.setOutputList(outputList);
            judgeContext.setJudgeCaseList(judgeCaseList);
            judgeContext.setQuestion(question);
            judgeContext.setQuestionSubmit(questionSubmit);
            judgeInfo = judgeManager.doJudge(judgeContext);

            // 6）更新判题结果信息
            questionSubmitUpdate = new QuestionSubmit();
            questionSubmitUpdate.setId(questionSubmitId);
            questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.SUCCEED.getValue());
            questionSubmitUpdate.setJudgeInfo(JSONUtil.toJsonStr(judgeInfo));
            update = questionSubmitService.updateById(questionSubmitUpdate);
            if (!update) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "题目状态更新失败");
            }
        } catch (Exception e) {
            log.error("判题失败, submitId={}", questionSubmitId, e);
            questionSubmitUpdate = new QuestionSubmit();
            questionSubmitUpdate.setId(questionSubmitId);
            questionSubmitUpdate.setStatus(QuestionSubmitStatusEnum.FAILED.getValue());
            questionSubmitUpdate.setJudgeInfo(JSONUtil.toJsonStr(new JudgeInfo()));
            questionSubmitService.updateById(questionSubmitUpdate);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "判题失败: " + e.getMessage());
        }
        QuestionSubmit questionSubmitResult = questionSubmitService.getById(questionSubmitId);
        return questionSubmitResult;
    }
}
