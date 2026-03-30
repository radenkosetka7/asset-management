import { Routes } from '@angular/router';
import { HomeComponent } from './components/home/home.component';
import { AssetDetailComponent } from './components/asset-detail/asset-detail.component';
import { LoginComponent } from './components/login/login.component';
import { AdminComponent } from './components/admin/admin.component';
import { DashboardComponent } from './components/admin/dashboard/dashboard.component';
import { AdminAssetsComponent } from './components/admin/admin-assets/admin-assets.component';
import { AdminCategoriesComponent } from './components/admin/admin-categories/admin-categories.component';
import { OAuth2CallbackComponent } from './components/oauth2-callback/oauth2-callback.component';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'assets/:id', component: AssetDetailComponent },
  { path: 'login', component: LoginComponent },
  { path: 'oauth2/callback', component: OAuth2CallbackComponent },
  {
    path: 'admin',
    component: AdminComponent,
    canActivate: [authGuard],
    children: [
      { path: '',           component: DashboardComponent },
      { path: 'assets',     component: AdminAssetsComponent },
      { path: 'assets/:id', component: AssetDetailComponent },
      { path: 'categories', component: AdminCategoriesComponent },
    ]
  },
  { path: '**', redirectTo: '' }
];
