package com.moden.modenapi.security;

import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import com.moden.modenapi.modules.customer.model.CustomerDetail;
import com.moden.modenapi.modules.customer.repository.CustomerDetailRepository;
import com.moden.modenapi.modules.designer.model.DesignerDetail;
import com.moden.modenapi.modules.designer.repository.DesignerDetailRepository;
import com.moden.modenapi.modules.studio.model.HairStudioDetail;
import com.moden.modenapi.modules.studio.repository.HairStudioDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * ✅ Unified UserDetailsService supporting multiple roles
 * - Loads User by UUID (preferred) or phone
 * - Dynamically attaches role details (Customer, Designer, Studio)
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    private final CustomerDetailRepository customerRepository;
    private final DesignerDetailRepository designerRepository;
    private final HairStudioDetailRepository studioRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user;

        try {
            // 🔹 Case 1: username is UUID
            UUID userId = UUID.fromString(username);
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + username));
        } catch (IllegalArgumentException e) {
            // 🔹 Case 2: username is phone number
            user = userRepository.findByPhone(username)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with phone: " + username));
        }

        UUID userId = user.getId();

        // 🔹 Attach possible role details
        CustomerDetail customerDetail = findCustomerDetail(userId);
        DesignerDetail designerDetail = findDesignerDetail(userId);
        HairStudioDetail studioDetail = findStudioDetail(userId);

        // 🔹 Build unified UserDetails
        return new UserDetailsImpl(user, customerDetail, designerDetail, studioDetail);
    }

    // 🧩 Lookup helpers
    private CustomerDetail findCustomerDetail(UUID userId) {
        return customerRepository.findByUserId(userId).orElse(null);
    }

    private DesignerDetail findDesignerDetail(UUID userId) {
        return designerRepository.findByUserId(userId).orElse(null);
    }

    private HairStudioDetail findStudioDetail(UUID userId) {
        return studioRepository.findByUserIdAndDeletedAtIsNull(userId).orElse(null);
    }
}
