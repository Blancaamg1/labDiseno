import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom, timeout } from 'rxjs';

@Component({
  selector: 'app-restablecer-contrasena',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './restablecer-contrasena.html',
  styleUrl: './restablecer-contrasena.css',
})
export class RestablecerContrasena {
  token = '';
  pwd1 = '';
  pwd2 = '';
  mostrarPwd1 = false;
  mostrarPwd2 = false;
  loading = false;
  error = '';
  success = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {
    this.token = this.route.snapshot.queryParamMap.get('token') || '';
  }

  togglePwd1() {
    this.mostrarPwd1 = !this.mostrarPwd1;
  }

  togglePwd2() {
    this.mostrarPwd2 = !this.mostrarPwd2;
  }

  private isPasswordValid(): boolean {
    if (this.pwd1.length < 8) {
      return false;
    }

    const hasUpper = /[A-Z]/.test(this.pwd1);
    const hasLower = /[a-z]/.test(this.pwd1);
    const hasDigit = /[0-9]/.test(this.pwd1);

    return hasUpper && hasLower && hasDigit;
  }

  async guardarNuevaContrasena() {
    if (this.loading) {
      return;
    }

    this.error = '';
    this.success = '';

    if (!this.token) {
      this.error = 'El enlace no es valido o no incluye token.';
      this.cdr.detectChanges();
      return;
    }

    if (this.pwd1 !== this.pwd2) {
      this.error = 'Las contrasenas no coinciden.';
      this.cdr.detectChanges();
      return;
    }

    if (!this.isPasswordValid()) {
      this.error = 'La contrasena debe tener al menos 8 caracteres, mayuscula, minuscula y numero.';
      this.cdr.detectChanges();
      return;
    }

    this.loading = true;

    try {
      const response = await firstValueFrom(
        this.http
          .post(
            'http://localhost:8081/users/password-reset/confirm',
            {
              token: this.token,
              pwd1: this.pwd1,
              pwd2: this.pwd2,
            },
            { responseType: 'text' }
          )
          .pipe(timeout(10000))
      );

      this.success = response || 'Contrasena actualizada correctamente.';
      this.cdr.detectChanges();

      setTimeout(() => {
        this.router.navigateByUrl('/login');
      }, 1400);
    } catch (err: any) {
      if (err?.status === 410) {
        this.error = 'El enlace ha caducado. Solicita uno nuevo.';
      } else if (err?.status === 404 || err?.status === 400) {
        this.error = 'El enlace no es valido o ya fue utilizado.';
      } else if (err?.name === 'TimeoutError') {
        this.error = 'El servidor tarda demasiado en responder.';
      } else {
        this.error = 'No se pudo actualizar la contrasena. Intentalo de nuevo.';
      }
      this.cdr.detectChanges();
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }
}
