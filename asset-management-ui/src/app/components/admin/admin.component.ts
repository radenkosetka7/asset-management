import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs';
import { AuthService } from '../../services/auth.service';

interface NavItem {
  label: string;
  route: string;
  icon: string;
}

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.css'
})
export class AdminComponent {
  sidebarOpen = true;
  currentRoute = '';
  currentUser: { firstName: string; lastName: string; username: string } | null = null;

  navItems: NavItem[] = [
    { label: 'Dashboard',  route: '/admin',            icon: 'dashboard' },
    { label: 'Assets',     route: '/admin/assets',     icon: 'assets' },
    { label: 'Categories', route: '/admin/categories', icon: 'categories' },
  ];

  constructor(private authService: AuthService, private router: Router) {
    this.currentUser = this.authService.getCurrentUser();
    this.currentRoute = this.router.url;
    this.router.events.pipe(filter(e => e instanceof NavigationEnd)).subscribe((e: NavigationEnd) => {
      this.currentRoute = e.urlAfterRedirects;
    });
  }

  isActive(route: string): boolean {
    if (route === '/admin') return this.currentRoute === '/admin';
    return this.currentRoute.startsWith(route);
  }

  signOut(): void {
    this.authService.signOut();
  }
}

