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

    /**
     * 토큰 유효성 검증
     *
     * @param token 검증할 JWT 토큰
     * @return 토큰이 유효하면 true, 유효하지 않으면 false
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 토큰에서 사용자 ID 추출
     *
     * @param token JWT 토큰
     * @return 토큰에 포함된 사용자 ID
     * @throws RuntimeException 토큰이 유효하지 않은 경우
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("userId", Long.class);
    }

    /**
     * 토큰에서 사용자 역할 추출
     *
     * @param token JWT 토큰
     * @return 토큰에 포함된 사용자 역할 (USER 등)
     * @throws RuntimeException 토큰이 유효하지 않은 경우
     */
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("role", String.class);
    }

    /**
     * Access Token 생성
     *
     * @param user 토큰을 생성할 사용자 엔티티
     * @return 생성된 Access Token 문자열
     */
    public String createAccessToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(user.getLoginId())  // setSubject → subject)
                .claim("userId", user.getId())
                .claim("role", user.getRole())
                .claim("name", user.getName())
                .claim("oauthProvider", user.getOauthProvider())
                .issuedAt(now)  // (setIssuedAt → issuedAt)
                .expiration(expiryDate)  //(setExpiration → expiration)
                .signWith(secretKey)  // (signWith(algorithm, key) → signWith(key))
                .compact();
    }

    /**
     * Refresh Token 생성
     *
     * @param user 토큰을 생성할 사용자 엔티티
     * @return 생성된 Refresh Token 문자열
     */
    public String createRefreshToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .subject(user.getLoginId())
                .claim("userId", user.getId())
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Refresh Token을 이용한 Access Token 재발급
     *
     * @param refreshToken 유효한 Refresh Token
     * @return 새로 생성된 Access Token
     * @throws RuntimeException Refresh Token이 유효하지 않은 경우
     */
    public String reissueAccessToken(String refreshToken) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(refreshToken)
                    .getPayload();

            String loginId = claims.getSubject();
            Long userId = claims.get("userId", Long.class);
            String role = claims.get("role", String.class);
            String name = claims.get("name", String.class);
            String oauthProvider = claims.get("oauthProvider", String.class);

            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expiration);

            return Jwts.builder()
                    .subject(loginId)
                    .claim("userId", userId)
                    .claim("role", role)
                    .claim("name", name)
                    .claim("oauthProvider", oauthProvider)
                    .issuedAt(now)
                    .expiration(expiryDate)
                    .signWith(secretKey)
                    .compact();

        } catch (JwtException | IllegalArgumentException e) {
            throw new RuntimeException("Invalid Refresh Token", e);
        }
    }
}