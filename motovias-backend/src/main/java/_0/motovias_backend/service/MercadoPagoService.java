package _0.motovias_backend.service;

import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferencePayerRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class MercadoPagoService {

    /**
     * Timeouts cortos para que, si el contenedor no tiene salida a internet o
     * Mercado Pago no responde, el SDK falle rápido con una excepción
     * controlada en lugar de dejar el hilo de Spring Boot colgado esperando
     * una respuesta que nunca llega (lo que termina en un 502 del proxy).
     */
    private static final int TIMEOUT_MS = 5000;

    @Value("${mercadopago.access-token}")
    private String accessToken;

    /**
     * Configura el SDK al arrancar el contexto. No aborta el arranque de la
     * aplicación si falta el token: Mercado Pago es una integración puntual
     * (pago de deudas) y no debe tumbar el resto del backend (mapa, reportes,
     * notificaciones, etc.) si el sandbox todavía no está configurado. El
     * error real se levanta recién cuando alguien intenta usar la función,
     * en {@link #crearPreferenciaPago}.
     */
    @PostConstruct
    public void configurarCredenciales() {
        MercadoPagoConfig.setConnectionTimeout(TIMEOUT_MS);
        MercadoPagoConfig.setConnectionRequestTimeout(TIMEOUT_MS);
        MercadoPagoConfig.setSocketTimeout(TIMEOUT_MS);

        if (accessToken == null || accessToken.isBlank()) {
            log.warn("MERCADOPAGO_ACCESS_TOKEN no está definido: la creación de "
                    + "preferencias de pago fallará hasta que se configure la variable de entorno.");
            return;
        }

        // El token puede llegar desde el .env de Docker con comillas o espacios
        // colgando (p. ej. MERCADOPAGO_ACCESS_TOKEN="APP-123..." ), que rompen
        // el HttpClient interno del SDK al armar el header Authorization.
        String tokenSaneado = accessToken.trim().replaceAll("^[\"']|[\"']$", "");
        if (!tokenSaneado.equals(accessToken)) {
            log.warn("MERCADOPAGO_ACCESS_TOKEN contenía comillas o espacios sobrantes; se sanearon antes de usarlo.");
        }
        accessToken = tokenSaneado;

        MercadoPagoConfig.setAccessToken(accessToken);
    }

    /**
     * Crea una preferencia de pago en Mercado Pago para saldar una deuda entre
     * dos usuarios de un viaje y retorna la URL de redirección (init_point).
     */
    public String crearPreferenciaPago(
            BigDecimal monto,
            String descripcionViaje,
            String deudorEmail,
            String acreedorEmail
    ) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException(
                    "Error crítico: el ACCESS_TOKEN de Mercado Pago no está configurado "
                    + "en las variables de entorno de Docker");
        }

        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .id("gasto-" + acreedorEmail)
                .title(descripcionViaje)
                .description("Liquidación de deuda hacia " + acreedorEmail)
                .categoryId("payments")
                .currencyId("ARS")
                .quantity(1)
                .unitPrice(monto)
                .build();

        PreferenceRequest request = PreferenceRequest.builder()
                .items(List.of(item))
                .payer(PreferencePayerRequest.builder()
                        .email(deudorEmail)
                        .build())
                .externalReference(acreedorEmail)
                .build();

        try {
            PreferenceClient client = new PreferenceClient();
            log.info("Iniciando llamada externa al SDK de Mercado Pago...");
            Preference preference = client.create(request);
            log.info("Respuesta recibida del SDK de Mercado Pago, preferencia id={}", preference.getId());
            return preference.getInitPoint();
        } catch (MPApiException e) {
            log.error("Mercado Pago respondió con error de API: {}", e.getApiResponse().getContent(), e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Mercado Pago rechazó la solicitud: " + e.getApiResponse().getContent());
        } catch (MPException e) {
            log.error("Fallo al invocar al SDK de Mercado Pago (posible timeout o error de red)", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo crear la preferencia de pago");
        }
    }
}
