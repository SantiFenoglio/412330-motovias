package _0.motovias_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean activo;

    /**
     * Tipo de motocicleta del usuario según perfil definido en el KickOff.
     * Ej: "SPORT", "NAKED", "ADVENTURE", "SCOOTER", etc.
     */
    @Column(name = "tipo_motocicleta")
    private String tipoMotocicleta;
}
