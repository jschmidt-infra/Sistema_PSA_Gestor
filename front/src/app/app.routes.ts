import { Routes } from '@angular/router';
import { anonimoGuard, autenticadoGuard, perfilGuard } from './core/auth/guards';
import { Shell } from './core/layout/shell';
import {
  AcessoNegado,
  EmConstrucao,
  Indisponivel,
  NaoEncontrada,
} from './shared/ui/paginas-status';

/** Funcionalidade com API pronta e tela ainda não implementada. */
function emConstrucao(path: string, titulo: string, ...perfis: Parameters<typeof perfilGuard>) {
  return { path, component: EmConstrucao, canActivate: [perfilGuard(...perfis)], data: { titulo } };
}

/**
 * Rotas organizadas pelos módulos do backend. Cada módulo é carregado sob demanda,
 * reduzindo o pacote inicial (Arquitetura 2.0, seção 11.1).
 */
export const routes: Routes = [
  {
    path: 'login',
    canActivate: [anonimoGuard],
    loadComponent: () => import('./features/acesso/login').then((m) => m.Login),
  },
  { path: 'indisponivel', component: Indisponivel },
  {
    path: '',
    component: Shell,
    canActivate: [autenticadoGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'inicio' },
      {
        path: 'inicio',
        loadComponent: () => import('./features/inicio/inicio').then((m) => m.Inicio),
      },

      // acesso
      {
        path: 'usuarios',
        canActivate: [perfilGuard('ADMINISTRADOR')],
        loadComponent: () => import('./features/acesso/usuarios').then((m) => m.Usuarios),
      },
      {
        path: 'conta/senha',
        loadComponent: () => import('./features/acesso/alterar-senha').then((m) => m.AlterarSenha),
      },

      // cadastros
      {
        path: 'produtores',
        canActivate: [perfilGuard('GESTOR', 'TECNICO')],
        loadChildren: () =>
          import('./features/cadastros/cadastros.routes').then((m) => m.PRODUTORES_ROUTES),
      },
      {
        path: 'propriedades',
        canActivate: [perfilGuard('GESTOR', 'TECNICO')],
        loadChildren: () =>
          import('./features/cadastros/cadastros.routes').then((m) => m.PROPRIEDADES_ROUTES),
      },

      // configuracao, contratos, financeiro e consultas: próximas etapas
      emConstrucao('parametros', 'Parâmetros dos projetos', 'ADMINISTRADOR'),
      emConstrucao('configuracoes/vencimento', 'Avisos de vencimento', 'ADMINISTRADOR'),
      emConstrucao('contratos', 'Contratos', 'GESTOR', 'TECNICO'),
      emConstrucao('financeiro/saldo', 'Saldo e conferência', 'GESTOR'),
      emConstrucao('financeiro/aportes', 'Aportes', 'GESTOR'),
      emConstrucao('financeiro/pagamentos', 'Pagamentos', 'GESTOR'),
      emConstrucao('consultas/relatorio', 'Relatório de aplicação de recursos', 'GESTOR'),
      emConstrucao('consultas/indicadores', 'Indicadores', 'GESTOR'),
      emConstrucao('meu-historico', 'Meu histórico', 'PRODUTOR'),

      { path: 'acesso-negado', component: AcessoNegado },
      { path: '**', component: NaoEncontrada },
    ],
  },
];
