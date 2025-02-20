package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.Channel;

import java.util.Scanner;

import static br.ufs.dcomp.ChatRabbitMQ.Chat.*;

public class InputOutput {
    public static String lerLinha() {
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }

    public static boolean ValidarEntrada(String[] entrada, int tamanhoEntrada) {
        if(entrada.length <= tamanhoEntrada) {
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
        return !entrada.isBlank() && entrada.charAt(0) == '@' || entrada.charAt(0) == '#';
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

    public static void getComando(String usuario, Channel channel, String entrada) {
        String[] parametros = entrada.split(" ");
        if(parametros[0].startsWith("!")) {
            switch (parametros[0].toLowerCase()) {
                case "!newgroup":
                case "!addgroup":
                    if(ValidarEntrada(parametros, 1))
                        criarGrupo(usuario, channel, parametros[1]);
                    else
                        System.out.println("!addGroup <nomeGrupo>");
                    break;
                case "!adduser":
                    if(ValidarEntrada(parametros, 2))
                        adicionarUsuarioGrupo(parametros[1], channel, parametros[2]);
                    else
                        System.out.println("!addUser <nomeUsuario> <nomeGrupo>");
                    break;
                case "!delfromgroup":
                    if(ValidarEntrada(parametros, 2))
                        removerUsuarioGrupo(parametros[1], channel, parametros[2]);
                    else
                        System.out.println("!delFromGroup <nomeUsuario> <nomeGrupo>");
                    break;
                case "!removegroup":
                    if(ValidarEntrada(parametros, 1))
                        deletarGrupo(channel, parametros[1]);
                    else
                        System.out.println("!removeGroup <nomeGrupo>");
                    break;
            }
        }
    }
}
