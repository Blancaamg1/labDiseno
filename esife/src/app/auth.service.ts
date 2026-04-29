import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly logoutUrl = 'http://localhost:8081/users/logout';
  private readonly cancelAccountUrl = 'http://localhost:8081/users/cancelAccount';

  constructor(private http: HttpClient, private router: Router) { }

  logout(): void {
    this.http.post(this.logoutUrl, {}, { withCredentials: true }).subscribe({
      next: () => this.completeLogout(),
      error: () => this.completeLogout()
    });
  }

  private completeLogout(): void {
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem('loggedUserName');
      localStorage.removeItem('authToken');
    }
    this.router.navigate(['/']);
  }

  cancelAccount(): void {
    const token = typeof localStorage !== 'undefined' ? localStorage.getItem('authToken') : null;
    this.http.post(this.cancelAccountUrl, { token }).subscribe({
      next: () => this.completeCancelAccount(),
      error: () => this.completeCancelAccount()
    });
  }

  private completeCancelAccount(): void {
    if (typeof localStorage !== 'undefined') {
      localStorage.removeItem('loggedUserName');
      localStorage.removeItem('authToken');
    }
  }
}
