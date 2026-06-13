package com.kkdj.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.kkdj.mapper.ContestMapper;
import com.kkdj.mapper.ContestParticipantMapper;
import com.kkdj.model.entity.Contest;
import com.kkdj.model.entity.ContestParticipant;
import com.kkdj.service.ContestParticipantService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 比赛参赛者服务实现
 */
@Service
public class ContestParticipantServiceImpl extends ServiceImpl<ContestParticipantMapper, ContestParticipant> implements ContestParticipantService {

    @Resource
    private ContestMapper contestMapper;

    @Resource
    private ContestParticipantMapper contestParticipantMapper;

    @Override
    public boolean isUserJoined(Long contestId, Long userId) {
        return contestParticipantMapper.countByContestIdAndUserId(contestId, userId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean joinContest(Long contestId, Long userId) {
        // 检查是否已报名
        if (isUserJoined(contestId, userId)) {
            return false;
        }
        // 检查是否曾报名后取消
        QueryWrapper<ContestParticipant> wrapper = new QueryWrapper<>();
        wrapper.eq("contestId", contestId);
        wrapper.eq("userId", userId);
        ContestParticipant existing = this.getOne(wrapper);

        if (existing != null) {
            // 恢复已删除的记录
            existing.setIsDelete(0);
            existing.setJoinTime(new Date());
            return this.updateById(existing);
        } else {
            // 新增记录
            ContestParticipant participant = new ContestParticipant();
            participant.setContestId(contestId);
            participant.setUserId(userId);
            participant.setJoinTime(new Date());
            boolean result = this.save(participant);
            if (result) {
                // 更新参赛人数 - 直接使用Mapper避免循环依赖
                Contest contest = contestMapper.selectById(contestId);
                if (contest != null) {
                    UpdateWrapper<Contest> updateWrapper = new UpdateWrapper<>();
                    updateWrapper.eq("id", contestId);
                    updateWrapper.setSql("participantCount = participantCount + 1");
                    contestMapper.update(null, updateWrapper);
                }
            }
            return result;
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitContest(Long contestId, Long userId) {
        // 检查是否已报名
        if (!isUserJoined(contestId, userId)) {
            return false;
        }
        QueryWrapper<ContestParticipant> wrapper = new QueryWrapper<>();
        wrapper.eq("contestId", contestId);
        wrapper.eq("userId", userId);
        boolean result = this.remove(wrapper);
        if (result) {
            // 更新参赛人数 - 直接使用Mapper避免循环依赖
            Contest contest = contestMapper.selectById(contestId);
            if (contest != null && contest.getParticipantCount() > 0) {
                UpdateWrapper<Contest> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("id", contestId);
                updateWrapper.setSql("participantCount = participantCount - 1");
                contestMapper.update(null, updateWrapper);
            }
        }
        return result;
    }
}