package ch.bzz.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public final class JwtHandler {
    private static final String JWT_SECRET = "jwt.secret=...";
    private static final SecretKey JWT_KEY = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));

    private JwtHandler() {
    }

    public static String createJwt(String subject, Integer userId) {
        Date currentTime = new Date();
        Date expirationTime = new Date(currentTime.getTime() + 3_600_000);

        return Jwts.builder()
                .subject(subject)
                .claim("userId", userId)
                .issuedAt(currentTime)
                .expiration(expirationTime)
                .signWith(JWT_KEY)
                .compact();
    }

    public static Integer getUserId(String token) {
        return Jwts.parser()
                .verifyWith(JWT_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("userId", Integer.class);
    }
}