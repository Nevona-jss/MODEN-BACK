package com.moden.modenapi.security;

import com.moden.modenapi.common.enums.Role;
import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.customer.model.CustomerDetail;
import com.moden.modenapi.modules.designer.model.DesignerDetail;
import com.moden.modenapi.modules.studio.model.HairStudioDetail;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.List;

/**
 * ✅ Unified UserDetails wrapper supporting multiple role sources.
 * Role is determined based on which detail entity is linked.
 */
public class UserDetailsImpl implements UserDetails {

    private final User user;
    private final CustomerDetail customerDetail;
    private final DesignerDetail designerDetail;
    private final HairStudioDetail studioDetail;

    public UserDetailsImpl(User user,
                           CustomerDetail customerDetail,
                           DesignerDetail designerDetail,
                           HairStudioDetail studioDetail) {
        this.user = user;
        this.customerDetail = customerDetail;
        this.designerDetail = designerDetail;
        this.studioDetail = studioDetail;
    }

    @Override
    public List<? extends GrantedAuthority> getAuthorities() {
        Role role = resolveRole();
        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        return authorities;
    }

    private Role resolveRole() {
        if (studioDetail != null && studioDetail.getRole() != null)
            return studioDetail.getRole();
        if (designerDetail != null && designerDetail.getRole() != null)
            return designerDetail.getRole();
        if (customerDetail != null && customerDetail.getRole() != null)
            return customerDetail.getRole();
        // fallback (should not happen)
        return Role.CUSTOMER;
    }

    @Override public String getPassword() { return null; }  // You handle password via AuthLocal
    @Override public String getUsername() { return user.getPhone(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }

    public User getUser() { return user; }
    public Role getRole() { return resolveRole(); }

    public CustomerDetail getCustomerDetail() { return customerDetail; }
    public DesignerDetail getDesignerDetail() { return designerDetail; }
    public HairStudioDetail getStudioDetail() { return studioDetail; }
}
