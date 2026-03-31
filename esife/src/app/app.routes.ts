import { Routes } from '@angular/router';
import { authGuard } from './auth.guard';
import { CompraComponent } from './compra/compra';
import { Espectaculos } from './espectaculos/espectaculos';
import { HomeComponent } from './home/home';
import { Login } from './login/login';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'espectaculos', component: Espectaculos },
  { path: 'login', component: Login },
  { path: 'comprar', component: CompraComponent, canActivate: [authGuard] },
];