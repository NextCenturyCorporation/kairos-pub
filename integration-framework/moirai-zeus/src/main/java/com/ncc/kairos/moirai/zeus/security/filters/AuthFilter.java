package com.ncc.kairos.moirai.zeus.security.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncc.kairos.moirai.zeus.model.JwtUser;
import com.ncc.kairos.moirai.zeus.resources.Constants;
import com.ncc.kairos.moirai.zeus.security.payloads.JwtUserDetails;
import com.ncc.kairos.moirai.zeus.security.utils.JwtUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;


/**
 * This class implements a web filter that is applied to end points needing a jwt but with no specific permissions.
 * This filter checks to see if the request includes a cookie holding a valid jwt with a reset permission.
 * Without a valid jwt the request is automatically denied.
 * @author ryan scott
 * @version 0.1
 */
public class AuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;
    
    private static final Logger logger = LoggerFactory.getLogger(AuthFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws ServletException, IOException {
        try {
            String jwt = jwtUtils.parseJwt(request);
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                String json = jwtUtils.getSubjectFromJwtToken(jwt);
                JwtUserDetails details = new ObjectMapper().readValue(json, JwtUserDetails.class);

                Collection<GrantedAuthority> authorities = Arrays.stream(details.getAuthorities())
                        .map(item -> new SimpleGrantedAuthority(item))
                        .collect(Collectors.toList());
                User user = new User(details.getUsername(), "", authorities);

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null, authorities);
                // Setting username in requests which is what we use to get rest of user data
                request.setAttribute(Constants.ATTRIBUTE_JWTUSER, new JwtUser().username(details.getUsername()));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception e) {
            logger.error("Cannot set user authentication: {}", e);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    public void destroy() {
    }

}
