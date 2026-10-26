import { AbstractControl, ValidationErrors } from '@angular/forms';

/** Mesma regra da API: de 12 caracteres a 72 bytes UTF-8 (limite do BCrypt). */
export function validarSenha(controle: AbstractControl<string>): ValidationErrors | null {
  const valor = controle.value ?? '';
  if (valor === '') return null;
  if (valor.length < 12) return { senha: 'A senha deve ter pelo menos 12 caracteres.' };
  if (new TextEncoder().encode(valor).length > 72) return { senha: 'A senha excede 72 bytes.' };
  return null;
}
