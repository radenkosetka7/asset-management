import { describe, it, expect, vi, afterEach } from 'vitest';
import { signal } from '@angular/core';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { LoginComponent } from './login.component';
import { AuthService } from '../../services/auth.service';
import { AuthResponse } from '../../models/auth.model';

// ── Helpers ────────────────────────────────────────────────────────────────

function buildComponent(isLoggedIn = false) {
  const mockAuthResponse: AuthResponse = { accessToken: 'tok', refreshToken: 'ref' };

  const authServiceMock = {
    isLoggedIn: signal(isLoggedIn),
    signIn: vi.fn().mockReturnValue(of(mockAuthResponse)),
  } as unknown as AuthService;

  const routerMock = { navigate: vi.fn() } as unknown as Router;

  const component = new LoginComponent(authServiceMock, routerMock);
  return { component, authServiceMock, routerMock };
}

// ── Tests ──────────────────────────────────────────────────────────────────

describe('LoginComponent', () => {
  afterEach(() => vi.clearAllMocks());

  // ── Constructor ───────────────────────────────────────────────────────

  it('redirects to /admin when the user is already logged in', () => {
    const { routerMock } = buildComponent(true);
    expect(routerMock.navigate).toHaveBeenCalledWith(['/admin']);
  });

  it('does NOT redirect when the user is not logged in', () => {
    const { routerMock } = buildComponent(false);
    expect(routerMock.navigate).not.toHaveBeenCalled();
  });

  // ── Validation ────────────────────────────────────────────────────────

  it('sets an error when both username and password are empty', () => {
    const { component } = buildComponent();
    component.username = '';
    component.password = '';
    component.onSubmit();
    expect(component.error()).toBe('Please enter your username and password.');
  });

  it('sets an error when only username is missing', () => {
    const { component } = buildComponent();
    component.username = '';
    component.password = 'secret';
    component.onSubmit();
    expect(component.error()).toBe('Username is required.');
  });

  it('sets an error when only password is missing', () => {
    const { component } = buildComponent();
    component.username = 'alice';
    component.password = '';
    component.onSubmit();
    expect(component.error()).toBe('Password is required.');
  });

  // ── Successful login ──────────────────────────────────────────────────

  it('calls signIn with correct credentials', () => {
    const { component, authServiceMock } = buildComponent();
    component.username = 'alice';
    component.password = 'secret';
    component.onSubmit();
    expect(authServiceMock.signIn).toHaveBeenCalledWith({ username: 'alice', password: 'secret' });
  });

  it('navigates to /admin on a successful login', () => {
    const { component, routerMock } = buildComponent();
    component.username = 'alice';
    component.password = 'secret';
    component.onSubmit();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/admin']);
  });

  it('clears username/password when page is shown again', () => {
    const { component } = buildComponent();
    component.username = 'alice';
    component.password = 'secret';
    component.error.set('x');
    component.showPassword.set(true);
    component.onPageShow();
    expect(component.username).toBe('');
    expect(component.password).toBe('');
    expect(component.error()).toBeNull();
    expect(component.showPassword()).toBe(false);
  });

  it('clears visible input values on page show', () => {
    const { component } = buildComponent();
    component.usernameInput = { nativeElement: { value: 'alice' } } as any;
    component.passwordInput = { nativeElement: { value: 'secret' } } as any;
    component.onPageShow();
    expect(component.usernameInput?.nativeElement.value).toBe('');
    expect(component.passwordInput?.nativeElement.value).toBe('');
  });

  it('clears the error on a successful login', () => {
    const { component } = buildComponent();
    component.username = 'alice';
    component.password = 'secret';
    component.onSubmit();
    expect(component.error()).toBeNull();
  });

  // ── Failed login ─────────────────────────────────────────────────────

  it('shows a credential error on 401', () => {
    const { component, authServiceMock } = buildComponent();
    (authServiceMock.signIn as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 401 })),
    );
    component.username = 'alice';
    component.password = 'wrong';
    component.onSubmit();
    expect(component.error()).toContain('No account found');
    expect(component.loading()).toBe(false);
  });

  it('shows a credential error on 403', () => {
    const { component, authServiceMock } = buildComponent();
    (authServiceMock.signIn as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 403 })),
    );
    component.username = 'alice';
    component.password = 'wrong';
    component.onSubmit();
    expect(component.error()).toContain('No account found');
  });

  it('shows a server-unreachable error on status 0', () => {
    const { component, authServiceMock } = buildComponent();
    (authServiceMock.signIn as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 0 })),
    );
    component.username = 'alice';
    component.password = 'pass';
    component.onSubmit();
    expect(component.error()).toContain('Cannot reach the server');
  });

  it('shows a generic error for unexpected status codes', () => {
    const { component, authServiceMock } = buildComponent();
    (authServiceMock.signIn as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500 })),
    );
    component.username = 'alice';
    component.password = 'pass';
    component.onSubmit();
    expect(component.error()).toContain('Something went wrong');
  });

  it('resets loading to false after an error', () => {
    const { component, authServiceMock } = buildComponent();
    (authServiceMock.signIn as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => new HttpErrorResponse({ status: 500 })),
    );
    component.username = 'alice';
    component.password = 'pass';
    component.onSubmit();
    expect(component.loading()).toBe(false);
  });
});

