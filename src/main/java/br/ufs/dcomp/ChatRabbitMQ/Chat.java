package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class Chat {

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("18.207.208.136"); // Alterar
        factory.setUsername("admin"); // Alterar
        factory.setPassword("password"); // Alterar
        factory.setVirtualHost("/");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        System.out.print("User: ");
        Scanner scanner = new Scanner(System.in);
        String nomeUsuario = scanner.nextLine();

        channel.queueDeclare(nomeUsuario, false, false, false, null);

        System.out.print(">> ");
        String nomeDestinatario = scanner.nextLine();
        System.out.println(nomeDestinatario);

//        Consumer consumer = new DefaultConsumer(channel) {
//            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
//                    throws IOException {
//
//                String message = new String(body, StandardCharsets.UTF_8);
//                System.out.println(message);
//
//            }
//        };
//        channel.basicConsume(nomeUsuario, true, consumer);

        channel.close();
        connection.close();
        scanner.close();
    }
}