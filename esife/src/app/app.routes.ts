import { Routes } from '@angular/router';
import { authGuard } from './auth.guard';
import { CompraComponent } from './compra/compra';
import { Espectaculos } from './espectaculos/espectaculos';
import { HomeComponent } from './home/home';
import { Login } from './login/login';
import { Register } from './register/register';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'me', component: HomeComponent, canActivate: [authGuard] },
  { path: 'espectaculos', component: Espectaculos },
  { path: 'login', component: Login },
  { path: 'register', component: Register },
  { path: 'comprar', component: CompraComponent, canActivate: [authGuard] },
];