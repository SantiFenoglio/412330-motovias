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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Base64;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de integración para la carga de fotos de reportes ({@code /api/reportes/{id}/fotos}).
 *
 * <p>Escenarios cubiertos:
 * <ol>
 *   <li>Sin token → 401</li>
 *   <li>Subida válida → 200, la URL queda en el DTO de creación y en la lectura del reporte</li>
 *   <li>La imagen queda accesible por su URL pública servida desde /uploads/</li>
 *   <li>Más de 3 fotos asociadas al mismo reporte → 400</li>
 *   <li>Tipo MIME no soportado (ej. PDF) → 400</li>
 *   <li>Archivo que supera el tamaño máximo de 5MB → 400</li>
 *   <li>Usuario que no es dueño del reporte ni ADMIN → 403</li>
 * </ol>
 */
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class ReporteFotoIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext wac;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String validToken;

    private static final String TEST_EMAIL = "fotos-integ@motovias.com";
    private static final String TEST_PASSWORD = "FotosInteg2025!";

    // PNG válido de 1x1 píxel, usado como contenido de archivo en los tests.
    private static final byte[] PNG_1X1 = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk"
                    + "+A8AAQUBAScY42YAAAAASUVORK5CYII=");

    @BeforeEach
    void obtenerToken() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        validToken = registrarYObtenerToken(TEST_EMAIL, TEST_PASSWORD, "Fotos", "Integ");
    }

    @Test
    @DisplayName("POST /api/reportes/{id}/fotos sin token → 401")
    void subirFotos_sinToken_retorna401() throws Exception {
        Long reporteId = crearReporte("Reporte sin token");

        MockMultipartFile foto = new MockMultipartFile("fotos", "foto.png", MediaType.IMAGE_PNG_VALUE, PNG_1X1);

        mockMvc.perform(multipart("/api/reportes/{id}/fotos", reporteId).file(foto))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/reportes/{id}/fotos con token → 200 y la URL queda en el reporte")
    void subirFotos_conToken_actualizaListadoDeFotos() throws Exception {
        Long reporteId = crearReporte("Reporte con foto");

        MockMultipartFile foto = new MockMultipartFile("fotos", "foto.png", MediaType.IMAGE_PNG_VALUE, PNG_1X1);

        mockMvc.perform(multipart("/api/reportes/{id}/fotos", reporteId)
                        .file(foto)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotos", hasSize(1)))
                .andExpect(jsonPath("$.fotos[0]", containsString("/uploads/")));

        // La foto también debe verse reflejada al recuperar el punto de interés
        mockMvc.perform(get("/api/puntos-interes/{id}", reporteId)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotos", hasSize(1)));
    }

    @Test
    @DisplayName("La imagen subida queda accesible por su URL pública en /uploads/")
    void subirFotos_luegoRecuperaImagenPorUrlPublica() throws Exception {
        Long reporteId = crearReporte("Reporte con imagen recuperable");

        MockMultipartFile foto = new MockMultipartFile("fotos", "foto.png", MediaType.IMAGE_PNG_VALUE, PNG_1X1);

        MvcResult result = mockMvc.perform(multipart("/api/reportes/{id}/fotos", reporteId)
                        .file(foto)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andReturn();

        String url = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("fotos").get(0).asText();
        String path = url.substring(url.indexOf("/uploads/"));

        mockMvc.perform(get(path))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /api/reportes/{id}/fotos con más de 3 fotos en total → 400")
    void subirFotos_excedeLimiteDeTresFotos_retorna400() throws Exception {
        Long reporteId = crearReporte("Reporte con demasiadas fotos");

        MockMultipartFile foto1 = new MockMultipartFile("fotos", "f1.png", MediaType.IMAGE_PNG_VALUE, PNG_1X1);
        MockMultipartFile foto2 = new MockMultipartFile("fotos", "f2.png", MediaType.IMAGE_PNG_VALUE, PNG_1X1);
        MockMultipartFile foto3 = new MockMultipartFile("fotos", "f3.png", MediaType.IMAGE_PNG_VALUE, PNG_1X1);
        MockMultipartFile foto4 = new MockMultipartFile("fotos", "f4.png", MediaType.IMAGE_PNG_VALUE, PNG_1X1);

        mockMvc.perform(multipart("/api/reportes/{id}/fotos", reporteId)
                        .file(foto1).file(foto2).file(foto3).file(foto4)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Subir una cuarta foto cuando ya hay 3 previamente cargadas → 400")
    void subirFotos_excedeLimiteEnCargasSucesivas_retorna400() throws Exception {
        Long reporteId = crearReporte("Reporte con cargas sucesivas");

        for (int i = 0; i < 3; i++) {
            MockMultipartFile foto = new MockMultipartFile("fotos", "f" + i + ".png", MediaType.IMAGE_PNG_VALUE, PNG_1X1);
            mockMvc.perform(multipart("/api/reportes/{id}/fotos", reporteId)
                            .file(foto)
                            .header("Authorization", "Bearer " + validToken))
                    .andExpect(status().isOk());
        }

        MockMultipartFile cuartaFoto = new MockMultipartFile("fotos", "extra.png", MediaType.IMAGE_PNG_VALUE, PNG_1X1);
        mockMvc.perform(multipart("/api/reportes/{id}/fotos", reporteId)
                        .file(cuartaFoto)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/reportes/{id}/fotos con tipo MIME inválido → 400")
    void subirFotos_tipoMimeInvalido_retorna400() throws Exception {
        Long reporteId = crearReporte("Reporte con archivo inválido");

        MockMultipartFile archivo = new MockMultipartFile(
                "fotos", "archivo.pdf", MediaType.APPLICATION_PDF_VALUE, "contenido de prueba".getBytes());

        mockMvc.perform(multipart("/api/reportes/{id}/fotos", reporteId)
                        .file(archivo)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/reportes/{id}/fotos con archivo que supera 5MB → 400")
    void subirFotos_tamanoExcedido_retorna400() throws Exception {
        Long reporteId = crearReporte("Reporte con foto pesada");

        byte[] contenidoPesado = new byte[6 * 1024 * 1024]; // 6MB, por encima del límite de 5MB
        MockMultipartFile fotoPesada = new MockMultipartFile(
                "fotos", "pesada.jpg", MediaType.IMAGE_JPEG_VALUE, contenidoPesado);

        mockMvc.perform(multipart("/api/reportes/{id}/fotos", reporteId)
                        .file(fotoPesada)
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/reportes/{id}/fotos de un usuario que no es dueño del reporte → 403")
    void subirFotos_usuarioNoAutorizado_retorna403() throws Exception {
        Long reporteId = crearReporte("Reporte ajeno");

        String otroToken = registrarYObtenerToken(
                "intruso-fotos@motovias.com", "Intruso2025!", "Intruso", "Fotos");

        MockMultipartFile foto = new MockMultipartFile("fotos", "foto.png", MediaType.IMAGE_PNG_VALUE, PNG_1X1);

        mockMvc.perform(multipart("/api/reportes/{id}/fotos", reporteId)
                        .file(foto)
                        .header("Authorization", "Bearer " + otroToken))
                .andExpect(status().isForbidden());
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Long crearReporte(String titulo) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/reportes")
                        .header("Authorization", "Bearer " + validToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo": "%s",
                                  "descripcion": "Descripción de prueba",
                                  "latitud": -31.4135,
                                  "longitud": -64.1811,
                                  "categoria": "GOMERIA"
                                }
                                """.formatted(titulo)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String registrarYObtenerToken(String email, String password, String nombre, String apellido) throws Exception {
        RegisterRequest reg = new RegisterRequest();
        reg.setEmail(email);
        reg.setPassword(password);
        reg.setNombre(nombre);
        reg.setApellido(apellido);
        reg.setRole(Role.USER);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andReturn();

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
