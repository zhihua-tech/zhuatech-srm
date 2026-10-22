/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.security;

import cn.zhuatech.srm.model.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

/** 签发短期 JWT，并携带密码版本以便修改密码后立即使旧令牌失效。 */
@Service
public class JwtService {
    private final SecretKey key;
    private final Duration expiration;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration:PT8H}") Duration expiration) {
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generate(UserAccount user) {
        Date now = new Date();
        long passwordVersion = user.getPasswordChangedAt() == null ? 0 : user.getPasswordChangedAt().withNano(0)
            .atZone(java.time.ZoneId.systemDefault()).toEpochSecond();
        return Jwts.builder().subject(user.getUsername()).claim("pwd", passwordVersion).issuedAt(now)
            .expiration(new Date(now.getTime() + expiration.toMillis())).signWith(key).compact();
    }

    public TokenClaims parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new TokenClaims(claims.getSubject(), claims.get("pwd", Long.class));
    }

    public record TokenClaims(String username, Long passwordVersion) {}
}
