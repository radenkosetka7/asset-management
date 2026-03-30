import { describe, it, expect, vi, afterEach } from 'vitest';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { OAuth2CallbackComponent } from './oauth2-callback.component';

// ── Helpers ────────────────────────────────────────────────────────────────

function buildComponent(search: string) {
  // Patch window.location.search
  Object.defineProperty(window, 'location', {
    writable: true,
    value: { ...window.location, search },
  });

  const authServiceMock = {
    storeTokensFromOAuth2: vi.fn(),
  } as unknown as AuthService;

  const routerMock = { navigate: vi.fn() } as unknown as Router;

  const component = new OAuth2CallbackComponent(routerMock, authServiceMock);
  return { component, authServiceMock, routerMock };
}

// ── Tests ──────────────────────────────────────────────────────────────────

describe('OAuth2CallbackComponent', () => {
  afterEach(() => vi.clearAllMocks());

  it('stores tokens and navigates to /admin when both tokens are present', () => {
    const { component, authServiceMock, routerMock } = buildComponent(
      '?accessToken=access-abc&refreshToken=refresh-xyz',
    );

    component.ngOnInit();

    expect(authServiceMock.storeTokensFromOAuth2).toHaveBeenCalledWith('access-abc', 'refresh-xyz');
    expect(routerMock.navigate).toHaveBeenCalledWith(['/admin']);
  });

  it('sets an error message and redirects to /login after 2 s when tokens are missing', async () => {
    vi.useFakeTimers();
    const { component, routerMock } = buildComponent('?error=access_denied');

    component.ngOnInit();

    expect(component.message).toBe('Login failed. Redirecting…');
    expect(routerMock.navigate).not.toHaveBeenCalled();

    vi.advanceTimersByTime(2000);
    expect(routerMock.navigate).toHaveBeenCalledWith(['/login']);

    vi.useRealTimers();
  });

  it('sets an error message when only accessToken is present', async () => {
    vi.useFakeTimers();
    const { component } = buildComponent('?accessToken=only-access');

    component.ngOnInit();

    expect(component.message).toBe('Login failed. Redirecting…');
    vi.useRealTimers();
  });
});

