package com.kkdj.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.kkdj.model.dto.question.QuestionQueryRequest;
import com.kkdj.model.entity.Question;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kkdj.model.vo.QuestionAdminVo;
import com.kkdj.model.vo.QuestionVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author 19457
* @description 针对表【question(题目)】的数据库操作Service
* @createDate 2024-05-20 11:26:49
*/
public interface QuestionService extends IService<Question> {

    /**
     * 校验
     *
     * @param question
     * @param add
     */
    void validQuestion(Question question, boolean add);

    /**
     * 获取查询条件（用户端，只显示公开题目）
     *
     * @param questionQueryRequest
     * @return
     */
    QueryWrapper<Question> getQueryWrapper(QuestionQueryRequest questionQueryRequest);

    /**
     * 获取查询条件（管理员端，显示所有题目）
     *
     * @param questionQueryRequest
     * @return
     */
    QueryWrapper<Question> getAdminQueryWrapper(QuestionQueryRequest questionQueryRequest);
    

    /**
     * 获取题目封装
     *
     * @param question
     * @param request
     * @return
     */
    QuestionVO getQuestionVO(Question question, HttpServletRequest request);

    /**
     * 分页获取题目封装
     *
     * @param questionPage
     * @param request
     * @return
     */
    Page<QuestionVO> getQuestionVOPage(Page<Question> questionPage, HttpServletRequest request);

    Page<QuestionAdminVo> getQuestionAdminVOPage(Page<Question> questionPage, HttpServletRequest request);
}
