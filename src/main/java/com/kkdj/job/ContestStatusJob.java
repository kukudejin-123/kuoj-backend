package com.kkdj.job;

import com.kkdj.service.ContestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 比赛状态定时任务
 * 每分钟检查比赛状态，自动更新
 */
@Component
@Slf4j
public class ContestStatusJob {

    @Resource
    private ContestService contestService;

    /**
     * 每分钟执行一次
     */
    @Scheduled(cron = "0 * * * * ?")
    public void updateContestStatus() {
        log.info("开始执行比赛状态检查任务");
        try {
            contestService.updateContestStatus();
            log.info("比赛状态检查任务执行完成");
        } catch (Exception e) {
            log.error("比赛状态检查任务执行失败", e);
        }
    }
}