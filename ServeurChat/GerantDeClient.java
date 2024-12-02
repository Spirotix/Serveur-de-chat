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

	private final List<String> messages = new ArrayList<>();

	//Ce constructeur permet d'initialiser chaque client du tchat et gère l'affichage des messages. 
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
			System.out.println("<SERVEUR> - " +  this.pseudo + " s'est connecté."); // Permet d'annoncer lorsqu'un nouvel utilisateur rejoint le tchat

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

	//Traite le corps du message et execute l'action appropriée.
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
						if ( message.equals("/h") || message.equals("/help")){ this.messageHelp();}
						else if (message.startsWith("/msg "))                           { this.messagePriv(message);} 
						else if ( message.startsWith("/rename"))                        { this.rename(message);}
						else if ( message.equals("/list"))                            { this.listUtilisateur();}
						else if ( message.startsWith("/mute"))                          { this.muteUtilisateur(message);}
						else if ( message.startsWith("/demute"))                        { this.demuteUtilisateur(message);}
						else if ( message.equals("/clear"))                           { this.clearlog();}
						else if ( message.equals("/exit"))                            { break; }
						
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

	//Envoie le message écrit à tous les utilisateurs (sauf les personnes mutes).
	private void messagePublic(String message)
	{
		synchronized (clients)
		{
			String fullMessage = "<" + this.pseudo + "> : " + message;
			for (GerantDeClient client : clients)
			{
				if (!client.mutes.contains(this.pseudo.toLowerCase()))
				{
					client.logMessage(fullMessage);
				}
			}
		}
	}
	
	//Envoie un message privée suivant la syntaxe suivante : /msg [Utilisateur] [Message]
	private void messagePriv(String commande)
	{
		String[] parties = commande.split(" ", 3);
		if (parties.length < 3)
		{
			logMessage("<SERVEUR> - Format incorrect. Utilisez : /msg <PseudoCible> <Message>");
			return;
		}

		String cible = parties[1].toLowerCase();
		String messagePriv = parties[2];

		synchronized (clients)
		{
			for (GerantDeClient client : clients)
			{
				if (client.pseudo.toLowerCase().equals(cible))
				{
					if (!client.mutes.contains(this.pseudo.toLowerCase()))
					{
						client.logMessage("[Message privé de " + this.pseudo + "] : " + messagePriv);
					}
					this.logMessage("[Message privé à " + client.pseudo + "] : " + messagePriv);
					return;
				}
			}
		}

		logMessage("<SERVEUR> - Utilisateur " + cible + " introuvable.");
	}

	//Change son nom affiché. /rename [Pseudo]
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

	//Renvoie la liste des utilisateurs : /list
	private void listUtilisateur()
	{
		String sRet = "<SERVEUR> - liste des utilisateurs : ";
		for (GerantDeClient client : clients){ sRet += "\n\t" + client.pseudo + "\n"; }

		this.logMessage(sRet);
	}

	//Permet de mute un autre utilisateur : /mute [Utilisateur]
	private void muteUtilisateur(String commande) 
	{
		String[] parties = commande.split(" ", 2);
		if (parties.length < 2) 
		{
			logMessage("<SERVEUR> - Format incorrect. Utilisez : /mute <Pseudo>");
			return;
		}

		String cible = parties[1].toLowerCase();
		synchronized (clients) 
		{
			boolean found = false;
			for (GerantDeClient client : clients) 
			{
				if (client.pseudo.toLowerCase().equals(cible)) 
				{
					if (client.mutes.contains(cible))
					{
						this.out.println("<SERVEUR> - " + client.pseudo + " est déjà mute");
					}
					mutes.add(cible);
					logMessage("<SERVEUR> - Vous avez mute " + client.pseudo);
					found = true;
					break;
				}
			}
			if (!found) 
			{
				logMessage("<SERVEUR> - Utilisateur " + cible + " introuvable.");
			}
		}
	}


	//Permet de démute un unitilateur : /demute [Utilisateur]
	private void demuteUtilisateur(String commande)
	{
		String[] parties = commande.split(" ", 2);
		if (parties.length < 2)
		{
			this.logMessage("<SERVEUR> - Format incorrect. Utilisez : /demute <Pseudo>");
			return;
		}
		synchronized (clients)
		{
			String cible = parties[1].toLowerCase();

			for (GerantDeClient client : clients)
			{
				if (client.pseudo.toLowerCase().equals(cible))
				{
					if (mutes.remove(cible))
					{

						logMessage("<SERVEUR> - " + client.pseudo + " n'est plus mute");
						return;

					}
					this.logMessage("<SERVEUR> - " + client.pseudo + " n'etait pas mute");
					return;
				}
			}
			logMessage("<SERVEUR> - Utilisateur " + cible + " introuvable.");
		}
	}

	//Renvoie la liste des commandes disponibles avec la syntaxe.
	private void messageHelp()
	{
		this.logMessage("<SERVEUR> - Liste des commandes :"                                                    +"\n" +
		                "\t/msg <PseudoCible> <Message> : Message privé à la personne cible"                     +"\n" +
						"\t/rename <NouveauPseudo> : Permet de changer de pseudo"                                +"\n" +
						"\t/list  : Affiche la liste des gens sur le tchat"                                      +"\n" +
						"\t/mute <PseudoCible>  : Permet de ne plus afficher les messages de la personne cible"  +"\n" +
						"\t/demute <PseudoCible> : Permet de réafficher les messages de la personne cible"       +"\n" +
						"\t/exit : Permet de quitter le tchat"                                                    +"\n" +
						"\t/h : Envoie la liste des commandes"                                                          );

	}

	//Permet d'afficher aux utilisateurs lorsque l'un d'entre eux vient de quitter le tchat.
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

		for (int i = 0; i < messages.size(); i++)
		{
			out.println(messages.get(i));
		}

		out.println();

		out.print("> ");
		out.flush();
	}

	private void logMessage(String message)
	{
		synchronized (this.messages)
		{

			this.messages.add(message);

			this.actualise();
		}
	}

	private void clearlog()
	{
		this.messages.clear();
		this.actualise();
	}
}
