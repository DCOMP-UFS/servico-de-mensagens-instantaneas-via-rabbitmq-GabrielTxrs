package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Chat {
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
}

