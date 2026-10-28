# Reformulação — continuidade após a auditoria

> Prioridade revisada em 21/10/2026: [direção de produto R1–R7](REFORMULACAO-PRODUTO-2026-10.md). A sequência futura abaixo é histórica; checkpoints 4.1–4.4 continuam entregues. R1a entrega a navegação/Hoje na 0.6.4; R1 ainda inclui tema claro, ícones e refinamento de conteúdo. Ensaio físico permanece antecipado em R2. Veja [validação R1a](VALIDACAO-R1A.md).

Plano do usuário: auditoria/contratos, especificação visual e implementação incremental. Composição Foco selecionada e continuação autorizada. O plano anterior de IA permanece como registro; a reformulação de dados e experiência passa à frente de sua execução.

## Checkpoints

| Etapa | Entrega | Regra para avançar |
|---|---|---|
| 4.1 | Build verificado, certificado original, saídas sem sobrescrita, recursos e regressões | Registrar resultados; fontes e dados do app preservados |
| 4.2 | Contratos de proveniência e catálogo local | Fontes/escopo/direitos por atributo; migração e desconhecidos testados |
| 4.3 | Garagem Foco e seleção encadeada | Mesmo ID para perfis antigos; novas instalações sem seleção presumida |
| 4.4 | Painel Foco | Valor/unidade/tempo/qualidade; zero não substitui ausência; gráfico histórico explícito |
| 4.5 | Falhas e investigações | Capturas por serviço; evidências e revisões imutáveis; formatos antigos acessíveis |
| 4.6 | Relatórios/exportações | Mesmos fatos e versões; PDF ensaiado separadamente |
| 4.7 | Validação física | Telefone/adaptador/veículo identificados; evidência por função |
| 4.8 | AutoSense AI | Após as validações previstas; dados estruturados/fontes e nenhum acesso a comandos |

## Contratos que orientam as próximas mudanças

- Perfil tem ID estável, distinto de nome comercial, código de fornecedor e registro de catálogo.
- Ano de fabricação e ano-modelo são atributos distintos. Motor/transmissão/mercado desconhecidos não são deduzidos do nome do veículo.
- Origem, qualidade e verificação são dimensões separadas. `FactProvenance` implementa LIVE_OBD, CATALOG, USER_INPUT, DERIVED, SIMULATION e UNKNOWN_LEGACY no checkpoint 4.2. A persistência por atributo está ligada aos perfis; os contratos gerais ainda não substituem telemetria, DTCs ou snapshots históricos.
- Leituras do adaptador e do veículo têm escopos próprios. ATRV não é teste de saúde da bateria.
- Valor válido é finito; ausência permanece sem valor; zero válido é preservado. Conversão OBD mantém origem da leitura; agregados referenciam entradas e herdam seu contexto real/simulado.
- Aquisição, gravação, captura e caso possuem identidades distintas. Resultados assíncronos pertencem ao contexto de solicitação.
- Catálogo local versionado guarda fonte/localização/edição/consulta/escopo/direitos e revisão por atributo; opções ilustrativas não entram como catálogo real.
- Suporte implementado, documentação, anúncio na sessão, resposta observada e validação física são registros distintos, por função.
- Casos congelam perfil, medições e horários originais. Revisar sintomas não faz nova coleta; análise futura também deve manter vínculo à entrada e versões.
- Documentos 1/2/3 e regras associadas são preservados. Uma mudança de apresentação não pode alterar a reprodução histórica usada na integridade.
- Demonstração tem contexto próprio; registros antigos não são excluídos nem reclassificados por suposição.
- Fonte acessível não concede direito de redistribuição. Nenhum material proprietário do Carista é incorporado.

## Limites do checkpoint atual

O checkpoint 4.1 foi concluído sem mudança funcional. O 4.2 entrega a 0.6.1 com perfis v2/migração, proveniência e núcleo de catálogo local vazio. Instalações novas não recebem veículo presumido; perfis antigos são preservados. A migração não escreve em históricos/SQLite, transportes ou regras. Critérios e evidências: [validação 4.2](VALIDACAO-CHECKPOINT-4.2.md), [contratos](CONTRATOS-DE-VEICULOS.md) e [catálogo](CATALOGO-DE-VEICULOS.md).

O checkpoint 4.3 entrega a 0.6.2: Garagem Foco, busca, identificação por atributo, cadastro em três passos, perfil parcial e seletor encadeado com “Não sei”. O esquema novo 4 congela os metadados do perfil e suas fontes em casos/exportações; 1/2/3 mantêm a reprodução histórica. A base veicular real continua vazia e a UI informa isso. Fontes/direitos e revisão técnica seguem necessários para publicar configurações reais. Evidências e limites em [validação 4.3](VALIDACAO-CHECKPOINT-4.3.md).

O checkpoint 4.4 entrega a 0.6.3: Painel Foco, estados/horários por sinal, gráfico histórico selecionável e proteção da gravação durante pausa. Consulte PAINEL-FOCO.md e VALIDACAO-CHECKPOINT-4.4.md. Próximo checkpoint 4.5: Falhas e investigações, com resultados por serviço, evidências e revisões preservadas; sem mudar transportes ou formatos históricos por apresentação.

A política de continuidade mantém o certificado de desenvolvimento; não equivale a distribuição de produção. Não há downgrade compatível para sincronizar edições v2 de volta à cópia v1.

Cada checkpoint deve apresentar arquivos, diff, testes/resultados, riscos e critérios atendidos. Testes físicos, simulados e automatizados são reportados separadamente.

Atualização 23/10/2026: R1b / 0.6.5 acrescenta aparência, ícones, detalhes dos sensores e carregamento de relatórios antigos. Ver VALIDACAO-R1B.md. Próximo foco: leitura guiada e ensaio piloto; sem alegação de compatibilidade física.

Atualização 24/10/2026: 0.6.6 entrega R1c visual/interação e partes de R2/R3 (leitura guiada e comparação). Consulte EXPERIENCIA-0.6.6.md para limites e sequência ainda autorizada. R2 não está concluída sem ensaio físico; R3 ainda não possui linha do tempo unificada.

Atualização 24/10/2026: 0.6.7 acrescenta diário offline (partes de R3/R4/R5), cálculo de consumo declarado, revisões e exportação própria. Escopo e pendências em DIARIO-0.6.7.md. Próximas dependências: linha do tempo/vínculos, backup/restauração, catálogo e validação física. Exportação de diário não é backup completo.



Atualização 26/10/2026 — 0.6.8: revisão de densidade/navegação Foco, destinos sem atalhos repetidos, opções em detalhes, coleções separadas no Histórico e primeira base factual MY19 (16 configurações). Ver FOCO-0.6.8.md e CATALOGO-MY19-0.6.8.md. Sem mudança em transportes, bancos ou formatos históricos.
