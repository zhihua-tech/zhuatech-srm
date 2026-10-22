/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.security;

import cn.zhuatech.srm.model.UserAccount;
import cn.zhuatech.srm.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.ZoneId;

/** 每次请求重新核验账号启用、锁定和密码版本，避免旧 JWT 绕过账号状态。 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserDetailsService userDetailsService;
    private final UserRepository users;

    public JwtAuthenticationFilter(JwtService jwt, UserDetailsService userDetailsService, UserRepository users) {
        this.jwt = jwt;
        this.userDetailsService = userDetailsService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")
            && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                JwtService.TokenClaims claims = jwt.parse(header.substring(7));
                UserAccount account = users.findByUsername(claims.username())
                    .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
                long passwordVersion = account.getPasswordChangedAt() == null ? 0
                    : account.getPasswordChangedAt().withNano(0).atZone(ZoneId.systemDefault()).toEpochSecond();
                if (!account.isEnabled() || account.isLocked() || claims.passwordVersion() == null
                    || claims.passwordVersion() != passwordVersion) {
                    throw new JwtException("账号状态或密码版本已变化");
                }
                UserDetails user = userDetailsService.loadUserByUsername(claims.username());
                if (!user.isEnabled() || !user.isAccountNonLocked()) throw new JwtException("账号不可用");
                var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | UsernameNotFoundException exception) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
