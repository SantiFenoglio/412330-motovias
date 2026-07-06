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
        if (accessToken == null || accessToken.isBlank()) {
            log.warn("MERCADOPAGO_ACCESS_TOKEN no está definido: la creación de "
                    + "preferencias de pago fallará hasta que se configure la variable de entorno.");
            return;
        }
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
            Preference preference = client.create(request);
            return preference.getInitPoint();
        } catch (MPApiException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Mercado Pago rechazó la solicitud: " + e.getApiResponse().getContent());
        } catch (MPException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo crear la preferencia de pago");
        }
    }
}
