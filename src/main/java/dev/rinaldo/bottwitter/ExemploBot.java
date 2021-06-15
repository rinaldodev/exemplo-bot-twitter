package dev.rinaldo.bottwitter;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ClassLoaderUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.redouane59.twitter.TwitterClient;
import com.github.redouane59.twitter.dto.tweet.Tweet;
import com.github.redouane59.twitter.signature.TwitterCredentials;

public class ExemploBot {

	// ---- INÍCIO CONFIGS ----
	private static final int INTERVALO_MINIMO_HORAS = 48; // compara com o último tweet feito pelo bot
	private static final boolean MOSTRAR_NO_NAVEGADOR = false; // abre navegador com texto do tweet
	private static final boolean COPIAR_PARA_CLIPBOARD = false; // deixa pronto pro CTRL+V
	private static final boolean POSTAR_TWEET_IGUAL = false; // compara só os números
	private static final boolean POSTAR_TWEET = true; // para poder testar sem postar de verdade
	// ---- FIM CONFIGS ----

	private static final Logger LOG = LoggerFactory.getLogger(ExemploBot.class);
	
	private static final String JSON_CREDENCIAIS = "exemplo.json";
	private static final TwitterCredentials TWITTER_CREDENTIALS;
	private static final TwitterClient TWITTER_CLIENT;
	
	private static final String ARQUIVO_PARA_DOWNLOAD = "URL_PARA_DOWNLOAD";
	private static final String ARQUIVO_DESTINO_LOCAL = "ARQUIVO_DESTINO_LOCAL_DO_DOWNLOAD";
	
	private static final List<String> PAISES = Arrays.asList("USA", "OWID_EUN", "BRA", "OWID_WRL");
	private static final Map<String, String> NOMES = new HashMap<>();
	private static final Map<String, BigDecimal> VALORES = new HashMap<>();
	private static final Duration INTERVALO_MINIMO = Duration.ofHours(INTERVALO_MINIMO_HORAS).minusMinutes(9);
	
	static {
		try {
			URL json = ResourceUtil.getResource(JSON_CREDENCIAIS, ExemploBot.class);
			
			TWITTER_CREDENTIALS = TwitterClient.OBJECT_MAPPER.readValue(json, TwitterCredentials.class);
			TWITTER_CLIENT = new TwitterClient(TWITTER_CREDENTIALS);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		NOMES.put("USA", "🇺🇸 Estados Unidos");
		NOMES.put("OWID_EUN", "🇪🇺 União Europeia");
		NOMES.put("BRA", "🇧🇷 Brasil");
		NOMES.put("OWID_WRL", "🌐 Mundo");
	}
	
	public static void main(String[] args) throws FileNotFoundException, URISyntaxException, IOException, InterruptedException {
		executar();
	}
	
	public static void executar() throws URISyntaxException, FileNotFoundException, IOException, InterruptedException {
		Instant agora = Instant.now();
		LOG.debug("Começando.");
		
		LOG.debug("Postando para a conta {}.", TWITTER_CLIENT.getUserFromUserId(TWITTER_CLIENT.getUserIdFromAccessToken()).getName());
		
		Tweet dadosUltimoTweet = ultimoTweet();
		if (!passouIntervaloMinimo(dadosUltimoTweet)) {
			LOG.info("Finalizou porque não passou o intervalo mínimo desde o último tweet.");
			return;
		}
		
		File file = downloadArquivo();
		String novoTweet = montarNovoTweet(file); // ALTERE O INTERIOR DESTE MÉTODO!

		mostrarNoNavegador(novoTweet);
	    copiarParaClipboard(novoTweet);
		
		if (!POSTAR_TWEET_IGUAL && numerosNaoMudaramDesdeUltimoTweet(dadosUltimoTweet, novoTweet)) {
			LOG.info("Finalizou porque os números não mudaram desde o último tweet.");
			return;
		}
		
		if (POSTAR_TWEET) {
			LOG.debug("Postando tweet.");
			TWITTER_CLIENT.postTweet(novoTweet);
			LOG.info("Tweet postado.");
		} else {
			LOG.info("Não postou tweet porque está configurado assim.");
		}
		
		LOG.debug("Acabou em {}ms.", Duration.between(agora, Instant.now()).toMillis());
	}

	private static File downloadArquivo() throws IOException, FileNotFoundException {
		File file = new File(ARQUIVO_DESTINO_LOCAL);
		LOG.debug("Fazendo download de {} para {}.", ARQUIVO_PARA_DOWNLOAD, file.getAbsolutePath());
		file.delete();
		HttpClient client = HttpClient.newBuilder().followRedirects(Redirect.ALWAYS).build();
		
		URI uri = URI.create(ARQUIVO_PARA_DOWNLOAD);
		HttpRequest request = HttpRequest.newBuilder().uri(uri).build();
		InputStream is = client.sendAsync(request, BodyHandlers.ofInputStream())
				.thenApply(HttpResponse::body).join();
		try (FileOutputStream out = new FileOutputStream(ARQUIVO_DESTINO_LOCAL)) {
			is.transferTo(out);
		}
		LOG.debug("Download realizado. Tamanho: {}.", file.length());
		return file;
	}
	
	private static String montarNovoTweet(File file) throws IOException {
		LOG.debug("Montando novo tweet.");
		// MONTE AQUI O SEU PROCESSO DE NOVO TWEET
		Files.lines(file.toPath()).forEachOrdered(l -> {
			PAISES.forEach(p -> {
				if (l.contains(p)) {
					String[] split = l.split(",");
					if (split.length > 10) {
						String val = split[10];
						if (!val.isBlank() ) {
							VALORES.put(p, new BigDecimal(val));
						}
					}
				}
			});
		});
		LOG.debug("Mapa preenchido com dados.");
		
		StringBuilder sb = new StringBuilder(280);
		
		for (String string : PAISES) {
			BigDecimal bigDecimal = VALORES.get(string);
			sb.append(NOMES.get(string));
			sb.append(":\n");
			sb.append(converterEmBarras(bigDecimal));
			sb.append(bigDecimal.intValue());
			sb.append("%\n");
		}
		sb.delete(sb.length()-1, sb.length());

		String novoTweet = sb.toString();
		LOG.debug("Tweet montado, será apresentado no próximo log.");
		LOG.debug("\n" + novoTweet);
		return novoTweet;
	}

	private static void copiarParaClipboard(String novoTweet) {
		if (COPIAR_PARA_CLIPBOARD) {
			LOG.debug("Copiando para clipboard.");
			StringSelection selection = new StringSelection(novoTweet);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			clipboard.setContents(selection, selection);
		} else {
			LOG.debug("Não será copiado para o clipboard.");
		}
	}

	private static void mostrarNoNavegador(String novoTweet) throws IOException {
		if (MOSTRAR_NO_NAVEGADOR) {
			LOG.debug("Mostrando tweet no navegador.");
			File file2 = new File("test.html");
			Files.write(file2.toPath(), novoTweet.replaceAll("\n", "<br>").getBytes());
			Desktop.getDesktop().browse(file2.toURI());
		} else {
			LOG.debug("Não será apresentado o tweet no navegador.");
		}
	}

	private static boolean numerosNaoMudaramDesdeUltimoTweet(Tweet dadosUltimoTweet, String novoTweet) {
		LOG.debug("Checando se números mudaram desde o último tweet.");
		String ultimoTweet = dadosUltimoTweet.getText();
		String numerosNovoTweet = novoTweet.replaceAll("[^\\d]", "");
		String numerosUltimoTweet = ultimoTweet.replaceAll("[^\\d]", "");
		boolean contentEquals = numerosNovoTweet.contentEquals(numerosUltimoTweet);
		LOG.debug("Números mudaram? {}. Tweet anterior: {}. Novo tweet: {}.", !contentEquals, numerosUltimoTweet, numerosNovoTweet);
		return contentEquals;
	}

	private static Boolean passouIntervaloMinimo(Tweet dadosUltimoTweet) {
		LOG.debug("Checando intervalo mínimo.");
		LocalDateTime dataUltimoTweet = dadosUltimoTweet.getCreatedAt();
		LocalDateTime agora = LocalDateTime.now();
		Duration between = Duration.between(dataUltimoTweet, agora);
		boolean passouIntervaloMinimo = between.toMinutes() > INTERVALO_MINIMO.toMinutes();
		LOG.debug("Passou intervalo mínimo? {}", passouIntervaloMinimo);
		return passouIntervaloMinimo;
	}

	private static Tweet ultimoTweet() {
		LOG.debug("Lendo último tweet.");
		List<Tweet> userTimeline = TWITTER_CLIENT.getUserTimeline(TWITTER_CLIENT.getUserIdFromAccessToken(), 5);
		Tweet dadosUltimoTweet = userTimeline.get(0);
		LOG.debug("Último tweet recuperado. Feito em {}.", dadosUltimoTweet.getCreatedAt());
		return dadosUltimoTweet;
	}
	
	private static final String converterEmBarras(BigDecimal bd) {
		LOG.debug("Convertendo {} em barras.", bd);
		int constante = 15;
		int intValue = bd.intValue() / (100/constante);
		StringBuilder sb = new StringBuilder(100);
		for (int i = 0; i < intValue; i++) {
			sb.append("▓");
		}
		for (int i = intValue; i < constante; i++) {
			sb.append("░");
		}
		String string = sb.toString();
		LOG.debug("Convertido em barras. {} virou {}.", bd, string);
		return string;
	}
	
}
