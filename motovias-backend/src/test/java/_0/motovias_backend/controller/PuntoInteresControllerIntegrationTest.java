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
import org.springframework.http.MediaType;
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
 * Tests de integración para {@code /api/puntos-interes}.
 *
 * <p>Los tests de /cercanos que requieren ST_DWithin (función PostGIS) no se
 * ejecutan aquí porque H2 en memoria no implementa esa función nativa.
 * La validación funcional de la consulta espacial se cubre con la colección
 * Postman {@code postman/motovias-MOTOVIAS18-spatial.postman_collection.json}
 * contra el stack Docker (PostgreSQL + PostGIS).
 *
 * <p>Escenarios cubiertos:
 * <ol>
 *   <li>GET /api/puntos-interes sin token → 401</li>
 *   <li>GET /api/puntos-interes con token → 200 + array JSON</li>
 *   <li>POST /api/puntos-interes con token → 201 + DTO con id</li>
 *   <li>POST /api/puntos-interes sin token → 401</li>
 *   <li>GET /api/puntos-interes/cercanos sin token → 401 (no toca la BD)</li>
 * </ol>
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class PuntoInteresControllerIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext wac;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String validToken;

    private static final String TEST_EMAIL    = "puntos-integ@motovias.com";
    private static final String TEST_PASSWORD = "PuntosInteg2025!";

    @BeforeEach
    void obtenerToken() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        RegisterRequest reg = new RegisterRequest();
        reg.setEmail(TEST_EMAIL);
        reg.setPassword(TEST_PASSWORD);
        reg.setNombre("Puntos");
        reg.setApellido("Integ");
        reg.setRole(Role.USER);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andReturn();

        LoginRequest login = new LoginRequest();
        login.setEmail(TEST_EMAIL);
        login.setPassword(TEST_PASSWORD);

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        validToken = objectMapper.readValue(
                result.getResponse().getContentAsString(), LoginResponse.class
        ).getToken();
    }

    @Test
    @DisplayName("GET /api/puntos-interes sin token → 401")
    void listarTodos_sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/puntos-interes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/puntos-interes con token → 200 y array JSON")
    void listarTodos_conToken_retorna200() throws Exception {
        mockMvc.perform(get("/api/puntos-interes")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("POST /api/puntos-interes sin token → 401")
    void crear_sinToken_retorna401() throws Exception {
        mockMvc.perform(post("/api/puntos-interes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyPunto("Taller en Colón", "TALLER")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/puntos-interes con token → 201 con id y categoria")
    void crear_conToken_retorna201ConDTO() throws Exception {
        mockMvc.perform(post("/api/puntos-interes")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyPunto("Gomería Av. Colón", "GOMERIA")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.titulo").value("Gomería Av. Colón"))
                .andExpect(jsonPath("$.categoria").value("GOMERIA"))
                .andExpect(jsonPath("$.latitud").value(-31.4135))
                .andExpect(jsonPath("$.longitud").value(-64.1811));
    }

    @Test
    @DisplayName("POST + GET → punto creado aparece en el listado")
    void crear_luego_listar_incluyeElNuevoPunto() throws Exception {
        mockMvc.perform(post("/api/puntos-interes")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyPunto("Parador Vélez Sarsfield", "PUNTO_INTERES")))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/puntos-interes")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.titulo == 'Parador Vélez Sarsfield')]").exists());
    }

    @Test
    @DisplayName("GET /api/puntos-interes/cercanos sin token → 401 (sin llegar a la BD)")
    void cercanos_sinToken_retorna401() throws Exception {
        mockMvc.perform(get("/api/puntos-interes/cercanos")
                        .param("lat", "-31.4135")
                        .param("lon", "-64.1811")
                        .param("radio", "1000"))
                .andExpect(status().isUnauthorized());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private String bodyPunto(String titulo, String categoria) {
        return """
                {
                  "titulo": "%s",
                  "descripcion": "Descripción de prueba",
                  "latitud": -31.4135,
                  "longitud": -64.1811,
                  "categoria": "%s"
                }
                """.formatted(titulo, categoria);
    }
}
