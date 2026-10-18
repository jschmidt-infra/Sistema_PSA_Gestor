"""Gera rastreabilidade a partir da matriz e dos resultados reais do Surefire."""
from pathlib import Path
import json
import sys
import xml.etree.ElementTree as ET
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[1]
rows = json.loads((ROOT / 'docs/aceitacao-matriz.json').read_text(encoding='utf-8'))
results = {}
counts = dict(aprovados=0, falhas=0, ignorados=0)
reports = list((ROOT / 'target/surefire-reports').glob('TEST-*.xml'))
if not reports:
    sys.exit('Execute scripts/Testar-MariaDB.ps1 antes de gerar o relatório.')
for path in reports:
    for case in ET.parse(path).getroot().iter('testcase'):
        state = 'APROVADO'
        if case.find('failure') is not None or case.find('error') is not None:
            state = 'FALHOU'
        elif case.find('skipped') is not None:
            state = 'NÃO EXECUTADO'
        method = case.attrib['name'].split('(', 1)[0]
        key = case.attrib['classname'].removeprefix('br.unisc.psagestor.') + '#' + method
        results[key] = state
        counts[{'APROVADO': 'aprovados', 'FALHOU': 'falhas', 'NÃO EXECUTADO': 'ignorados'}[state]] += 1

lines = ['# Evidências de aceitação da API', '',
         'Gerado em ' + datetime.now(timezone.utc).isoformat() + ' a partir de `target/surefire-reports/TEST-*.xml`.', '',
         f"Suíte: {counts['aprovados']} aprovados, {counts['falhas']} falhas, {counts['ignorados']} não executados.", '',
         '**Aceitação global: EM ANDAMENTO.** Teste aprovado comprova seu cenário, não todos os critérios do requisito.', '',
         'O resultado se refere à última execução local. Não representa homologação da VPS ou do frontend. Consulte [o plano](ACEITACAO.md).', '',
         '| Requisito / origem | Critério coberto | Evidência automatizada | Pendência de aceitação |',
         '|---|---|---|---|']
missing = []
for row in rows:
    tests = row['testes']
    evidence = '<br>'.join(f"`{name}`: **{results.get(name, 'SEM RESULTADO')}**" for name in tests) or 'Sem teste automatizado; validação manual pendente.'
    missing.extend(name for name in tests if name not in results)
    pending = row['pendencia'] or 'Cenários automatizados cobertos; confirmar integração e aceitação final.'
    lines.append(f"| {row['id']} — {row['origem']} | {row['criterio']} | {evidence} | {pending} |")

perf = ROOT / 'target/aceitacao/desempenho-local.json'
if perf.exists():
    data = json.loads(perf.read_text(encoding='utf-8'))
    values = sorted(sample['totalMs'] for sample in data['amostras'])
    lines += ['', '## Medição preliminar de desempenho', '',
              f"Execução: {data['executadoEm']}. Ambiente: {data['ambiente']}.", '',
              f"{data['perguntas']} perguntas; {data['aquecimentos']} aquecimentos excluídos; {len(values)} amostras sequenciais de um usuário.", '',
              f"Tempo combinado de concluir diagnóstico, criar cálculo e consultar o resultado: mínimo **{min(values):.2f} ms**, média **{sum(values)/len(values):.2f} ms**, máximo **{max(values):.2f} ms**. Limite comparado: {data['limiteMs']} ms.", '',
              'O tempo usa MockMvc e MariaDB real descartável, sem transporte HTTP. Não inclui renderização, rede, proxy nem carga da VPS. A metodologia é demonstrativa. Não comprova o requisito ponta a ponta.', '']

out = ROOT / 'docs/ACEITACAO_RESULTADOS.md'
out.write_text('\n'.join(lines) + '\n', encoding='utf-8')
print(f'Relatório gerado: {out}')
if missing:
    sys.exit('Referências sem resultado: ' + ', '.join(missing))
if counts['falhas']:
    sys.exit('A suíte contém falhas. Não considerar aprovada.')
