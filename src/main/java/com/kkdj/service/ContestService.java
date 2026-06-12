package com.kkdj.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.kkdj.model.dto.contest.ContestAddRequest;
import com.kkdj.model.dto.contest.ContestQueryRequest;
import com.kkdj.model.dto.contest.ContestUpdateRequest;
import com.kkdj.model.entity.Contest;
import com.kkdj.model.entity.User;
import com.kkdj.model.vo.ContestVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 比赛服务
 */
public interface ContestService extends IService<Contest> {

    /**
     * 校验比赛数据
     */
    void validContest(Contest contest, boolean add);

    /**
     * 获取查询条件
     */
    QueryWrapper<Contest> getQueryWrapper(ContestQueryRequest contestQueryRequest);

    /**
     * 获取比赛VO
     */
    ContestVO getContestVO(Contest contest, HttpServletRequest request);

    /**
     * 分页获取比赛VO
     */
    Page<ContestVO> getContestVOPage(Page<Contest> contestPage, HttpServletRequest request);

    /**
     * 创建比赛
     */
    Long addContest(ContestAddRequest contestAddRequest, User loginUser);

    /**
     * 更新比赛
     */
    Boolean updateContest(ContestUpdateRequest contestUpdateRequest, User loginUser);

    /**
     * 检查用户是否可以报名比赛
     */
    boolean canJoinContest(Long userId);

    /**
     * 检查比赛是否正在进行
     */
    boolean isContestRunning(Long contestId);

    /**
     * 更新比赛状态
     */
    void updateContestStatus();

    /**
     * 获取比赛题目列表
     */
    List<ContestVO> getContestQuestions(Long contestId, HttpServletRequest request);
}