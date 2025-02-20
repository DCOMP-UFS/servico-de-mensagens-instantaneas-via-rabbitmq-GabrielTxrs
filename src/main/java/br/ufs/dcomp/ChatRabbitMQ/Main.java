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
                String destinatario = mensagemRecebida.getDestinatario();

                synchronized (System.out) {
                    if (!grupo.isEmpty()) {
                        System.out.println("\n(" + data + " às " + hora + ") " + sender + "#" + grupo + " diz: " + conteudo);
                        System.out.print("#" + grupo.concat(PROMPT));
                    } else {
                        System.out.println("\n(" + data + " às " + hora + ") " + sender + "@" + destinatario + " diz: " + conteudo);
                        System.out.print("@" + destinatario.concat(PROMPT));
                    }
                }
            }
        };
        channel.basicConsume(nomeUsuario, true, consumer);
        String nomeDestinatario = "";
        String nomeGrupo = "";

        while (true) {
            if (!nomeDestinatario.isEmpty() ^ !nomeGrupo.isEmpty()) {
                if (!nomeDestinatario.isEmpty()) {
                    System.out.print("@");
                } else {
                    System.out.print("#");
                }
            }
            System.out.print(nomeDestinatario + nomeGrupo.concat(PROMPT));
            String entrada = lerLinha();
            if (entrada.equalsIgnoreCase("exit") || entrada.equalsIgnoreCase("sair")) {
                break;
            }
            if (isAlterarDestinatarioGrupo(entrada)) {
                String[] parametros = entrada.split(" ");
                if (isGrupo(entrada)) {
                    nomeGrupo = parametros[0].substring(1);
                    nomeDestinatario = "";
                    criarGrupo(nomeUsuario, channel, nomeGrupo);
                }
                if (isDestinatario(entrada)) {
                    nomeDestinatario = parametros[0].substring(1);
                    nomeGrupo = "";
                    channel.queueDeclare(nomeDestinatario, false, false, false, null);
                    channel.basicConsume(nomeDestinatario, true, consumer);
                }
            } else {
                getComando(nomeUsuario, channel, entrada);

                if (!isComando(entrada) && (!nomeDestinatario.isEmpty() || !nomeGrupo.isEmpty())) {
                    MensagemOuterClass.Conteudo conteudo = MensagemOuterClass.Conteudo.newBuilder()
                            .setTipo(String.valueOf(MensagemOuterClass.Conteudo.TIPO_FIELD_NUMBER))
                            .setCorpo(ByteString.copyFromUtf8(entrada)).build();

                    MensagemOuterClass.Mensagem mensagem = MensagemOuterClass.Mensagem.newBuilder()
                            .setEmissor(nomeUsuario)
                            .setData(LocalDate.now().toString())
                            .setHora(LocalTime.now().toString())
                            .setDestinatario(nomeDestinatario)
                            .setGrupo(nomeGrupo)
                            .setConteudo(conteudo).build();
                    enviarMensagem(channel, mensagem);
                }
            }
        }
    }
}