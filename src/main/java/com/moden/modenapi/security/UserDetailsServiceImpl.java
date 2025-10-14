package com.moden.modenapi.security;

import com.moden.modenapi.modules.auth.model.User;
import com.moden.modenapi.modules.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepo;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        try {
            UUID uuid = UUID.fromString(userId);
            User user = userRepo.findById(uuid)
                    .orElseThrow(() ->
                            new UsernameNotFoundException("User not found with id: " + userId));
            return new UserDetailsImpl(user);

        } catch (IllegalArgumentException e) {
            // Token subject wasn’t a valid UUID → optional fallback (find by phone)
            var user = userRepo.findByPhone(userId)
                    .orElseThrow(() ->
                            new UsernameNotFoundException("Invalid UUID and no user found by phone: " + userId));
            return new UserDetailsImpl(user);
        }
    }
}
