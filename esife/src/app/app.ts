import { Component, HostListener, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('esife');

  @HostListener('window:popstate')
  onPopState(): void {
    this.reloadIfSpectaculos();
  }

  @HostListener('window:pageshow', ['$event'])
  onPageShow(event: PageTransitionEvent): void {
    if (event.persisted) {
      this.reloadIfSpectaculos();
    }
  }

  private reloadIfSpectaculos(): void {
    if (typeof window !== 'undefined' && window.location.pathname === '/espectaculos') {
      window.location.reload();
    }
  }
}
