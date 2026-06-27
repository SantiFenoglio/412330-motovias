import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { Dialog } from 'primeng/dialog';
import { Button } from 'primeng/button';
import { PrimeTemplate } from 'primeng/api';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-onboarding-wizard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Dialog, Button, PrimeTemplate],
  templateUrl: './onboarding-wizard.component.html',
  styleUrl: './onboarding-wizard.component.css',
})
export class OnboardingWizardComponent implements OnInit {
  private readonly storage = inject(DOCUMENT).defaultView?.localStorage;
  private readonly authService = inject(AuthService);

  protected readonly visible = signal(false);
  protected readonly pasoActual = signal(1);
  protected readonly totalPasos = 3;

  private get storageKey(): string {
    const email = this.authService.currentUser()?.email ?? 'guest';
    return `motovias_onboarding_visto_${email}`;
  }

  ngOnInit(): void {
    if (!this.storage?.getItem(this.storageKey)) {
      this.visible.set(true);
    }
  }

  protected siguiente(): void {
    this.pasoActual.update((p) => p + 1);
  }

  protected anterior(): void {
    this.pasoActual.update((p) => p - 1);
  }

  protected finalizar(): void {
    this.storage?.setItem(this.storageKey, 'true');
    this.visible.set(false);
  }
}
