import { Perfil } from '../api/modelos';

export const NOMES_PERFIS: Record<Perfil, string> = {
  ADMINISTRADOR: 'Administrador',
  GESTOR: 'Gestor do programa',
  TECNICO: 'Técnico de campo',
  PRODUTOR: 'Produtor rural',
};

export const PERFIS: Perfil[] = ['ADMINISTRADOR', 'GESTOR', 'TECNICO', 'PRODUTOR'];
