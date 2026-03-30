import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, throwError, BehaviorSubject, of } from 'rxjs';
import { tap, catchError, filter, take, switchMap } from 'rxjs/operators';
import { AuthResponse, SignInRequest, RefreshTokenRequest } from '../models/auth.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly AUTH_URL = '/api/auth';

  private readonly ACCESS_TOKEN_KEY = 'access_token';
  private readonly REFRESH_TOKEN_KEY = 'refresh_token';

  isLoggedIn = signal<boolean>(this.hasValidToken());

  private refreshInProgress = false;
  private refreshSubject = new BehaviorSubject<AuthResponse | null>(null);

  constructor(private http: HttpClient, private router: Router) {}


  signIn(credentials: SignInRequest): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.AUTH_URL}/signin`, credentials).pipe(
      tap(response => this.storeTokens(response))
    );
  }

  /** Called by the OAuth2 callback component after a successful provider redirect. */
  storeTokensFromOAuth2(accessToken: string, refreshToken: string): void {
    this.storeTokens({ accessToken, refreshToken });
  }

  /**
   * Ensures only ONE refresh HTTP call is made at a time.
   * Any concurrent caller will wait for the ongoing refresh to settle
   * and then receive the same result.
   */
  refreshAccessToken(): Observable<AuthResponse> {
    if (this.refreshInProgress) {
      return this.refreshSubject.pipe(
        filter(result => result !== null),
        take(1),
        switchMap(result => of(result as AuthResponse))
      );
    }

    const refreshToken = this.getRefreshToken();
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token stored'));
    }

    this.refreshInProgress = true;
    this.refreshSubject.next(null);

    const body: RefreshTokenRequest = { refreshToken };
    return this.http.post<AuthResponse>(`${this.AUTH_URL}/refresh`, body).pipe(
      tap(response => {
        this.storeTokens(response);
        this.refreshInProgress = false;
        this.refreshSubject.next(response);
      }),
      catchError(err => {
        this.refreshInProgress = false;
        this.refreshSubject.next(null);
        return throwError(() => err);
      })
    );
  }

  signOut(): void {
    const refreshToken = this.getRefreshToken();
    if (refreshToken) {
      const body: RefreshTokenRequest = { refreshToken };
      this.http.post<void>(`${this.AUTH_URL}/signout`, body).subscribe({
        error: err => console.warn('Logout API error (ignored):', err)
      });
    }
    this.clearTokens();
    this.router.navigate(['/login']);
  }


  getAccessToken(): string | null {
    return sessionStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  getRefreshToken(): string | null {
    return sessionStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  /**
   * Returns true when the stored access token is expired (or missing).
   * Uses the JWT `exp` claim directly — no server round-trip needed.
   */
  isAccessTokenExpired(): boolean {
    const token = this.getAccessToken();
    if (!token) return true;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return Date.now() >= payload.exp * 1000;
    } catch {
      return true;
    }
  }

  /**
   * Decodes the stored JWT and returns basic user info embedded as claims.
   */
  getCurrentUser(): { firstName: string; lastName: string; username: string } | null {
    const token = this.getAccessToken();
    if (!token) return null;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return {
        firstName: payload['firstName'] ?? '',
        lastName:  payload['lastName']  ?? '',
        username:  payload['sub']       ?? ''
      };
    } catch {
      return null;
    }
  }


  private storeTokens(response: AuthResponse): void {
    sessionStorage.setItem(this.ACCESS_TOKEN_KEY, response.accessToken);
    sessionStorage.setItem(this.REFRESH_TOKEN_KEY, response.refreshToken);
    this.isLoggedIn.set(true);
  }

  private clearTokens(): void {
    sessionStorage.removeItem(this.ACCESS_TOKEN_KEY);
    sessionStorage.removeItem(this.REFRESH_TOKEN_KEY);
    this.isLoggedIn.set(false);
  }

  private hasValidToken(): boolean {
    return !!sessionStorage.getItem(this.ACCESS_TOKEN_KEY);
  }
}


