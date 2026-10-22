/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.service;

import cn.zhuatech.srm.common.BusinessException;
import cn.zhuatech.srm.dto.AuthDto;
import cn.zhuatech.srm.model.UserAccount;
import cn.zhuatech.srm.repository.UserRepository;
import cn.zhuatech.srm.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 登录失败锁定、登录审计和密码变更。 */
@Service
public class AuthenticationService {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwt;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final int maxAttempts;
    private final int lockMinutes;

    public AuthenticationService(AuthenticationManager authenticationManager, JwtService jwt, UserRepository users,
                                 PasswordEncoder encoder, AuditService audit,
                                 @Value("${app.security.max-login-attempts:5}") int maxAttempts,
                                 @Value("${app.security.lock-minutes:15}") int lockMinutes) {
        this.authenticationManager = authenticationManager; this.jwt = jwt; this.users = users;
        this.encoder = encoder; this.audit = audit; this.maxAttempts = maxAttempts; this.lockMinutes = lockMinutes;
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public AuthDto.LoginResponse login(AuthDto.LoginRequest request) {
        UserAccount user = users.findByUsername(request.username()).orElse(null);
        if (user == null) throw new BadCredentialsException("用户名或密码错误");
        if (!user.isEnabled()) throw new BusinessException("账号已停用");
        if (user.isLocked()) throw new BusinessException("账号已临时锁定，请稍后重试");
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (BadCredentialsException exception) {
            user.recordLoginFailure(maxAttempts, lockMinutes);
            users.save(user);
            audit.record(user.getOrganizationCode(), user.getUsername(), "AUTH", "LOGIN_FAILED", "USER", user.getId(), null, null);
            throw new BadCredentialsException("用户名或密码错误");
        }
        user.recordLoginSuccess();
        users.save(user);
        audit.record(user.getOrganizationCode(), user.getUsername(), "AUTH", "LOGIN_SUCCESS", "USER", user.getId(), null, null);
        return new AuthDto.LoginResponse(jwt.generate(user), AuthDto.UserView.from(user));
    }

    @Transactional
    public void changePassword(UserAccount user, AuthDto.ChangePasswordRequest request) {
        if (!encoder.matches(request.currentPassword(), user.getPassword())) throw new BusinessException("当前密码不正确");
        if (encoder.matches(request.newPassword(), user.getPassword())) throw new BusinessException("新密码不能与当前密码相同");
        user.changePassword(encoder.encode(request.newPassword()));
        audit.record(user.getOrganizationCode(), user.getUsername(), "AUTH", "PASSWORD_CHANGED", "USER", user.getId(), null, null);
    }
}
