package br.ufs.dcomp.ChatRabbitMQ;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Chat {
    public static Connection iniciarConexao(String host, String username, String password) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        factory.setUsername(username);
        factory.setPassword(password);
        factory.setVirtualHost("/");
        return factory.newConnection();
    }

    public static Channel iniciarCanal(Connection connection) {
        try {
            return connection.createChannel();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void criarGrupo(String nomeGrupo) {
        System.out.println(nomeGrupo);
    }
}

