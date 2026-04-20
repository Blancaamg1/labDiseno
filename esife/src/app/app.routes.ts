import { Routes } from '@angular/router';
import { authGuard } from './auth.guard';
import { CompraComponent } from './compra/compra';
import { Espectaculos } from './espectaculos/espectaculos';
import { HomeComponent } from './home/home';
import { Login } from './login/login';
import { Register } from './register/register';
import { ElegirEntradas } from './elegir-entradas/elegir-entradas';
import { RecuperarContrasena } from './recuperar-contrasena/recuperar-contrasena';
import { RestablecerContrasena } from './restablecer-contrasena/restablecer-contrasena';

export const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'me', component: HomeComponent, canActivate: [authGuard] },
  { path: 'espectaculos', component: Espectaculos },
  { path: 'login', component: Login },
  { path: 'recuperar-contrasena', component: RecuperarContrasena },
  { path: 'restablecer-contrasena', component: RestablecerContrasena },
  { path: 'register', component: Register },
  { path: 'comprar', component: CompraComponent, canActivate: [authGuard] },
  { path: 'elegirEntradas', component: ElegirEntradas},
];