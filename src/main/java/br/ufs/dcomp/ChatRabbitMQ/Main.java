package br.ufs.dcomp.ChatRabbitMQ;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.rabbitmq.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeoutException;

import static br.ufs.dcomp.ChatRabbitMQ.Chat.*;
import static br.ufs.dcomp.ChatRabbitMQ.InputOutput.*;

public class Main {
    //    private static final String HOST = "172.24.145.223";
    public static final String HOST = "172.21.29.186";
    public static final String USERNAME = "admin";
    public static final String PASSWORD = "password";
    private static final String PROMPT = ">> ";
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);


    public static void main(String[] args) throws IOException, TimeoutException {
        Connection connection = iniciarConexao(HOST, USERNAME, PASSWORD);
        Channel channel = iniciarCanal(connection);

        try {
            iniciarChat(channel);
        } catch (EOFException e) {
            throw new EOFException("CTRL + D pressed");
        } finally {
            channel.close();
            connection.close();
        }

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

                if (!grupo.isEmpty()) {
                    System.out.println("\n(" + data + " às " + hora + ") " + sender + "#" + grupo + " diz: " + conteudo);
                    System.out.print("#" + grupo.concat(PROMPT));
                } else {
                    System.out.println("\n(" + data + " às " + hora + ") " + sender + "@" + destinatario + " diz: " + conteudo);
                    System.out.print("@" + destinatario.concat(PROMPT));
                }

            }
        };
        if (channel.consumerCount(nomeUsuario) == 0) {
            channel.basicConsume(nomeUsuario, true, consumer);
        }

        channel.queueDeclare(nomeUsuario + "Arquivos", false, false, false, null);
        Consumer consumidorArquivos = new DefaultConsumer(channel) {
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws InvalidProtocolBufferException {
                MensagemOuterClass.Mensagem mensagemRecebida = MensagemOuterClass.Mensagem.parseFrom(body);
                String data = mensagemRecebida.getData();
                String hora = mensagemRecebida.getHora().substring(0, 5);
                String grupo = mensagemRecebida.getGrupo();
                String destinatario = mensagemRecebida.getDestinatario();
                MensagemOuterClass.Conteudo conteudo = mensagemRecebida.getConteudo();
                if (mensagemRecebida.getConteudo().getIsArquivo()) {
                    try {
                        Path directory = Paths.get("src/main/resources/recebidos");
                        if (!Files.exists(directory)) {
                            Files.createDirectories(directory);
                        }
                        Path filePath = directory.resolve(conteudo.getNome());
                        Files.write(filePath, conteudo.getCorpo().toByteArray());
                        System.out.println("\n(" + data + " às " + hora + ") Arquivo \"" + conteudo.getNome()
                                + "\" recebido de " + imprimirDestinatarioOuGrupo(grupo + "Arquivos", destinatario));
                        System.out.print(imprimirDestinatarioOuGrupo(grupo + "Arquivos", destinatario) + PROMPT);
                    } catch (IOException e) {
                        LOGGER.info(e.getMessage());
                    }
                }
            }
        };
        if (channel.consumerCount(nomeUsuario + "Arquivos") == 0) {
            channel.basicConsume(nomeUsuario + "Arquivos", true, consumidorArquivos);
        }

        String nomeDestinatario = "";
        String nomeGrupo = "";

        while (true) {
            if (!nomeDestinatario.isEmpty() ^ !nomeGrupo.isEmpty()) {
                imprimirTipoPromptCorreto(nomeDestinatario);
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
                    if (channel.consumerCount(nomeDestinatario) == 0) {
                        channel.basicConsume(nomeDestinatario, true, consumer);
                    }
                }
            } else {
                getComando(nomeUsuario, channel, entrada, nomeDestinatario, nomeGrupo);

                if (!isComando(entrada) && (!nomeDestinatario.isEmpty() || !nomeGrupo.isEmpty())) {
                    MensagemOuterClass.Conteudo conteudo = MensagemOuterClass.Conteudo.newBuilder()
                            .setTipo(String.valueOf(MensagemOuterClass.Conteudo.TIPO_FIELD_NUMBER))
                            .setCorpo(ByteString.copyFromUtf8(entrada)).setIsArquivo(false).build();

                    MensagemOuterClass.Mensagem mensagem = MensagemOuterClass.Mensagem.newBuilder()
                            .setEmissor(nomeUsuario)
                            .setData(LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
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