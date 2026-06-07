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

    @Column(nullable = true)
    private String apellido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean activo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_motocicleta")
    private TipoMotocicleta tipoMotocicleta;

    @Column(name = "tipo_sangre")
    private String tipoSangre;

    @Column(name = "contacto_emergencia_nombre")
    private String contactoEmergenciaNombre;

    @Column(name = "contacto_emergencia_telefono")
    private String contactoEmergenciaTelefono;

    @Column(name = "direccion")
    private String direccion;
}
