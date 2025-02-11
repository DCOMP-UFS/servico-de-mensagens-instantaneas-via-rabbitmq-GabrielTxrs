package br.ufs.dcomp.ChatRabbitMQ;

import com.google.protobuf.ByteString;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
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
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws UnsupportedEncodingException {
                String message = new String(body, StandardCharsets.UTF_8);
                String sender = properties.getHeaders().get("sender").toString();
                String timestamp = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm").format(new Date());
                String group = envelope.getExchange().isEmpty() ? "" : "#" + envelope.getExchange();
                System.out.println("(" + timestamp + ") " + sender + group + " diz: " + message);
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
                channel.basicConsume(nomeDestinatario.substring(1), true, consumer);
            } else {
                InputOutput.getComando(entrada);
                if (!InputOutput.isComando(entrada)) {
                    MensagemOuterClass.Conteudo conteudo = MensagemOuterClass.Conteudo.newBuilder()
                            .setTipo(String.valueOf(MensagemOuterClass.Conteudo.TIPO_FIELD_NUMBER))
                            .setCorpo(ByteString.copyFromUtf8(entrada)).build();

                    MensagemOuterClass.Mensagem mensagem = MensagemOuterClass.Mensagem.newBuilder()
                            .setEmissor(nomeUsuario)
                            .setData(LocalDate.now().toString())
                            .setHora(LocalTime.now().toString())
                            .setGrupo(nomeDestinatario)
                            .setConteudo(conteudo).build();
                    Chat.enviarMensagem(channel, mensagem);
                }
            }
        }
    }
}