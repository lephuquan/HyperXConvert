package com.hyperxconvert.api.service;

import com.hyperxconvert.api.entity.Subscription;
import com.hyperxconvert.api.entity.User;
import com.hyperxconvert.api.repository.SubscriptionRepository;
import com.hyperxconvert.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = findByEmail(email);

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .accountExpired(false)
                .accountLocked(user.getStatus() == User.UserStatus.INACTIVE)
                .credentialsExpired(false)
                .disabled(user.getStatus() == User.UserStatus.INACTIVE)
                .build();
    }

    @Transactional
    public User createUser(String email, String password, String fullName) {
        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .fullName(fullName)
                .role(User.UserRole.USER)
                .status(User.UserStatus.ACTIVE)
                .loginProvider(User.LoginProvider.EMAIL)
                .build();

        user = userRepository.save(user);

        // Create a basic subscription for the new user
        Subscription subscription = Subscription.builder()
                .user(user)
                .planType(Subscription.PlanType.BASIC)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(30))
                .status(Subscription.SubscriptionStatus.ACTIVE)
                .autoRenew(false)
                .build();

        subscriptionRepository.save(subscription);

        return user;
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    @Transactional
    public User updateUser(User user) {
        return userRepository.save(user);
    }
}
