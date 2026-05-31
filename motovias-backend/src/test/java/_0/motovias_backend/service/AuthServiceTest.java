package _0.motovias_backend.service;

import _0.motovias_backend.dto.LoginRequest;
import _0.motovias_backend.dto.LoginResponse;
import _0.motovias_backend.dto.RegisterRequest;
import _0.motovias_backend.model.Role;
import _0.motovias_backend.model.User;
import _0.motovias_backend.repository.UserRepository;
import _0.motovias_backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link AuthService}.
 *
 * <p>Todas las dependencias están mockeadas con Mockito; no se levanta
 * el contexto de Spring ni se accede a ninguna base de datos.
 *
 * <p>Escenarios cubiertos:
 * <ol>
 *   <li>Registro exitoso: email nuevo → persiste usuario y retorna LoginResponse con token.</li>
 *   <li>Registro duplicado: email ya existente → lanza {@code 409 CONFLICT}.</li>
 *   <li>Login exitoso: credenciales correctas → retorna LoginResponse con token.</li>
 *   <li>Login fallido: credenciales inválidas → lanza {@code 401 UNAUTHORIZED}.</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    // ── Dependencias mockeadas ────────────────────────────────────
    @Mock private UserRepository        userRepository;
    @Mock private PasswordEncoder       passwordEncoder;
    @Mock private JwtService            jwtService;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    // ── Fixtures reutilizables ────────────────────────────────────
    private static final String EMAIL    = "rider@motovias.com";
    private static final String PASSWORD = "Moto2025!";
    private static final String TOKEN    = "mocked.jwt.token";

    private UserDetails mockUserDetails;
    private User        mockUser;

    @BeforeEach
    void setUp() {
        mockUserDetails = org.springframework.security.core.userdetails.User
                .withUsername(EMAIL)
                .password("$2a$hashed")
                .authorities(Collections.emptyList())
                .build();

        mockUser = User.builder()
                .id(1L)
                .email(EMAIL)
                .password("$2a$hashed")
                .nombre("Rider")
                .apellido("Test")
                .role(Role.USER)
                .activo(true)
                .build();
    }

    // ──────────────────────────────────────────────────────────────
    // REGISTRO
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register: email nuevo → persiste usuario y retorna token en LoginResponse")
    void register_newEmail_persistsUserAndReturnsToken() {
        // Arrange
        RegisterRequest request = buildRegisterRequest();

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(PASSWORD)).thenReturn("$2a$hashed");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        // register() construye UserDetails desde el User guardado (sin llamar a userDetailsService)
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn(TOKEN);

        // Act
        LoginResponse response = authService.register(request);

        // Assert
        assertThat(response.getToken()).isEqualTo(TOKEN);
        assertThat(response.getEmail()).isEqualTo(EMAIL);
        assertThat(response.getRole()).isEqualTo(Role.USER.name());

        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode(PASSWORD);
        verify(jwtService).generateToken(any(UserDetails.class));
    }

    @Test
    @DisplayName("register: email duplicado → lanza ResponseStatusException 409 CONFLICT")
    void register_duplicateEmail_throws409Conflict() {
        // Arrange
        RegisterRequest request = buildRegisterRequest();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(mockUser));

        // Act & Assert
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        // No se debe persistir nada
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    // ──────────────────────────────────────────────────────────────
    // LOGIN
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login: credenciales válidas → autentica y retorna token en LoginResponse")
    void login_validCredentials_returnsTokenAndUserData() {
        // Arrange
        LoginRequest request = buildLoginRequest(EMAIL, PASSWORD);

        // authenticationManager retorna un Authentication cuyo principal es el UserDetails.
        // Así evitamos la segunda consulta a la BD que había antes.
        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(mockUserDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(jwtService.generateToken(mockUserDetails)).thenReturn(TOKEN);

        // Act
        LoginResponse response = authService.login(request);

        // Assert
        assertThat(response.getToken()).isEqualTo(TOKEN);
        assertThat(response.getEmail()).isEqualTo(EMAIL);
        assertThat(response.getRole()).isEqualTo(Role.USER.name());

        // El AuthenticationManager debe haberse invocado exactamente una vez
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken(mockUserDetails);
    }

    @Test
    @DisplayName("login: credenciales inválidas → lanza ResponseStatusException 401 UNAUTHORIZED")
    void login_invalidCredentials_throws401Unauthorized() {
        // Arrange
        LoginRequest request = buildLoginRequest(EMAIL, "clave-incorrecta");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));

        // No debe generarse token si la autenticación falló
        verifyNoInteractions(jwtService);
    }

    // ── Helpers de construcción de requests ──────────────────────

    private RegisterRequest buildRegisterRequest() {
        RegisterRequest r = new RegisterRequest();
        r.setEmail(EMAIL);
        r.setPassword(PASSWORD);
        r.setNombre("Rider");
        r.setApellido("Test");
        r.setRole(Role.USER);
        r.setTipoMotocicleta("NAKED");
        return r;
    }

    private LoginRequest buildLoginRequest(String email, String password) {
        LoginRequest r = new LoginRequest();
        r.setEmail(email);
        r.setPassword(password);
        return r;
    }
}
