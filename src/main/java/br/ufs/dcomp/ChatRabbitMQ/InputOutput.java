package br.ufs.dcomp.ChatRabbitMQ;

import java.util.Scanner;

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

    public static boolean isAlterarDestinatario(String entrada) {
        return entrada.charAt(0) == '@' || entrada.charAt(0) == '#';
    }

    public static boolean isComando(String entrada) {
        return entrada.charAt(0) == '!';
    }

    public static void getComando(String entrada) {
        String[] parametros = entrada.split(" ");
        if(parametros[0].startsWith("!")) {
            switch (parametros[0].toLowerCase()) {
                case "!addgroup":
                    if(ValidarEntrada(parametros, 1))
                        Chat.criarGrupo(parametros[1]);
                    break;
                case "!delfromgroup":
                    if(ValidarEntrada(parametros, 2))
                        Chat.removerUsuarioGrupo(parametros[1], parametros[2]);
                    break;
                case "!removegroup":
                    if(ValidarEntrada(parametros, 1))
                        Chat.deletarGrupo(parametros[1]);
                    break;
            }
        }
    }
}
