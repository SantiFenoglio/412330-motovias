import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { Router, RouterLink } from '@angular/router';
import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';
import { Button } from 'primeng/button';
import { Message } from 'primeng/message';
import { SelectModule } from 'primeng/select';
import { AuthService } from '../../../core/services/auth.service';
import { TipoMotocicleta } from '../../../core/services/user.service';
import { PASSWORD_REGEX } from '../../perfil/perfil.component';

function passwordsMatch(control: AbstractControl): ValidationErrors | null {
  const password = control.get('password')?.value;
  const confirm = control.get('confirmPassword')?.value;
  return password && confirm && password !== confirm
    ? { passwordsMismatch: true }
    : null;
}

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, InputText, Password, Button, Message, RouterLink, SelectModule],
  templateUrl: './register.component.html',
  styleUrl: './register.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly tipoMotocicletaOptions: { label: string; value: TipoMotocicleta }[] = [
    { label: 'Custom', value: 'CUSTOM' },
    { label: 'Adventure', value: 'ADVENTURE' },
    { label: 'Sport', value: 'SPORT' },
    { label: 'Naked', value: 'NAKED' },
    { label: 'Touring', value: 'TOURING' },
    { label: 'Enduro', value: 'ENDURO' },
  ];

  readonly form = this.fb.group(
    {
      nombre: ['', [Validators.required, Validators.minLength(2)]],
      apellido: [''],
      email: ['', [Validators.required, Validators.email]],
      tipoMotocicleta: [null as TipoMotocicleta | null],
      password: ['', [Validators.required, Validators.pattern(PASSWORD_REGEX)]],
      confirmPassword: ['', Validators.required],
    },
    { validators: passwordsMatch },
  );

  readonly passwordValue = toSignal(this.form.controls.password.valueChanges, {
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

  readonly isLoading = signal(false);
  readonly errorMessage = signal<string | null>(null);

  get nombreControl() {
    return this.form.controls.nombre;
  }
  get emailControl() {
    return this.form.controls.email;
  }
  get passwordControl() {
    return this.form.controls.password;
  }
  get confirmPasswordControl() {
    return this.form.controls.confirmPassword;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    const { nombre, apellido, email, tipoMotocicleta, password } = this.form.getRawValue();
    this.authService
      .register({
        nombre: nombre!,
        apellido: apellido || undefined,
        tipoMotocicleta: tipoMotocicleta ?? undefined,
        email: email!,
        password: password!,
      })
      .subscribe({
        next: () => {
          this.isLoading.set(false);
          this.router.navigate(['/']);
        },
        error: (err) => {
          this.isLoading.set(false);
          this.errorMessage.set(
            err?.error?.message ?? 'Error al registrarse. Intentá de nuevo.',
          );
        },
      });
  }
}
