import { describe, it, expect, vi, afterEach } from 'vitest';
import { HttpRequest, HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { AuthResponse } from '../models/auth.model';

let _authService: unknown;

vi.mock('@angular/core', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@angular/core')>();
  return {
    ...actual,
    inject: vi.fn((token: unknown) => {
      if (token === AuthService) return _authService;
      return actual.inject(token as never);
    }),
  };
});

import { authInterceptor } from './auth.interceptor';


interface AuthServiceMock {
  isAccessTokenExpired: ReturnType<typeof vi.fn>;
  getRefreshToken: ReturnType<typeof vi.fn>;
  getAccessToken: ReturnType<typeof vi.fn>;
  refreshAccessToken: ReturnType<typeof vi.fn>;
  signOut: ReturnType<typeof vi.fn>;
}

function buildAuthServiceMock(overrides: Partial<AuthServiceMock> = {}): AuthServiceMock {
  return {
    isAccessTokenExpired: vi.fn().mockReturnValue(false),
    getRefreshToken: vi.fn().mockReturnValue('refresh-token'),
    getAccessToken: vi.fn().mockReturnValue('access-token'),
    refreshAccessToken: vi.fn(),
    signOut: vi.fn(),
    ...overrides,
  };
}

function makeRequest(url = '/api/assets'): HttpRequest<unknown> {
  return new HttpRequest('GET', url);
}

function run(
  req: HttpRequest<unknown>,
  mockService: AuthServiceMock,
  nextFn: ReturnType<typeof vi.fn>,
) {
  _authService = mockService;
  const result$ = authInterceptor(req, nextFn);
  let value: unknown;
  let error: unknown;
  result$.subscribe({ next: v => (value = v), error: e => (error = e) });
  return { value, error };
}


describe('authInterceptor', () => {
  afterEach(() => vi.clearAllMocks());

  it('does NOT add Authorization header for /api/auth/* endpoints', () => {
    const req = makeRequest('/api/auth/signin');
    const mockService = buildAuthServiceMock();
    const next = vi.fn().mockReturnValue(of({ status: 200 }));

    run(req, mockService, next);

    const calledWith: HttpRequest<unknown> = next.mock.calls[0][0];
    expect(calledWith.headers.has('Authorization')).toBe(false);
  });

  it('attaches the Bearer token to non-auth requests', () => {
    const req = makeRequest('/api/assets');
    const mockService = buildAuthServiceMock({ getAccessToken: vi.fn().mockReturnValue('my-token') });
    const next = vi.fn().mockReturnValue(of({ status: 200 }));

    run(req, mockService, next);

    const calledWith: HttpRequest<unknown> = next.mock.calls[0][0];
    expect(calledWith.headers.get('Authorization')).toBe('Bearer my-token');
  });

  it('proactively refreshes the token when it is expired before sending', () => {
    const req = makeRequest('/api/assets');
    const newTokens: AuthResponse = { accessToken: 'new-access', refreshToken: 'new-refresh' };
    const mockService = buildAuthServiceMock({
      isAccessTokenExpired: vi.fn().mockReturnValue(true),
      refreshAccessToken: vi.fn().mockReturnValue(of(newTokens)),
    });
    const next = vi.fn().mockReturnValue(of({ status: 200 }));

    run(req, mockService, next);

    expect(mockService.refreshAccessToken).toHaveBeenCalledTimes(1);
    const calledWith: HttpRequest<unknown> = next.mock.calls[0][0];
    expect(calledWith.headers.get('Authorization')).toBe('Bearer new-access');
  });

  it('signs out when the proactive refresh fails', () => {
    const req = makeRequest('/api/assets');
    const mockService = buildAuthServiceMock({
      isAccessTokenExpired: vi.fn().mockReturnValue(true),
      refreshAccessToken: vi.fn().mockReturnValue(throwError(() => new Error('refresh failed'))),
    });
    const next = vi.fn().mockReturnValue(of({ status: 200 }));

    const { error } = run(req, mockService, next);

    expect(mockService.signOut).toHaveBeenCalledTimes(1);
    expect((error as Error).message).toBe('refresh failed');
  });

  it('retries the request with a new token on a 401 response', () => {
    const req = makeRequest('/api/assets');
    const newTokens: AuthResponse = { accessToken: 'refreshed-access', refreshToken: 'r' };
    const mockService = buildAuthServiceMock({
      refreshAccessToken: vi.fn().mockReturnValue(of(newTokens)),
    });

    const next = vi.fn()
      .mockReturnValueOnce(throwError(() => new HttpErrorResponse({ status: 401 })))
      .mockReturnValueOnce(of({ status: 200 }));

    const { error } = run(req, mockService, next);

    expect(error).toBeUndefined();
    expect(next).toHaveBeenCalledTimes(2);
    const retryReq: HttpRequest<unknown> = next.mock.calls[1][0];
    expect(retryReq.headers.get('Authorization')).toBe('Bearer refreshed-access');
  });

  it('signs out when the reactive refresh also fails', () => {
    const req = makeRequest('/api/assets');
    const mockService = buildAuthServiceMock({
      refreshAccessToken: vi.fn().mockReturnValue(throwError(() => new Error('token expired'))),
    });
    const next = vi.fn().mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 401 })),
    );

    const { error } = run(req, mockService, next);

    expect(mockService.signOut).toHaveBeenCalledTimes(1);
    expect(error).toBeDefined();
  });

  it('does NOT retry on non-401 errors', () => {
    const req = makeRequest('/api/assets');
    const mockService = buildAuthServiceMock();
    const next = vi.fn().mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500 })),
    );

    const { error } = run(req, mockService, next);

    expect(mockService.refreshAccessToken).not.toHaveBeenCalled();
    expect((error as HttpErrorResponse).status).toBe(500);
  });
});

