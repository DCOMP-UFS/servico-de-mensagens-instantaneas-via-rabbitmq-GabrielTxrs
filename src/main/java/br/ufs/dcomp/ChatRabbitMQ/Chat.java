package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Chat {
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

    public static void criarGrupo(String nomeGrupo) {
        System.out.println("Grupo Criado: " + nomeGrupo);
    }

    public static void deletarGrupo(String nomeGrupo) {
        System.out.println("Grupo Deletado: " + nomeGrupo);
    }

    public static void removerUsuarioGrupo(String usuario, String nomeGrupo) {
        System.out.println("Usuario: " + usuario + " removido do grupo: " + nomeGrupo);
    }

    public static void enviarMensagem(Channel channel, MensagemOuterClass.Mensagem mensagem) {
        if (!isMensagemValida(mensagem)) {
            System.out.println("Não foi possível enviar a mensagem");
            return;
        }
        try {
            channel.basicPublish("", mensagem.getGrupo().substring(1), null, mensagem.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isMensagemValida(MensagemOuterClass.Mensagem mensagem) {
        return mensagem.hasConteudo() && !mensagem.getConteudo().getNome().isBlank()
                && !mensagem.getConteudo().getTipo().isBlank() && !mensagem.getConteudo().getCorpo().isEmpty()
                && mensagem.hasField(MensagemOuterClass.Mensagem.getDescriptor().findFieldByName("emissor"))
                && mensagem.hasField(MensagemOuterClass.Mensagem.getDescriptor().findFieldByName("data"))
                && mensagem.hasField(MensagemOuterClass.Mensagem.getDescriptor().findFieldByName("hora"))
                && mensagem.hasField(MensagemOuterClass.Mensagem.getDescriptor().findFieldByName("grupo"))
                && mensagem.hasField(MensagemOuterClass.Conteudo.getDescriptor().findFieldByName("tipo"))
                && mensagem.hasField(MensagemOuterClass.Conteudo.getDescriptor().findFieldByName("corpo"))
                && mensagem.hasField(MensagemOuterClass.Conteudo.getDescriptor().findFieldByName("nome"));
    }
}

