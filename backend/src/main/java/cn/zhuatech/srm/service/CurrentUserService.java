/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.service;

import cn.zhuatech.srm.common.BusinessException;
import cn.zhuatech.srm.model.Supplier;
import cn.zhuatech.srm.model.UserAccount;
import cn.zhuatech.srm.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** 当前登录用户及数据范围上下文。 */
@Service
public class CurrentUserService {
    private final UserRepository users;
    public CurrentUserService(UserRepository users) { this.users = users; }

    public UserAccount get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            throw new BusinessException("请先登录");
        }
        return users.findByUsername(authentication.getName()).orElseThrow(() -> new BusinessException("当前用户不存在"));
    }

    public Supplier requireSupplier() {
        Supplier supplier = get().getSupplier();
        if (supplier == null) throw new BusinessException("当前账号未关联供应商");
        return supplier;
    }

    public String organizationCode() { return get().getOrganizationCode(); }
    public boolean isSupplier() { return get().getRole() == UserAccount.Role.SUPPLIER; }
}
