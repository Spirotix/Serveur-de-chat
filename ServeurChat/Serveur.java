import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Serveur 
{
	private ServerSocket serverSocket;
	private List<GerantDeClient> clients;

	public Serveur(int port) {
		clients = new ArrayList<>();
		try {
			serverSocket = new ServerSocket(port);
			System.out.println("Serveur de tchat démarré sur le port " + port);

			while (true) {
				Socket clientSocket = serverSocket.accept();

				GerantDeClient gerant = new GerantDeClient(clientSocket, clients);
				Thread thread = new Thread(gerant);
				thread.start();
			}
		} catch (IOException e) {
			System.out.println("Erreur du serveur : " + e.getMessage());
		} finally {
			try {
				if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
			} catch (IOException e) {
				System.out.println("Erreur lors de la fermeture du serveur : " + e.getMessage());
			}
		}
	}
	public static void main(String[] args) 
	{
		new Serveur(6000);
	}
}
