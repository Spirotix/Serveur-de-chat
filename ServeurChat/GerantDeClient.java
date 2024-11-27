import java.io.*;
import java.net.Socket;
import java.util.*;

public class GerantDeClient implements Runnable
{
	private Socket socket;
	private BufferedReader in;
	private PrintWriter out;
	private List<GerantDeClient> clients;
	private String pseudo;
	private Set<String> mutes;

	private static final int MAX_MESSAGES = 10; 
	private final List<String> messages = new ArrayList<>();


	public GerantDeClient(Socket s, List<GerantDeClient> clients)
	{
		this.socket = s;
		this.clients = clients;

		this.mutes = new HashSet<>();
		try
		{
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);

			boolean estUnique = false;
			while (!estUnique)
			{
				estUnique = true;
				this.logMessage("Veuillez entrer votre nom d'utilisateur : ");
				this.pseudo = in.readLine();

				for (GerantDeClient client : clients)
				{
					if (client.pseudo.toLowerCase().equals(this.pseudo.toLowerCase()))
					{
						estUnique = false;
					}
				}
				if (!estUnique)
				{
					this.logMessage("<SERVEUR> - Ce nom d'utilisateur est déjà pris. Veuillez en choisir un autre.");
				}
			}

			this.logMessage("<SERVEUR> - Bienvenue mon petit canard, " + this.pseudo + " !\nFais \"/help\" pour voir la liste des commandes");
			System.out.println("<SERVEUR> - " +  this.pseudo + " s'est connecté.");

			synchronized (clients)
			{
				clients.add(this);
				for (GerantDeClient client : clients)
				{
					if (client != this)
					{
						client.logMessage("<SERVEUR> - " + this.pseudo + " a rejoint l'étang");
					}
				}
			}
		} catch (IOException e)
		{
			System.out.println("Erreur lors de l'initialisation des flux : " + e.getMessage());
		}
	}

	public void run()
	{
		try
		{
			String message;
			while ((message = in.readLine()) != null)
			{
				if (!message.equals(""))
				{
					if (message.charAt(0) == '/')
					{
						if (message.startsWith("/msg "))                                     { this.messagePriv(message);} 
						else if ( message.equals("/h") || message.equals("/help")){ this.messageHelp();}
						else if ( message.startsWith("/rename"))                             { this.rename(message);}
						else if ( message.equals("/list"))                                 { this.listUtilisateur();}
						else if ( message.startsWith("/mute"))                               { this.muteUtilisateur(message);}
						else if ( message.startsWith("/demute"))                             { this.demuteUtilisateur(message);}
						else if ( message.equals("/exit"))                                 { break;}
						else    {this.out.println("<SERVEUR> - Commande inconnue faites \"/h\" ou \"/help\" pour avoir la liste des commandes");}
					}
					else 
					{ 
						this.messagePublic(message);
					}
				}
			} 
		}

		catch (IOException e)
		{
			System.out.println(e.getMessage());
		} 
		finally
		{
			this.deconnexion();
		}
	}

	private void messagePriv(String commande)
	{
		String[] parties = commande.split(" ", 3);
		if (parties.length < 3)
		{
			logMessage("<SERVEUR> - Format incorrect. Utilisez : /msg <PseudoCible> <Message>");
		}
		else
		{
			String cible = parties[1];
			String messagePriv = parties[2];

			boolean found = false;
			synchronized (clients)
			{
				for (GerantDeClient client : clients)
				{
					if (client.pseudo.equals(cible))
					{
						client.logMessage("[Message privé de " + this.pseudo + "] : " + messagePriv);
						this.logMessage("[Message privé à " + cible + "] : " + messagePriv);
						found = true;
						break;
					}
				}
			}
			if (!found)
			{
				logMessage("<SERVEUR> - Utilisateur " + cible + " introuvable.");
			}
		}
	}

	private void messagePublic(String message)
	{
		synchronized (clients)
		{
			String fullMessage = "<" + this.pseudo + "> : " + message;
			for (GerantDeClient client : clients)
			{
				if (!client.mutes.contains(this.pseudo))
				{
					client.logMessage(fullMessage);
				}
			}
		}
	}


	private void rename(String commande)
	{
		String[] parties = commande.split(" ", 2);
		if (parties.length < 2)
		{
			this.logMessage("<SERVEUR> - Format incorrect. Utilisez : /rename <NouveauPseudo>");
			return;
		}

		String nouveauPseudo = parties[1];
		if ( this.pseudo.equals(nouveauPseudo))
		{
			this.logMessage("<SERVEUR> - " + nouveauPseudo + " est déjà votre pseudo");
			return;
		}

		for (GerantDeClient client : clients)
		{
			if (client.pseudo.toLowerCase().equals(nouveauPseudo.toLowerCase()))
			{
				this.logMessage("<SERVEUR> - Ce nom d'utilisateur est déjà pris.");
				return;
			}
		}

		for (GerantDeClient client : clients)
		{
			if (client != this)
			{
				client.logMessage("<SERVEUR> - " + this.pseudo + " c'est renommé en " + nouveauPseudo);
			}
		}

		this.logMessage("<SERVEUR> - Votre Pseudo est maintenant " + nouveauPseudo);
		this.pseudo = nouveauPseudo;

	}

	private void listUtilisateur()
	{
		String sRet = "<SERVEUR> - liste des utilisateurs : ";
		for (GerantDeClient client : clients){ sRet += "\n\t" + client.pseudo + "\n"; }

		this.logMessage(sRet);
	}


	private void muteUtilisateur(String commande) {
	String[] parties = commande.split(" ", 2);
	if (parties.length < 2) {
		logMessage("<SERVEUR> - Format incorrect. Utilisez : /mute <Pseudo>");
		return;
	}

	String cible = parties[1];
	synchronized (clients) {
		boolean found = false;
		for (GerantDeClient client : clients) {
			if (client.pseudo.equals(cible)) {
				mutes.add(cible);
				logMessage("<SERVEUR> - Vous avez mute " + cible);
				found = true;
				break;
			}
		}
		if (!found) {
			logMessage("<SERVEUR> - Utilisateur " + cible + " introuvable.");
		}
	}
	}



	private void demuteUtilisateur(String commande)
	{
		String[] parties = commande.split(" ", 2);
		if (parties.length < 2)
		{
			this.logMessage("<SERVEUR> - Format incorrect. Utilisez : /demute <Pseudo>");
			return;
		}

		String cible = parties[1];
		if (mutes.remove(cible))
		{
			logMessage("<SERVEUR> - " + cible + " n'est plus mute");
			return;
		}

		this.logMessage("<SERVEUR> - " + parties[1] + " n'etait pas mute");
	}


	private void messageHelp()
	{
		this.logMessage("<SERVEUR> - liste des commandes :"                                                    +"\n" +
		                "\t/msg <PseudoCible> <Message> : message privé à la personne cible"                     +"\n" +
						"\t/rename <NouveauPseudo> : permet de changer de pseudo"                                +"\n" +
						"\t/list  : affiche la liste des gens sur le tchat"                                      +"\n" +
						"\t/mute <PseudoCible>  : permet de ne plus afficher les messages de la personne cible"  +"\n" +
						"\t/demute <PseudoCible> : permet de réafficher les messages de la personne cible"       +"\n" +
						"\t/exit : permet de quitte le tchat"                                                    +"\n" +
						"\t/h : envoie la liste des commandes"                                                          );

	}

	private void deconnexion()
	{
		synchronized (clients)
		{
			clients.remove(this);
			System.out.println("<SERVEUR> - " + pseudo + " s'est déconnecté.");
			for (GerantDeClient client : clients)
			{
				if (client != this)
				{
					client.logMessage("<SERVEUR> - " + pseudo + " a quitté l'étang");
				}
			}
		}
		try
		{
			if (in != null)
				in.close();
			if (out != null)
				out.close();
			if (socket != null && !socket.isClosed())
				socket.close();
		} catch (IOException e)
		{
			System.out.println("Erreur lors de la fermeture des ressources : " + e.getMessage());
		}
	}

	private void actualise()
	{
		out.print("\033[H");
		out.print("\033[2J");

		int start = Math.max(0, messages.size() - MAX_MESSAGES);
		for (int i = start; i < messages.size(); i++)
		{
			out.println(messages.get(i));
		}

		out.println();

		out.print("> ");
		out.flush();
	}

	private void logMessage(String message)
	{
		synchronized (messages)
		{

			messages.add(message);

			if (messages.size() > MAX_MESSAGES)
			{
				messages.remove(0);
			}

			this.actualise();
		}
	}

}
