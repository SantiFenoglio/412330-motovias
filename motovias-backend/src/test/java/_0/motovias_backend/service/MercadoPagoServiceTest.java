package _0.motovias_backend.service;

import _0.motovias_backend.controller.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class MercadoPagoServiceTest {

    @Test
    @DisplayName("crearPreferenciaPago sin access token configurado lanza IllegalStateException")
    void crearPreferenciaPago_sinAccessToken_lanzaIllegalStateException() {
        MercadoPagoService service = new MercadoPagoService();
        ReflectionTestUtils.setField(service, "accessToken", "");

        assertThatThrownBy(() -> service.crearPreferenciaPago(
                new BigDecimal("100.00"), "Viaje Test", "deudor@test.com", "acreedor@test.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ACCESS_TOKEN de Mercado Pago no está configurado");
    }

    @Test
    @DisplayName("GlobalExceptionHandler mapea la falta de access token a 503 Service Unavailable")
    void handleIllegalState_tokenFaltante_retorna503() {
        MercadoPagoService service = new MercadoPagoService();
        ReflectionTestUtils.setField(service, "accessToken", "");

        IllegalStateException ex = catchThrowableOfType(
                () -> service.crearPreferenciaPago(
                        new BigDecimal("100.00"), "Viaje Test", "deudor@test.com", "acreedor@test.com"),
                IllegalStateException.class);

        ResponseEntity<Map<String, String>> response = new GlobalExceptionHandler().handleIllegalState(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("message", ex.getMessage());
    }

    @Test
    @DisplayName("configurarCredenciales sin access token no aborta el arranque del contexto")
    void configurarCredenciales_sinAccessToken_noLanzaExcepcion() {
        MercadoPagoService service = new MercadoPagoService();
        ReflectionTestUtils.setField(service, "accessToken", null);

        assertThatCode(service::configurarCredenciales).doesNotThrowAnyException();
    }
}
