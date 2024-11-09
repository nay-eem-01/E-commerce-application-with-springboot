package com.ecommerce.project.controller;

import com.ecommerce.project.Security.Request.LogInRequest;
import com.ecommerce.project.Security.Request.SignUpRequest;
import com.ecommerce.project.Security.Response.MessageResponse;
import com.ecommerce.project.Security.Response.UserResponse;
import com.ecommerce.project.Security.Services.UserDetailsImpl;
import com.ecommerce.project.Security.jwt.JwtUtils;
import com.ecommerce.project.model.APP_ROLE;
import com.ecommerce.project.model.Roles;
import com.ecommerce.project.model.Users;
import com.ecommerce.project.repositories.RoleRepository;
import com.ecommerce.project.repositories.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public AuthController(AuthenticationManager authenticationManager, JwtUtils jwtUtils, UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/signin")
    public ResponseEntity<?> userSignIn(@RequestBody LogInRequest logInRequest) {
        Authentication authentication;
        try {
            authentication = authenticationManager.
                    authenticate(new UsernamePasswordAuthenticationToken(logInRequest.getUserName(), logInRequest.getPassword()));
        } catch (AuthenticationException e) {
            Map<String, Object> map = new HashMap<>();
            map.put("message", "Bad credentials");
            map.put("status", false);
            return new ResponseEntity<Object>(map, HttpStatus.NOT_FOUND);
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        ResponseCookie jwtCookies = jwtUtils.generateJwtCookie(userDetails);
        List<String> roles = userDetails.getAuthorities().stream().map(item -> item.getAuthority()).toList();

        UserResponse userResponse = new UserResponse(userDetails.getId(), jwtCookies.toString(), userDetails.getUsername(), roles);

        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,
                        jwtCookies.toString())
                .body(userResponse);


    }

    @Transactional
    @PostMapping("/signup")
    public ResponseEntity<?> userSignUp(@Valid @RequestBody SignUpRequest signUpRequest) {

        if (userRepository.existsByUserName(signUpRequest.getUserName())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error : User name is already taken"));
        }
        if (userRepository.existsByUserEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body(new MessageResponse("Error : E-mail is already in use"));
        }
        Users users = new Users(signUpRequest.getUserName(), signUpRequest.getEmail(), passwordEncoder.encode(signUpRequest.getPassword()));
        Set<String> strRoles = signUpRequest.getRole();
        Set<Roles> roles = new HashSet<>();

        if (strRoles == null) {
            Roles userRole = roleRepository.findByRoleName(APP_ROLE.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Role not found!!!"));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                        switch (role) {
                            case "admin":
                                Roles adminRole = roleRepository.findByRoleName(APP_ROLE.ROLE_ADMIN)
                                        .orElseThrow(() -> new RuntimeException("Role not found!!!"));
                                roles.add(adminRole);
                                break;

                            case "seller":
                                Roles sellerRole = roleRepository.findByRoleName(APP_ROLE.ROLE_SELLER)
                                        .orElseThrow(() -> new RuntimeException("Role not found!!!"));
                                roles.add(sellerRole);
                                break;

                            default:
                                Roles userRole = roleRepository.findByRoleName(APP_ROLE.ROLE_USER)
                                        .orElseThrow(() -> new RuntimeException("Role not found!!!"));
                                roles.add(userRole);
                                break;
                        }
                    }

            );
        }
        users.setRoles(roles);
        userRepository.save(users);

        return ResponseEntity.ok(new MessageResponse("User registered successfully!"));
    }

    @GetMapping("/username")
    public String getCurrentUserName(Authentication authentication) {

        if (authentication != null) {
            return authentication.getName();
        } else {
            return "";
        }
    }

    @GetMapping("/userdetails")
    public ResponseEntity<?> getCurrentUserDetails(Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().
                stream().
                map(item -> item.getAuthority()).
                toList();
        UserResponse userResponse = new UserResponse(userDetails.getId(), userDetails.getUsername(), roles);
        return ResponseEntity.ok().body(userResponse);
    }

    @PostMapping("/signout")
    public ResponseEntity<?> signOut(HttpServletRequest request, HttpServletResponse response) {

        request.getSession().invalidate();

        ResponseCookie cookie = jwtUtils.getCleanJwtCookie();
        return ResponseEntity.ok().
                header(HttpHeaders.SET_COOKIE, cookie.toString()).
                body(new MessageResponse("You have been signed out"));
    }
}
