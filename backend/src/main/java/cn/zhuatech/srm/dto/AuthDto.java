/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.dto;

import cn.zhuatech.srm.model.UserAccount;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public final class AuthDto {
    private AuthDto() {}
    public record LoginRequest(@NotBlank(message = "请输入用户名") String username,
                               @NotBlank(message = "请输入密码") String password) {}
    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{12,72}$",
                                            message = "新密码至少 12 位并同时包含字母和数字") String newPassword) {}
    public record UserView(Long id, String username, String fullName, String role, String organizationCode,
                           Long supplierId, String supplierName) {
        public static UserView from(UserAccount user) {
            return new UserView(user.getId(), user.getUsername(), user.getFullName(), user.getRole().name(),
                user.getOrganizationCode(), user.getSupplier() == null ? null : user.getSupplier().getId(),
                user.getSupplier() == null ? null : user.getSupplier().getSupplierName());
        }
    }
    public record LoginResponse(String token, UserView user) {}
}
