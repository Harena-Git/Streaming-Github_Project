package moozik;

import java.awt.*;
import java.io.*;
import java.net.*;
import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

public class MusicClient {
    private static final String SERVER_IP = "127.0.0.1"; // "127.0.0.1"
    private static final int SERVER_PORT = 12345;
    private static Clip currentClip;
    private static JTable musicTable;
    private static DefaultTableModel tableModel;
    private static int currentPlayingRow = -1;
    private static JButton prevButton, pauseButton, nextButton;
    private static boolean isPaused = false;
    private static long pausedPosition = 0;

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

        // micreer anle fenetre

            JFrame frame = new JFrame("Liste des Musiques");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 500);

            // le tableau
            String[] columnNames = {"ID", "Titre", "Artiste", "Chemin", "Action"};
            tableModel = new DefaultTableModel(columnNames, 0) {
                @Override
                public Class<?> getColumnClass(int columnIndex) {
                    if (columnIndex == 4) return JButton.class;
                    return String.class;
                }

                @Override
                public boolean isCellEditable(int row, int column) {
                    return column == 4; // Rendre la colonne "Action" éditable (affichage des boutons)
                }
            };

            musicTable = new JTable(tableModel);
            ButtonRenderer buttonRenderer = new ButtonRenderer();
            ButtonEditor buttonEditor = new ButtonEditor(musicTable);
            
            musicTable.getColumn("Action").setCellRenderer(buttonRenderer);
            musicTable.getColumn("Action").setCellEditor(buttonEditor);
            
            JScrollPane scrollPane = new JScrollPane(musicTable);

            // panneau ho anl'ilay boutton
            JPanel navigationPanel = new JPanel();
            prevButton = new JButton("Previous");
            pauseButton = new JButton("Pause");
            nextButton = new JButton("Next");

            // le hanaovana saisie ho anle message
            JTextField messageTextField = new JTextField(30);

            // mandefa anle message
            JButton sendMessageButton = new JButton("Envoyer");

            // listner ho anle boutons
            prevButton.addActionListener(e -> playPreviousTrack());
            pauseButton.addActionListener(e -> togglePause());
            nextButton.addActionListener(e -> playNextTrack());

            JTextArea messageTextArea = new JTextArea(5, 40);
            messageTextArea.setEditable(false);
            JScrollPane messageScrollPane = new JScrollPane(messageTextArea);

            navigationPanel.add(prevButton);
            navigationPanel.add(pauseButton);
            navigationPanel.add(nextButton);
            navigationPanel.add(messageTextField);
            navigationPanel.add(sendMessageButton);

            sendMessageButton.addActionListener(e -> {
                String message = messageTextField.getText();
                if (!message.isEmpty()) {
                    // mampipotra anizay message lasa
                    messageTextArea.append("Vous: " + message + "\n");

                    // alefa any amin ny serveur le message
                    sendMessageToServer(message);
                    
                    // fafana ny texte nosoratana farany
                    messageTextField.setText("");
                }
            });

            // placen'ilay elements rehetra
            JPanel mainPanel = new JPanel(new BorderLayout());
            mainPanel.add(scrollPane, BorderLayout.CENTER);
            mainPanel.add(navigationPanel, BorderLayout.SOUTH);

            frame.add(mainPanel);
            frame.setVisible(true);

            // connection sy maka an'ilay donnees
            try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println("GET_MUSICS"); // mandefa zavatra tokony ho ataon'ilay serveur

                String response;
                while (!(response = in.readLine()).equals("END")) {
                    String[] musicInfo = response.split(" - ");
                    if (musicInfo.length >= 4) {
                        Object[] rowData = {
                            musicInfo[0],  // ID
                            musicInfo[1],  // Titre
                            musicInfo[2],  // Artiste
                            musicInfo[3],  // Chemin
                            "Lire"         // Bouton
                        };
                        tableModel.addRow(rowData);
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(frame, 
                    "Erreur : Impossible de se connecter au serveur.",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }

    // fandefasana message any amin'ny serveur
    private static void sendMessageToServer(String message) {
        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            // alefa any amin'ny serveur ilay message
            out.println("MESSAGE " + message);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                "Erreur lors de l'envoi du message.",
                "Erreur", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // Hira prev
    private static void playPreviousTrack() {
        if (currentClip != null) {
            currentClip.stop();
            currentClip.close();
        }
    
        if (currentPlayingRow > 0) {
            currentPlayingRow--;
            String prevMusicId = tableModel.getValueAt(currentPlayingRow, 0).toString();
            ButtonEditor editor = (ButtonEditor) musicTable.getColumn("Action").getCellEditor();
            
            // maka an'ilay ligne taloha
            musicTable.setRowSelectionInterval(currentPlayingRow, currentPlayingRow);
            
            // averina hatramin'ny voalohany ny etat an'ilay pause
            isPaused = false;
            pauseButton.setText("Pause");
            
            editor.playMusic(prevMusicId);
        }
    }

    private static void playNextTrack() {
        if (currentClip != null) {
            currentClip.stop();
            currentClip.close();
        }
    
        if (currentPlayingRow < tableModel.getRowCount() - 1) {
            currentPlayingRow++;
            String nextMusicId = tableModel.getValueAt(currentPlayingRow, 0).toString();
            ButtonEditor editor = (ButtonEditor) musicTable.getColumn("Action").getCellEditor();
            
            musicTable.setRowSelectionInterval(currentPlayingRow, currentPlayingRow);
            
            isPaused = false;
            pauseButton.setText("Pause");
            
            editor.playMusic(nextMusicId);
        }
    }

    // Pause/Play
    private static void togglePause() {
        if (currentClip != null) {
            if (!isPaused) {
                // pause
                pausedPosition = currentClip.getMicrosecondPosition();
                currentClip.stop();
                pauseButton.setText("Reprendre");
                isPaused = true;
            } else {
                // Reprendre
                currentClip.setMicrosecondPosition(pausedPosition);
                currentClip.start();
                pauseButton.setText("Pause");
                isPaused = false;
            }
        }
    }
    
    // Classe pour le rendu des boutons
    static class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            setText((value == null) ? "" : value.toString());
            return this;
        }
    }

    // Classe pour l'édition des boutons
    static class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String label;
        private boolean isPushed;
        private JTable parentTable;

        public ButtonEditor(JTable table) {
            super(new JCheckBox());
            this.parentTable = table;
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }

        private void playAudioFile(File audioFile) {
            try {
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                currentClip = AudioSystem.getClip();
                currentClip.open(audioStream);
                
                // manisy ecouteur mijery we tapitra sa mbola
                currentClip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP && !isPaused) {
                        // verifiena hoe eraha efa tapaka ilay hira
                        if (currentClip.getMicrosecondPosition() >= currentClip.getMicrosecondLength()) {
                            // mampiasa SwingUtilities.invokeLater mba hiassurena f hiexcexute ilay code
                            // ao anatin'ilay thread interface
                            SwingUtilities.invokeLater(() -> MusicClient.playNextTrack());
                        }
                    }
                });
                
                currentClip.start();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                    "Erreur : Impossible de lire le fichier audio.",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }

        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            label = (value == null) ? "" : value.toString();
            button.setText(label);
            isPushed = true;
            return button;
        }

        public Object getCellEditorValue() {
            if (isPushed) {
                // maka id an'ilay musique
                String musicId = parentTable.getValueAt(parentTable.getSelectedRow(), 0).toString();
                currentPlayingRow = parentTable.getSelectedRow();
                
                // averina hatramin'ny voalohany ny etat an'ilay pause
                isPaused = false;
                pauseButton.setText("Pause");
                
                playMusic(musicId);
            }
            isPushed = false;
            return label;
        }

        public boolean stopCellEditing() {
            isPushed = false;
            return super.stopCellEditing();
        }

        // fandefasana an'ilay musique
        public void playMusic(String musicId) {
            try {
                // ajanona nya musique efa mandeha rah misy
                if (currentClip != null && currentClip.isRunning()) {
                    currentClip.stop();  // ajanona ny clips mandeha rah misy
                    currentClip.close(); // miliberer ressource
                }

                // mangataka fichier musical any amin'ny serveur
                try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                     PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                     InputStream in = socket.getInputStream()) {

                    out.println("GET " + musicId);

                    // micreer an'ilay fichier musical temporaire ihany
                    File tempFile = File.createTempFile("music_", ".mp3");
                    tempFile.deleteOnExit();

                    // sauvena ilay fichier azo
                    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            fos.write(buffer, 0, bytesRead);
                        }
                    }

                    // Jouer le fichier audio
                    playAudioFile(tempFile);

                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null,
                        "Erreur : Impossible de récupérer ou jouer le fichier.",
                        "Erreur", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                    "Erreur : Impossible de récupérer ou jouer le fichier.",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }

        // Méthode pour jouer le fichier audio
        // private void playAudioFile(File audioFile) {
        //     try {
        //         AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
        //         currentClip = AudioSystem.getClip();
        //         currentClip.open(audioStream);
                
        //         // Ajouter un écouteur pour détecter la fin de la lecture
        //         currentClip.addLineListener(event -> {
        //             if (event.getType() == LineEvent.Type.STOP && !isPaused) {
        //                 // Si la lecture s'arrête et que ce n'est pas une pause
        //                 if (currentClip.getMicrosecondPosition() >= currentClip.getMicrosecondLength()) {
        //                     // Si on a atteint la fin du morceau, lancer le suivant
        //                     autoPlayNextTrack();
        //                 }
        //             }
        //         });
                
        //         currentClip.start();
        //     } catch (Exception e) {
        //         JOptionPane.showMessageDialog(null,
        //             "Erreur : Impossible de lire le fichier audio.",
        //             "Erreur", JOptionPane.ERROR_MESSAGE);
        //         e.printStackTrace();
        //     }
        // }
}
}