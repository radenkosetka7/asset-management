import { AfterViewInit, Component, ElementRef, HostListener, ViewChild, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css'
})
export class LoginComponent implements AfterViewInit {
  username = '';
  password = '';
  loading = signal(false);
  error = signal<string | null>(null);
  showPassword = signal(false);

  @ViewChild('usernameInput') usernameInput?: ElementRef<HTMLInputElement>;
  @ViewChild('passwordInput') passwordInput?: ElementRef<HTMLInputElement>;

  constructor(private authService: AuthService, private router: Router) {
    // Already logged in → go to admin panel
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/admin']);
    }
  }

  ngAfterViewInit(): void {
    // Force-empty credentials after navigation/back-forward cache restores.
    queueMicrotask(() => this.resetCredentials());
  }

  @HostListener('window:pageshow')
  onPageShow(): void {
    this.resetCredentials();
  }

  private resetCredentials(): void {
    this.username = '';
    this.password = '';
    this.error.set(null);
    this.showPassword.set(false);
    if (this.usernameInput) this.usernameInput.nativeElement.value = '';
    if (this.passwordInput) this.passwordInput.nativeElement.value = '';
  }

  onSubmit(): void {

    const username = this.username.trim();
    const password = this.password.trim();
    this.username = username;
    this.password = password;

    if (!username && !password) {
      this.error.set('Please enter your username and password.');
      return;
    }
    if (!username) {
      this.error.set('Username is required.');
      return;
    }
    if (!password) {
      this.error.set('Password is required.');
      return;
    }

    this.loading.set(true);
    this.error.set(null);

    this.authService.signIn({ username, password }).subscribe({
      next: () => this.router.navigate(['/admin']),
      error: (err) => {
        this.loading.set(false);
        if (err.status === 401 || err.status === 403) {
          this.error.set(
            `No account found for username "${this.username}", or the password is incorrect.`
          );
        } else if (err.status === 0) {
          this.error.set('Cannot reach the server. Please make sure the backend is running.');
        } else {
          this.error.set(`Something went wrong. Please try again.`);
        }
      }
    });
  }
}

