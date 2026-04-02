import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { firstValueFrom, timeout } from 'rxjs';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './register.html',
  styleUrl: './register.css',
})
export class Register {
  username = '';
  email = '';
  pwd1 = '';
  pwd2 = '';

  mostrarPwd1 = false;
  mostrarPwd2 = false;

  error = '';
  success = '';
  loading = false;

  // Validaciones de contraseña
  hasMinLength = false;
  hasUpperCase = false;
  hasLowerCase = false;
  hasDigit = false;

  passwordErrors: string[] = [];

  constructor(
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  togglePwd1() {
    this.mostrarPwd1 = !this.mostrarPwd1;
  }

  togglePwd2() {
    this.mostrarPwd2 = !this.mostrarPwd2;
  }

  validatePassword(pwd: string) {
    this.pwd1 = pwd;
    this.hasMinLength = pwd.length >= 8;
    this.hasUpperCase = /[A-Z]/.test(pwd);
    this.hasLowerCase = /[a-z]/.test(pwd);
    this.hasDigit = /[0-9]/.test(pwd);

    this.passwordErrors = [];
    if (!this.hasMinLength) this.passwordErrors.push('Mínimo 8 caracteres');
    if (!this.hasUpperCase) this.passwordErrors.push('Al menos 1 mayúscula');
    if (!this.hasLowerCase) this.passwordErrors.push('Al menos 1 minúscula');
    if (!this.hasDigit) this.passwordErrors.push('Al menos 1 número');
  }

  isPasswordValid(): boolean {
    return this.hasMinLength && this.hasUpperCase && this.hasLowerCase && this.hasDigit;
  }

  passwordsMatch(): boolean {
    return this.pwd1 === this.pwd2 && this.pwd1.length > 0;
  }

  async registrarse() {
    if (this.loading) return;
    
    // Validaciones en frontend antes de enviar
    if (!this.username || !this.email || !this.pwd1 || !this.pwd2) {
      this.error = 'Por favor, completa todos los campos';
      this.cdr.detectChanges();
      return;
    }
    
    if (!this.isPasswordValid()) {
      this.error = 'La contraseña no cumple todos los requisitos mostrados';
      this.cdr.detectChanges();
      return;
    }
    
    if (!this.passwordsMatch()) {
      this.error = 'Las contraseñas no coinciden';
      this.cdr.detectChanges();
      return;
    }

    this.error = '';
    this.success = '';
    this.loading = true;
    this.cdr.detectChanges();

    const payload = {
      username: this.username,
      email: this.email,
      pwd1: this.pwd1,
      pwd2: this.pwd2,
    };

    try {
      const response = await firstValueFrom(
        this.http
          .post('http://localhost:8081/users/register', payload, {
            responseType: 'text',
          })
          .pipe(timeout(10000))
      );

      this.success = response || 'Cuenta creada exitosamente. Redirigiendo...';
      this.cdr.detectChanges();

      setTimeout(() => {
        this.router.navigate(['/login']);
      }, 1200);
    } catch (err: any) {
      if (err?.status === 400) {
        // No mostrar el mensaje del servidor; es genérico por seguridad
        this.error = 'No se pudo crear la cuenta. Verifica que los datos sean válidos e intenta de nuevo.';
      } else if (err?.name === 'TimeoutError') {
        this.error = 'El servidor tarda demasiado en responder. Intenta de nuevo en unos momentos.';
      } else if (err?.status === 0) {
        this.error = 'No se pudo conectar con el servidor. Verifica tu conexión a internet.';
      } else {
        this.error = 'Error inesperado. Intenta de nuevo más tarde.';
      }
      this.cdr.detectChanges();
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }
}
