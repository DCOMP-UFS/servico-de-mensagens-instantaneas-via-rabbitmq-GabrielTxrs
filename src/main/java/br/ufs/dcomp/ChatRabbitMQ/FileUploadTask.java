// FileUploadTask.java
package br.ufs.dcomp.ChatRabbitMQ;

import com.google.protobuf.ByteString;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;

import static br.ufs.dcomp.ChatRabbitMQ.InputOutput.lerArquivoPorCaminho;

public class FileUploadTask implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUploadTask.class);
    private final Channel channel;
    private final String caminhoArquivo;
    private final String nomeDestinatario;
    private final String nomeGrupo;
    private final String usuario;

    public FileUploadTask(Channel channel, String caminhoArquivo, String nomeDestinatario, String nomeGrupo, String usuario) {
        this.channel = channel;
        this.caminhoArquivo = caminhoArquivo;
        this.nomeDestinatario = nomeDestinatario;
        this.nomeGrupo = nomeGrupo;
        this.usuario = usuario;
    }

    @Override
    public void run() {
        try {
            ByteString arquivo = lerArquivoPorCaminho(caminhoArquivo);

            Path path = Paths.get(caminhoArquivo);
            String contentType = Files.probeContentType(path);
            if (contentType == null) {
                contentType = URLConnection.guessContentTypeFromName(path.getFileName().toString());
                if (contentType == null) {
                    contentType = "text/plain";
                }
            }
            MensagemOuterClass.Conteudo conteudo = MensagemOuterClass.Conteudo.newBuilder()
                    .setNome(path.getFileName().toString()).setTipo(contentType).setCorpo(arquivo).setIsArquivo(true).build();

            MensagemOuterClass.Mensagem mensagem = MensagemOuterClass.Mensagem.newBuilder()
                    .setEmissor(usuario)
                    .setData(LocalDate.now().toString())
                    .setHora(LocalTime.now().toString())
                    .setDestinatario(nomeDestinatario)
                    .setGrupo(nomeGrupo)
                    .setConteudo(conteudo).build();

            if (!mensagem.getGrupo().isEmpty()) {
                System.out.println("\nEnviando " + caminhoArquivo + " para #" + nomeGrupo + " !");
                channel.basicPublish(mensagem.getGrupo()+"Arquivo", "", null, mensagem.toByteArray());
            } else {
                System.out.println("\nEnviando " + caminhoArquivo + " para @" + nomeDestinatario + " !");
                channel.basicPublish("", mensagem.getDestinatario()+"Arquivo", null, mensagem.toByteArray());
            }
        } catch (IOException e) {
            LOGGER.info(e.getMessage());
        }
    }
}