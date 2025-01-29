package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class Main {
    private static final String HOST = "54.174.110.248";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "password";
    private static final String PROMPT = ">>";

    public static void main(String[] args) throws IOException, TimeoutException {
        Connection connection = Chat.iniciarConexao(HOST, USERNAME, PASSWORD);
        Channel channel = Chat.iniciarCanal(connection);

        iniciarChat(channel);
    }

    public static void iniciarChat(Channel channel) throws IOException {
        System.out.println("User: ");
        String nomeUsuario = InputOutput.lerLinha();

        channel.queueDeclare(nomeUsuario, false, false, false, null);
        Consumer consumer = new DefaultConsumer(channel) {
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
            }
        };
        channel.basicConsume(nomeUsuario, true, consumer);
        String nomeDestinatario = "";
        while (true) {
            System.out.println(nomeDestinatario.concat(PROMPT));
            String entrada = InputOutput.lerLinha();
            if (nomeDestinatario.isBlank() || InputOutput.isUsuario(entrada)) {
                nomeDestinatario = entrada;
                channel.queueDeclare(nomeDestinatario.substring(1), false, false, false, null);
//                channel.basicConsume(nomeDestinatario, true, consumer); Isso está certo? definir um consumidor pra cada destinatário de resposta?
                continue;
            }
            String mensagem = entrada;
            System.out.println(mensagem);
            System.out.println(nomeDestinatario);
            channel.basicPublish("", nomeDestinatario.substring(1), null, mensagem.getBytes(StandardCharsets.UTF_8));
        }
    }
}
