package com.ncc.kairos.moirai.zeus.security.utils;

import com.google.gson.Gson;
import com.ncc.kairos.moirai.zeus.security.payloads.JwtUserDetails;
import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

import java.util.Calendar;
import java.util.Date;

@Component
public class JwtUtils {

    @Value("${jwt.token.secret}")
    private String jwtSecret;

    @Value("${jwt.expire.minutes}")
    private int jwtExpirationMn;

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    public String generateJwtToken(Authentication authentication) {
        return generateJwtToken(new JwtUserDetails(authentication));
	}

	public String generateJwtToken(JwtUserDetails jwtUserDetails) {
		Calendar cl = Calendar.getInstance();
		cl.setTime(new Date());
		cl.add(Calendar.MINUTE, jwtExpirationMn);
		return Jwts.builder()
				.setSubject(new Gson().toJson(jwtUserDetails))
				.setIssuedAt(new Date())
				.setExpiration(cl.getTime())
				.signWith(SignatureAlgorithm.HS512, jwtSecret)
				.compact();
	}

	public  String updateJwtTokenExpiration() {
		return generateJwtToken(SecurityContextHolder.getContext().getAuthentication());
	}

	public String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7, headerAuth.length());
        }

        return null;
    }


	public String getSubjectFromJwtToken(String token) {
		return Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(token).getBody().getSubject();
	}

	public boolean validateJwtToken(String authToken) {
		try {
			Jwts.parser().setSigningKey(jwtSecret).parseClaimsJws(authToken);
			return true;
		} catch (SignatureException e) {
			logger.error("Invalid JWT signature: {}", e.getMessage());
		} catch (MalformedJwtException e) {
			logger.error("Invalid JWT token: {}", e.getMessage());
		} catch (ExpiredJwtException e) {
			logger.error("JWT token is expired: {}", e.getMessage());
		} catch (UnsupportedJwtException e) {
			logger.error("JWT token is unsupported: {}", e.getMessage());
		} catch (IllegalArgumentException e) {
			logger.error("JWT claims string is empty: {}", e.getMessage());
		}

		return false;
	}
}
