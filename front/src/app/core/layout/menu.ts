import { Perfil } from '../api/modelos';

export interface ItemMenu {
  rotulo: string;
  rota: string;
  perfis: Perfil[];
  /** Tela ainda não implementada; a rota existe e mostra o aviso correspondente. */
  emConstrucao?: boolean;
}

export interface GrupoMenu {
  titulo: string;
  itens: ItemMenu[];
}

const TODOS: Perfil[] = ['ADMINISTRADOR', 'GESTOR', 'TECNICO', 'PRODUTOR'];

/**
 * Menu agrupado pelos módulos do backend. Os perfis seguem api/docs/PERMISSOES_ROTAS.md;
 * o administrador não herda as permissões de gestor ou técnico.
 */
export const MENU: GrupoMenu[] = [
  {
    titulo: 'Geral',
    itens: [{ rotulo: 'Início', rota: '/inicio', perfis: TODOS }],
  },
  {
    titulo: 'Administração',
    itens: [
      { rotulo: 'Usuários', rota: '/usuarios', perfis: ['ADMINISTRADOR'] },
      {
        rotulo: 'Parâmetros dos projetos',
        rota: '/parametros',
        perfis: ['ADMINISTRADOR'],
        emConstrucao: true,
      },
      {
        rotulo: 'Avisos de vencimento',
        rota: '/configuracoes/vencimento',
        perfis: ['ADMINISTRADOR'],
        emConstrucao: true,
      },
    ],
  },
  {
    titulo: 'Cadastros',
    itens: [
      { rotulo: 'Produtores', rota: '/produtores', perfis: ['GESTOR', 'TECNICO'] },
      { rotulo: 'Propriedades', rota: '/propriedades', perfis: ['GESTOR', 'TECNICO'] },
    ],
  },
  {
    titulo: 'Contratos',
    itens: [
      {
        rotulo: 'Contratos',
        rota: '/contratos',
        perfis: ['GESTOR', 'TECNICO'],
        emConstrucao: true,
      },
    ],
  },
  {
    titulo: 'Financeiro',
    itens: [
      {
        rotulo: 'Saldo e conferência',
        rota: '/financeiro/saldo',
        perfis: ['GESTOR'],
        emConstrucao: true,
      },
      { rotulo: 'Aportes', rota: '/financeiro/aportes', perfis: ['GESTOR'], emConstrucao: true },
      {
        rotulo: 'Pagamentos',
        rota: '/financeiro/pagamentos',
        perfis: ['GESTOR'],
        emConstrucao: true,
      },
    ],
  },
  {
    titulo: 'Consultas',
    itens: [
      {
        rotulo: 'Relatório de aplicação',
        rota: '/consultas/relatorio',
        perfis: ['GESTOR'],
        emConstrucao: true,
      },
      {
        rotulo: 'Indicadores',
        rota: '/consultas/indicadores',
        perfis: ['GESTOR'],
        emConstrucao: true,
      },
      { rotulo: 'Meu histórico', rota: '/meu-historico', perfis: ['PRODUTOR'], emConstrucao: true },
    ],
  },
  {
    titulo: 'Minha conta',
    itens: [{ rotulo: 'Alterar senha', rota: '/conta/senha', perfis: TODOS }],
  },
];

export function menuDoPerfil(perfil: Perfil | null): GrupoMenu[] {
  if (!perfil) return [];
  return MENU.map((g) => ({
    ...g,
    itens: g.itens.filter((i) => i.perfis.includes(perfil)),
  })).filter((g) => g.itens.length > 0);
}
