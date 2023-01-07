Apache Fineract: uma plataforma para microfinanças
============
[![Swagger Validation](https://validator.swagger.io/validator?url=https://demo.fineract.dev/fineract-provider/swagger-ui/fineract.yaml)](https://validator.swagger.io/validator/debug?url=https://demo.fineract.dev/fineract-provider/swagger-ui/fineract.yaml) [![build](https://github.com/apache/fineract/actions/workflows/build.yml/badge.svg)](https://github.com/apache/fineract/actions/workflows/build.yml) [![Docker Hub](https://img.shields.io/docker/pulls/apache/fineract.svg?logo=Docker)](https://hub.docker.com/r/apache/fineract)  [![Docker Build](https://img.shields.io/docker/cloud/build/apache/fineract.svg?logo=Docker)](https://hub.docker.com/r/apache/fineract/builds) [![Technical Debt](https://sonarcloud.io/api/project_badges/measure?project=apache_fineract&metric=sqale_index)](https://sonarcloud.io/summary/new_code?id=apache_fineract)


O Fineract é uma plataforma madura com APIs abertas que fornece uma solução de core banking confiável, robusta e acessível para instituições financeiras que oferecem serviços aos 3 bilhões de subbancários e não bancários do mundo.

[Dê uma olhada no FAQ em nosso Wiki em apache.org](https://cwiki.apache.org/confluence/display/FINERACT/FAQ) se este README não responder o que você está procurando.  [Visite nosso Painel do JIRA](https://issues.apache.org/jira/secure/Dashboard.jspa?selectPageId=12335824) para encontrar problemas para trabalhar, ver no que os outros estão trabalhando ou abrir novos problemas.

[![Código agora! (Gitpod)](https://gitpod.io/button/open-in-gitpod.svg)](https://gitpod.io/#https://github.com/apache/fineract)
para começar a contribuir para este projeto no IDE on-line GitPod.io baseado na web imediatamente!
(Inicialmente, você pode ter que pressionar F1 para localizar o comando e executar "Java: Start Language Server".)
É claro que também é possível contribuir com um ambiente de desenvolvimento local "tradicional" (veja abaixo).

Comunidade
=========

Se você estiver interessado em contribuir para este projeto, mas talvez não saiba bem como e por onde começar, por favor [insira-se à nossa lista de discussão de desenvolvedores](http://fineract.apache.org/#contribute), ouça nosso conversas, participe de tópicos e apenas nos envie um "Olá!" e-mail de apresentação; somos um grupo amigável e estamos ansiosos para ouvir de você.


Requisitos
============
* Java >= 17 (Azul Zulu JVM é testado por nosso CI no GitHub Actions)
* MariaDB 10.9

Você pode executar a versão necessária do servidor de banco de dados em um contêiner, em vez de instalá-lo, assim:

    docker run --name mariadb-10.9 -p 3306:3306 -e MARIADB_ROOT_PASSWORD=mysql -d mariadb:10.9

e pare e destrua assim:

    docker rm -f mariadb-10.9

Esteja ciente de que este banco de dados de contêiner de banco de dados mantém seu estado dentro do contêiner e não no sistema de arquivos do host. Ele é perdido quando você destrói (rm) este contêiner. Isso normalmente é bom para o desenvolvimento.Consulte [Advertências: onde armazenar dados na documentação do contêiner do banco de dados](https://hub.docker.com/_/mariadb). como torná-lo persistente em vez de efêmero.

O Tomcat v9 é necessário apenas se você deseja implantar o Fineract WAR em um contêiner de servlet externo separado. Observe que você não precisa instalar o Tomcat para desenvolver o Fineract ou executá-lo na produção se usar o JAR independente, que incorpora de forma transparente um contêiner de servlet usando o Spring Boot. (Até o FINERACT-730, o Tomcat 7/8 também era suportado, mas agora o Tomcat 9 é necessário.)

IMPORTANTE: se você usa MySQL ou MariaDB
============

Recentemente (após a versão 1.7.0), introduzimos um tratamento aprimorado de data e hora no Fineract. A partir de agora, a data e a hora são armazenadas em UTC e estamos aplicando o fuso horário UTC mesmo no driver JDBC, por exemplo. g. para MySQL:

```
serverTimezone=UTC&useLegacyDatetimeCode=false&sessionVariables=time_zone=‘-00:00’
```

__DO__: Se você usar o MySQL como seu banco de dados Fineract, a seguinte configuração é altamente recomendada:

* Execute o aplicativo em UTC (a linha de comando padrão em nossa imagem do Docker já possui os parâmetros necessários)
* Execute o servidor de banco de dados MySQL em UTC (se você usar serviços gerenciados como AWS RDS, esse deve ser o padrão de qualquer maneira, mas seria bom verificar novamente)

__DON'T__: Caso a instância do Fineract e o servidor MySQL __not__ sejam executados em UTC, pode acontecer o seguinte:

* O MySQL está salvando valores de data e hora de maneira diferente do PostgreSQL
* Cenário de exemplo: se a instância do Fineract for executada no fuso horário: GMT+2 e a data e hora local for 2022-08-11 17:15 ...
* ... então __PostgreSQL saves__ o LocalDateTime como está: __2022-08-11 17:15__
* ... e __MySQL saves__ o LocalDateTime em UTC: __2022-08-11 15:15__
* ... mas quando nós __read__ a data hora de PostgreSQL __or__ do MySQL, então ambos os sistemas nos dão os mesmos valores: __2022-08-11 17:15 GMT+2__

Se uma instância do Fineract usada anteriormente não foi executada em UTC (compatibilidade com versões anteriores), todas as datas anteriores serão lidas incorretamente pelo MySQL/MariaDB.Isso pode causar problemas ao executar os scripts de migração do banco de dados.

__RECOMMENDATION__: você precisa mudar todas as datas em seu banco de dados pelo deslocamento de fuso horário que sua instância Fineract usou.

Instruções de como concorrer ao desenvolvimento local
============

Execute os seguintes comandos:
1. `./gradlew createDB -PdbName=fineract_tenants`
1. `./gradlew createDB -PdbName=fineract_default`
1. `./gradlew bootRun`


Instruções para construir o arquivo JAR
============
1. Clone o repositório ou baixe e extraia o arquivo compactado para seu diretório local.
2. Execute `./gradlew clean bootJar` para criar um arquivo JAR totalmente independente nativo da nuvem moderna que será criado no diretório `fineract-provider/build/libs`.
3. Como não temos permissão para incluir um driver JDBC no JAR integrado, baixe um driver JDBC de sua escolha. Por exemplo: `wget https://downloads.mariadb.com/Connectors/java/connector-java-2.7.5/mariadb-java-client-2.7.5.jar`
4. Inicie o jar e passe o diretório onde você baixou o driver JDBC como loader.path, por exemplo: `java -Dloader.path=. -jar fineract-provider/build/libs/fineract-provider.jar` (não requer Tomcat externo)

NOTA: ainda não podemos atualizar para a versão 3.0.x do driver MariaDB; tem que esperar até 3.0.4 sair para uma correção de bug.

Os detalhes da conexão do banco de dados dos locatários são configurados [por meio de variáveis de ambiente (como no contêiner do Docker)](#instructions-to-run-using-docker-and-docker-compose), por exemplo assim:

    export FINERACT_HIKARI_PASSWORD=verysecret
    ...
    java -jar fineract-provider.jar


Segurança
============
NOTA: Os esquemas de autenticação HTTP Basic e OAuth2 são mutuamente exclusivos. Você não pode habilitar os dois ao mesmo tempo. O Fineract verifica essas configurações na inicialização e falhará se mais de um esquema de autenticação estiver ativado.

Autenticação básica HTTP
------------
Por padrão, o Fineract é configurado com um esquema de autenticação básica HTTP, então você não precisa fazer nada se quiser usá-lo. Mas se você quiser escolher explicitamente esse esquema de autenticação, há duas maneiras de ativá-lo:
1. Use variáveis de ambiente (melhor escolha se você executar com Docker Compose):
```
FINERACT_SECURITY_BASICAUTH_ENABLED=true
FINERACT_SECURITY_OAUTH_ENABLED=false
```
2. Use os parâmetros da JVM (melhor escolha se você executar o Spring Boot JAR):
```
java -Dfineract.security.basicauth.enabled=true -Dfineract.security.oauth.enabled=false -jar fineract-provider.jar
```

Autenticação OAuth2
------------
Há também um esquema de autenticação OAuth2 disponível. Novamente, duas maneiras de habilitá-lo:
1. Use variáveis de ambiente (melhor escolha se você executar com Docker Compose):
```
FINERACT_SECURITY_BASICAUTH_ENABLED=false
FINERACT_SECURITY_OAUTH_ENABLED=true
```
2. Use parâmetros JVM (melhor escolha se você executar o Spring Boot JAR):
```
java -Dfineract.security.basicauth.enabled=false -Dfineract.security.oauth.enabled=true -jar fineract-provider.jar
```

Autenticação de dois fatores
------------
Você também pode ativar a autenticação 2FA. Dependendo de como você inicia o Fineract, adicione o seguinte:

1. Use a variável de ambiente (melhor escolha se você executar com o Docker Compose):
```
FINERACT_SECURITY_2FA_ENABLED=true
```
2. Use o parâmetro JVM (melhor escolha se você executar o Spring Boot JAR):
```
-Dfineract.security.2fa.enabled=true
```


Instruções para criar um arquivo WAR
============
1. Clone o repositório ou baixe e extraia o arquivo compactado para seu diretório local.
2. Execute `./gradlew :fineract-war:clean :fineract-war:war` para construir um arquivo WAR tradicional que será criado no diretório `fineract-war/build/libs`.
3. Implemente este WAR em seu Contêiner de Servlet Tomcat v9.

Recomendamos usar o JAR em vez da implantação do arquivo WAR, porque é muito mais fácil.

Observe que, com a versão 1.4, a configuração do pool de banco de dados de locatários mudou de Tomcat DBCP em XML para um Hikari integrado, configurado por variáveis de ambiente, veja acima.


Instruções para executar testes de integração
============
> Observe que, se esta for a primeira vez que você acessa o banco de dados MySQL, talvez seja necessário redefinir sua senha.

Execute os seguintes comandos:
1. `./gradlew createDB -PdbName=fineract_tenants`
1. `./gradlew createDB -PdbName=fineract_default`
1. `./gradlew clean test`


Instruções para executar e depurar no Eclipse IDE
============

É possível executar o Fineract no Eclipse IDE e também depurar o Fineract usando os recursos de depuração do Eclipse.
Para fazer isso, você precisa criar os arquivos do projeto Eclipse e importar o projeto para um espaço de trabalho Eclipse:

1. Crie arquivos de projeto Eclipse no projeto Fineract executando `./gradlew cleanEclipse eclipse`
2. Importe o projeto fineract-provider para sua área de trabalho do Eclipse (File->Import->General->Existing Projects into Workspace, choose root directory fineract/fineract-provider)
3. Faça uma compilação limpa do projeto no Eclipse (Project->Clean...)
3. Execute / depure o Fineract clicando com o botão direito do mouse na classe org.apache.fineract.ServerApplication e escolhendo Run As / Debug As -> Java Application. Todos os recursos normais de depuração do Eclipse (pontos de interrupção, pontos de controle, etc.) devem funcionar conforme o esperado.

Se você alterar as configurações do projeto (dependências, etc.) no Gradle, refaça a etapa 1 e atualize o projeto no Eclipse.

Você também pode usar o suporte do Eclipse Junit para executar testes no Eclipse (Run As->Junit Test)

Por fim, a modificação do código-fonte no Eclipse aciona automaticamente a substituição do código quente para uma instância em execução, permitindo que você teste imediatamente suas alterações

Instruções para executar usando Docker e docker-compose
===================================================

É possível fazer uma instalação 'one-touch' do Fineract usando containers (AKA "Docker").
O Fineract agora inclui a interface do usuário da web do aplicativo da comunidade mifos em sua implantação do docker.
Agora você pode executar e testar o fineract com uma GUI diretamente das compilações do docker combinadas.
Isso inclui o banco de dados em execução em um contêiner.

Como Pré-requisitos, você deve ter `docker` e `docker-compose` instalados em sua máquina; 
[Instalação do Docker](https://docs.docker.com/install/) and
[Instalação do Docker Compose](https://docs.docker.com/compose/install/).

Alternativamente, você também pode usar [Podman](https://github.com/containers/libpod)
(e.g. via `dnf install podman-docker`), e [Podman Compose](https://github.com/containers/podman-compose/)
(e.g. via `pip3 install podman-compose`) em vez do Docker.

Agora, para executar uma nova instância do Fineract, você pode simplesmente:

1. `git clone https://github.com/apache/fineract.git ; cd fineract`
1. para Windows, use `git clone https://github.com/apache/fineract.git --config core.autocrlf=input ; cd fineract`
1. `./gradlew :fineract-provider:jibDockerBuild -x test`
1. `docker-compose up -d`
1. fineract (back-end) está rodando em https://localhost:8443/fineract-provider/
1. Esperar por https://localhost:8443/fineract-provider/actuator/health to return `{"status":"UP"}`
1. você deve ir para https://localhost:8443 e lembre-se de aceitar o certificado SSL autoassinado da API uma vez em seu navegador, caso contrário, você receberá uma mensagem bastante enganosa da interface do usuário.
1. community-app (UI) está sendo executado em http://localhost:9090/?baseApiUrl=https://localhost:8443/fineract-provider&tenantIdentifier=default
1. faça login usando o padrão _username_ `mifos` e _password_ `password`

https://hub.docker.com/r/apache/fineract tem uma imagem de contêiner pré-construída deste projeto, construída continuamente.

Você deve especificar o URL JDBC do banco de dados de locatários MySQL passando-o para o contêiner `fineract` por meio de variáveis de ambiente; 
please consult the [`docker-compose.yml`](docker-compose.yml) for exact details how to specify those.
_(Note that in previous versions, the `mysqlserver` environment variable used at `docker build` time instead of at
`docker run` time did something similar; this has changed in [FINERACT-773](https://issues.apache.org/jira/browse/FINERACT-773)),
and the `mysqlserver` environment variable is now no longer supported.)_


Configuração do pool de conexões
=============================

Verifique `application.properties` para ver quais configurações do pool de conexão podem ser ajustadas. As variáveis de ambiente associadas são prefixadas com `FINERACT_HIKARI_*`. You can find more information about specific connection pool settings (Hikari) at https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby

NOTA: manteremos a compatibilidade com versões anteriores até um dos próximos lançamentos para garantir que tudo esteja funcionando conforme o esperado. As variáveis de ambiente prefixadas `fineract_tenants_*` ainda podem ser usadas para configurar a conexão com o banco de dados, mas recomendamos fortemente o uso de `FINERACT_HIKARI_*` com mais opções.

Configuração SSL
=================

Leia também [o documento relacionado ao HTTPS](fineract-doc/src/docs/en/deployment.adoc#https).

Por padrão, o SSL está ativado, mas todas as propriedades relacionadas ao SSL agora são ajustáveis. O SSL pode ser desativado definindo a variável de ambiente `FINERACT_SERVER_SSL_ENABLED` como false. Se você fizer isso, certifique-se também de alterar a porta do servidor para `8080` por meio da variável `FINERACT_SERVER_PORT`, apenas para manter as convenções.
Agora você pode escolher facilmente um armazenamento de chaves SSL diferente definindo `FINERACT_SERVER_SSL_KEY_STORE` com um caminho para um armazenamento de chaves diferente (não incorporado). A senha pode ser definida via `FINERACT_SERVER_SSL_KEY_STORE_PASSWORD`. Consulte o arquivo `application.properties` e a documentação mais recente do Spring Boot (https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html) para obter mais detalhes.


Configuração do Tomcat
====================

Consulte `application.properties` e a documentação oficial do Spring Boot (https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html) sobre como fazer o ajuste de desempenho para Tomcat. Nota: você pode definir agora o tamanho aceitável do POST do formulário (o padrão é 2 MB) por meio da variável de ambiente `FINERACT_SERVER_TOMCAT_MAX_HTTP_FORM_POST_SIZE`.


Instruções para executar no Kubernetes
=================================

Clusters gerais
----------------

Você também pode executar o Fineract usando contêineres em um cluster Kubernetes.
Certifique-se de configurar e conectar-se ao seu cluster Kubernetes.
Você pode seguir [este](https://cwiki.apache.org/confluence/display/FINERACT/Install+and+configure+kubectl+and+Google+Cloud+SDK+on+ubuntu+16.04) guia para configurar um Cluster do Kubernetes no GKE.Certifique-se de substituir `apache-fineract-cn` por `apache-fineract`

Agora, por exemplo no shell do Google Cloud, execute os seguintes comandos:

1. `git clone https://github.com/apache/fineract.git ; cd fineract/kubernetes`
1. `./kubectl-startup.sh`

Para desligar e redefinir seu Cluster, execute:

    ./kubectl-shutdown.sh

Usando o Minikube
--------------

Como alternativa, você pode executar o fineract em um cluster kubernetes local usando [minikube](https://minikube.sigs.k8s.io/docs/).
As Prerequisites, you must have `minikube` and `kubectl` installed on your machine; see
[Minikube & Kubectl install](https://kubernetes.io/docs/tasks/tools/install-minikube/).

Agora, para executar uma nova instância do Fineract no Minikube, você pode simplesmente:

1. `git clone https://github.com/apache/fineract.git ; cd fineract/kubernetes`
1. `minikube start`
1. `./kubectl-startup.sh`
1. `minikube service fineract-server --url --https`
1. O Fineract agora está sendo executado no URL impresso (observe HTTP), que você pode verificar, por exemplo, usando:

    http --verify=no --timeout 240 --check-status get $(minikube service fineract-server --url --https)/fineract-provider/actuator/health

Para verificar o status de seus contêineres em seu cluster minikube Kubernetes local, execute:

    minikube dashboard

Você pode verificar os logs do Fineract usando:

    kubectl logs deployment/fineract-server

Para desligar e redefinir seu cluster, execute:

    ./kubectl-shutdown.sh

Para desligar e redefinir seu cluster, execute:

    minikube ssh

    sudo rm -rf /mnt/data/

Temos [alguns problemas em aberto no JIRA com ideias de aprimoramento relacionadas ao Kubernetes](https://jira.apache.org/jira/browse/FINERACT-783?jql=labels%20%3D%20kubernetes%20AND%20project%20%3D%20%22Apache%20Fineract%22%20) which you are welcome to contribute to.


Instruções para baixar o wrapper do Gradle
============
O arquivo binário gradle/wrapper/gradle-wrapper.jar é verificado no repositório de origem Git do projeto,
mas não existirá em sua cópia da base de código Fineract se você baixou um arquivo de origem liberado de apache.org.
Nesse caso, você precisa baixá-lo usando os comandos abaixo:

    wget --no-check-certificate -P gradle/wrapper https://github.com/apache/fineract/raw/develop/gradle/wrapper/gradle-wrapper.jar

(or)

    curl --insecure -L https://github.com/apache/fineract/raw/develop/gradle/wrapper/gradle-wrapper.jar > gradle/wrapper/gradle-wrapper.jar


Instruções para executar o Apache RAT (ferramenta de auditoria de lançamento)
============
1. Extraia o arquivo compactado para seu diretório local.
2. Run `./gradlew rat`. Um relatório será gerado sob build/reports/rat/rat-report.txt


Instruções para ativar o ActiveMQ
============
A configuração de mensagens é desativada por padrão. Se você quiser ativá-lo e registrar alguns ouvintes de mensagens, o aplicativo precisa ser iniciado com o perfil Spring adequado, ou seja, `-Dspring.profiles.active=activeMqEnabled` (ou uma das outras formas Spring de configurá-lo).

Checkstyle and Spotless
============

Este projeto reforça suas convenções de código usando [checkstyle.xml](config/checkstyle/checkstyle.xml) até Checkstyle e [fineract-formatting-preferences.xml](config/fineract-formatting-preferences.xml) até Spotless. Eles são configurados para serem executados automaticamente durante a compilação normal do Gradle e falham se houver alguma violação detectada. You can run the following command to automatically fix spotless violations:

    `./gradlew spotlessApply`

Como algumas verificações estão presentes tanto no Checkstyle quanto no Spotless, o mesmo comando pode ajudá-lo a corrigir algumas das violações do Checkstyle (mas não todas, outras violações do Checkstyle precisam ser corrigidas manualmente).

Você também pode verificar se há violações Spotless (apenas; mas normalmente não é necessário, porque a compilação regular completa já inclui isso de qualquer maneira):

    `./gradlew spotlessCheck`

Recomendamos que você configure seu Java IDE favorito para corresponder a essas convenções. Para Eclipse, você pode ir para
Window > Java > Code Style e importar o nosso [config/fineractdev-formatter.xml](config/fineractdev-formatter.xml) na seção formatador e [config/fineractdev-cleanup.xml](config/fineractdev-cleanup.xml) na seção Limpar. O mesmo arquivo de configuração fineractdev-formatter.xml (que pode ser usado no Eclipse IDE) também é usado pelo Spotless para verificar violações e código de formatação automática na CLI.
Você também pode usar o Checkstyle diretamente no seu IDE (mas não necessariamente, pode ser mais conveniente para você).  Para Eclipse, use https://checkstyle.org/eclipse-cs/ e carregue nosso checkstyle.xml nele, para IntelliJ você pode usar [CheckStyle-IDEA](https://plugins.jetbrains.com/plugin/1065-checkstyle-idea).


Relatórios de Cobertura de Código
============

O projeto usa Jacoco para medir a cobertura do código de testes de unidade, para gerar um relatório, execute o seguinte comando:

    `./gradlew clean build jacocoTestReport`

Os relatórios gerados podem ser encontrados no diretório build/code-coverage.


Versões
============

A versão estável mais recente pode ser visualizada no ramo de desenvolvimento: [Latest Release on Develop](https://github.com/apache/fineract/tree/develop "Último lançamento").

O andamento deste projeto pode ser visualizado aqui: [Ver change log](https://github.com/apache/fineract/blob/develop/CHANGELOG.md "Latest release change log")


Licença
============

Este projeto está licenciado sob a Licença Apache Versão 2.0. Consulte <https://github.com/apache/incubator-fineract/blob/develop/LICENSE.md> para referência.

A biblioteca cliente Connector/J JDBC Driver da MariaDB.org, que é licenciada sob a LGPL, é usada no 
desenvolvimento ao executar testes de integração que usam a biblioteca Liquibase. Esse driver JDBC, no entanto, 
não está incluído e distribuído com o produto Fineract e não é necessário para usar o produto.
Se você é um desenvolvedor e se opõe ao uso do driver JDBC Connector/J licenciado pela LGPL, 
simplesmente não execute os testes de integração que usam a biblioteca Liquibase e/ou use outro driver JDBC.
Conforme discutido em [LEGAL-462](https://issues.apache.org/jira/browse/LEGAL-462), este projeto, portanto, 
está em conformidade com a [política de licença de terceiros da Apache Software Foundation](https://www. .apache.org/legal/resolved.html).


API da plataforma Apache Fineract
============

A API para Fineract está documentada em [apiLive.htm](fineract-provider/src/main/resources/static/api-docs/apiLive.htm), e a [apiLive.htm can be viewed on Fineract.dev](https://fineract.apache.org/legacy-docs/apiLive.htm "API Documentation").  Se você tiver sua própria instância do Fineract em execução, poderá encontrar esta documentação em [/fineract-provider/api-docs/apiLive.htm](https://localhost:8443/fineract-provider/api-docs/apiLive.htm).

A documentação do Swagger (work in progress; see [FINERACT-733](https://issues.apache.org/jira/browse/FINERACT-733)) can be accessed under [/fineract-provider/swagger-ui/index.html](https://localhost:8443/fineract-provider/swagger-ui/index.html) and [live Swagger UI here on Fineract.dev](https://demo.fineract.dev/fineract-provider/swagger-ui/index.html).

O Apache Fineract oferece suporte à geração de código do cliente usando [Swagger Codegen](https://github.com/swagger-api/swagger-codegen) baseado no [OpenAPI Specification](https://swagger.io/specification/). Para obter mais instruções sobre como gerar o código do cliente, consulte [docs/developers/swagger/client.md](docs/developers/swagger/client.md).


API clients (Web UIs, Mobile, etc.)
============

* https://github.com/openMF/community-app/ é a interface do usuário da Web do aplicativo cliente de referência "tradicional" para a API oferecida por este projeto
* https://github.com/openMF/web-app é a reescrita de interface do usuário de próxima geração também usando a API deste projeto
* https://github.com/openMF/android-client é um cliente Android Mobile App para a API deste projeto
* https://github.com/openMF tem mais projetos relacionados


Demonstrações on-line
============

* [fineract.dev](https://www.fineract.dev)sempre executa a versão mais recente deste código
* [demo.mifos.io](https://demo.mifos.io) Uma conta de demonstração é fornecida para que os usuários experimentem a funcionalidade do Community App. Os usuários podem usar "mifos" para USERNAME e "password" para PASSWORD (sem aspas).
* [Swagger-UI Demo video](https://www.youtube.com/watch?v=FlVd-0YAo6c) Este é um vídeo de demonstração da documentação do Swagger-UI, mais informações [aqui](https://github.com/apache/fineract#swagger-ui-documentation).



Desenvolvedores
============
Por favor, veja <https://cwiki.apache.org/confluence/display/FINERACT/Contributor%27s+Zone> para a página wiki dos desenvolvedores.

Por favor, consulte <https://cwiki.apache.org/confluence/display/FINERACT/Fineract+101> pela primeira contribuição a este projeto.

Por favor, veja <https://cwiki.apache.org/confluence/display/FINERACT/How-to+articles> para obter detalhes técnicos para começar.

Por favor, visite [our JIRA Dashboard](https://issues.apache.org/jira/secure/Dashboard.jspa?selectPageId=12335824) para encontrar problemas para trabalhar, ver no que os outros estão trabalhando ou abrir novos problemas.


Demonstração em vídeo
============

Apache Fineract / Mifos X Demo (novembro de 2016) - <https://www.youtube.com/watch?v=h61g9TptMBo>

Documentação Swagger-UI
============

Usamos Swagger-UI para gerar e manter nossa documentação de API, você pode ver o vídeo de demonstração [aqui](https://www.youtube.com/watch?v=FlVd-0YAo6c) ou uma versão ao vivo [aqui](https://demo.fineract.dev/fineract-provider/swagger-ui/index.html). Se você estiver interessado em saber mais sobre o Swagger-UI, verifique o [site] (https://swagger.io/).

Governança e Políticas
=======================

[Tornando-se um Committer](https://cwiki.apache.org/confluence/display/FINERACT/Becoming+a+Committer)
documenta o processo pelo qual você pode se tornar um committer neste projeto.


Diretrizes de tratamento de erros
------------------
* Ao capturar exceções, lance-as novamente ou registre-as. De qualquer forma, inclua a causa raiz usando `catch (SomeException e)` e então `throw AnotherException("..details..", e)` ou `LOG.error("...contexto...", e)`.
* Blocos catch completamente vazios são MUITO suspeitos! Tem certeza de que deseja apenas "engolir" uma exceção?  Realmente, 100% totalmente absolutamente certo?? ;-) Essas "exceções normais que acontecem às vezes, mas na verdade não são realmente erros" são quase sempre uma má ideia, podem ser um problema de desempenho e normalmente são uma indicação de outro problema - por exemplo, o uso de uma API errada que lança uma exceção para uma condição esperada, quando na verdade você gostaria de usar outra API que retornasse algo vazio ou opcional.
* Em testes, você normalmente nunca captura exceções, mas apenas as propaga, com `@Test void testXYZ() throws SomeException, AnotherException`..., para que o teste falhe se a exceção acontecer.  A menos que você realmente queira testar a ocorrência de um problema - nesse caso, use [JUnit's Assert.assertThrows()](https://github.com/junit-team/junit4/wiki/Exception-testing) (mas não `@Test(expected = SomeException.class)`).
* Never catch `NullPointerException` & Co.

Diretrizes de registro
------------------
* Nós usamos [SLF4J](http://www.slf4j.org) como nossa API de registro.
* Nunca, jamais, use `System.out` and `System.err` or `printStackTrace()` anywhere, but always `LOG.info()` or `LOG.error()` instead.
*Usar espaço reservado (`LOG.error("Could not... details: {}", something, exception)`) e nunca Concatenação de strings (`LOG.error("Could not... details: " + something, exception)`)
* Qual nível de registro é apropriado?
  * `LOG.error()` deve ser usado para informar um "operador" executando o Fineract que supervisiona os logs de erro de uma condição inesperada. Isso inclui problemas técnicos com um "ambiente" externo (por exemplo, não é possível acessar um banco de dados) e situações que são prováveis erros que precisam ser corrigidos no código.  Eles NÃO incluem, por exemplo, erros de validação para solicitações de API recebidas - that is signaled through the API response - and does (should) not be logged as an error.  (Note that there is no _FATAL_ level in SLF4J; a "FATAL" event should just be logged as an _ERROR_.)
  * `LOG.warn()` should be using sparingly.  Make up your mind if it's an error (above) - or not!
  * `LOG.info()` pode ser usado principalmente para ações únicas realizadas durante a inicialização.  It should typically NOT be used to print out "regular" application usage information.  The default logging configuration always outputs the application INFO logs, and in production under load, there's really no point to constantly spew out lots of information from frequently traversed paths in the code about what's going on.  (Metrics are a better way.)  `LOG.info()` *can* be used freely in tests though.
  * `LOG.debug()` pode ser usado em qualquer lugar do código para registrar coisas que podem ser úteis durante investigações de problemas específicos.  They are not shown in the default logging configuration, but can be enabled for troubleshooting.  Developers should typically "turn down" most `LOG.info()` which they used while writing a new feature to "follow along what happens during local testing" to `LOG.debug()` for production before we merge their PRs.
  * `LOG.trace()` is not used in Fineract.

Requisições pull
-------------

Solicitamos que sua mensagem de confirmação inclua um problema FINERACT JIRA, recomendado para ser colocado entre parênteses, adicione o final da primeira linha.  Start with an upper case imperative verb (not past form), and a short but concise clear description. (E.g. _Add enforced HideUtilityClassConstructor checkstyle (FINERACT-821)_ or _Fix inability to reschedule when interest accrued larger than EMI (FINERACT-1109)_ etc.).

Se o seu PR não passar em nossa compilação de CI devido a uma falha no teste, então:

1. Entenda se a falha se deve ao seu PR ou a um teste instável não relacionado.
1. Se você suspeitar que é por causa de um teste "esquisito", e não devido a uma mudança em seu PR, então, por favor, não espere simplesmente que um mantenedor ativo venha ajudá-lo, mas, em vez disso, seja um colaborador proativo do projeto - veja próximos passos.  Do understand that we may not review PRs that are not green - it is the contributor's (that's you!) responsability to get a proposed PR to pass the build, not primarily the maintainers.
1. Search for the name of the failed test on https://issues.apache.org/jira/, e.g. for `AccountingScenarioIntegrationTest` you would find [FINERACT-899](https://issues.apache.org/jira/browse/FINERACT-899).
1. If you happen to read in such bugs that tests were just recently fixed, or ignored, then rebase your PR to pick up that change.
1. If you find previous comments "proving" that the same test has arbitrarily failed in at least 3 past PRs, then please do yourself raise a small separate new PR proposing to add an `@Disabled // TODO FINERACT-123` to the respective unstable test (e.g. [#774](https://github.com/apache/fineract/pull/774)) with the commit message mentioning said JIRA, as always.  (Please do NOT just `@Disabled` any existing tests mixed in as part of your larger PR.)
1. If there is no existing JIRA for the test, then first please evaluate whether the failure couldn't be a (perhaps strange) impact of the change you are proposing after all.  If it's not, then please raise a new JIRA to document the suspected Flaky Test, and link it to [FINERACT-850](https://issues.apache.org/jira/browse/FINERACT-850).  This will allow the next person coming along hitting the same test failure to easily find it, and eventually propose to ignore the unstable test.
1. Then (only) Close and Reopen your PR, which will cause a new build, to see if it passes.
1. Of course, we very much appreciate you then jumping onto any such bugs and helping us figure out how to fix all ignored tests!

[Pull Request Size Limit](https://cwiki.apache.org/confluence/display/FINERACT/Pull+Request+Size+Limit)
documents that we cannot accept huge "code dump" Pull Requests, with some related suggestions.

Guideline for new Feature commits involving Refactoring: If you are submitting PR for a new Feature,
and it involves refactoring, try to differentiate "new Feature code" with "Refactored" by placing
them in different commits. This helps review to review your code faster.

We have an automated Bot which marks pull requests as "stale" after a while, and ultimately automatically closes them.


Estratégia de Mesclagem
--------------

Os committers deste projeto normalmente preferem trazer suas Pull Requests through _Rebase and Merge_ instead of _Create a Merge Commit_. (Se você não estiver familiarizado com a interface do usuário do GitHub, re. isso, observe o pequeno triângulo suspenso um tanto oculto na parte inferior do PR, visível apenas para os committers, não para os contribuidores.)  This avoids the "merge commits" which we consider to be somewhat "polluting" the projects commits log history view.  We understand this doesn't give an easy automatic reference to the original PR (which GitHub automatically adds to the Merge Commit message it generates), but we consider this an only very minor inconvenience; it's typically relatively easy to find the original PR even just from the commit message, and JIRA.

We expect most proposed PRs to typically consist of a single commit.  Committers may use _Squash and merge_ to combine your commits at merge time, and if they do so will rewrite your commit message as they see fit.

Neither of these two are hard absolute rules, but mere conventions.  Multiple commits in single PR make sense in certain cases (e.g. branch backports).


Dependency Upgrades
-------------------

This project uses a number of 3rd-party libraries, and this section provides some guidance for their updates. We have set-up [Renovate's bot](https://renovate.whitesourcesoftware.com) to automatically raise Pull Requests for our review when new dependencies are available [FINERACT-962](https://issues.apache.org/jira/browse/FINERACT-962).

Upgrades sometimes require package name changes.  Changed code should ideally have test coverage.

Our `ClasspathHellDuplicatesCheckRuleTest` detects classes that appear in more than 1 JAR.  If a version bump in [`build.gradle`](https://github.com/search?q=repo%3Aapache%2Ffineract+filename%3Abuild.gradle&type=Code&ref=advsearch&l=&l=) causes changes in transitives dependencies, then you may have to add related `exclude` to our [`dependencies.gradle`](https://github.com/apache/fineract/search?q=dependencies.gradle).  Running `./gradlew dependencies` helps to understand what is required.


Mais informações
============
Mais detalhes do projeto podem ser encontrados em<https://cwiki.apache.org/confluence/display/FINERACT>.
