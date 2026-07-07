package _0.motovias_backend.service;

import _0.motovias_backend.dto.LoginRequest;
import _0.motovias_backend.dto.LoginResponse;
import _0.motovias_backend.dto.RegisterRequest;
import _0.motovias_backend.model.Role;
import _0.motovias_backend.model.User;
import _0.motovias_backend.repository.UserRepository;
import _0.motovias_backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public LoginResponse login(LoginRequest request) {
        // Capturamos la excepción ANTES de que llegue al ExceptionTranslationFilter.
        // Si BadCredentialsException escapa al filtro, Spring Security llama sendError(403),
        // el error dispatch no lleva CORS headers y el browser bloquea la respuesta.
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (DisabledException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cuenta deshabilitada");
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        // DaoAuthenticationProvider ya cargó el UserDetails al autenticar.
        // Lo extraemos del objeto Authentication para evitar una segunda consulta a la BD.
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // authorities tiene el prefijo "ROLE_" que agrega Spring Security en .roles()
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse(Role.USER.name());

        String token = jwtService.generateToken(rolClaim(role), userDetails);

        return new LoginResponse(token, userDetails.getUsername(), role);
    }

    public LoginResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El email ya está registrado: " + request.getEmail());
        }

        // El registro público jamás debe honrar un rol provisto por el cliente:
        // permitir eso habilitaría una escalada de privilegios trivial (auto-alta como ADMIN).
        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nombre(request.getNombre())
                .apellido(request.getApellido() != null ? request.getApellido() : "")
                .role(Role.USER)
                .activo(true)
                .tipoMotocicleta(request.getTipoMotocicleta())
                .build();

        userRepository.save(user);

        // Construimos UserDetails directamente del usuario guardado; sin nueva consulta.
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .disabled(!user.isActivo())
                .build();
        String token = jwtService.generateToken(rolClaim(user.getRole().name()), userDetails);
        return new LoginResponse(token, user.getEmail(), user.getRole().name());
    }

    // El claim "role" viaja en el JWT para que el frontend pueda derivar permisos
    // (ej. AdminGuard) sin depender de una consulta adicional al backend.
    private Map<String, Object> rolClaim(String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role);
        return claims;
    }
}
