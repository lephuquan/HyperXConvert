package com.hyperxconvert.api.controller;

import com.hyperxconvert.api.entity.User;
import com.hyperxconvert.api.model.auth.AuthResponse;
import com.hyperxconvert.api.model.auth.LoginRequest;
import com.hyperxconvert.api.model.auth.RefreshTokenRequest;
import com.hyperxconvert.api.model.auth.RegisterRequest;
import com.hyperxconvert.api.security.JwtTokenProvider;
import com.hyperxconvert.api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        if (userService.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.badRequest().body("Email is already in use");
        }

        User user = userService.createUser(
                registerRequest.getEmail(),
                registerRequest.getPassword(),
                registerRequest.getFullName()
        );

        UserDetails userDetails = userService.loadUserByUsername(user.getEmail());
        String token = jwtTokenProvider.generateToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        return ResponseEntity.ok(new AuthResponse(token, refreshToken, user.getId()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        
        String token = jwtTokenProvider.generateToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);
        
        User user = userService.findByEmail(userDetails.getUsername());

        return ResponseEntity.ok(new AuthResponse(token, refreshToken, user.getId()));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.getRefreshToken();
        
        if (jwtTokenProvider.validateToken(refreshToken)) {
            String email = jwtTokenProvider.getUsernameFromToken(refreshToken);
            UserDetails userDetails = userService.loadUserByUsername(email);
            
            String newToken = jwtTokenProvider.generateToken(userDetails);
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(userDetails);
            
            User user = userService.findByEmail(email);
            
            return ResponseEntity.ok(new AuthResponse(newToken, newRefreshToken, user.getId()));
        }
        
        return ResponseEntity.badRequest().body("Invalid refresh token");
    }
}
