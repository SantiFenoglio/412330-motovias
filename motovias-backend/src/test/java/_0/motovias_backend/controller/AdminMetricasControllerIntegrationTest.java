package _0.motovias_backend.controller;

import _0.motovias_backend.dto.LoginRequest;
import _0.motovias_backend.dto.LoginResponse;
import _0.motovias_backend.dto.RegisterRequest;
import _0.motovias_backend.model.Role;
import _0.motovias_backend.model.User;
import _0.motovias_backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para /api/admin/metricas.
 *
 * <p>Escenarios cubiertos:
 * <ol>
 *   <li>Un usuario con rol USER golpea el endpoint → 403 Forbidden</li>
 *   <li>Un usuario con rol ADMIN se autentica → 200 OK con la estructura completa de métricas</li>
 * </ol>
 *
 * <p>La agregación geoespacial de "zonas con más actividad" usa ST_SnapToGrid, exclusivo de
 * PostGIS: bajo el perfil de test (H2) el servicio la degrada a una lista vacía (ver
 * AdminMetricasService), por lo que este test valida la presencia y el tipo del campo, no su
 * contenido geoespacial real (cubierto contra PostgreSQL con la colección de Postman).
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AdminMetricasControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String tokenUser;
    private String tokenAdmin;

    private static final String USER_EMAIL = "metricas-integ-user@motovias.com";
    private static final String USER_PASSWORD = "MetricasInteg2025!";
    private static final String ADMIN_EMAIL = "metricas-integ-admin@motovias.com";
    private static final String ADMIN_PASSWORD = "MetricasInteg2025!";

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        tokenUser = registrarYLoguear(USER_EMAIL, USER_PASSWORD);
        tokenAdmin = sembrarAdminYLoguear(ADMIN_EMAIL, ADMIN_PASSWORD);

        crearReporteComo(tokenUser);
    }

    @Test
    @DisplayName("GET /api/admin/metricas con rol USER → 403 Forbidden")
    void obtenerMetricas_conRolUser_retorna403() throws Exception {
        mockMvc.perform(get("/api/admin/metricas")
                        .header("Authorization", "Bearer " + tokenUser))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/metricas con rol ADMIN → 200 OK con estructura completa")
    void obtenerMetricas_conRolAdmin_retorna200ConEstructuraCompleta() throws Exception {
        mockMvc.perform(get("/api/admin/metricas")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalActivos").isNumber())
                .andExpect(jsonPath("$.reportesUltimaSemana").isNumber())
                .andExpect(jsonPath("$.usuariosNuevosMes").isNumber())
                .andExpect(jsonPath("$.reportesPorCategoria").isArray())
                .andExpect(jsonPath("$.zonasConMasActividad").isArray())
                .andExpect(jsonPath("$.totalActivos", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.reportesUltimaSemana", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.usuariosNuevosMes", greaterThanOrEqualTo(1)));
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String registrarYLoguear(String email, String password) throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail(email);
        reg.setPassword(password);
        reg.setNombre("Test");
        reg.setApellido("User");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andReturn();

        return loguear(email, password);
    }

    // El registro público fuerza siempre el rol USER (ver AuthService.register), por lo que
    // un usuario ADMIN para pruebas se siembra directamente en el repositorio. La inserción es
    // idempotente porque el mismo email se siembra en el @BeforeEach de cada test del bloque.
    private String sembrarAdminYLoguear(String email, String password) throws Exception {
        if (userRepository.findByEmail(email).isEmpty()) {
            User admin = User.builder()
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .nombre("Test")
                    .apellido("Admin")
                    .role(Role.ADMIN)
                    .activo(true)
                    .build();
            userRepository.save(admin);
        }

        return loguear(email, password);
    }

    private String loguear(String email, String password) throws Exception {
        LoginRequest login = new LoginRequest();
        login.setEmail(email);
        login.setPassword(password);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readValue(
                result.getResponse().getContentAsString(), LoginResponse.class
        ).getToken();
    }

    private void crearReporteComo(String token) throws Exception {
        mockMvc.perform(post("/api/reportes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "Bache en Av. Colón",
                                  "descripcion": "Pozo profundo en la mano derecha",
                                  "latitud": -31.4135,
                                  "longitud": -64.1811,
                                  "categoria": "PUNTO_INTERES"
                                }
                                """))
                .andExpect(status().isCreated());
    }
}
