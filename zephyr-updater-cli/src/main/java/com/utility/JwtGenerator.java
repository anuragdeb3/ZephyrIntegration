package com.utility;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

public class JwtGenerator {
    public static String generateJwt(String method, String uri, String baseUrl, String accessKey, String secretKey) {
        long expire = System.currentTimeMillis() + 3600000;
        Algorithm algorithm = Algorithm.HMAC256(secretKey);
        return JWT.create()
            .withClaim("sub", accessKey)
            .withClaim("qsh", QshCalculator.calculateQsh(method, uri))
            .withClaim("iss", accessKey)
            .withClaim("exp", expire / 1000)
            .sign(algorithm);
    }
}