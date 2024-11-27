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
				out.println("Veuillez entrer votre nom d'utilisateur : ");
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
					out.println("<SERVEUR> - Ce nom d'utilisateur est déjà pris. Veuillez en choisir un autre.");
				}
			}

			out.println("<SERVEUR> - Bienvenue mon petit canard, " + this.pseudo + " !");
			System.out.println("<SERVEUR> - " +  this.pseudo + " s'est connecté.");

			synchronized (clients)
			{
				clients.add(this);
				for (GerantDeClient client : clients)
				{
					if (client != this)
					{
						client.out.println("<SERVEUR> - " + this.pseudo + " a rejoint l'étang");
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
			out.println("<SERVEUR> - Format incorrect. Utilisez : /msg <PseudoCible> <message>");
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
					if (client.pseudo.toLowerCase().equals(cible.toLowerCase()))
					{
						client.out.println("[Message privé de " + this.pseudo + "] : " + messagePriv);
						this.out.println("[Message privé à " + cible + "] : " + messagePriv);
						found = true;
						break;
					}
				}
			}
			if (!found)
			{
				out.println("<SERVEUR> - Utilisateur " + cible + " introuvable.");
			}
		}
	}

	private void messagePublic(String message)
	{
		System.out.println(pseudo + " : " + message);

		synchronized (clients)
		{
			for (GerantDeClient client : clients)
			{
				if (client != this && !client.mutes.contains(this.pseudo))
				{

					client.out.println("<" + this.pseudo + "> : " + message);

				}
			}
		}
	}

	private void rename(String commande)
	{
		String[] parties = commande.split(" ", 2);
		if (parties.length < 2)
		{
			this.out.println("<SERVEUR> - Format incorrect. Utilisez : /rename <NouveauPseudo>");
			return;
		}

		String nouveauPseudo = parties[1];
		if ( this.pseudo.equals(nouveauPseudo))
		{
			this.out.println("<SERVEUR> - " + nouveauPseudo + " est déjà votre pseudo");
			return;
		}

		for (GerantDeClient client : clients)
		{
			if (client.pseudo.toLowerCase().equals(nouveauPseudo.toLowerCase()))
			{
				this.out.println("<SERVEUR> - Ce nom d'utilisateur est déjà pris.");
				return;
			}
		}

		for (GerantDeClient client : clients)
		{
			if (client != this)
			{
				client.out.println("<SERVEUR> - " + this.pseudo + " c'est renommé en " + nouveauPseudo);
			}
		}

		this.out.println("<SERVEUR> - Votre Pseudo est maintenant " + nouveauPseudo);
		this.pseudo = nouveauPseudo;

	}

	private void listUtilisateur()
	{
		String sRet = "<SERVEUR> - liste des utilisateurs \n";
		for (GerantDeClient client : clients){ sRet += client.pseudo + "\n"; }

		this.out.println(sRet);
	}


	private void muteUtilisateur(String commande)
	{
		String[] parties = commande.split(" ", 2);
		if (parties.length < 2)
		{
			out.println("<SERVEUR> - Format incorrect. Utilisez : /mute <Pseudo>");
			return;
		}

		String cible = parties[1];
		synchronized (clients)
		{
			boolean found = false;
			for (GerantDeClient client : clients)
			{
				if (client.pseudo.equals(cible))
				{
					mutes.add(cible);
					this.out.println("<SERVEUR> - " + cible + " est désormais mute");
					found = true;
					break;
				}
			}
			if (!found)
			{
				out.println("<SERVEUR> - Utilisateur " + cible + " introuvable.");
			}
		}
	}


	private void demuteUtilisateur(String commande)
	{
		String[] parties = commande.split(" ", 2);
		if (parties.length < 2)
		{
			this.out.println("<SERVEUR> - Format incorrect. Utilisez : /demute <Pseudo>");
			return;
		}

		String cible = parties[1];
		if (mutes.remove(cible))
		{
			out.println("<SERVEUR> - " + cible + " n'est plus mute");
			return;
		}

		this.out.println("<SERVEUR> - " + parties[1] + " n'etait pas mute");
	}


	private void messageHelp()
	{
		this.out.println("<SERVEUR> - liste des commandes :"                                                    +"\n" +
		                 "/msg <PseudoCible> <Message> : message privé à la personne cible"                     +"\n" +
						 "/rename <NouveauPseudo> : permet de changer de pseudo"                                +"\n" +
						 "/list  : affiche la liste des gens sur le tchat"                                      +"\n" +
						 "/mute <PseudoCible>  : permet de ne plus afficher les messages de la personne cible"  +"\n" +
						 "/demute <PseudoCible> : permet de réafficher les messages de la personne cible"       +"\n" +
						 "/exit : permet de quitte le tchat"                                                    +"\n" +
						 "/h : envoie la liste des commandes"                                                          );

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
					client.out.println("<SERVEUR> - " + pseudo + " a quitté l'étang");
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
}
