/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.config;

import cn.zhuatech.srm.model.UserAccount;
import cn.zhuatech.srm.model.Organization;
import cn.zhuatech.srm.repository.OrganizationRepository;
import cn.zhuatech.srm.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 显式启用的一次性管理员初始化。生产环境默认关闭，不提供演示账号或万能密码。
 */
@Configuration
public class BootstrapAdminInitializer {
    @Bean
    @ConditionalOnProperty(prefix = "app.bootstrap-admin", name = "enabled", havingValue = "true")
    CommandLineRunner bootstrapAdmin(UserRepository users, OrganizationRepository organizations, PasswordEncoder encoder,
                                     @Value("${app.bootstrap-admin.username}") String username,
                                     @Value("${app.bootstrap-admin.password}") String password,
                                     @Value("${app.bootstrap-admin.full-name:系统管理员}") String fullName,
                                     @Value("${app.bootstrap-admin.organization-code:DEFAULT}") String organizationCode) {
        return args -> {
            if (username.isBlank()) throw new IllegalStateException("BOOTSTRAP_ADMIN_USERNAME 不能为空");
            if (!password.matches("^(?=.*[A-Za-z])(?=.*\\d).{12,72}$") || password.equalsIgnoreCase("password")) {
                throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD 需要 12-72 位且同时包含字母和数字");
            }
            if (users.findByUsername(username).isEmpty()) {
                if (organizations.findByOrganizationCodeAndDeletedFalse(organizationCode).isEmpty()) {
                    organizations.save(new Organization(organizationCode, "默认采购组织"));
                }
                users.save(new UserAccount(username, encoder.encode(password), fullName,
                    UserAccount.Role.ADMIN, organizationCode, null));
            }
        };
    }
}
