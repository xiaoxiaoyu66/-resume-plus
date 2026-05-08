package com.ruoyi.web.controller.ai;

import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.system.service.ISysUserService;
import com.ruoyi.web.controller.ai.domain.InternProfile;
import com.ruoyi.web.controller.ai.service.InternProfileService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.Map;

/**
 * 实习档案控制器
 */
@RestController
@RequestMapping("/ai/profile")
public class ProfileController {

    @Resource
    private InternProfileService internProfileService;

    @Resource
    private ISysUserService sysUserService;

    /**
     * 获取当前用户档案 - 默认路径
     */
    @GetMapping("")
    public AjaxResult getProfile() {
        Long userId = SecurityUtils.getUserId();
        InternProfile profile = internProfileService.getProfileByUserId(userId);
        return AjaxResult.success(profile);
    }

    /**
     * 获取当前用户档案 - 兼容路径
     */
    @GetMapping("/get")
    public AjaxResult getProfileByPath() {
        return getProfile();
    }

    /**
     * 保存/更新档案（同步姓名/手机/邮箱到系统用户）
     */
    @PostMapping("/save")
    public AjaxResult saveProfile(@RequestBody Map<String, Object> data) {
        Long userId = SecurityUtils.getUserId();
        try {
            internProfileService.saveProfile(userId, data);

            // 同步姓名/手机/邮箱到 sys_user，保持数据一致
            String name = (String) data.get("name");
            String phone = (String) data.get("phone");
            String email = (String) data.get("email");
            if (name != null || phone != null || email != null) {
                SysUser sysUser = new SysUser();
                sysUser.setUserId(userId);
                sysUser.setNickName(name);
                sysUser.setPhonenumber(phone);
                sysUser.setEmail(email);
                sysUserService.updateUserProfile(sysUser);
            }

            return AjaxResult.success("保存成功");
        } catch (Exception e) {
            return AjaxResult.error("保存失败: " + e.getMessage());
        }
    }
}
