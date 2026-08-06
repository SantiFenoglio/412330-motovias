import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { NgOptimizedImage } from '@angular/common';

const BACKGROUND_IMAGES = [
  'ambitious-studio-rick-barrett-eNhYc5e8A_g-unsplash.jpg',
  'ben-kupke-iYVhKG-nOKc-unsplash.jpg',
  'dariussss-iacob-yyZ8xSN5VLc-unsplash.jpg',
  'duc-van-I_-sWa8bj-M-unsplash.jpg',
  'fridi-antrack-0VRirUhjX_w-unsplash.jpg',
  'harley-davidson-xAHtaYIHlPI-unsplash.jpg',
  'ilya-godze-HpdykLH8g_4-unsplash.jpg',
  'patrick-hendry-OKKzEIBj8cQ-unsplash.jpg',
  'robby-henry-1hsmgqi9uiw-unsplash.jpg',
  'royal-enfield-SlF40JIv82s-unsplash.jpg',
  'volodymyr-diadechko-lTWn2Ei2QQ8-unsplash.jpg',
] as const;

const SLIDE_DURATION_MS = 7000;

@Component({
  selector: 'app-auth-layout',
  imports: [NgOptimizedImage],
  templateUrl: './auth-layout.component.html',
  styleUrl: './auth-layout.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuthLayoutComponent {
  private readonly destroyRef = inject(DestroyRef);

  readonly backgroundImages = BACKGROUND_IMAGES;
  readonly activeSlide = signal(0);

  constructor() {
    const intervalId = setInterval(() => {
      this.activeSlide.update((index) => (index + 1) % this.backgroundImages.length);
    }, SLIDE_DURATION_MS);

    this.destroyRef.onDestroy(() => clearInterval(intervalId));
  }
}
