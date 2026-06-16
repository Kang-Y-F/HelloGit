package com.neusoft.demo.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtil {

    // 密钥（后期可以放到配置文件）
    private static final String SECRET_KEY =
            "cloudBrainHospital2026SecureKey!@#$%^&*()_+";

    // token有效期（24小时）
    private static final long EXPIRE_TIME =
            24 * 60 * 60 * 1000;

    private static final SecretKey KEY = Keys.hmacShaKeyFor(
            SECRET_KEY.getBytes(StandardCharsets.UTF_8)
    );

    /**
     * 生成Token
     */
    public static String createToken(Long userId,
                                     String role) {

        return Jwts.builder()
                .claim("userId", userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .signWith(KEY)
                .compact();
    }

    /**
     * 解析Token
     */
    public static Claims parseToken(String token) {

        return Jwts.parser()
                .verifyWith(KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 判断Token是否有效
     */
    public static boolean validateToken(String token) {

        try {

            parseToken(token);

            return true;

        } catch (Exception e) {

            return false;

        }
    }

}
