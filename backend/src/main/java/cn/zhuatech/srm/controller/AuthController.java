/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.controller;

import cn.zhuatech.srm.common.ApiResponse;
import cn.zhuatech.srm.dto.AuthDto;
import cn.zhuatech.srm.service.AuthenticationService;
import cn.zhuatech.srm.service.CurrentUserService;
import cn.zhuatech.srm.service.PermissionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationService authentication;
    private final CurrentUserService current;
    private final PermissionService permissions;
    public AuthController(AuthenticationService authentication, CurrentUserService current, PermissionService permissions) {
        this.authentication = authentication; this.current = current; this.permissions = permissions;
    }
    @PostMapping("/login")
    public ApiResponse<AuthDto.LoginResponse> login(@Valid @RequestBody AuthDto.LoginRequest request) {
        return ApiResponse.ok("登录成功", authentication.login(request));
    }
    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        return ApiResponse.ok(Map.of("user", AuthDto.UserView.from(current.get()), "permissions", permissions.currentPermissions()));
    }
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody AuthDto.ChangePasswordRequest request) {
        authentication.changePassword(current.get(), request);
        return ApiResponse.ok("密码已更新，请重新登录", null);
    }
}
