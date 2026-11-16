# Validação da entrega Android

## Atualização visual — 16/11/2026

- Interface nativa adaptada ao site, com fotos e fonte existentes, fundo escuro e destaques vermelhos.
- Busca, filtros por categoria, carrinho, formulário e navegação inferior revisados no emulador Android 15.
- Compilação `assembleDebug` e `assembleDebugAndroidTest`: passaram.
- Testes unitários: 14 aprovados. Instrumentação: 3 cenários aprovados, incluindo os dois anteriores
  e um novo cenário de filtros, carrinho, preservação da categoria e navegação.
- Android Lint: zero erros, 30 avisos não impeditivos.
- Capturas reais com dados simulados em `previews/`; descrição em [VISUAL.md](VISUAL.md).
- ViewModel, Repository e API preservados. Nenhuma gravação de teste foi feita no Azure nesta atualização.

## Validação inicial da entrega MVVM

- Compilação `assembleDebug`: passou, APK gerado.
- `testDebugUnitTest`: 14 testes passaram (10 do ViewModel, 4 de HTTP/Repository).
- `lintDebug`: passou, zero erros. Há avisos de recursos de texto, versões de dependências,
  estilo/autofill e ícone. As versões de Lifecycle seguem o roteiro da aula.
- Azure: GET `/produtos`, `/cardapio` e `/pedidos` responderam HTTP 200 no domínio fornecido.
- CRUD real no Azure: POST de produto temporário, GET comprovando cadastro, PUT alterando valor,
  GET comprovando edição, DELETE e GET comprovando remoção passaram. O registro temporário foi removido;
  nenhum produto anterior foi alterado. Isso valida o servidor, não substitui a demonstração pelo app.
- Testes de tela: os dois cenários de `TelaCrudTest` passaram no emulador Android 15/API 35,
  sem falhas ou testes ignorados. Cadastro atualiza a lista; validação impede envio;
  erro HTTP preserva os campos; recriação e rotação para paisagem preservam o estado sem repetir GET.
  A execução via Gradle passou na primeira rodada; após o ajuste de contraste, o runner do Gradle
  foi interrompido antes de iniciar testes e a validação foi repetida diretamente por instrumentação.
- Inspeção visual: o app carregou a lista real do Azure e manteve a mensagem ao girar
  para paisagem. Encontrado e corrigido baixo contraste dos ícones das barras do sistema
  no tema claro do Android 15. Ainda é recomendada a demonstração no aparelho da squad.
- A apostila de leitura de apoio mencionada no enunciado não estava disponível.

O back-end existente não foi alterado e é mantido em outro repositório.
Este repositório Android não inclui o workflow de deploy da API.

## Correção da execução no Android Studio — 16/11/2026

Reproduzida a falha `Unable to delete directory` na tarefa `dexBuilderDebug`, em arquivos
gerados dentro do OneDrive. Adicionada saída de compilação local opcional, configurada
por `mousseomanos.appBuildDir` em `local.properties`. O código-fonte permanece no mesmo lugar.
Compilação nova e recompilação forçada (`--rerun-tasks`) validadas com as opções de build
usadas pelo Android Studio. O app precisa ser executado novamente após sincronizar o Gradle.
