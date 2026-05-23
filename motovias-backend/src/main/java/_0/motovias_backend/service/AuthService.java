package _0.motovias_backend.service;

import _0.motovias_backend.dto.LoginRequest;
import _0.motovias_backend.dto.LoginResponse;
import _0.motovias_backend.dto.RegisterRequest;
import _0.motovias_backend.model.Role;
import _0.motovias_backend.model.User;
import _0.motovias_backend.repository.UserRepository;
import _0.motovias_backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    public LoginResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String token = jwtService.generateToken(userDetails);
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        return new LoginResponse(token, user.getEmail(), user.getRole().name());
    }

    public LoginResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("El email ya está registrado: " + request.getEmail());
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .role(request.getRole() != null ? request.getRole() : Role.USER)
                .activo(true)
                .tipoMotocicleta(request.getTipoMotocicleta())
                .build();

        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);
        return new LoginResponse(token, user.getEmail(), user.getRole().name());
    }
}
