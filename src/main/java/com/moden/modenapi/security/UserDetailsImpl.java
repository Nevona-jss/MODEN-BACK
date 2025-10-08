package com.moden.modenapi.security;

import com.moden.modenapi.modules.auth.model.User;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.List;

public class UserDetailsImpl implements UserDetails {
    private final User user;
    public UserDetailsImpl(User user){ this.user = user; }

    @Override public List<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getUserType().name()));
    }
    @Override public String getPassword() { return null; }
    @Override public String getUsername() { return user.getPhone(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

    public User getUser(){ return user; }
}
