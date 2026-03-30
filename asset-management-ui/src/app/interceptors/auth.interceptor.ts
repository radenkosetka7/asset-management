import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

function withBearerToken(req: HttpRequest<unknown>, token: string): HttpRequest<unknown> {
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}

export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const authService = inject(AuthService);
  const isAuthEndpoint = req.url.includes('/api/auth/');

  if (!isAuthEndpoint && authService.isAccessTokenExpired() && authService.getRefreshToken()) {
    return authService.refreshAccessToken().pipe(
      switchMap(newTokens => next(withBearerToken(req, newTokens.accessToken))),
      catchError(refreshErr => {
        authService.signOut();
        return throwError(() => refreshErr);
      })
    );
  }

  const token = authService.getAccessToken();
  const authReq = token && !isAuthEndpoint ? withBearerToken(req, token) : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !isAuthEndpoint) {
        return authService.refreshAccessToken().pipe(
          switchMap(newTokens => next(withBearerToken(req, newTokens.accessToken))),
          catchError(refreshErr => {
            authService.signOut();
            return throwError(() => refreshErr);
          })
        );
      }
      return throwError(() => error);
    })
  );
};

