import { Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { Router, RouterLink, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, HttpClientModule],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class HomeComponent implements OnInit {
  loggedUser: string | null = null;
  private isBrowser: boolean;

  constructor(
    private http: HttpClient,
    private router: Router,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(this.platformId);
  }

  ngOnInit() {
    if (!this.isBrowser) {
      return;
    }

    this.refreshLoggedUser();

    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe(() => {
        this.refreshLoggedUser();
      });
  }

  private refreshLoggedUser() {
    this.loggedUser = localStorage.getItem('loggedUser');
    this.loadLoggedUser();
  }

  private loadLoggedUser() {
    if (!this.isBrowser) {
      return;
    }

    const token = localStorage.getItem('authToken');
    if (!token) {
      this.loggedUser = localStorage.getItem('loggedUser');
      return;
    }

    this.http
      .get('http://localhost:8081/users/me', {
        params: { token },
        responseType: 'text',
      })
      .subscribe({
        next: (name) => {
          localStorage.setItem('loggedUser', name);
          this.loggedUser = name;
        },
        error: () => {
          localStorage.removeItem('authToken');
          localStorage.removeItem('loggedUser');
          this.loggedUser = null;
        },
      });
  }

  logout() {
    if (this.isBrowser) {
      localStorage.removeItem('authToken');
      localStorage.removeItem('loggedUser');
    }
    this.loggedUser = null;
  }
}
