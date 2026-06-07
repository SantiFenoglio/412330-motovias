import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  OnInit,
  signal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router } from '@angular/router';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { TipoMotocicleta, UserProfile, UserService } from '../../core/services/user.service';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { SelectModule } from 'primeng/select';
import { ToastModule } from 'primeng/toast';
import { SkeletonModule } from 'primeng/skeleton';
import { Password } from 'primeng/password';
import { MessageService } from 'primeng/api';

export const PASSWORD_REGEX =
  /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;

function optionalPasswordValidator(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null;
  return PASSWORD_REGEX.test(control.value) ? null : { passwordWeak: true };
}

function confirmPasswordValidator(group: AbstractControl): ValidationErrors | null {
  const pw = group.get('newPassword')?.value;
  const confirm = group.get('confirmPassword')?.value;
  if (!pw && !confirm) return null;
  return pw !== confirm ? { passwordMismatch: true } : null;
}

@Component({
  selector: 'app-perfil',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    ButtonModule,
    InputTextModule,
    SelectModule,
    ToastModule,
    SkeletonModule,
    Password,
  ],
  providers: [MessageService],
  templateUrl: './perfil.component.html',
  styleUrl: './perfil.component.css',
})
export class PerfilComponent implements OnInit {
  private readonly authService = inject(AuthService);
  private readonly userService = inject(UserService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly messageService = inject(MessageService);

  readonly profile = signal<UserProfile | null>(null);
  readonly isEditing = signal(false);
  readonly isSaving = signal(false);
  readonly isLoading = signal(true);

  readonly displayName = computed(() => {
    const p = this.profile();
    if (!p) return this.authService.currentUser()?.nombre ?? '';
    return p.apellido ? `${p.nombre} ${p.apellido}` : p.nombre;
  });

  readonly initials = computed(() => {
    const p = this.profile();
    const src = p
      ? p.apellido
        ? `${p.nombre} ${p.apellido}`
        : p.nombre
      : this.authService.currentUser()?.nombre || this.authService.currentUser()?.email || '';
    return (
      src
        .split(/[\s@.]+/)
        .slice(0, 2)
        .map((w) => w[0]?.toUpperCase() ?? '')
        .join('') || '?'
    );
  });

  readonly tipoMotoLabel = computed(() => {
    const tipo = this.profile()?.tipoMotocicleta;
    if (!tipo) return null;
    return this.tipoMotocicletaOptions.find((o) => o.value === tipo)?.label ?? tipo;
  });

  readonly tipoMotocicletaOptions: { label: string; value: TipoMotocicleta }[] = [
    { label: 'Custom', value: 'CUSTOM' },
    { label: 'Adventure', value: 'ADVENTURE' },
    { label: 'Sport', value: 'SPORT' },
    { label: 'Naked', value: 'NAKED' },
    { label: 'Touring', value: 'TOURING' },
    { label: 'Enduro', value: 'ENDURO' },
  ];

  readonly tipoSangreOptions = [
    { label: 'A+', value: 'A+' },
    { label: 'A-', value: 'A-' },
    { label: 'B+', value: 'B+' },
    { label: 'B-', value: 'B-' },
    { label: 'AB+', value: 'AB+' },
    { label: 'AB-', value: 'AB-' },
    { label: 'O+', value: 'O+' },
    { label: 'O-', value: 'O-' },
  ];

  readonly form = this.fb.group(
    {
      nombre: ['', [Validators.required, Validators.minLength(3)]],
      apellido: [''],
      tipoMotocicleta: [null as TipoMotocicleta | null],
      tipoSangre: [null as string | null],
      direccion: [''],
      contactoEmergenciaNombre: [''],
      contactoEmergenciaTelefono: [''],
      email: [{ value: '', disabled: true }],
      newPassword: ['', [optionalPasswordValidator]],
      confirmPassword: [''],
    },
    { validators: confirmPasswordValidator },
  );

  readonly passwordValue = toSignal(this.form.controls.newPassword.valueChanges, {
    initialValue: '',
  });

  readonly passwordRequirements = computed(() => {
    const pw = this.passwordValue() ?? '';
    return [
      { label: 'Al menos 8 caracteres', met: pw.length >= 8 },
      { label: 'Al menos una minúscula (a-z)', met: /[a-z]/.test(pw) },
      { label: 'Al menos una mayúscula (A-Z)', met: /[A-Z]/.test(pw) },
      { label: 'Al menos un número (0-9)', met: /\d/.test(pw) },
      { label: 'Al menos un carácter especial (@$!%*?&)', met: /[@$!%*?&]/.test(pw) },
    ];
  });

  readonly showPasswordReqs = computed(() => !!this.passwordValue());

  ngOnInit(): void {
    this.userService.getProfile().subscribe({
      next: (profile) => {
        this.profile.set(profile);
        this.isLoading.set(false);
        this.form.patchValue({
          nombre: profile.nombre,
          apellido: profile.apellido ?? '',
          tipoMotocicleta: profile.tipoMotocicleta,
          tipoSangre: profile.tipoSangre ?? null,
          direccion: profile.direccion ?? '',
          contactoEmergenciaNombre: profile.contactoEmergenciaNombre ?? '',
          contactoEmergenciaTelefono: profile.contactoEmergenciaTelefono ?? '',
          email: profile.email,
        });
      },
      error: () => {
        this.isLoading.set(false);
        const user = this.authService.currentUser();
        if (user) {
          this.form.patchValue({ nombre: user.nombre, email: user.email });
        }
      },
    });
  }

  startEdit(): void {
    this.isEditing.set(true);
  }

  cancelEdit(): void {
    const p = this.profile();
    if (p) {
      this.form.patchValue({
        nombre: p.nombre,
        apellido: p.apellido ?? '',
        tipoMotocicleta: p.tipoMotocicleta,
        tipoSangre: p.tipoSangre ?? null,
        direccion: p.direccion ?? '',
        contactoEmergenciaNombre: p.contactoEmergenciaNombre ?? '',
        contactoEmergenciaTelefono: p.contactoEmergenciaTelefono ?? '',
        newPassword: '',
        confirmPassword: '',
      });
    }
    this.form.markAsUntouched();
    this.isEditing.set(false);
  }

  saveProfile(): void {
    if (this.form.invalid) return;
    this.isSaving.set(true);
    const v = this.form.getRawValue();
    this.userService
      .updateProfile({
        nombre: v.nombre!,
        apellido: v.apellido || null,
        tipoMotocicleta: v.tipoMotocicleta ?? null,
        tipoSangre: v.tipoSangre ?? null,
        direccion: v.direccion || null,
        contactoEmergenciaNombre: v.contactoEmergenciaNombre || null,
        contactoEmergenciaTelefono: v.contactoEmergenciaTelefono || null,
        newPassword: v.newPassword || null,
      })
      .subscribe({
        next: (updated) => {
          this.profile.set(updated);
          this.isEditing.set(false);
          this.isSaving.set(false);
          this.form.patchValue({ newPassword: '', confirmPassword: '' });
          this.messageService.add({
            severity: 'success',
            summary: 'Perfil actualizado',
            detail: 'Los cambios se guardaron correctamente.',
          });
        },
        error: () => {
          this.isSaving.set(false);
          this.messageService.add({
            severity: 'error',
            summary: 'Error',
            detail: 'No se pudo actualizar el perfil. Intentá de nuevo.',
          });
        },
      });
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/auth/login']);
  }
}
