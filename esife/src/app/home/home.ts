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
  userAvatarUrl: string | null = null;
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

    const cachedUser = localStorage.getItem('loggedUserName');
    if (cachedUser) {
      this.applyLoggedUser(cachedUser);
    }

    this.refreshLoggedUser();

    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe(() => {
        this.refreshLoggedUser();
      });
  }

  private refreshLoggedUser() {
    this.loadLoggedUser();
  }

  private loadLoggedUser() {
    if (!this.isBrowser) {
      return;
    }

    this.http
      .get<{ name: string }>('http://localhost:8081/users/session', { withCredentials: true })
      .subscribe({
        next: (session) => {
          this.applyLoggedUser(session.name);
          localStorage.setItem('loggedUserName', session.name);
        },
        error: () => {
          this.clearLoggedUser();
        },
      });
  }

  private applyLoggedUser(name: string) {
    this.loggedUser = name;
    this.userAvatarUrl = this.buildAvatarUrl(name);
  }

  private clearLoggedUser() {
    this.loggedUser = null;
    this.userAvatarUrl = null;
    localStorage.removeItem('loggedUserName');
  }

  private buildAvatarUrl(name: string): string {
    return `https://api.dicebear.com/8.x/initials/svg?seed=${encodeURIComponent(name)}`;
  }

  logout() {
    this.http.post('http://localhost:8081/users/logout', {}, { withCredentials: true }).subscribe({
      next: () => {
        this.clearLoggedUser();
        this.router.navigateByUrl('/');
      },
      error: () => {
        this.clearLoggedUser();
        this.router.navigateByUrl('/');
      },
    });
  }
}
