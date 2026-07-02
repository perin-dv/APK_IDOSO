# Cuidar Proximo - App do Usuario

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Cloud Functions](https://img.shields.io/badge/Cloud_Functions-Node.js-339933?style=for-the-badge&logo=node.js&logoColor=white)
![Mercado Pago](https://img.shields.io/badge/Pagamentos-Mercado_Pago-00B1EA?style=for-the-badge)

Aplicativo Android para usuarios e familias acompanharem cuidados de idosos. O projeto centraliza cadastro, agenda, medicamentos, contratacao de profissionais, acompanhamento, pagamentos e perfil em uma experiencia mobile nativa.

Este repositorio faz parte do ecossistema **Cuidar Proximo** e demonstra desenvolvimento Android com Kotlin, Firebase e backend serverless.

## Funcionalidades

- Cadastro de conta e dados do idoso.
- Login e fluxo inicial com splash/home.
- Agenda de cuidados e compromissos.
- Cadastro e listagem de medicamentos.
- Busca e contratacao de profissionais.
- Acompanhamento de atendimentos em andamento.
- Perfil, configuracoes, avaliacoes e suporte.
- Pagamentos com telas para PIX/cartao e historico.
- Integracao com Firebase e Cloud Functions.

## Tecnologias

- Kotlin
- Android Views/XML
- ViewBinding
- AndroidX Navigation
- ViewModel e LiveData
- Firebase Auth
- Cloud Firestore
- Firebase Realtime Database
- Firebase Cloud Functions
- Google Sign-In
- Google Play Services Location
- Node.js 22 para backend
- Mercado Pago SDK no backend

## Estrutura

```txt
app/src/main/java/com/mesawa/cuidarproximo/
  cadastros/
  location/
  medicamentos/
  model/
  nav_bottom/
  splash/
  ui/
    acompanhamento/
    agenda/
    dashboard/
    home/
    notificacao/
    pagamento/
    pedidos/
    profile/
functions/
  index.js
```

## Como executar

### Pre-requisitos

- Android Studio
- JDK 21
- Conta/projeto Firebase
- Firebase CLI para deploy das Functions
- Credenciais do Mercado Pago, se for testar pagamentos reais

### App Android

```bash
git clone https://github.com/perin-dv/APK_IDOSO.git
cd APK_IDOSO
./gradlew assembleDebug
```

No Windows:

```powershell
.\gradlew.bat assembleDebug
```

### Configuracao Firebase

O app usa Firebase e espera o arquivo local `app/google-services.json`, que nao fica versionado por seguranca. O package name do aplicativo e `com.mesawa.cuidarproximo`.

### Cloud Functions

```bash
cd functions
npm install
npm run lint
npm run deploy
```

## Configuracao local

Este repositorio nao versiona arquivos gerados, dependencias instaladas, logs locais ou credenciais de ambiente. Para executar em uma nova maquina, e necessario configurar um projeto Firebase proprio e instalar as dependencias do backend em `functions/`.

## Aprendizados

- Construcao de app Android nativo com multiplas telas.
- Integracao entre app Android e Firebase.
- Separacao de responsabilidades usando ViewModel, adapters e classes de modelo.
- Backend serverless com Node.js e Firebase Functions.
- Fluxos de produto reais: cadastro, agenda, contratacao, pagamento e acompanhamento.

## Evolucao do projeto

O projeto segue em evolucao enquanto aprofundo meus estudos em Android, Firebase, organizacao de codigo, testes e arquitetura de aplicativos mobile.

## Autor

Desenvolvido por [Perin](https://github.com/perin-dv) como projeto de portfolio para Desenvolvimento Android Junior e Engenharia de Software.
