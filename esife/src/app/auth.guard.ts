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

  const token = localStorage.getItem('authToken');
  if (!token || token.trim() === '') {
    return router.createUrlTree(['/login'], {
      queryParams: { returnUrl: state.url },
    });
  }

  return http.get(`http://localhost:8081/external/checkToken/${token}`).pipe(
    map(() => true),
    catchError((err) => {
      if (err?.status === 401 || err?.status === 403) {
        return of(
          router.createUrlTree(['/login'], {
            queryParams: { returnUrl: state.url },
          })
        );
      }

      // Si hay token pero falla por CORS/red, no bloqueamos la compra.
      return of(true);
    })
  );
};
