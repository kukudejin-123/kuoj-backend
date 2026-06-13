package com.kkdj.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kkdj.model.dto.contestsubmit.ContestSubmitAddRequest;
import com.kkdj.model.dto.contestsubmit.ContestSubmitQueryRequest;
import com.kkdj.model.entity.ContestSubmit;
import com.kkdj.model.entity.User;
import com.kkdj.model.vo.ContestSubmitVO;

import javax.servlet.http.HttpServletRequest;

/**
 * 比赛提交服务
 */
public interface ContestSubmitService extends IService<ContestSubmit> {

    /**
     * 获取查询条件
     */
    QueryWrapper<ContestSubmit> getQueryWrapper(ContestSubmitQueryRequest contestSubmitQueryRequest);

    /**
     * 获取比赛提交VO
     */
    ContestSubmitVO getContestSubmitVO(ContestSubmit contestSubmit, HttpServletRequest request);

    /**
     * 分页获取比赛提交VO
     */
    Page<ContestSubmitVO> getContestSubmitVOPage(Page<ContestSubmit> contestSubmitPage, HttpServletRequest request);

    /**
     * 提交比赛代码
     */
    Long doContestSubmit(ContestSubmitAddRequest contestSubmitAddRequest, User loginUser);

    /**
     * 获取比赛提交详情（包含代码隔离逻辑）
     */
    ContestSubmitVO getContestSubmitVOWithCode(ContestSubmit contestSubmit, User loginUser);
}