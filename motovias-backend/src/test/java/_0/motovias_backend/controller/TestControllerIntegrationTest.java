package _0.motovias_backend.controller;

import _0.motovias_backend.dto.LoginRequest;
import _0.motovias_backend.dto.LoginResponse;
import _0.motovias_backend.dto.RegisterRequest;
import _0.motovias_backend.model.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

// ObjectMapper se instancia como POJO: no depende de JacksonAutoConfiguration
// (que puede no estar garantizada en @SpringBootTest sin @AutoConfigureMockMvc).

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de integración del endpoint {@code GET /api/v1/test/protected}.
 *
 * <p>Levanta el contexto completo de Spring Boot con la base de datos H2
 * en memoria (perfil "test") para validar el flujo real de seguridad:
 *
 * <pre>
 *   Request
 *     → CorsFilter
 *     → JwtAuthenticationFilter  ← verifica y carga el SecurityContext
 *     → AuthorizationFilter      ← rechaza si no hay autenticación
 *     → TestController
 * </pre>
 *
 * <p>Escenarios cubiertos:
 * <ol>
 *   <li>Sin header Authorization → 401 Unauthorized (entry point custom).</li>
 *   <li>Con token JWT válido → 200 OK con datos del usuario autenticado.</li>
 *   <li>Con token completamente inválido → 401 Unauthorized.</li>
 *   <li>Con token manipulado (firma alterada) → 401 Unauthorized.</li>
 * </ol>
 *
 * <p><b>Validación manual en Postman:</b>
 * <ol>
 *   <li>POST {@code /api/auth/register} con body JSON → obtener token.</li>
 *   <li>GET  {@code /api/v1/test/protected} sin header → 401.</li>
 *   <li>GET  {@code /api/v1/test/protected} con {@code Authorization: Bearer <token>} → 200.</li>
 *   <li>Modificar cualquier carácter del token → 401.</li>
 * </ol>
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class TestControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext wac;

    /**
     * ObjectMapper instanciado como POJO puro — no via @Autowired.
     *
     * Por qué: @SpringBootTest sin @AutoConfigureMockMvc no garantiza que
     * JacksonAutoConfiguration haya registrado el bean ObjectMapper antes de
     * que Spring intente inyectar la clase de test. Instanciar directamente
     * evita la dependencia del ciclo de vida del contexto y es el patrón
     * recomendado para clases de utilidad sin estado Spring.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Token JWT válido obtenido en cada setup antes de cada test. */
    private String validToken;

    private static final String TEST_EMAIL    = "integ@motovias.com";
    private static final String TEST_PASSWORD = "IntegTest2025!";

    @BeforeEach
    void obtenerTokenValido() throws Exception {
        // Inicializamos MockMvc con seguridad para que los filtros se apliquen
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        // Intentar registrar al usuario (el 409 en repetición es esperado y se ignora)
        RegisterRequest register = new RegisterRequest();
        register.setEmail(TEST_EMAIL);
        register.setPassword(TEST_PASSWORD);
        register.setNombre("Integracion");
        register.setApellido("Test");
        register.setRole(Role.USER);
        register.setTipoMotocicleta("SPORT");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andReturn(); // 201 en primera ejecución, 409 en siguientes — ambos aceptables

        // Login para obtener token fresco
        LoginRequest login = new LoginRequest();
        login.setEmail(TEST_EMAIL);
        login.setPassword(TEST_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                LoginResponse.class
        );
        validToken = loginResponse.getToken();
    }

    // ──────────────────────────────────────────────────────────────
    // Test 1: Sin token → 401
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Sin header Authorization → 401 Unauthorized")
    void sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/v1/test/protected"))
                .andExpect(status().isUnauthorized());
    }

    // ──────────────────────────────────────────────────────────────
    // Test 2: Token válido → 200 con datos del usuario
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Token JWT válido → 200 OK con email del usuario autenticado")
    void conTokenValido_retorna200ConDatosUsuario() throws Exception {
        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Acceso autorizado"))
                .andExpect(jsonPath("$.user").value(TEST_EMAIL))
                .andExpect(jsonPath("$.status").value("OK"));
    }

    // ──────────────────────────────────────────────────────────────
    // Test 3: Token inválido (no parseable) → 401
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Token completamente inválido → 401 Unauthorized")
    void conTokenInvalido_retorna401() throws Exception {
        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Bearer token.completamente.invalido"))
                .andExpect(status().isUnauthorized());
    }

    // ──────────────────────────────────────────────────────────────
    // Test 4: Token manipulado (firma alterada) → 401
    // ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Token con firma alterada → 401 Unauthorized (SignatureException capturada)")
    void conTokenManipulado_retorna401() throws Exception {
        // Reemplazamos los últimos 4 caracteres de la firma por "ZZZZ"
        String tokenManipulado = validToken.substring(0, validToken.length() - 4) + "ZZZZ";

        mockMvc.perform(get("/api/v1/test/protected")
                        .header("Authorization", "Bearer " + tokenManipulado))
                .andExpect(status().isUnauthorized());
    }
}
