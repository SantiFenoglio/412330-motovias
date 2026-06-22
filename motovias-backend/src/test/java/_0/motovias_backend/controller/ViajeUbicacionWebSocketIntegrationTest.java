package _0.motovias_backend.controller;

import _0.motovias_backend.dto.ParticipanteUbicacionDTO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de integración del canal de ubicación en tiempo real (US-29).
 *
 * <p>Levanta el servidor en un puerto aleatorio y verifica que un mensaje
 * enviado a {@code /app/viajes/{viajeId}/compartir} sea retransmitido
 * por el broker al canal {@code /topic/viajes/{viajeId}/ubicaciones}.
 *
 * <p>Usa {@code /ws-native} (endpoint sin SockJS) para simplificar
 * la conexión con {@link StandardWebSocketClient} en el test.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class ViajeUbicacionWebSocketIntegrationTest {

    @LocalServerPort
    private int port;

    private WebSocketStompClient stompClient;

    @BeforeEach
    void setUp() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @AfterEach
    void tearDown() {
        stompClient.stop();
    }

    @Test
    @DisplayName("Ubicación enviada al canal de compartir llega a los suscriptores del viaje")
    void ubicacionEsRetransmitidaAlTopicDelViaje() throws Exception {
        long viajeId = 99L;
        CompletableFuture<ParticipanteUbicacionDTO> futuro = new CompletableFuture<>();

        String url = "ws://localhost:" + port + "/ws-native";
        StompSession session = stompClient
                .connectAsync(url, new StompSessionHandlerAdapter() {})
                .get(5, TimeUnit.SECONDS);

        session.subscribe("/topic/viajes/" + viajeId + "/ubicaciones", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return ParticipanteUbicacionDTO.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                futuro.complete((ParticipanteUbicacionDTO) payload);
            }
        });

        // Breve espera para asegurar que la suscripción quede registrada en el broker
        // antes de que el SEND desencadene el broadcast.
        Thread.sleep(200);

        ParticipanteUbicacionDTO payload = ParticipanteUbicacionDTO.builder()
                .viajeId(viajeId)
                .email("piloto@motovias.com")
                .nombre("Ramiro Sosa")
                .latitud(-31.4201)
                .longitud(-64.1888)
                .conectado(true)
                .build();

        session.send("/app/viajes/" + viajeId + "/compartir", payload);

        ParticipanteUbicacionDTO recibido = futuro.get(5, TimeUnit.SECONDS);

        assertThat(recibido.getEmail()).isEqualTo("piloto@motovias.com");
        assertThat(recibido.getNombre()).isEqualTo("Ramiro Sosa");
        assertThat(recibido.getViajeId()).isEqualTo(viajeId);
        assertThat(recibido.getLatitud()).isEqualTo(-31.4201);
        assertThat(recibido.getLongitud()).isEqualTo(-64.1888);
        assertThat(recibido.isConectado()).isTrue();

        session.disconnect();
    }
}
