import { HttpErrorResponse } from '@angular/common/http';
import { ErroApi, ErroCampo } from './modelos';

/** Erro de API já interpretado para exibição. */
export interface FalhaExibivel {
  status: number;
  codigo: string;
  mensagem: string;
  campos: ErroCampo[];
  requisicaoId: string | null;
}

const MENSAGENS_PADRAO: Record<number, string> = {
  0: 'Não foi possível contatar o servidor. Verifique a conexão e tente novamente.',
  400: 'A requisição não pôde ser interpretada. Revise os dados informados.',
  401: 'Sua sessão terminou. Entre novamente para continuar.',
  403: 'Seu perfil não tem permissão para esta operação, ou o formulário expirou. Tente novamente.',
  404: 'Registro não encontrado.',
  409: 'A operação conflita com o estado atual do registro.',
  413: 'O arquivo excede o tamanho permitido.',
  415: 'Tipo de arquivo não aceito.',
  422: 'Há dados inválidos ou incompletos.',
  429: 'Muitas tentativas. Aguarde antes de tentar novamente.',
  503: 'Serviço temporariamente indisponível. Nenhuma alteração foi confirmada; tente mais tarde.',
};

function ehErroApi(corpo: unknown): corpo is ErroApi {
  return typeof corpo === 'object' && corpo !== null && 'codigo' in corpo && 'mensagem' in corpo;
}

/**
 * Converte qualquer falha em mensagem para o usuário.
 * 401, 403 e 503 recebem textos distintos: indisponibilidade não é senha errada.
 */
export function interpretarErro(erro: unknown): FalhaExibivel {
  if (erro instanceof HttpErrorResponse) {
    const corpo = erro.error;
    const requisicaoId = erro.headers?.get('X-Request-ID') ?? null;
    if (ehErroApi(corpo)) {
      return {
        status: erro.status,
        codigo: corpo.codigo,
        // 403 da camada de segurança tem texto técnico; o padrão explica melhor ao usuário.
        mensagem: erro.status === 403 ? MENSAGENS_PADRAO[403] : corpo.mensagem,
        campos: corpo.campos ?? [],
        requisicaoId: corpo.requisicaoId ?? requisicaoId,
      };
    }
    return {
      status: erro.status,
      codigo: `HTTP_${erro.status}`,
      mensagem: MENSAGENS_PADRAO[erro.status] ?? 'Ocorreu um erro inesperado.',
      campos: [],
      requisicaoId,
    };
  }
  return {
    status: -1,
    codigo: 'ERRO_CLIENTE',
    mensagem: 'Ocorreu um erro inesperado na aplicação.',
    campos: [],
    requisicaoId: null,
  };
}
