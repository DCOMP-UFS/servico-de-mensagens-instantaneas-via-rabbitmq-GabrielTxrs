package br.ufs.dcomp.ChatRabbitMQ;

import java.util.Scanner;

public class InputOutput {
    public static String lerLinha() {
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }

    public static boolean isUsuario(String nomeUsuario) {
        return nomeUsuario.charAt(0) == '@';
    }

    public static boolean isGrupo(String entrada) {
        return entrada.split(" ")[0].equalsIgnoreCase("!newGroup");
    }
}
