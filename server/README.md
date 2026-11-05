# Servidor de IA do BorbaCar Labs

Servidor opcional para conversar sobre uma captura OBD-II. A chave de API permanece no computador; o celular usa um convite de conexão. As respostas são hipóteses de apoio acadêmico, separadas dos fatos e relatórios.

## Começar

Requer Node.js 20+ e uma chave de API de Gemini ou Claude. Uma assinatura pessoal do aplicativo de chat não substitui acesso à API. Disponibilidade, preços e limites dependem do provedor e da conta; não há promessa de gratuidade.

No Windows, na raiz do repositório:

```powershell
./start-ai.ps1
```

Em outros sistemas, nesta pasta:

```sh
npm ci
npm run setup
npm start
```

O assistente gera o token de acesso e salva a configuração em `.env`, ignorado pelo Git. Variáveis já presentes no ambiente têm precedência. Para reconfigurar no Windows, use `./start-ai.ps1 -Configure`.

O terminal mostra um convite `http://IP:8787/#TOKEN`. No app de teste, abra **Ajustes → Análise por IA**, cole o convite e toque em **Salvar e testar conexão**. Computador e celular devem estar na mesma rede. O convite é uma credencial: não o publique em slides, mensagens públicas, capturas ou no Git.

O teste de conexão autentica o token e consulta o servidor, sem chamar o modelo. A chave do provedor é verificada na primeira análise. No app também há **Usar minha IA**, que compartilha o prompt revisado pelo seletor Android e dispensa este servidor.

## Configuração manual

| Variável | Uso |
|---|---|
| `BORBACAR_PROVIDER` | `gemini` ou `claude`; sem valor, usa Gemini quando sua chave está no ambiente |
| `GEMINI_API_KEY` / `ANTHROPIC_API_KEY` | Chave do provedor selecionado |
| `BORBACAR_TOKEN` | Token do app, com pelo menos 16 caracteres |
| `BORBACAR_MODEL` | Substitui o modelo padrão do provedor |
| `BORBACAR_DAILY_LIMIT` | Cota diária, padrão 20 |
| `BORBACAR_USAGE_FILE` | Arquivo de consumo, padrão `usage.json` |
| `BORBACAR_TIMEOUT_MS` | Tempo-limite, padrão 60000 |
| `PORT` | Porta HTTP, padrão 8787 |

O modelo padrão Gemini é `gemini-3.5-flash-lite`; o padrão Claude é `claude-opus-5-5`. São configurações substituíveis, não uma garantia de disponibilidade permanente. O ensaio real desta entrega foi feito com Gemini, conforme a evidência versionada em `docs/evidence/`.

## Contrato

- `GET /health`: estado básico, modelo e cota; não chama o provedor.
- `GET /v1/status`: teste autenticado do token e capacidade de conversa; não chama o provedor.
- `POST /v1/analyze`: recebe `borbacar-ai-request-1`, uma captura minimizada, pergunta de até 500 caracteres e até seis pares de contexto. O app envia no máximo três resumos recentes.

As observações usam referências únicas `D1`, `M1`, `T1` etc. O servidor e o app recusam citações inexistentes e respostas fora do contrato. Isso não comprova sustentação mecânica: é necessária revisão humana. Perguntas e respostas anteriores são contexto, não novas medições. O modelo nunca recebe acesso a comandos do adaptador.

O app filtra padrões de telefone, e-mail, CPF, CNPJ e placa; nomes, endereços e outros padrões podem permanecer. Cada envio tem prévia e confirmação. As políticas de tratamento e retenção do provedor também se aplicam.

## Testar e avaliar

```sh
npm test
npm run evaluate:fixtures
```

Os testes usam provedor falso, sem chamadas cobradas. As cinco fixtures são sintéticas. Para avaliar respostas reais, inicie o servidor e execute em outro terminal:

```sh
npm run evaluate:live
```

Isso faz cinco chamadas reais e grava `evaluation-result.json`, excluído do Git, com modelo, latência e campos de revisão humana. HTTP 200 não significa diagnóstico correto. Consulte `docs/TRABALHO-ACADEMICO.md` na raiz do repositório para método, critérios e roteiro de apresentação.

## Limites

Este servidor é destinado à apresentação em rede local. HTTP deve ser usado somente na rede de demonstração, com a APK de teste; uma hospedagem externa exige HTTPS. A cota é persistida e reservada antes da chamada, para um único processo. Não há contas individuais, coordenação entre réplicas ou cobrança comercial. Perguntas podem consumir créditos mesmo se uma resposta for rejeitada ou a espera terminar após o envio ao provedor.
