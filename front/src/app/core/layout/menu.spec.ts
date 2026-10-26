import { menuDoPerfil } from './menu';

function rotas(perfil: Parameters<typeof menuDoPerfil>[0]): string[] {
  return menuDoPerfil(perfil).flatMap((g) => g.itens.map((i) => i.rota));
}

describe('menuDoPerfil', () => {
  it('administrador não herda telas de gestor ou técnico', () => {
    const r = rotas('ADMINISTRADOR');
    expect(r).toContain('/usuarios');
    expect(r).not.toContain('/produtores');
    expect(r).not.toContain('/financeiro/pagamentos');
  });

  it('financeiro e relatórios são exclusivos do gestor', () => {
    expect(rotas('GESTOR')).toContain('/financeiro/saldo');
    expect(rotas('TECNICO')).not.toContain('/financeiro/saldo');
  });

  it('produtor vê apenas início, histórico e conta', () => {
    expect(rotas('PRODUTOR')).toEqual(['/inicio', '/meu-historico', '/conta/senha']);
  });

  it('sem perfil não há menu', () => {
    expect(menuDoPerfil(null)).toEqual([]);
  });
});
