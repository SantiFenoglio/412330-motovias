package _0.motovias_backend.service;

import _0.motovias_backend.dto.UserProfileResponseDTO;
import _0.motovias_backend.dto.UserProfileUpdateDTO;
import _0.motovias_backend.model.Role;
import _0.motovias_backend.model.TipoMotocicleta;
import _0.motovias_backend.model.User;
import _0.motovias_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private UserService userService;

    private static final String EMAIL = "rider@motovias.com";
    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .email(EMAIL)
                .password("$2a$hashed")
                .nombre("Juan")
                .apellido("Perez")
                .role(Role.USER)
                .activo(true)
                .tipoMotocicleta(TipoMotocicleta.SPORT)
                .build();
    }

    @Test
    @DisplayName("updateProfile: datos válidos → actualiza nombre y tipoMotocicleta, retorna DTO")
    void updateProfile_validData_updatesAndReturnsDTO() {
        UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
        dto.setNombre("Carlos");
        dto.setTipoMotocicleta(TipoMotocicleta.NAKED);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileResponseDTO result = userService.updateProfile(EMAIL, dto);

        assertThat(result.getNombre()).isEqualTo("Carlos");
        assertThat(result.getTipoMotocicleta()).isEqualTo(TipoMotocicleta.NAKED);
        assertThat(result.getEmail()).isEqualTo(EMAIL);
        assertThat(result.getRole()).isEqualTo(Role.USER.name());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("updateProfile: no modifica email ni rol del usuario")
    void updateProfile_doesNotChangeEmailOrRole() {
        UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
        dto.setNombre("Nuevo Nombre");
        dto.setTipoMotocicleta(TipoMotocicleta.ADVENTURE);

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileResponseDTO result = userService.updateProfile(EMAIL, dto);

        assertThat(result.getEmail()).isEqualTo(EMAIL);
        assertThat(result.getRole()).isEqualTo(Role.USER.name());
    }

    @Test
    @DisplayName("updateProfile: actualiza campos de emergencia, tipo de sangre y dirección")
    void updateProfile_updatesEmergencyAndBloodFields() {
        UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
        dto.setNombre("Carlos");
        dto.setTipoSangre("A+");
        dto.setContactoEmergenciaNombre("Maria Perez");
        dto.setContactoEmergenciaTelefono("+54 351 123456");
        dto.setDireccion("Av. Siempre Viva 123");

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileResponseDTO result = userService.updateProfile(EMAIL, dto);

        assertThat(result.getTipoSangre()).isEqualTo("A+");
        assertThat(result.getContactoEmergenciaNombre()).isEqualTo("Maria Perez");
        assertThat(result.getContactoEmergenciaTelefono()).isEqualTo("+54 351 123456");
        assertThat(result.getDireccion()).isEqualTo("Av. Siempre Viva 123");
    }

    @Test
    @DisplayName("updateProfile: con newPassword válida → codifica y actualiza contraseña")
    void updateProfile_withNewPassword_encodesAndUpdatesPassword() {
        UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
        dto.setNombre("Carlos");
        dto.setNewPassword("Nueva@Clave1");

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(mockUser));
        when(passwordEncoder.encode("Nueva@Clave1")).thenReturn("$2a$newHashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.updateProfile(EMAIL, dto);

        assertThat(mockUser.getPassword()).isEqualTo("$2a$newHashed");
        verify(passwordEncoder).encode("Nueva@Clave1");
    }

    @Test
    @DisplayName("updateProfile: sin newPassword → no modifica contraseña")
    void updateProfile_withoutNewPassword_doesNotChangePassword() {
        UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
        dto.setNombre("Carlos");

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(mockUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.updateProfile(EMAIL, dto);

        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("updateProfile: usuario inexistente → lanza ResponseStatusException 404 NOT_FOUND")
    void updateProfile_userNotFound_throws404() {
        UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
        dto.setNombre("Carlos");

        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(EMAIL, dto))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("getProfile: email existente → retorna DTO con datos completos del usuario")
    void getProfile_existingUser_returnsDTO() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(mockUser));

        UserProfileResponseDTO result = userService.getProfile(EMAIL);

        assertThat(result.getNombre()).isEqualTo("Juan");
        assertThat(result.getApellido()).isEqualTo("Perez");
        assertThat(result.getEmail()).isEqualTo(EMAIL);
        assertThat(result.getTipoMotocicleta()).isEqualTo(TipoMotocicleta.SPORT);
        assertThat(result.isActivo()).isTrue();
        assertThat(result.getRole()).isEqualTo(Role.USER.name());
        verify(userRepository).findByEmail(EMAIL);
    }

    @Test
    @DisplayName("getProfile: usuario inexistente → lanza ResponseStatusException 404 NOT_FOUND")
    void getProfile_userNotFound_throws404() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(EMAIL))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
