# Servidor de IA do BorbaCar Labs

Servidor intermediário para a **análise por IA (demonstração)** das investigações. A chave da
Anthropic fica só aqui; o celular conhece apenas o endereço deste servidor e um token próprio.

## O que ele faz

1. Recebe do app um caso **com dados mínimos** (tipo do veículo, códigos, luz de injeção, leituras e
   sintomas declarados, cada item com uma referência `D1`, `M1`, `T1`…). Nome do perfil, VIN,
   notas, adaptador, identificadores e horários não saem do celular; telefones, e-mails, CPF, CNPJ e
   placas são removidos dos sintomas, mas nomes e endereços escritos ali não são detectados.
2. Pede ao Claude hipóteses em formato JSON fixo (structured outputs), com esforço `low` para
   respostas curtas e baratas.
3. Recusa respostas fora do contrato ou que citem referências que não foram enviadas. O app
   confere tudo de novo antes de exibir.

Limites: token obrigatório, caso de até 16 KB, `BORBACAR_DAILY_LIMIT` análises por dia (padrão 20).
Cada análise reserva uma unidade **antes** de chamar o provedor, então pedidos simultâneos não
ultrapassam o limite. Pedidos recusados antes da chamada, ou que falham sem resposta do provedor,
devolvem a unidade. O consumo do dia fica em `server/usage.json` (ou em `BORBACAR_USAGE_FILE`),
sobrevive a reinícios e zera à meia-noite no horário do servidor.
Se o provedor não responder em `BORBACAR_TIMEOUT_MS` (padrão 60000), o app recebe 504.

Testes: `npm test` sobe o servidor com um provedor falso, sem rede e sem chaves.

## Como rodar (Node 20+)

```bash
cd server
npm install
```

Defina as variáveis e inicie (PowerShell):

```powershell
$env:ANTHROPIC_API_KEY = "sk-ant-..."     # ou faça `ant auth login` antes
$env:BORBACAR_TOKEN = (node -e "console.log(require('crypto').randomBytes(24).toString('hex'))")
$env:BORBACAR_TOKEN                        # copie para o app
npm start
```

No app de teste: **Ajustes › Análise por IA**, informe `http://IP-DO-PC:8787` (o IP do computador
na mesma rede Wi-Fi, por exemplo pelo `ipconfig`) e o token. Depois abra uma investigação e toque
em **Análise por IA (demonstração)**; o app mostra o que será enviado e pede confirmação.

`GET /health` mostra o modelo em uso e quantas análises restam hoje.

## Testar de graça com o Gemini (Google)

A API do Gemini tem um nível gratuito, sem cartão, bom para poucos testes. No nível gratuito o
Google usa o conteúdo enviado para melhorar seus produtos; o app envia só dados mínimos,
mas o texto livre dos sintomas pode conter dados pessoais que o filtro não reconhece; não envie o que você não queira compartilhar. Os limites por modelo aparecem no AI Studio.

1. Entre em **aistudio.google.com** com uma conta Google e clique em **Get API key** › **Create API key**.
2. Salve a chave nas variáveis do seu usuário (PowerShell):

   ```powershell
   [Environment]::SetEnvironmentVariable("GEMINI_API_KEY", "cole-a-chave-aqui", "User")
   ```

3. Abra um novo PowerShell e inicie o servidor com o provedor Gemini:

   ```powershell
   $env:BORBACAR_PROVIDER = "gemini"     # modelo padrão: gemini-3.5-flash-lite
   $env:BORBACAR_TOKEN = "seu-token"
   npm start
   ```

O app não muda: o mesmo token e endereço funcionam com qualquer provedor, e o contrato da resposta
é o mesmo. O nome do modelo aparece no título da análise.

## Modelo e custo

Padrão: `claude-opus-5-5` (US$ 4 / US$ 20 por milhão de tokens de entrada/saída), esforço `low`,
com fallback automático do servidor da Anthropic em caso de recusa. Um caso típico usa ~1–2 mil
tokens de entrada e ~1–2 mil de saída, ou seja, centavos de dólar por análise.

Para gastar ainda menos, troque o modelo por uma variável:

```powershell
$env:BORBACAR_MODEL = "claude-haiku-5-5"   # US$ 0,10 / US$ 0,50 por milhão
```

## Limites desta versão

- Feito para demonstrações na rede local: HTTP simples (o build de teste permite) e contador
  diário em memória. Para distribuir o app, use HTTPS e um servidor hospedado.
- A análise não é salva na investigação e não altera regras, catálogo ou relatórios.
- É uma interpretação gerada por IA, não diagnóstico confirmado.
