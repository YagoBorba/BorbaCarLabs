# Validação da experiência acadêmica — 04/11/2026

Base: `develop` no commit `1e141bc`. Implementação na branch `codex/academic-ai-experience`; não é uma nova release publicada.

## Resultado executado

| Verificação | Resultado |
|---|---|
| `./gradlew testDebugUnitTest lintDebug assembleDebug` | Passou; 164 testes, zero falhas, erros ou ignorados |
| `cd server; npm test` | Passou; 22 testes com provedor falso e assistente de configuração isolado |
| `npm run evaluate:fixtures` | Cinco entradas sintéticas válidas; não chama a IA |
| Chamada real via servidor local → Gemini | HTTP 200, modelo `gemini-3.5-flash-lite`, 10.389 ms |
| Resposta real | Dados simulados identificados; hipóteses citaram `D1`; correção mecânica não avaliada |
| Revisão visual | Assistente em português e inglês e resultado de diagnóstico inspecionados |
| Regressão visual | Duas referências de Diagnóstico atualizadas pelo novo acesso direto à IA; demais referências preservadas |
| `git diff --check` | Sem erros de whitespace |

A evidência da chamada real está em [gemini-smoke.json](evidence/gemini-smoke.json). A entrada foi sintética: P0301 armazenado, sintoma de falha em marcha lenta e pergunta sobre o que permanece incerto, sem identificadores de usuário, horários ou VIN. Uma chamada demonstra conectividade e aceitação do contrato; não representa acurácia diagnóstica.

## Comportamentos novos cobertos

- Abrir o assistente diretamente da leitura e reutilizar a investigação sem duplicar a captura.
- Compartilhar somente a pergunta revisada com os dados filtrados, sem exigir servidor.
- Manter conversas separadas por caso e revisão, limitar histórico e rejeitar evidências inventadas antes de salvar.
- Excluir somente as conversas do caso removido, sem afetar outro caso.
- Autenticar o teste de conexão sem consultar o provedor nem consumir cota.
- Rejeitar histórico de conversa excessivo e referências duplicadas antes da chamada.
- Gerar convite no assistente do servidor sem imprimir a chave da API.
- Priorizar manutenção vencida ou próxima por quilometragem sobre uma data distante.
- Restaurar garagem, investigações e diário, desfazer, rejeitar arquivos alterados, esquema desconhecido, revisão descontínua e arquivo maior que 8 MB.
- Recuperar uma restauração interrompida antes do carregamento dos registros do aplicativo.

## Artefato

APK de teste: aplicação `io.rea.obd.teste`, versão `1.1.1-teste`, versionCode 24. Mantém a identidade de desenvolvimento existente; as mudanças ainda não geram tag ou release.

SHA-256 da APK desta validação:

```text
3c25410c48c77ac6503a7b505a5819398963f7ca5c5b5ffc2dcbf325e7a6b53d
```

## Pendências explicitamente não executadas

- Telefone, adaptador e carro reais; seletor de documentos e PDF em aparelho físico.
- Revisão mecânica das respostas e avaliação com participantes.
- Matriz de compatibilidade por combinação de hardware.
- Hospedagem externa com HTTPS e autenticação por usuário.

O protocolo de cinco chamadas reais e revisão humana está em [TRABALHO-ACADEMICO.md](TRABALHO-ACADEMICO.md); o roteiro de hardware está em [ENSAIO-FISICO.md](ENSAIO-FISICO.md). Nenhum resultado pendente foi contabilizado como aprovado.
