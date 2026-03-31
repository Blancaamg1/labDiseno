import { Routes } from '@angular/router';
import { CompraComponent } from './compra/compra';
import { Espectaculos } from './espectaculos/espectaculos';
import { Login } from './login/login';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: '', component: Espectaculos },
  { path: 'comprar', component: CompraComponent },
];