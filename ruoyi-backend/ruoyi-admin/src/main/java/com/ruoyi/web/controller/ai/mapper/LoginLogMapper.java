package com.ruoyi.web.controller.ai.mapper;

import com.ruoyi.web.controller.ai.domain.LoginLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 登录日志Mapper接口
 *
 * @author ruoyi
 * @date 2026-04-25
 */
public interface LoginLogMapper
{
    /**
     * 查询登录日志
     *
     * @param logId 登录日志主键
     * @return 登录日志
     */
    public LoginLog selectLoginLogByLogId(Long logId);

    /**
     * 查询登录日志列表
     *
     * @param loginLog 登录日志
     * @return 登录日志集合
     */
    public List<LoginLog> selectLoginLogList(LoginLog loginLog);

    /**
     * 新增登录日志
     *
     * @param loginLog 登录日志
     * @return 结果
     */
    public int insertLoginLog(LoginLog loginLog);

    /**
     * 批量删除登录日志
     *
     * @param logIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteLoginLogByLogIds(Long[] logIds);

    /**
     * 清空登录日志
     *
     * @return 结果
     */
    public int cleanLoginLog();

    /**
     * 查询用户的登录日志
     *
     * @param userId 用户ID
     * @return 登录日志列表
     */
    public List<LoginLog> selectLoginLogByUserId(@Param("userId") Long userId);
}
