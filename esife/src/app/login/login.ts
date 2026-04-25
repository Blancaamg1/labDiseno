import { ChangeDetectorRef, Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom, timeout } from 'rxjs';

type LoginResponse = {
  authToken?: string;
  userId?: string;
  name: string;
  httpSessionId: string;
};

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css',
})
export class Login {
  mostrarPassword = false;
  name = '';
  pwd = '';
  error = '';
  loading = false;

  constructor(
    private http: HttpClient,
    private router: Router,
    private route: ActivatedRoute,
    private cdr: ChangeDetectorRef
  ) {}
  
  togglePassword() {
    this.mostrarPassword = !this.mostrarPassword;
  }

  async iniciarSesion() {
    if (this.loading) {
      return;
    }

    this.error = '';
    this.loading = true;

    const payload = {
      name: this.name,
      pwd: this.pwd
    };

    try {
      const response = await firstValueFrom(
        this.http.post<LoginResponse>('http://localhost:8081/users/login', payload)
          .pipe(timeout(10000))
      );

      const displayName = this.name.trim();
      if (displayName) {
        localStorage.setItem('loggedUserName', displayName);
      }
      const authToken = response?.authToken ?? response?.userId;
      if (authToken) {
        localStorage.setItem('authToken', authToken);
      }

      const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/me';
      this.router.navigateByUrl(returnUrl);
    } catch (err: any) {
      if (err?.status === 401) {
        this.error = 'Invalid Credentials';
      } else if (err?.name === 'TimeoutError') {
        this.error = 'El servidor tarda demasiado en responder';
      } else {
        this.error = 'Error de conexión con el servidor';
      }
      this.cdr.detectChanges();
    } finally {
      this.loading = false;
      this.cdr.detectChanges();
    }
  }
}