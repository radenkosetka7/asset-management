import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-oauth2-callback',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="oauth-callback">
      <div class="spinner"></div>
      <p>{{ message }}</p>
    </div>
  `,
  styles: [`
    .oauth-callback {
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #0f172a 0%, #1e3a5f 50%, #1e40af 100%);
      color: #fff;
      font-family: 'Inter', 'Segoe UI', sans-serif;
      gap: 20px;
    }
    .spinner {
      width: 44px;
      height: 44px;
      border: 4px solid rgba(255,255,255,0.3);
      border-top-color: #fff;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }
    p { font-size: 16px; font-weight: 500; opacity: 0.85; }
    @keyframes spin { to { transform: rotate(360deg); } }
  `]
})
export class OAuth2CallbackComponent implements OnInit {
  message = 'Completing sign-in…';

  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const params = new URLSearchParams(window.location.search);
    const accessToken  = params.get('accessToken');
    const refreshToken = params.get('refreshToken');

    if (accessToken && refreshToken) {
      this.authService.storeTokensFromOAuth2(accessToken, refreshToken);
      this.router.navigate(['/admin']);
    } else {
      this.message = 'Login failed. Redirecting…';
      setTimeout(() => this.router.navigate(['/login']), 2000);
    }
  }
}

