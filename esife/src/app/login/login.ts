import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { ActivatedRoute, Router } from '@angular/router';

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
    private route: ActivatedRoute
  ) {}
  
  togglePassword() {
    this.mostrarPassword = !this.mostrarPassword;
  }

  iniciarSesion() {
    this.error = '';
    this.loading = true;

    const payload = {
      name: this.name,
      pwd: this.pwd
    };

    this.http.post('http://localhost:8081/users/login', payload, { responseType: 'text' })
      .subscribe({
        next: (response: string) => {
          if (response === 'Login successful') {
            localStorage.setItem('loggedUser', this.name);

            const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/';
            this.router.navigateByUrl(returnUrl);
          } else {
            this.error = 'Credenciales inválidas';
          }
        },
        error: (err) => {
          this.error = err?.error || 'Error de conexión con el servidor';
          this.loading = false;
        },
        complete: () => {
          this.loading = false;
        }
      });
  }
}