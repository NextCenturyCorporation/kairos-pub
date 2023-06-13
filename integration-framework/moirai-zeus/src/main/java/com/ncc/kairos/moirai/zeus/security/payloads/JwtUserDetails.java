package com.ncc.kairos.moirai.zeus.security.payloads;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;

public class JwtUserDetails {
    private String username;
    private String[] authorities;
    private Boolean isCredentialsNonExpired;

    public JwtUserDetails() {
    }

    public JwtUserDetails(Authentication auth) {
        User details = (User) auth.getPrincipal();
        this.username = details.getUsername();
        this.authorities = (String[]) details.getAuthorities().stream()
				.map(item -> item.getAuthority().toString())
                .toArray(String[]::new);
        this.isCredentialsNonExpired = details.isCredentialsNonExpired();
    }
    
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String[] getAuthorities() {
        return authorities;
    }

    public void setAuthorities(String[] authorities) {
        this.authorities = authorities;
    }

    public Boolean getIsCredentialsNonExpired() {
        return isCredentialsNonExpired;
    }

    public void setIsCredentialsNonExpired(Boolean isCredentialsNonExpired) {
        this.isCredentialsNonExpired = isCredentialsNonExpired;
    }
}
