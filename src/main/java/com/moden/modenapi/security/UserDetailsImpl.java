package com.moden.modenapi.security;

import com.moden.modenapi.common.enums.Role;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.customer.model.CustomerDetail;
import com.moden.modenapi.modules.designer.model.DesignerDetail;
import com.moden.modenapi.modules.studio.model.HairStudioDetail;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * ✅ Unified UserDetails wrapper.
 * - Role comes from User.role
 * - Detail entities are optional context holders (no role on them)
 */
public class UserDetailsImpl implements UserDetails {

    private final User user;
    private final CustomerDetail customerDetail;
    private final DesignerDetail designerDetail;
    private final HairStudioDetail studioDetail;

    public UserDetailsImpl(
            User user,
            CustomerDetail customerDetail,
            DesignerDetail designerDetail,
            HairStudioDetail studioDetail
    ) {
        this.user = user;
        this.customerDetail = customerDetail;
        this.designerDetail = designerDetail;
        this.studioDetail = studioDetail;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Role role = (user != null && user.getRole() != null) ? user.getRole() : Role.CUSTOMER;
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public Role getRole() {
        return (user != null && user.getRole() != null) ? user.getRole() : Role.CUSTOMER;
    }

    @Override
    public String getPassword() {
        // Password is managed by AuthLocalService; return empty to satisfy interface.
        return "";
    }

    @Override
    public String getUsername() {
        // Prefer phone if present; fallback to userId
        if (user == null) return "";
        return (user.getPhone() != null && !user.getPhone().isBlank())
                ? user.getPhone()
                : user.getId() != null ? user.getId().toString() : "";
    }

    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

    public User getUser() { return user; }
    public CustomerDetail getCustomerDetail() { return customerDetail; }
    public DesignerDetail getDesignerDetail() { return designerDetail; }
    public HairStudioDetail getStudioDetail() { return studioDetail; }
}
