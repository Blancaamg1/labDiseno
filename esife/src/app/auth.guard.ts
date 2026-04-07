import { isPlatformBrowser } from '@angular/common';
import { inject, PLATFORM_ID } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';

export const authGuard: CanActivateFn = (_route, state) => {
  const router = inject(Router);
  const platformId = inject(PLATFORM_ID);
  const http = inject(HttpClient);

  if (!isPlatformBrowser(platformId)) {
    return true;
  }

  return http
    .get('http://localhost:8081/users/session', { withCredentials: true })
    .pipe(
      map(() => true),
      catchError(() =>
        of(
          router.createUrlTree(['/login'], {
            queryParams: { returnUrl: state.url },
          })
        )
      )
    );
};
