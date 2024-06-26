/*
 * Copyright (c) 2024.  Jerome David. Univ. Grenoble Alpes.
 * This file is part of DcissChatService.
 *
 * DcissChatService is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * DcissChatService is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */
package fr.uga.miashs.dciss.chatservice.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.UnknownHostException;
import java.sql.*;
import javax.swing.JOptionPane;
import java.util.ArrayList;

import fr.uga.miashs.dciss.chatservice.common.Packet;
import fr.uga.miashs.dciss.chatservice.common.db.DatabaseManager;
import fr.uga.miashs.dciss.chatservice.server.ChatSession;

import java.util.List;
import java.util.logging.Logger;


public class ChatWindow extends JFrame {
    private static final Logger LOG = Logger.getLogger(DatabaseManager.class.getName());
    private JTextArea chatArea;
    private JTextField messageField;
    private JButton sendButton;
    private JComboBox<String> userComboBox; // Agregar JComboBox para seleccionar usuario
    private ClientMsg client;
    private int userId;

    public ChatWindow(String nom_utilisateur, ClientMsg client) {
        this.userId = client.getIdentifier(); // Obtener el identificador de usuario del cliente
        this.client = client;
        setTitle("Chat - User : " + nom_utilisateur);
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());

        // Botón de Cerrar Sesión
        JButton logoutButton = new JButton("Se déconnecter");
        logoutButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Cierra la conexión del cliente si es necesario
                client.closeSession(); // Suponiendo que tienes un método closeSession en tu clase ClientMsg para cerrar la sesión del cliente

                // Cierra la ventana actual
                dispose();

                ChatSession chatSession = new ChatSession();
                chatSession.setVisible(true);
            }
        });
        topPanel.add(logoutButton, BorderLayout.EAST);


        add(topPanel, BorderLayout.NORTH); // Agregar el panel superior con el JComboBox

        // ComboBox para seleccionar usuario
        userComboBox = new JComboBox<>();
        topPanel.add(userComboBox, BorderLayout.CENTER);

        JPanel chatPanel = new JPanel(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(chatArea);

        JPanel messagePanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        sendButton = new JButton("Envoyer");

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        messageField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendButton, BorderLayout.EAST);

        chatPanel.add(scrollPane, BorderLayout.CENTER);
        chatPanel.add(messagePanel, BorderLayout.SOUTH);

        add(chatPanel, BorderLayout.CENTER);

        // Recuperar y mostrar mensajes desde la base de datos al iniciar la ventana
        retrieveAndDisplayMessages();

        // Cargar usuarios disponibles en el JComboBox
        loadUsers();

        // Agregar un MessageListener al cliente
        client.addMessageListener(new MessageListener() {
            @Override
            public void messageReceived(Packet p) {
                // Cuando se recibe un mensaje, actualizar la interfaz de usuario
                try {
                    String senderName = DatabaseManager.getUserNameById(p.srcId);
                    LOG.warning("Message received from " + senderName);
                    String message = new String(p.data);
                    appendMessage(senderName, message);
                } catch (SQLException e) {
                    e.printStackTrace(); // o manejo de error apropiado
                }
            }
        });
    }


    private void retrieveAndDisplayMessages() {
        try {
            List<Message> messages = DatabaseManager.getMessagesForUser(userId);
            for (Message message : messages) {
                String senderName = DatabaseManager.getUserNameById(message.getSenderId());
                appendMessage(senderName, message.getMessage());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String message = messageField.getText();
        String selectedUser = (String) userComboBox.getSelectedItem(); // Obtener el usuario seleccionado
        if (selectedUser != null) {
            // Aquí necesitarás obtener el ID del usuario seleccionado
            // luego enviar el mensaje al servidor y guardar en la base de datos
            // Supongamos que obtienes el ID del usuario seleccionado en selectedUserId
            int selectedUserId = getUserIdByName(selectedUser); // Corregir el nombre del método
            client.sendPacket(selectedUserId, message.getBytes());
            System.out.println("Message sent to " + selectedUser + ": " + message);
            appendMessage("You", message);
    
            saveMessage(userId, selectedUserId, message);
            System.out.println(userId + " " + selectedUserId + " " + message);
            System.out.println("Message saved in the database.");
            messageField.setText("");
        } else {
            JOptionPane.showMessageDialog(ChatWindow.this, "Please select a user to send message.");
        }
    }

    private void saveMessage(int senderId, int recipientId, String message) {
        try {
            // Verificar que el senderId existe en la tabla Utilisateurs
            if (DatabaseManager.userExists(senderId)) {
                DatabaseManager.saveMessage(senderId, recipientId, message);
            } else {
                System.out.println("Error: sender_id " + senderId + " does not exist in the Utilisateurs table.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadUsers() {
        try {
            ArrayList<String> usernames = DatabaseManager.getAllUsernames();
            for (String username : usernames) {
                userComboBox.addItem(username);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private int getUserIdByName(String username) {
        try {
            return DatabaseManager.getUserIdByUsername(username);
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void appendMessage(String sender, String message) {
        chatArea.append(sender + ": " + message + "\n");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ClientMsg client = new ClientMsg("lainean", "localhost", 1666); // Añade el nombre de usuario
                client.startSession();

                ChatWindow chatWindow = new ChatWindow("lainean", client);
                chatWindow.setVisible(true);
            }
        });
    }
}