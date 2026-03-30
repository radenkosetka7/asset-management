import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { AuthResponse } from '../models/auth.model';

function buildJwt(payload: Record<string, unknown>, expOffsetSeconds = 3600): string {
  const now = Math.floor(Date.now() / 1000);
  const fullPayload = { sub: 'user@test.com', exp: now + expOffsetSeconds, ...payload };
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const body = btoa(JSON.stringify(fullPayload));
  return `${header}.${body}.signature`;
}

const mockAuthResponse: AuthResponse = {
  accessToken: buildJwt({ firstName: 'John', lastName: 'Doe' }),
  refreshToken: 'refresh-token-abc',
};

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: { post: ReturnType<typeof vi.fn>; get: ReturnType<typeof vi.fn> };
  let routerMock: { navigate: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    sessionStorage.clear();
    httpMock = { post: vi.fn(), get: vi.fn() };
    routerMock = { navigate: vi.fn() };
    service = new AuthService(
      httpMock as unknown as HttpClient,
      routerMock as unknown as Router,
    );
  });

  afterEach(() => {
    sessionStorage.clear();
    vi.clearAllMocks();
  });

  describe('signIn', () => {
    it('stores tokens and sets isLoggedIn to true on success', () => {
      httpMock.post.mockReturnValue(of(mockAuthResponse));
      let result: AuthResponse | undefined;
      service.signIn({ username: 'user', password: 'pass' }).subscribe(r => (result = r));
      expect(result).toEqual(mockAuthResponse);
      expect(sessionStorage.getItem('access_token')).toBe(mockAuthResponse.accessToken);
      expect(sessionStorage.getItem('refresh_token')).toBe(mockAuthResponse.refreshToken);
      expect(service.isLoggedIn()).toBe(true);
    });

    it('calls the correct endpoint', () => {
      httpMock.post.mockReturnValue(of(mockAuthResponse));
      service.signIn({ username: 'u', password: 'p' }).subscribe();
      expect(httpMock.post).toHaveBeenCalledWith('/api/auth/signin', { username: 'u', password: 'p' });
    });
  });

  describe('storeTokensFromOAuth2', () => {
    it('saves tokens in sessionStorage and marks user as logged in', () => {
      service.storeTokensFromOAuth2('access-abc', 'refresh-xyz');
      expect(sessionStorage.getItem('access_token')).toBe('access-abc');
      expect(sessionStorage.getItem('refresh_token')).toBe('refresh-xyz');
      expect(service.isLoggedIn()).toBe(true);
    });
  });

  describe('signOut', () => {
    it('clears tokens, sets isLoggedIn to false, and navigates to /login', () => {
      sessionStorage.setItem('access_token', 'tok');
      sessionStorage.setItem('refresh_token', 'ref');
      httpMock.post.mockReturnValue(of(undefined));
      service.signOut();
      expect(sessionStorage.getItem('access_token')).toBeNull();
      expect(sessionStorage.getItem('refresh_token')).toBeNull();
      expect(service.isLoggedIn()).toBe(false);
      expect(routerMock.navigate).toHaveBeenCalledWith(['/login']);
    });

    it('still clears tokens even when the signout API fails', () => {
      sessionStorage.setItem('refresh_token', 'ref');
      httpMock.post.mockReturnValue(throwError(() => new Error('network error')));
      service.signOut();
      expect(sessionStorage.getItem('access_token')).toBeNull();
      expect(service.isLoggedIn()).toBe(false);
    });

    it('skips the API call when there is no refresh token', () => {
      service.signOut();
      expect(httpMock.post).not.toHaveBeenCalled();
    });
  });

  describe('token accessors', () => {
    it('returns null when no token is stored', () => {
      expect(service.getAccessToken()).toBeNull();
      expect(service.getRefreshToken()).toBeNull();
    });

    it('returns stored tokens', () => {
      sessionStorage.setItem('access_token', 'at');
      sessionStorage.setItem('refresh_token', 'rt');
      expect(service.getAccessToken()).toBe('at');
      expect(service.getRefreshToken()).toBe('rt');
    });
  });

  describe('isAccessTokenExpired', () => {
    it('returns true when no token is stored', () => {
      expect(service.isAccessTokenExpired()).toBe(true);
    });

    it('returns false for a valid (future-expiry) token', () => {
      sessionStorage.setItem('access_token', buildJwt({}, 3600));
      expect(service.isAccessTokenExpired()).toBe(false);
    });

    it('returns true for an already-expired token', () => {
      sessionStorage.setItem('access_token', buildJwt({}, -60));
      expect(service.isAccessTokenExpired()).toBe(true);
    });

    it('returns true when the token is malformed', () => {
      sessionStorage.setItem('access_token', 'not.a.jwt');
      expect(service.isAccessTokenExpired()).toBe(true);
    });
  });

  describe('getCurrentUser', () => {
    it('returns null when no token is stored', () => {
      expect(service.getCurrentUser()).toBeNull();
    });

    it('returns user info decoded from a valid JWT', () => {
      const token = buildJwt({ firstName: 'Alice', lastName: 'Smith', sub: 'alice@test.com' });
      sessionStorage.setItem('access_token', token);
      const user = service.getCurrentUser();
      expect(user).toEqual({ firstName: 'Alice', lastName: 'Smith', username: 'alice@test.com' });
    });

    it('returns null for a malformed token', () => {
      sessionStorage.setItem('access_token', 'bad-token');
      expect(service.getCurrentUser()).toBeNull();
    });
  });

  describe('refreshAccessToken', () => {
    it('calls /api/auth/refresh and stores new tokens', () => {
      sessionStorage.setItem('refresh_token', 'old-refresh');
      const newTokens: AuthResponse = { accessToken: buildJwt({}), refreshToken: 'new-refresh' };
      httpMock.post.mockReturnValue(of(newTokens));
      let result: AuthResponse | undefined;
      service.refreshAccessToken().subscribe(r => (result = r));
      expect(result).toEqual(newTokens);
      expect(sessionStorage.getItem('access_token')).toBe(newTokens.accessToken);
      expect(sessionStorage.getItem('refresh_token')).toBe(newTokens.refreshToken);
    });

    it('throws when no refresh token is stored', () => {
      let error: Error | undefined;
      service.refreshAccessToken().subscribe({ error: (e: Error) => (error = e) });
      expect(error?.message).toBe('No refresh token stored');
    });

    it('propagates HTTP errors from the refresh endpoint', () => {
      sessionStorage.setItem('refresh_token', 'ref');
      const apiError = new Error('401 Unauthorized');
      httpMock.post.mockReturnValue(throwError(() => apiError));
      let error: Error | undefined;
      service.refreshAccessToken().subscribe({ error: (e: Error) => (error = e) });
      expect(error).toBe(apiError);
    });
  });
});

