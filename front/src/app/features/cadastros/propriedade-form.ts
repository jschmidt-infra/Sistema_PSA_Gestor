import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { catchError, firstValueFrom, map, of } from 'rxjs';
import { FalhaExibivel, interpretarErro } from '../../core/api/erros';
import { normalizarDecimal, vazioParaNulo } from '../../shared/formatos';
import { idOpcional } from '../../shared/rotas';
import { MensagemErro } from '../../shared/ui/mensagem-erro';
import { CarSituacao, NOMES_CAR, PropriedadeEntrada } from './cadastros.modelos';
import { CadastrosService } from './cadastros.service';

/** Área em hectares: até 8 dígitos inteiros e 4 decimais, aceitando vírgula. */
const AREA = /^\d{1,8}([.,]\d{1,4})?$/;

/** UC01 — cadastro e edição da propriedade (área total e CAR). */
@Component({
  selector: 'app-propriedade-form',
  imports: [ReactiveFormsModule, RouterLink, MensagemErro],
  templateUrl: './propriedade-form.html',
})
export class PropriedadeForm {
  readonly id = input(undefined, { transform: idOpcional });
  /** Pré-seleção vinda da tela do produtor (?produtorId=). */
  readonly produtorId = input(undefined, { transform: idOpcional });

  private readonly service = inject(CadastrosService);
  private readonly router = inject(Router);

  protected readonly nomesCar = NOMES_CAR;
  protected readonly situacoesCar = Object.keys(NOMES_CAR) as CarSituacao[];
  protected readonly ehNovo = computed(() => this.id() === undefined);

  protected readonly localidades = toSignal(
    this.service.localidades().pipe(catchError(() => of([]))),
    {
      initialValue: [],
    },
  );
  protected readonly subBacias = toSignal(this.service.subBacias().pipe(catchError(() => of([]))), {
    initialValue: [],
  });
  // Lista de apoio para escolher o produtor: primeiros 100 registros.
  protected readonly produtores = toSignal(
    this.service.listarProdutores(0, 100).pipe(
      map((p) => p.items),
      catchError(() => of([])),
    ),
    { initialValue: [] },
  );

  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly form = this.fb.group({
    produtorId: this.fb.control<number | null>(null, Validators.required),
    nome: ['', [Validators.required, Validators.maxLength(150)]],
    localidadeId: this.fb.control<number | null>(null, Validators.required),
    subBaciaId: this.fb.control<number | null>(null),
    areaTotal: ['', [Validators.required, Validators.pattern(AREA)]],
    carSituacao: this.fb.control<CarSituacao>('NAO_POSSUI', Validators.required),
    carNumero: ['', Validators.maxLength(100)],
  });

  protected readonly carregando = signal(false);
  protected readonly salvando = signal(false);
  protected readonly falha = signal<FalhaExibivel | null>(null);
  protected readonly possuiCar = signal(false);

  constructor() {
    this.form.controls.carSituacao.valueChanges.subscribe((s) => this.atualizarCar(s));

    effect(() => {
      const id = this.id();
      const produtor = this.produtorId();
      untracked(() => {
        if (id !== undefined) void this.carregar(id);
        else if (produtor !== undefined) this.form.controls.produtorId.setValue(produtor);
      });
    });
  }

  private atualizarCar(situacao: CarSituacao): void {
    const possui = situacao === 'POSSUI';
    this.possuiCar.set(possui);
    const numero = this.form.controls.carNumero;
    numero.setValidators(
      possui ? [Validators.required, Validators.maxLength(100)] : [Validators.maxLength(100)],
    );
    numero.updateValueAndValidity();
  }

  private async carregar(id: number): Promise<void> {
    this.carregando.set(true);
    this.falha.set(null);
    try {
      const p = await firstValueFrom(this.service.consultarPropriedade(id));
      this.form.reset({
        produtorId: p.produtorId,
        nome: p.nome,
        localidadeId: p.localidadeId,
        subBaciaId: p.subBaciaId,
        areaTotal: p.areaTotal,
        carSituacao: p.carSituacao,
        carNumero: p.carNumero ?? '',
      });
      this.atualizarCar(p.carSituacao);
    } catch (erro) {
      this.falha.set(interpretarErro(erro));
    } finally {
      this.carregando.set(false);
    }
  }

  protected async salvar(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const entrada: PropriedadeEntrada = {
      produtorId: v.produtorId!,
      localidadeId: v.localidadeId!,
      subBaciaId: v.subBaciaId,
      nome: v.nome.trim(),
      areaTotal: normalizarDecimal(v.areaTotal)!,
      carSituacao: v.carSituacao,
      carNumero: v.carSituacao === 'POSSUI' ? vazioParaNulo(v.carNumero) : null,
    };
    this.salvando.set(true);
    this.falha.set(null);
    try {
      const id = this.id();
      if (id === undefined) {
        const criado = await firstValueFrom(this.service.criarPropriedade(entrada));
        await this.router.navigate(['/propriedades', criado.id], {
          state: { mensagem: 'Propriedade cadastrada. Registre agora o levantamento hídrico.' },
        });
      } else {
        await firstValueFrom(this.service.editarPropriedade(id, entrada));
        await this.router.navigate(['/propriedades', id], {
          state: { mensagem: 'Propriedade atualizada.' },
        });
      }
    } catch (erro) {
      this.falha.set(interpretarErro(erro));
    } finally {
      this.salvando.set(false);
    }
  }
}
