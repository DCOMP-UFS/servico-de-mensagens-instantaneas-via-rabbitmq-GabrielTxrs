package br.ufs.dcomp.ChatRabbitMQ;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonParser;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import static br.ufs.dcomp.ChatRabbitMQ.Main.*;

public class Chat {
    private static final String auth = USERNAME + ":" + PASSWORD;
    private static final Logger LOGGER = LoggerFactory.getLogger(Chat.class);

    public static Connection iniciarConexao(String host, String username, String password) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost("/");
        return factory.newConnection();
    }

    public static Channel iniciarCanal(Connection connection) {
        try {
            return connection.createChannel();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static StringBuilder requisicaoHttp(String url) {
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        try {
            // Configurar a conexão HTTP
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "Basic " + encodedAuth);

            // Ler a resposta da API
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
//            System.out.println(response);
            in.close();
            connection.disconnect();
            return response;
        } catch (IOException | JsonIOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void criarGrupo(String usuario, Channel channel, String nomeGrupo) {
        try {
            channel.exchangeDeclare(nomeGrupo, "fanout");
            channel.queueBind(usuario, nomeGrupo, "");
            channel.exchangeDeclare(nomeGrupo + "Arquivos", "fanout");
            channel.queueBind(usuario + "Arquivos", nomeGrupo + "Arquivos", "");
            System.out.println("O Usuario: " + usuario + " Criou o grupo: " + nomeGrupo);
        } catch (IOException e) {
            LOGGER.info(e.getMessage());
        }
    }

    public static void adicionarUsuarioGrupo(String usuarioAdicionado, Channel channel, String nomeGrupo) {
        try {
            channel.queueBind(usuarioAdicionado, nomeGrupo, "");
            channel.queueBind(usuarioAdicionado + "Arquivos", nomeGrupo + "Arquivos", "");
            System.out.println("Usuario: " + usuarioAdicionado + " adicionado ao grupo: " + nomeGrupo);
        } catch (IOException e) {
            LOGGER.info(e.getMessage());
        }
    }

    public static void deletarGrupo(Channel channel, String nomeGrupo) {
        try {
            channel.exchangeDelete(nomeGrupo);
            channel.exchangeDelete(nomeGrupo + "Arquivos");
            System.out.println("Grupo deletado: " + nomeGrupo);
        } catch (IOException e) {
            LOGGER.info(e.getMessage());
        }
    }

    public static void removerUsuarioGrupo(String usuarioRemovido, Channel channel, String nomeGrupo) {
        try {
            channel.queueUnbind(usuarioRemovido, nomeGrupo, "");
            channel.queueUnbind(usuarioRemovido + "Arquivos", nomeGrupo + "Arquivos", "");
        } catch (IOException e) {
            LOGGER.info(e.getMessage());
        }
        System.out.println("Usuario: " + usuarioRemovido + " removido do grupo: " + nomeGrupo);
    }

    public static void enviarMensagem(Channel channel, MensagemOuterClass.Mensagem mensagem) {
        try {
            if (!mensagem.getGrupo().isEmpty()) {
                channel.basicPublish(mensagem.getGrupo(), "", null, mensagem.toByteArray());
            } else {
                channel.basicPublish("", mensagem.getDestinatario(), null, mensagem.toByteArray());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void enviarArquivo(Channel channel, String caminhoArquivo, String nomeDestinatario, String nomeGrupo, String usuario) {
        Thread uploadThread = new Thread(new FileUploadTask(channel, caminhoArquivo, nomeDestinatario, nomeGrupo, usuario));
        uploadThread.start();
    }

    public static void listarGrupos(String usuario) {
        try {
            // Configurar a URL da API de gerenciamento do RabbitMQ
            String url = "http://" + HOST + ":15672/api/bindings";

            JsonArray jsonArray = JsonParser.parseString(requisicaoHttp(url).toString()).getAsJsonArray();
            Set<String> grupos = new HashSet<>();
            for (JsonElement element : jsonArray) {
                String source = element.getAsJsonObject().get("source").getAsString();
                String destination = element.getAsJsonObject().get("destination").getAsString();
                if (!source.isEmpty() && !source.endsWith("Arquivos")
                        && !destination.isEmpty() && destination.equals(usuario)) {
                    grupos.add(source);
                }
            }
            System.out.println("Groups: " + grupos);
        } catch (JsonIOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void listarMembrosGrupo(String group) {
        try {
            // Configurar a URL da API de gerenciamento do RabbitMQ
            String url = "http://" + HOST + ":15672/api/exchanges/%2F/" + group + "/bindings/source";

            JsonArray jsonArray = JsonParser.parseString(requisicaoHttp(url).toString()).getAsJsonArray();
            Set<String> queues = new HashSet<>();
            for (JsonElement element : jsonArray) {
                String destination = element.getAsJsonObject().get("destination").getAsString();
                if (!destination.isEmpty()) {
                    queues.add(destination);
                }
            }
            System.out.println("Membros do grupo " + group + ": " + queues);
        } catch (JsonIOException e) {
            throw new RuntimeException(e);
        }
    }
}