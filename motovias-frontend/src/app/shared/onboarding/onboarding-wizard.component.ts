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

@Component({
  selector: 'app-onboarding-wizard',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [Dialog, Button, PrimeTemplate],
  templateUrl: './onboarding-wizard.component.html',
  styleUrl: './onboarding-wizard.component.css',
})
export class OnboardingWizardComponent implements OnInit {
  private readonly storage = inject(DOCUMENT).defaultView?.localStorage;

  protected readonly visible = signal(false);
  protected readonly pasoActual = signal(1);
  protected readonly totalPasos = 3;

  ngOnInit(): void {
    if (!this.storage?.getItem('motovias_onboarding_visto')) {
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
    this.storage?.setItem('motovias_onboarding_visto', 'true');
    this.visible.set(false);
  }
}
