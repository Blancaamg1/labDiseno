import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom, timeout } from 'rxjs';

@Component({
  selector: 'app-recuperar-contrasena',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './recuperar-contrasena.html',
  styleUrl: './recuperar-contrasena.css',
})
export class RecuperarContrasena {
  email = '';
  loading = false;
  error = '';
  success = '';

  constructor(private http: HttpClient, private cdr: ChangeDetectorRef) {}

  async enviarSolicitud() {
    if (this.loading || !this.email) {
      return;
    }

    this.loading = true;
    this.error = '';
    this.success = '';

    try {
      const response = await firstValueFrom(
        this.http
          .post('http://localhost:8081/users/password-reset/request', { email: this.email }, { responseType: 'text' })
          .pipe(timeout(10000))
      );

      this.success = response || 'Si el correo existe, recibiras un enlace para restablecer tu contrasena.';
    } catch (err: any) {
      if (err?.name === 'TimeoutError') {
        this.error = 'El servidor tarda demasiado en responder.';
      } else {
        this.error = 'No se pudo enviar la solicitud. Intentalo de nuevo.';
      }
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }
}
