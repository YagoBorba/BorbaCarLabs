# Roteiro de demonstração — BorbaCar Labs 1.0.0

Apresentação prevista para 02–05/11/2026. Duração: 10 a 12 minutos, mais perguntas.
Tudo abaixo funciona **sem carro e sem adaptador** (modo demonstração). A análise por IA é
opcional e precisa do servidor rodando no computador.

## Ordem importa

O app protege os dados reais durante a demonstração: **não é possível criar perfil nem registrar
no Diário com a demonstração ativa**, e a leitura guiada exige um perfil. Por isso: perfil e Diário
primeiro, demonstração depois.

## Preparação (na véspera)

1. Instale `BorbaCar-Labs-1.0.0-teste.apk` no celular (fica ao lado do app original, com dados próprios).
2. **Garagem › Adicionar veículo**: escolha no catálogo um Chevrolet Cobalt MY19 (o catálogo tem 516 versões de 34 marcas, todas com fonte Inmetro).
3. **Diário › Novo registro**, para o Hoje ter o que mostrar:
   - Abastecimento com tanque cheio (ex.: 48.000 km, 40 L, gasolina).
   - Outro abastecimento com tanque cheio, marcando que todos os abastecimentos do intervalo foram registrados (ex.: 48.500 km, 38 L).
   - Um serviço com lembrete (ex.: troca de óleo, próxima em 6 meses ou 58.000 km).
4. Opcional, IA: no computador, `cd server`, `npm install`, defina `BORBACAR_PROVIDER=gemini`,
   `BORBACAR_TOKEN` e a chave (ver `server/README.md`), rode `npm start`. No celular,
   **Ajustes › Análise por IA**: `http://IP-DO-PC:8787` e o token. Celular e PC na mesma rede Wi-Fi.
5. Faça o roteiro inteiro uma vez e apague as investigações de ensaio se quiser começar limpo.

## Roteiro

| Tempo | Tela | O que mostrar | O que dizer |
|---|---|---|---|
| 0:00 | **Hoje** | Veículo, consumo medido ao lado do consumo oficial do Inmetro ("dentro da faixa oficial") e próximo serviço | O app é um caderno do carro: o que você informa, o que o carro informa e o dado oficial ficam separados e com fonte. |
| 0:45 | **Ajustes › Lembretes de manutenção** | Ligar; notificação quando o serviço está a 7 dias ou 500 km | Os lembretes são calculados no celular a partir do Diário; nada sai do aparelho. |
| 1:00 | **Diagnóstico › Explorar demonstração** | Aviso âmbar "dados simulados" | Sem adaptador, a demonstração está a um toque. Tudo que é simulado é marcado como tal, inclusive nos relatórios. |
| 1:30 | **Diagnóstico › Iniciar leitura guiada** | Confirmação do que será consultado; progresso por grupo | A leitura é só de leitura: códigos de emissões, luz de injeção, prontidão e quadro congelado. Não apaga nada. |
| 3:00 | **Veredito** | "2 códigos de falha encontrados", MIL acesa, ressalva de cobertura | Primeiro a resposta, depois o detalhe. A ressalva aparece uma vez: OBD2 cobre emissões, não ABS ou airbag. |
| 3:30 | **Cartões dos códigos** | P0301 e P0133 com a área do sistema e o significado do catálogo com fonte | A área vem da estrutura do código (padrão SAE J2012); o significado vem de um catálogo com fonte por entrada. Nada é inventado. |
| 4:30 | **Investigar esta leitura** | Sintomas declarados, criação do caso | Sintoma é declaração do usuário, não medição. O caso congela a leitura; revisar sintomas cria uma nova revisão. |
| 5:30 | **Investigação › Exportar PDF** | PDF com faixa "DADOS SIMULADOS" em todas as páginas, seções e paginação | O relatório é rastreável: cada fato aponta a evidência e a fonte, e as limitações vêm escritas no final. |
| 7:00 | **Análise por IA (demonstração)** (opcional) | Prévia do que será enviado; resposta com hipóteses e evidências | Só sai do celular o caso sem identificação. A IA só pode citar evidências enviadas; se inventar, a resposta é recusada. |
| 9:00 | **Histórico** | Leituras com o mesmo título do veredito; comparar duas leituras | Dá para comparar antes e depois de um reparo sem confundir dado ausente com defeito resolvido. |
| 10:00 | **Ajustes › Sair da demonstração** | Volta ao estado real | Encerrar. |

## Se algo der errado

- **Sem Wi-Fi ou servidor fora do ar:** pule a etapa da IA. O app mostra a mensagem do servidor;
  explique que regras, catálogo e relatórios funcionam offline e que a IA é um complemento.
- **Gemini sobrecarregado (HTTP 503):** troque para `BORBACAR_MODEL=gemini-3.5-flash-lite` (já é o
  padrão) ou mostre a análise feita no ensaio por captura de tela.
- **PDF não abre no celular:** compartilhe para o Drive/e-mail e abra no computador.
- **App fechou:** os rascunhos do Diário e o caso salvo são restaurados ao reabrir.

## Perguntas prováveis

- **Funciona num carro de verdade?** Os transportes Bluetooth clássico, BLE e Wi-Fi estão
  implementados e cobertos por testes com adaptador simulado; **não houve ensaio físico** registrado.
- **A IA dá o diagnóstico?** Não. Ela propõe hipóteses citando as evidências do caso; o app rejeita
  respostas fora do contrato. As regras locais continuam sendo a base e não dependem de internet.
- **Que dados saem do celular?** Só na análise por IA, após confirmação: tipo do veículo, códigos,
  luz de injeção, leituras e sintomas. Nome do perfil, VIN, notas, adaptador, IDs e horários não saem.
- **Como vocês garantem que funciona?** 62 testes automáticos (sessão ELM simulada, parser, telas do app abertas pelo Robolectric,
  leitura, catálogo real, persistência, consumo, contrato da IA, layout do PDF); ver `README.md`.
