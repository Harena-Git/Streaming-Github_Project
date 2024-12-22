package moozik;

import java.io.*;
import java.net.*;
import java.sql.*;

public class MusicServer {
    private static final int PORT = 12345;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Serveur en écoute sur le port " + PORT + "...");

            // Manao ny nouvelle class de connexion
            Connection conn = DatabaseConnection.getConnection();
            System.out.println("Connexion à la base de données réussie.");

            // Gerena ny connexion an le kill
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connecté : " + clientSocket.getInetAddress());
                new Thread(() -> handleClient(clientSocket, conn)).start(); // fonction mi_gerer an handleClient() eo ambany
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket, Connection conn) { // eo ambony no gerena(hanekena ny client hafa)
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream out = clientSocket.getOutputStream()) {

            String request = in.readLine(); // Mamaky requete client
            if (request != null) {
                if ("GET_MUSICS".equals(request)) {
                    sendMusicList(out, conn);
                } else if (request.startsWith("GET ")) {
                    String id = request.split(" ")[1]; // Avoaka ny ID nanontaniana
                    sendMusicFile(out, id, conn);
                } else if (request.startsWith("MESSAGE ")) {
                String message = request.substring(8);
                System.out.println("Message du client: " + message);
            }
            }

        } catch (IOException e) {
            System.err.println("Erreur avec le client : " + e.getMessage());
        }
    }

    private static void sendMusicList(OutputStream out, Connection conn) {
        try (PrintWriter writer = new PrintWriter(out, true);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT id, titre, artiste, chemin FROM Musiques")) {

            while (rs.next()) {
                // mandefa ny chemin anle hira
                String music = rs.getInt("id") + " - " + 
                               rs.getString("titre") + " - " + 
                               rs.getString("artiste") + " - " + 
                               rs.getString("chemin");
                writer.println(music); // mandefa anle hira
            }
            writer.println("END");

        } catch (SQLException e) {
            System.err.println("Erreur lors de l'envoi de la liste des musiques : " + e.getMessage());
        }
    }

    private static void sendMusicFile(OutputStream out, String id, Connection conn) {
        try {
            // dedavina par id le hira
            String query = "SELECT chemin FROM Musiques WHERE id = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, Integer.parseInt(id));
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String filePath = rs.getString("chemin");
                File file = new File(filePath);

                if (file.exists()) {
                    // alefa any amin ny client
                    try (FileInputStream fis = new FileInputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;

                        while ((bytesRead = fis.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }
                } else {
                    System.err.println("Fichier non trouvé : " + filePath);
                }
            } else {
                System.err.println("ID de musique non trouvé : " + id);
            }

        } catch (SQLException | IOException e) {
            System.err.println("Erreur lors de l'envoi du fichier : " + e.getMessage());
        }
    }
}