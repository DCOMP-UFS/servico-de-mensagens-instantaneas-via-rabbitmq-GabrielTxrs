package br.ufs.dcomp.ChatRabbitMQ;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.rabbitmq.client.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.TimeoutException;

import static br.ufs.dcomp.ChatRabbitMQ.Chat.*;
import static br.ufs.dcomp.ChatRabbitMQ.InputOutput.*;

public class Main {
    private static final String HOST = "172.24.145.223";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "password";
    private static final String PROMPT = ">> ";


    public static void main(String[] args) throws IOException, TimeoutException {
        Connection connection = iniciarConexao(HOST, USERNAME, PASSWORD);
        Channel channel = iniciarCanal(connection);

        iniciarChat(channel);

        channel.close();
        connection.close();
    }

    public static void iniciarChat(Channel channel) throws IOException {
        System.out.print("User: ");
        String nomeUsuario = lerLinha();

        channel.queueDeclare(nomeUsuario, false, false, false, null);
        Consumer consumer = new DefaultConsumer(channel) {
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws InvalidProtocolBufferException {
                MensagemOuterClass.Mensagem mensagemRecebida = MensagemOuterClass.Mensagem.parseFrom(body);
                String sender = mensagemRecebida.getEmissor();
                String data = mensagemRecebida.getData();
                String hora = mensagemRecebida.getHora().substring(0, 5);
                String conteudo = mensagemRecebida.getConteudo().getCorpo().toStringUtf8();
                String grupo = mensagemRecebida.getGrupo();

                synchronized (System.out) {
                    System.out.println("(" + data + " às " + hora + ") " + sender+"#"+grupo + " diz: " + conteudo);
                    System.out.print(grupo.concat(PROMPT));
                }
            }
        };
        channel.basicConsume(nomeUsuario, true, consumer);
        String nomeDestinatario = "";
        String nomeGrupo = "";

        while (true) {
            System.out.print(nomeDestinatario.concat(PROMPT));
            String entrada = lerLinha();
            if (entrada.equalsIgnoreCase("exit") || entrada.equalsIgnoreCase("sair")) {
                break;
            }
            if (isAlterarDestinatario(entrada)) {
                String[] parametros = entrada.split(" ");
                if(isGrupo(entrada)) {
                    nomeGrupo = parametros[0];
                    criarGrupo(nomeUsuario, channel, nomeGrupo.substring(1));
                }
                nomeDestinatario = parametros[0];
                channel.queueDeclare(nomeDestinatario.substring(1), false, false, false, null);
                channel.basicConsume(nomeDestinatario.substring(1), true, consumer);
            } else {
                getComando(nomeUsuario, channel, entrada);

                if (!isComando(entrada) && !nomeDestinatario.isBlank()) {
                    MensagemOuterClass.Conteudo conteudo = MensagemOuterClass.Conteudo.newBuilder()
                            .setTipo(String.valueOf(MensagemOuterClass.Conteudo.TIPO_FIELD_NUMBER))
                            .setCorpo(ByteString.copyFromUtf8(entrada)).build();

                    MensagemOuterClass.Mensagem mensagem = MensagemOuterClass.Mensagem.newBuilder()
                            .setEmissor(nomeUsuario)
                            .setData(LocalDate.now().toString())
                            .setHora(LocalTime.now().toString())
                            .setGrupo(nomeDestinatario.substring(1))
                            .setConteudo(conteudo).build();
                    enviarMensagem(channel, mensagem, nomeDestinatario.equals(nomeGrupo));
                }
            }
        }
    }
}