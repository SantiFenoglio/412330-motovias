package _0.motovias_backend.service;

import _0.motovias_backend.dto.UserProfileResponseDTO;
import _0.motovias_backend.dto.UserProfileUpdateDTO;
import _0.motovias_backend.model.User;
import _0.motovias_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileResponseDTO getProfile(String email) {
        User user = findByEmailOrThrow(email);
        return toResponseDTO(user);
    }

    public UserProfileResponseDTO updateProfile(String email, UserProfileUpdateDTO dto) {
        User user = findByEmailOrThrow(email);
        user.setNombre(dto.getNombre());
        user.setApellido(dto.getApellido());
        user.setTipoMotocicleta(dto.getTipoMotocicleta());
        user.setTipoSangre(dto.getTipoSangre());
        user.setContactoEmergenciaNombre(dto.getContactoEmergenciaNombre());
        user.setContactoEmergenciaTelefono(dto.getContactoEmergenciaTelefono());
        user.setDireccion(dto.getDireccion());
        if (dto.getNewPassword() != null && !dto.getNewPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        }
        userRepository.save(user);
        return toResponseDTO(user);
    }

    private User findByEmailOrThrow(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private UserProfileResponseDTO toResponseDTO(User user) {
        return new UserProfileResponseDTO(
                user.getNombre(),
                user.getApellido(),
                user.getEmail(),
                user.getTipoMotocicleta(),
                user.isActivo(),
                user.getRole().name(),
                user.getTipoSangre(),
                user.getContactoEmergenciaNombre(),
                user.getContactoEmergenciaTelefono(),
                user.getDireccion()
        );
    }
}
