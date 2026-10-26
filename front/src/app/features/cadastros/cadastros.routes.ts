import { Routes } from '@angular/router';
import { perfilGuard } from '../../core/auth/guards';

/** Rotas do módulo cadastros. Criação é exclusiva do técnico; gestor consulta e edita. */
export const PRODUTORES_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./produtores-lista').then((m) => m.ProdutoresLista) },
  {
    path: 'novo',
    canActivate: [perfilGuard('TECNICO')],
    loadComponent: () => import('./produtor-form').then((m) => m.ProdutorForm),
  },
  { path: ':id', loadComponent: () => import('./produtor-form').then((m) => m.ProdutorForm) },
];

export const PROPRIEDADES_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () => import('./propriedades-lista').then((m) => m.PropriedadesLista),
  },
  {
    path: 'nova',
    canActivate: [perfilGuard('TECNICO')],
    loadComponent: () => import('./propriedade-form').then((m) => m.PropriedadeForm),
  },
  {
    path: ':id',
    loadComponent: () => import('./propriedade-detalhe').then((m) => m.PropriedadeDetalhe),
  },
  {
    path: ':id/editar',
    loadComponent: () => import('./propriedade-form').then((m) => m.PropriedadeForm),
  },
];
