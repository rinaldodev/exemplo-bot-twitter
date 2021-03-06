# Exemplo de Bot para Twitter
 
 ## COMO criar um BOT de TWITTER usando JAVA e rodar na NUVEM de GRAÇA!
 
 Abaixo o passo a passo de como implementar, configurar e rodar seu bot.
 
 Para entender melhor, assista o vídeo completo: https://youtu.be/sIpHwW81LxQ

## OBTENDO AS CHAVES

1. Obter Conta de Desenvolvedor: https://developer.twitter.com/en/portal/dashboard
2. Criar App na Dashboard do Twitter (já aproveite pra copiar a API TOKEN e API SECRET)
3. Crie a conta do twitter do bot e logue em outro navegador pra facilitar
4. Obtenha as Chaves: 
* Seguir o resumo abaixo, ou esse tutorial: https://medium.com/geekculture/how-to-create-multiple-bots-with-a-single-twitter-developer-account-529eaba6a576
* Na conta original: https://twitter.com/oauth/request_token?oauth_consumer_key=<API_KEY_DEV>&oauth_callback=oob
* Na conta do bot: https://twitter.com/oauth/authenticate?oauth_token=<OATH_TOKEN_DEV>
* Na conta original: https://twitter.com/oauth/access_token?oauth_token=<OAUTH_TOKEN_DEV>&oauth_verifier=<NUMEROS_VERIFICACAO_BOT>
* Ao final, deve ter API KEY, API SECRET, OAUTH KEY, OAUTH SECRET.

## CÓDIGO
1. API Twitter: https://github.com/redouane59/twittered
2. Estrutura (Código neste repositório)
*  Checar intervalo mínimo desde o último tweet
*  Montar seu tweet
*  Abrir em navegador?
*  Copiar para clipboard?
*  Checar se informação mudaram?
*  Postar tweet

## BUILD
1. Plugin Maven: http://maven.apache.org/plugins/maven-assembly-plugin/
2. Maven Wrapper: https://www.baeldung.com/maven-wrapper
3. Testar build/execucao: 
* ./mvnw clean package
* java -jar target/<seu_jar_completo>.jar

## GITHUB
1. Novo repo privado: https://github.com/new
2. Commit/Push
* git init
* git add .
* git commit -m "first commit"
* git branco -M main
* git remote add origin <repositorio>
* git push -u origin main

5. Deploy
1. Heroku: https://dashboard.heroku.com/apps
2. Scheduler: https://devcenter.heroku.com/articles/scheduler
3. VER OS LOGS: https://dashboard.heroku.com/apps/<SUA_APP>/logs
