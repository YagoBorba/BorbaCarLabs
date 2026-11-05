# Preparação para apresentar o BorbaCar Labs

Tudo funciona **sem carro e sem adaptador** (modo demonstração). A IA é opcional e precisa só de
internet no celular e de uma chave de API.

## Na véspera

1. Instale o APK no celular. O de teste (`app-debug.apk`, nome "BorbaCar Labs (teste)") fica ao lado do app
   original, com dados próprios.
2. **IA:** crie uma chave do Gemini no [Google AI Studio](https://aistudio.google.com/apikey) (ou do Claude no
   [Claude Console](https://platform.claude.com/settings/keys)). No app, **Ajustes › Análise por IA**: escolha o
   provedor, cole a chave e toque em **Salvar e testar chave**. Deve aparecer "Chave aceita pelo provedor".
3. **Garagem › Adicionar veículo**: use **Buscar meu carro** (ex.: "cobalt 2019") e salve o perfil. O catálogo
   tem 9.362 versões de 43 marcas, de 2012 a 2026, todas com fonte Inmetro.
4. **Diário › Novo registro**, para a tela Hoje ter o que mostrar:
   - Dois abastecimentos com tanque cheio (ex.: 48.000 km e 48.500 km).
   - Um serviço com lembrete (ex.: troca de óleo, próxima em 6 meses ou 58.000 km).
5. Faça o percurso inteiro uma vez, incluindo uma pergunta para a IA, e apague as investigações de ensaio
   se quiser começar limpo.

**Ordem importa:** com a demonstração ativa não é possível criar perfil nem registrar no Diário, e a leitura
guiada exige um perfil. Por isso: perfil e Diário primeiro, demonstração depois.

## No dia

- Confira que o celular tem internet (dados móveis servem; não depende do Wi-Fi da sala).
- Não mostre a tela de Ajustes da IA no projetor com a chave digitada. Ela fica oculta depois de salva.
- Cada pergunta usa a cota da sua chave. No plano gratuito do Gemini há limite por minuto e por dia.

## Percurso sugerido

| Tela | O que mostrar |
|---|---|
| **Hoje** | Veículo, consumo medido ao lado do consumo oficial do Inmetro e próximo serviço |
| **Diagnóstico › Explorar demonstração** | Aviso "dados simulados": tudo que é simulado é marcado, inclusive nos relatórios |
| **Leitura guiada** | Só leitura: códigos, luz de injeção, prontidão e quadro congelado. Não apaga nada |
| **Veredito e códigos** | "2 códigos de falha encontrados", área do sistema (SAE J2012) e significado com fonte |
| **Conversar com a IA** | Prévia do que será enviado, resposta com hipóteses que citam as evidências e o nome do modelo |
| **Investigação › Exportar PDF** | Faixa "DADOS SIMULADOS" em todas as páginas, evidências e limitações |
| **Histórico** | Comparar duas leituras sem confundir dado ausente com defeito resolvido |
| **Ajustes › Sair da demonstração** | Volta ao estado real |

## Se algo der errado

- **Sem internet ou chave recusada:** o app mostra a mensagem do provedor. Use o **Guia offline · sem IA** e
  explique que regras, catálogo e relatórios funcionam offline; a IA é um complemento.
- **Limite de uso atingido (HTTP 429):** espere um minuto ou troque de provedor em Ajustes.
- **Provedor sobrecarregado:** tente de novo em instantes ou troque o modelo em **Avançado**.
- **PDF não abre no celular:** compartilhe para o Drive ou e-mail e abra no computador.

## Perguntas prováveis

- **Funciona num carro de verdade?** Bluetooth clássico, BLE e Wi-Fi estão implementados e cobertos por testes
  com adaptador simulado; **não houve ensaio físico** registrado ([roteiro](../PHYSICAL-TEST-CHECKLIST.md)).
- **A IA dá o diagnóstico?** Não. Ela propõe hipóteses citando as evidências do caso; o app recusa respostas
  fora do contrato ou com evidências inventadas. As regras locais continuam sendo a base e não dependem de internet.
- **Que dados saem do celular?** Só ao conversar com a IA, após confirmação, e direto para o provedor escolhido:
  tipo do veículo, códigos, luz de injeção, leituras, sintomas e a pergunta. Nome do perfil, VIN, notas,
  adaptador, IDs e horários não saem. Ver o [aviso de privacidade](../PRIVACY.md).
- **Onde fica a chave?** Criptografada no celular pelo Android Keystore, enviada só ao provedor escolhido e fora
  dos backups.
- **Como vocês garantem que funciona?** Mais de 160 testes automáticos rodam a cada pull request: sessão ELM
  simulada, parser, telas abertas pelo Robolectric em português e inglês, acessibilidade, catálogo real,
  persistência, consumo, contrato da IA com provedor falso e layout do PDF.
