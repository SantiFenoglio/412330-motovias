package _0.motovias_backend.controller;

import _0.motovias_backend.dto.LoginRequest;
import _0.motovias_backend.dto.LoginResponse;
import _0.motovias_backend.dto.RegisterRequest;
import _0.motovias_backend.model.Role;
import _0.motovias_backend.model.User;
import _0.motovias_backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Tests de integración para /api/admin/comercios y /api/comercios/activos.
 *
 * <p>Escenarios cubiertos:
 * <ol>
 *   <li>Un usuario con rol USER golpea los endpoints de admin → 403 Forbidden</li>
 *   <li>Un usuario con rol ADMIN recorre el ciclo CRUD completo (alta, edición,
 *       listado y baja lógica) → respuestas 2xx y datos consistentes</li>
 *   <li>El endpoint público /api/comercios/activos solo devuelve comercios activos</li>
 * </ol>
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AdminComercioControllerIntegrationTest {

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

    private static final String USER_EMAIL = "comercio-integ-user@motovias.com";
    private static final String USER_PASSWORD = "ComercioInteg2025!";
    private static final String ADMIN_EMAIL = "comercio-integ-admin@motovias.com";
    private static final String ADMIN_PASSWORD = "ComercioInteg2025!";

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        tokenUser = registrarYLoguear(USER_EMAIL, USER_PASSWORD);
        tokenAdmin = sembrarAdminYLoguear(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    @Test
    @DisplayName("POST /api/admin/comercios con rol USER → 403 Forbidden")
    void crear_conRolUser_retorna403() throws Exception {
        mockMvc.perform(post("/api/admin/comercios")
                        .header("Authorization", "Bearer " + tokenUser)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(comercioJson("Taller Don José", "GOMERIA")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/admin/comercios con rol USER → 403 Forbidden")
    void listar_conRolUser_retorna403() throws Exception {
        mockMvc.perform(get("/api/admin/comercios")
                        .header("Authorization", "Bearer " + tokenUser))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Ciclo CRUD completo con rol ADMIN: alta, listado, edición y baja lógica")
    void cicloCrudCompleto_conRolAdmin() throws Exception {
        // Alta
        MvcResult creado = mockMvc.perform(post("/api/admin/comercios")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(comercioJson("Gomería Central", "GOMERIA")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Gomería Central"))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.verificado").value(true))
                .andReturn();

        Long id = objectMapper.readTree(creado.getResponse().getContentAsString()).get("id").asLong();

        // Listado completo (visible para ADMIN)
        mockMvc.perform(get("/api/admin/comercios")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());

        // Edición
        mockMvc.perform(put("/api/admin/comercios/" + id)
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(comercioJson("Gomería Central Reformada", "TALLER_MECANICO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Gomería Central Reformada"))
                .andExpect(jsonPath("$.categoria").value("TALLER_MECANICO"));

        // Aparece en el mapa público mientras está activo
        mockMvc.perform(get("/api/comercios/activos")
                        .header("Authorization", "Bearer " + tokenUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());

        // Baja lógica
        mockMvc.perform(patch("/api/admin/comercios/" + id + "/estado")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "activo": false }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        // Deja de aparecer en el mapa público
        mockMvc.perform(get("/api/comercios/activos")
                        .header("Authorization", "Bearer " + tokenUser))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").doesNotExist());

        // Pero sigue visible en el listado completo de administración
        mockMvc.perform(get("/api/admin/comercios")
                        .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());
    }

    @Test
    @DisplayName("POST /api/admin/comercios con datos inválidos → 400 Bad Request")
    void crear_conDatosInvalidos_retorna400() throws Exception {
        mockMvc.perform(post("/api/admin/comercios")
                        .header("Authorization", "Bearer " + tokenAdmin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "",
                                  "direccion": "",
                                  "categoria": "GOMERIA",
                                  "latitud": 200,
                                  "longitud": -64.1811
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String comercioJson(String nombre, String categoria) {
        return """
                {
                  "nombre": "%s",
                  "direccion": "Av. Colón 1234, Córdoba",
                  "telefono": "351-1234567",
                  "categoria": "%s",
                  "latitud": -31.4135,
                  "longitud": -64.1811
                }
                """.formatted(nombre, categoria);
    }

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
}
