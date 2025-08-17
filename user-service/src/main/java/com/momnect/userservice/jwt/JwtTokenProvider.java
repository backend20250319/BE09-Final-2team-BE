package com.momnect.userservice.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

import com.momnect.userservice.command.entity.User;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long expiration; // Access token 유효시간

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration; // Refresh token 유효시간

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // AccessToken 생성
    public String createAccessToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setSubject(user.getLoginId())       // username(sub)으로 loginId 사용
                .claim("userId", user.getId())
                .claim("role", user.getRole())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    // RefreshToken 생성
    public String createRefreshToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .setSubject(user.getLoginId())
                .claim("userId", user.getId())
                .claim("role", user.getRole())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    // AccessToken 재발급 (RefreshToken 검증 후 사용)
    public String reissueAccessToken(String refreshToken) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(refreshToken)
                    .getBody();

            String loginId = claims.getSubject();
            Long userId = claims.get("userId", Long.class);
            String role = claims.get("role", String.class);

            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expiration);

            return Jwts.builder()
                    .setSubject(loginId)
                    .claim("userId", userId)
                    .claim("role", role)
                    .setIssuedAt(now)
                    .setExpiration(expiryDate)
                    .signWith(SignatureAlgorithm.HS256, secretKey)
                    .compact();

        } catch (JwtException | IllegalArgumentException e) {
            throw new RuntimeException("Invalid Refresh Token", e);
        }
    }
}
