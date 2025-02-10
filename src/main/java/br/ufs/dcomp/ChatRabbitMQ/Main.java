package br.ufs.dcomp.ChatRabbitMQ;

import com.google.protobuf.ByteString;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.TimeoutException;

public class Main {
    private static final String HOST = "3.83.136.248";
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
            if (entrada.equalsIgnoreCase("exit") || entrada.equalsIgnoreCase("sair")) {
                break;
            }
            if (InputOutput.isAlterarDestinatario(entrada)) {
                String[] parametros = entrada.split(" ");
                nomeDestinatario = parametros[0];
                channel.queueDeclare(nomeDestinatario.substring(1), false, false, false, null);
//                channel.basicConsume(nomeDestinatario.substring(1), true, consumer);
            } else {
                InputOutput.getComando(entrada);
                if (!InputOutput.isComando(entrada)) {
//                    String mensagem = entrada;
//                    Chat.enviarMensagem(channel, , entrada);

                    MensagemOuterClass.Conteudo conteudo = MensagemOuterClass.Conteudo.newBuilder()
                            .setTipo(String.valueOf(MensagemOuterClass.Conteudo.TIPO_FIELD_NUMBER))
                            .setCorpo(ByteString.copyFromUtf8(entrada)).build();

                    MensagemOuterClass.Mensagem mensagem = MensagemOuterClass.Mensagem.newBuilder()
                            .setEmissor(nomeUsuario)
                            .setData(LocalDate.now().toString())
                            .setHora(LocalTime.now().toString())
                            .setGrupo(nomeDestinatario)
                            .setConteudo(conteudo).build();

                    channel.basicPublish("", nomeDestinatario.substring(1), null, mensagem.toByteArray());
                }
            }
        }
    }
}