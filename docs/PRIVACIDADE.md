# Privacidade — BorbaCar Labs

Aviso de privacidade do aplicativo, escrito com base na Lei Geral de Proteção de Dados (Lei nº 13.709/2018).
Projeto acadêmico; este texto descreve o funcionamento do app e não substitui orientação jurídica.

## O que fica só no seu celular

Tudo o que o app guarda fica no armazenamento interno dele, sem conta, sem nuvem e sem envio automático:

- Perfis de veículos (nome que você deu, marca, modelo, ano, versão e anotações).
- Leituras OBD, investigações, sintomas que você descreveu e o histórico.
- Diário: abastecimentos, serviços, valores, hodômetro e lembretes.
- Preferências (tema, animações, lembretes, endereço e token do servidor de IA).

A conexão com o adaptador OBD é local (Bluetooth ou Wi-Fi do próprio adaptador). Os lembretes de
manutenção são calculados no aparelho; a notificação não sai do celular.

## O que pode sair do celular

Somente quando **você** toca em **Análise por IA (demonstração)** em uma investigação e confirma a
prévia exibida. Nesse caso é enviado ao servidor configurado em Ajustes:

- Tipo do veículo: marca, modelo, ano, versão, motor, combustível, transmissão e mercado.
- Códigos de falha observados, estado da luz de injeção e leituras do caso.
- Os sintomas que você escreveu na investigação (até 500 caracteres por campo). Antes do envio, o app
  remove telefones, e-mails, CPF, CNPJ e placas encontrados nesse texto. Nomes, endereços e outros dados
  pessoais escritos livremente **não** são detectados; a prévia mostra o texto exato que será enviado.

**Não são enviados:** nome do perfil, VIN, anotações, nome ou endereço do adaptador,
identificadores internos e horários. A resposta da IA não é salva no caso.

O servidor intermediário repassa o caso a um provedor de IA (Anthropic/Claude ou Google/Gemini,
conforme configurado por quem opera o servidor). No **nível gratuito do Gemini**, o Google pode usar o
conteúdo recebido para melhorar seus produtos; por isso o app envia só os campos acima, sem identificadores,
mas não garante anonimização completa do texto livre dos sintomas.

## Exportações

Relatórios (JSON, TXT, PDF, CSV) só saem do app quando você escolhe onde salvar ou com quem
compartilhar. Eles podem conter o nome do perfil e outros dados que você registrou.

## Seus controles

- Apagar o histórico de um veículo, investigações ou registros do Diário pelo próprio app.
- Desligar a análise por IA apagando o endereço do servidor em Ajustes.
- Desligar os lembretes em Ajustes.
- Remover todos os dados: Configurações do Android › Apps › BorbaCar Labs › Armazenamento › Limpar dados.

## Base legal e finalidade

Os dados são tratados para a finalidade que você pede (diagnóstico, histórico e acompanhamento do seu
veículo), por sua iniciativa. O envio para análise por IA depende de confirmação a cada uso.
