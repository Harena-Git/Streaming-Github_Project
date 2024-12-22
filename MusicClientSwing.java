package Affichage_SWING;

import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class MusicClientSwing extends JFrame {
    private DefaultListModel<String> listModel;
    private JList<String> musicList;
    private JButton refreshButton, playButton, downloadButton;

    public MusicClientSwing() {
        // Configurer la fenêtre principale
        setTitle("Client Musique - Liste des Chansons");
        setSize(500, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Créer les composants
        listModel = new DefaultListModel<>();
        musicList = new JList<>(listModel);
        JScrollPane scrollPane = new JScrollPane(musicList);

        refreshButton = new JButton("Rafraîchir");
        playButton = new JButton("Lire");
        downloadButton = new JButton("Télécharger");

        // Ajouter les composants à la fenêtre
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        buttonPanel.add(refreshButton);
        buttonPanel.add(playButton);
        buttonPanel.add(downloadButton);

        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Ajouter des actions aux boutons
        refreshButton.addActionListener(e -> refreshMusicList());
        playButton.addActionListener(e -> playMusic());
        downloadButton.addActionListener(e -> downloadMusic());

        setVisible(true);
    }

    private void refreshMusicList() {
        try (Socket socket = new Socket("192.168.x.x", 12345); // Remplacez avec l'IP du serveur
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            out.println("GET_MUSICS"); // Envoyer la requête au serveur

            listModel.clear(); // Vider la liste actuelle
            String response;
            while (!(response = in.readLine()).equals("END")) {
                listModel.addElement(response); // Ajouter les musiques à la liste
            }

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erreur : Impossible de se connecter au serveur.",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void playMusic() {
        String selectedMusic = musicList.getSelectedValue();
        if (selectedMusic == null) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une musique à lire.",
                    "Avertissement", JOptionPane.WARNING_MESSAGE);
            return;
        }
        JOptionPane.showMessageDialog(this, "Lecture de : " + selectedMusic,
                "Info", JOptionPane.INFORMATION_MESSAGE);
        // Implémentez la lecture réelle si nécessaire
    }

    private void downloadMusic() {
        String selectedMusic = musicList.getSelectedValue();
        if (selectedMusic == null) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une musique à télécharger.",
                    "Avertissement", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (Socket socket = new Socket("192.168.x.x", 12345); // Remplacez avec l'IP du serveur
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             InputStream in = socket.getInputStream()) {

            // Extraire l'ID de la musique sélectionnée
            String musicId = selectedMusic.split(" - ")[0]; // Suppose que l'ID est au début
            out.println("GET " + musicId);

            // Télécharger la musique
            FileOutputStream fos = new FileOutputStream("musique_" + musicId + ".mp3");
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.close();

            JOptionPane.showMessageDialog(this, "Musique téléchargée avec succès.",
                    "Info", JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erreur lors du téléchargement.",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MusicClientSwing::new);
    }
}
