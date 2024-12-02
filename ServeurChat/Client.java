import java.io.*;
import java.net.Socket;

public class Client
{
	public static void main(String[] args)
	{
		if ( args.length < 2 )
		{
			System.out.println("Syntaxe invalide : java Client <adresse> <port>");
		}
		else
		{
			int port  = Integer.parseInt(args[1]);
			String ip = args[0];
			try (Socket socket = new Socket(ip, port);
				 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				 BufferedReader inClient = new BufferedReader(new InputStreamReader(System.in));
				 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);)
			{

				System.out.println(in.readLine());
				out.println(inClient.readLine());

				Thread receiverThread = new Thread(() -> {
					try
					{
						String serverMessage;
						while ((serverMessage = in.readLine()) != null)
						{
							System.out.println(serverMessage);
						}
					} catch (IOException e)
					{
						System.out.println("Déconnexion du serveur : " + e.getMessage());
					}
				});
				receiverThread.start();

				while (true)
				{
					if (inClient.ready())
					{
						out.println(inClient.readLine());
					}

				}
			} catch (IOException e)
			{
				System.out.println("Erreur de connexion : " + e.getMessage());
			}
		}
	}
}
