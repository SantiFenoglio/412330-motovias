package _0.motovias_backend.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios para {@link JwtService}.
 *
 * <p>No levanta el contexto de Spring: crea las instancias manualmente
 * para máxima velocidad de ejecución y aislamiento total.
 *
 * <p>Escenarios cubiertos:
 * <ol>
 *   <li>generateToken: el subject del JWT es el username del UserDetails.</li>
 *   <li>isTokenValid: token recién generado → válido para el mismo usuario.</li>
 *   <li>isTokenValid: token de otro usuario → inválido (false).</li>
 *   <li>Token expirado: el parser de JJWT lanza {@link ExpiredJwtException}.</li>
 *   <li>Firma alterada: el parser lanza una excepción de seguridad.</li>
 * </ol>
 */
class JwtServiceTest {

    /**
     * Clave Base64 de 256 bits — idéntica a la de {@code application.properties}
     * para que los tests reflejen la configuración real de producción.
     */
    private static final String TEST_SECRET =
            "bW90b3ZpYXMtand0LXN1cGVyLXNlY3JldC1rZXktcGFyYS1obWFjLXNoYTI1Ni0yMDI1LW11c3QtYmUtYXQtbGVhc3QtMjU2LWJpdHM=";

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret(TEST_SECRET);
        props.setExpiration(86_400_000L); // 24 horas en milisegundos

        jwtService  = new JwtService(props);
        userDetails = User.withUsername("rider@motovias.com")
                .password("irrelevante-en-tests-unitarios")
                .authorities(Collections.emptyList())
                .build();
    }

    // ──────────────────────────────────────────────────────────────
    // 1. Generación y extracción de subject
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("generateToken → el subject del JWT coincide con el username")
    void generateToken_subjectMatchesUsername() {
        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.extractUsername(token))
                .isEqualTo("rider@motovias.com");
    }

    // ──────────────────────────────────────────────────────────────
    // 2. Validación positiva
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("isTokenValid → token recién generado es válido para el mismo usuario")
    void isTokenValid_freshToken_returnsTrue() {
        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    // ──────────────────────────────────────────────────────────────
    // 3. Validación con usuario incorrecto
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("isTokenValid → token de otro usuario retorna false")
    void isTokenValid_differentUser_returnsFalse() {
        String token = jwtService.generateToken(userDetails);

        UserDetails otroUsuario = User.withUsername("admin@motovias.com")
                .password("irrelevante")
                .authorities(Collections.emptyList())
                .build();

        assertThat(jwtService.isTokenValid(token, otroUsuario)).isFalse();
    }

    // ──────────────────────────────────────────────────────────────
    // 4. Token expirado
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Token expirado → extractUsername lanza ExpiredJwtException")
    void expiredToken_throwsExpiredJwtException() {
        // expiration negativa ⇒ el token ya expiró en el momento de creación
        JwtProperties expiredProps = new JwtProperties();
        expiredProps.setSecret(TEST_SECRET);
        expiredProps.setExpiration(-100_000L);

        JwtService expiredJwtService = new JwtService(expiredProps);
        String expiredToken = expiredJwtService.generateToken(userDetails);

        // JJWT 0.12 valida la expiración durante el parsing
        assertThatThrownBy(() -> expiredJwtService.extractUsername(expiredToken))
                .isInstanceOf(ExpiredJwtException.class);
    }

    // ──────────────────────────────────────────────────────────────
    // 5. Firma manipulada
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Firma alterada → extractUsername lanza excepción de seguridad JWT")
    void tamperedSignature_throwsJwtException() {
        String validToken   = jwtService.generateToken(userDetails);
        // Reemplazamos los últimos 4 caracteres de la firma con "XXXX"
        String tamperedToken = validToken.substring(0, validToken.length() - 4) + "XXXX";

        assertThatThrownBy(() -> jwtService.extractUsername(tamperedToken))
                .isInstanceOf(Exception.class) // JwtException o subclase (SignatureException, MalformedJwtException…)
                .hasMessageContaining(tamperedToken.contains("XXXX") ? "" : ""); // dispara la excepción
    }
}
