import { describe, it, expect, vi, afterEach } from 'vitest';
import { signal } from '@angular/core';
import { Router, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';

let _authService: unknown;
let _router: unknown;

vi.mock('@angular/core', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@angular/core')>();
  return {
    ...actual,
    inject: vi.fn((token: unknown) => {
      if (token === AuthService) return _authService;
      if ((token as { name?: string })?.name === 'Router') return _router;
      return actual.inject(token as never);
    }),
  };
});

import { authGuard } from './auth.guard';


describe('authGuard', () => {
  afterEach(() => vi.clearAllMocks());

  it('returns true when the user is logged in', () => {
    const loginTree = {} as UrlTree;
    _authService = { isLoggedIn: signal(true) } as unknown as AuthService;
    _router = { createUrlTree: vi.fn().mockReturnValue(loginTree) } as unknown as Router;

    const result = (authGuard as any)(undefined, undefined);
    expect(result).toBe(true);
  });

  it('returns a UrlTree pointing to /login when the user is NOT logged in', () => {
    const loginTree = { toString: () => '/login' } as unknown as UrlTree;
    const routerMock = { createUrlTree: vi.fn().mockReturnValue(loginTree) } as unknown as Router;
    _authService = { isLoggedIn: signal(false) } as unknown as AuthService;
    _router = routerMock;

    const result = (authGuard as any)(undefined, undefined);
    expect(result).toBe(loginTree);
    expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/login']);
  });
});
