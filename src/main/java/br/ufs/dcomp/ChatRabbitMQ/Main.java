package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class Main {
    private static final String HOST = "52.91.199.175";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "password";
    private static final String PROMPT = ">> ";

    
    public static void main(String[] args) throws IOException, TimeoutException {
        Connection connection = Chat.iniciarConexao(HOST, USERNAME, PASSWORD);
        Channel channel = Chat.iniciarCanal(connection);

        iniciarChat(channel);
        channel.close();
        connection.close();
    }

    public static void iniciarChat(Channel channel) throws IOException {
        System.out.print("User: ");
        String nomeUsuario = InputOutput.lerLinha();

        channel.queueDeclare(nomeUsuario, false, false, false, null);
        Consumer consumer = new DefaultConsumer(channel) {
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
            }
        };
        channel.basicConsume(nomeUsuario, true, consumer);
        String nomeDestinatario = "";
        while (true) {
            System.out.print(nomeDestinatario.concat(PROMPT));
            String entrada = InputOutput.lerLinha();
            if (nomeDestinatario.isBlank() || InputOutput.isUsuario(entrada)) {
                nomeDestinatario = entrada;
                channel.queueDeclare(nomeDestinatario.substring(1), false, false, false, null);
                System.out.println("entrou no loop");
                channel.basicConsume(nomeDestinatario.substring(1), true, consumer);
                continue;
            }
            if(InputOutput.isGrupo(entrada)) {
                Chat.criarGrupo(entrada.split(" ")[1]);
            }
            String mensagem = entrada;
            System.out.println(mensagem);
            if(mensagem.equals("exit") || mensagem.equals("sair"))
                break;
            System.out.println(nomeDestinatario);
            channel.basicPublish("", nomeDestinatario.substring(1), null, mensagem.getBytes(StandardCharsets.UTF_8));
        }
    }
}
