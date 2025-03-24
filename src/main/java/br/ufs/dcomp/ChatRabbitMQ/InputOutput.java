package br.ufs.dcomp.ChatRabbitMQ;

import com.google.protobuf.ByteString;
import com.rabbitmq.client.Channel;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

import static br.ufs.dcomp.ChatRabbitMQ.Chat.*;

public class InputOutput {
    public static String lerLinha() throws EOFException {
        Scanner scanner = new Scanner(System.in);
        if (scanner.hasNextLine()) {
            return scanner.nextLine();
        } else {
            throw new EOFException("CTRL + D pressed");
        }
    }

    public static String imprimirDestinatarioOuGrupo(String grupo, String destinatario) {
        if (!grupo.isEmpty()) {
            return "#" + grupo;
        } else {
            return "@" + destinatario;
        }
    }

    public static void imprimirTipoPromptCorreto(String nomeDestinatario) {
        if (!nomeDestinatario.isEmpty()) {
            System.out.print("@");
        } else {
            System.out.print("#");
        }
    }

    public static boolean ValidarEntrada(String[] entrada, int tamanhoEntrada) {
        if (entrada.length <= tamanhoEntrada) {
            System.out.println("Número de argumentos inválido");
            return false;
        }
        for (String e : entrada) {
            if (e == null || e.isBlank()) {
                System.out.println("Entrada inválida");
                return false;
            }
        }
        return true;
    }

    public static boolean isAlterarDestinatarioGrupo(String entrada) {
        return !entrada.isBlank() && (entrada.charAt(0) == '@' || entrada.charAt(0) == '#');
    }

    public static boolean isGrupo(String entrada) {
        return !entrada.isBlank() && entrada.charAt(0) == '#';
    }

    public static boolean isDestinatario(String entrada) {
        return !entrada.isBlank() && entrada.charAt(0) == '@';
    }

    public static boolean isComando(String entrada) {
        return !entrada.isBlank() && entrada.charAt(0) == '!';
    }

    public static ByteString lerArquivoPorCaminho(String caminhoArquivo) {
        try {
            byte[] fileBytes = Files.readAllBytes(Paths.get(caminhoArquivo));
            return ByteString.copyFrom(fileBytes);
        } catch (IOException e) {
            return ByteString.EMPTY;
        }
    }

    public static void getComando(String usuario, Channel channel, String entrada, String nomeDestinatario,
                                  String nomeGrupo) {
        String[] parametros = entrada.split(" ");
        if (parametros[0].startsWith("!")) {
            switch (parametros[0].toLowerCase()) {
                case "!newgroup":
                case "!addgroup":
                    if (ValidarEntrada(parametros, 1))
                        criarGrupo(usuario, channel, parametros[1]);
                    else
                        System.out.println("!addGroup <nomeGrupo>");
                    break;
                case "!adduser":
                    if (ValidarEntrada(parametros, 2))
                        adicionarUsuarioGrupo(parametros[1], channel, parametros[2]);
                    else
                        System.out.println("!addUser <nomeUsuario> <nomeGrupo>");
                    break;
                case "!delfromgroup":
                    if (ValidarEntrada(parametros, 2))
                        removerUsuarioGrupo(parametros[1], channel, parametros[2]);
                    else
                        System.out.println("!delFromGroup <nomeUsuario> <nomeGrupo>");
                    break;
                case "!removegroup":
                    if (ValidarEntrada(parametros, 1))
                        deletarGrupo(channel, parametros[1]);
                    else
                        System.out.println("!removeGroup <nomeGrupo>");
                    break;
                case "!upload":
                    if (ValidarEntrada(parametros, 1))
                        enviarArquivo(channel, parametros[1], nomeDestinatario, nomeGrupo, usuario);
                    else
                        System.out.println("!upload <caminhoArquivo>");
                    break;
                case "!listusers":
                    if (ValidarEntrada(parametros, 1))
                        listarMembrosGrupo(parametros[1]);
                    else
                        System.out.println("!listUsers <nomeGrupo>");
                    break;
                case "!listgroups":
                    listarGrupos(usuario);
                    break;
            }
        }
    }
}
