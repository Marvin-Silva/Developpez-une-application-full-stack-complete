package com.openclassrooms.mddapi.configuration;

import com.openclassrooms.mddapi.configuration.model.Token;
import com.openclassrooms.mddapi.configuration.security.RsaKeyProperties;
import com.openclassrooms.mddapi.model.User;
import com.openclassrooms.mddapi.model.dto.UserDto;
import com.openclassrooms.mddapi.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.nio.file.attribute.UserPrincipalNotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;


@Service
public class AuthService {
   private final JwtEncoder encoder;

   private final JwtDecoder decoder;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthenticationProvider authenticationProvider;

    private final RsaKeyProperties rsaKeys;

    public AuthService(JwtDecoder decoder, UserRepository userRepository, BCryptPasswordEncoder passwordEncoder, AuthenticationProvider authenticationProvider, JwtEncoder encoder, RsaKeyProperties rsaKeys){
        this.decoder = decoder;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationProvider = authenticationProvider;
        this.encoder = encoder;
        this.rsaKeys = rsaKeys;
    }

    // TODO: 22/09/2023 Login user
    public Token login(User user)  {

        try {
            if (user.getEmail().isEmpty()){
                throw new UserPrincipalNotFoundException("EMAIL NOT FOUND!!!");
            }

            User loguser = userRepository.findByEmail(user.getEmail());

            if (loguser == null){
                return null;
            }

            if (!passwordEncoder.matches(user.getPassword(), loguser.getPassword())){
                throw new UserPrincipalNotFoundException("USER CREDENTIALS NOT MATCH");
            }

            User isAuthenticated = User.builder()
                    .email(loguser.getEmail())
                    .password(loguser.getPassword())
                    .build();

            authenticationProvider.authenticate(new UsernamePasswordAuthenticationToken(isAuthenticated.getEmail(), isAuthenticated.getPassword()));

            String tokenGenerated = generateToken(loguser);

            Token tokenAuth = Token.builder()
                    .token(tokenGenerated)
                    .build();

            return tokenAuth;

        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }

    // TODO: 22/09/2023 Create user endPoint

    public User register(User user){

        if (user == null){
            return null;
        }

        User buildUser = User.builder()
                .username(user.getUsername())
                .lastname(user.getLastname())
                .email(user.getEmail())
                .password(passwordEncoder.encode(user.getPassword()))
                .build();
        try {
            if (buildUser.getEmail().isBlank() | buildUser.getUsername().isBlank() | buildUser.getPassword().isBlank()){
                throw new UserPrincipalNotFoundException("USER NOT VALID");
            }

            new UsernamePasswordAuthenticationToken(buildUser.getEmail(), buildUser.getPassword());

            userRepository.save(buildUser);

            return buildUser;
        }catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
    }
    public UserDto getMe() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication != null && authentication.getPrincipal() instanceof Jwt){
                    String email =  ((Jwt) authentication.getPrincipal()).getSubject();
                    User user = userRepository.findByEmail(email);

                return UserDto.builder()
                        .id_user(user.getId_user())
                        .email(user.getEmail())
                        .username(user.getUsername())
                        .lastname(user.getLastname())
                        .build();
            }
            return null;

    }

    public String generateToken(User user) {

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .subject(user.getEmail())
                .claim("scope", "")
                .build();

        // Signature
        return encoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

}
