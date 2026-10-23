import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, expand, map, reduce, EMPTY, shareReplay } from 'rxjs';
import { Criado, Pagina } from '../../core/api/modelos';
import {
  FonteEntrada,
  FonteHidrica,
  Localidade,
  Produtor,
  ProdutorEntrada,
  Projeto,
  Propriedade,
  PropriedadeEntrada,
  SubBacia,
  Visita,
  VisitaEntrada,
} from './cadastros.modelos';

/** Cliente das rotas do módulo cadastros (RF01, RF02, RF17 e dados de referência). */
@Injectable({ providedIn: 'root' })
export class CadastrosService {
  private readonly http = inject(HttpClient);
  private readonly api = '/api/v1';

  // Produtores
  listarProdutores(page: number, size: number): Observable<Pagina<Produtor>> {
    return this.http.get<Pagina<Produtor>>(`${this.api}/produtores`, { params: { page, size } });
  }

  consultarProdutor(id: number): Observable<Produtor> {
    return this.http.get<Produtor>(`${this.api}/produtores/${id}`);
  }

  criarProdutor(entrada: ProdutorEntrada): Observable<Criado> {
    return this.http.post<Criado>(`${this.api}/produtores`, entrada);
  }

  editarProdutor(id: number, entrada: ProdutorEntrada): Observable<Produtor> {
    return this.http.put<Produtor>(`${this.api}/produtores/${id}`, entrada);
  }

  // Propriedades
  listarPropriedades(
    page: number,
    size: number,
    produtorId?: number,
  ): Observable<Pagina<Propriedade>> {
    const params: Record<string, number> = { page, size };
    if (produtorId) params['produtorId'] = produtorId;
    return this.http.get<Pagina<Propriedade>>(`${this.api}/propriedades`, { params });
  }

  consultarPropriedade(id: number): Observable<Propriedade> {
    return this.http.get<Propriedade>(`${this.api}/propriedades/${id}`);
  }

  criarPropriedade(entrada: PropriedadeEntrada): Observable<Criado> {
    return this.http.post<Criado>(`${this.api}/propriedades`, entrada);
  }

  editarPropriedade(id: number, entrada: PropriedadeEntrada): Observable<Propriedade> {
    return this.http.put<Propriedade>(`${this.api}/propriedades/${id}`, entrada);
  }

  /** UC02/A2: registra explicitamente a ausência (ou presença) de fontes hídricas. */
  registrarLevantamento(id: number, situacao: 'AUSENTES' | 'PRESENTES'): Observable<Propriedade> {
    return this.http.put<Propriedade>(`${this.api}/propriedades/${id}/levantamento-hidrico`, {
      situacao,
    });
  }

  // Fontes hídricas
  listarFontes(
    propriedadeId: number,
    page: number,
    size: number,
  ): Observable<Pagina<FonteHidrica>> {
    return this.http.get<Pagina<FonteHidrica>>(
      `${this.api}/propriedades/${propriedadeId}/fontes-hidricas`,
      {
        params: { page, size },
      },
    );
  }

  criarFonte(propriedadeId: number, entrada: FonteEntrada): Observable<Criado> {
    return this.http.post<Criado>(
      `${this.api}/propriedades/${propriedadeId}/fontes-hidricas`,
      entrada,
    );
  }

  editarFonte(id: number, entrada: FonteEntrada): Observable<FonteHidrica> {
    return this.http.put<FonteHidrica>(`${this.api}/fontes-hidricas/${id}`, entrada);
  }

  // Visitas
  listarVisitas(propriedadeId: number, page: number, size: number): Observable<Pagina<Visita>> {
    return this.http.get<Pagina<Visita>>(`${this.api}/propriedades/${propriedadeId}/visitas`, {
      params: { page, size },
    });
  }

  criarVisita(propriedadeId: number, entrada: VisitaEntrada): Observable<Criado> {
    return this.http.post<Criado>(`${this.api}/propriedades/${propriedadeId}/visitas`, entrada);
  }

  editarVisita(id: number, entrada: VisitaEntrada): Observable<Visita> {
    return this.http.put<Visita>(`${this.api}/visitas/${id}`, entrada);
  }

  // Referências: conjuntos pequenos, carregados por completo e reaproveitados na sessão.
  private localidades$?: Observable<Localidade[]>;
  private subBacias$?: Observable<SubBacia[]>;
  private projetos$?: Observable<Projeto[]>;

  localidades(): Observable<Localidade[]> {
    return (this.localidades$ ??= this.todas<Localidade>('localidades'));
  }

  subBacias(): Observable<SubBacia[]> {
    return (this.subBacias$ ??= this.todas<SubBacia>('sub-bacias'));
  }

  projetos(): Observable<Projeto[]> {
    return (this.projetos$ ??= this.todas<Projeto>('projetos'));
  }

  /** Percorre as páginas (máximo de 100 itens cada) até obter toda a coleção. */
  private todas<T>(rota: string): Observable<T[]> {
    const size = 100;
    const pagina = (page: number) =>
      this.http.get<Pagina<T>>(`${this.api}/${rota}`, { params: { page, size } });
    return pagina(0).pipe(
      expand((p) => ((p.page + 1) * size < p.totalItems ? pagina(p.page + 1) : EMPTY)),
      map((p) => p.items),
      reduce((todos, itens) => todos.concat(itens), [] as T[]),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
  }
}
