# Instructions pour le serveur et le tchat

## Lancement du serveur

1. Assurez-vous d'avoir Java installé sur votre machine.
2. Compilez les fichiers source avec la commande :
   ```
   javac Serveur.java GerantDeClient.java
   ```
3. Démarrez le serveur en spécifiant un port :
   ```
   java Serveur <port>
   ```
   Remplacez `<port>` par le numéro de port souhaité (par exemple, `12345`).
4. Le serveur affiche un message indiquant qu'il est en cours d'exécution et qu'il est prêt à accepter des connexions.

## Connexion et utilisation du tchat

1. **Démarrage du client**
   - Compilez le fichier client avec la commande :
     ```
     javac Client.java
     ```
   - Lancez le client en spécifiant le même port et l'adresse du serveur :
     ```
     java Client <adresse> <port>
     ```
     Assurez-vous que le serveur est en cours d'exécution avant de lancer le client.

2. **Connexion au tchat**
   - Lors de la connexion, un nom d'utilisateur unique vous sera demandé.
   - Une fois connecté, vous recevrez un message de bienvenue et vous pourrez commencer à utiliser le tchat.

3. **Commandes disponibles**
   Les commandes suivantes sont accessibles pour interagir avec le tchat :
   - **/msg <PseudoCible> <Message>** : Envoie un message privé à l'utilisateur cible.
   - **/rename <NouveauPseudo>** : Change votre pseudonyme.
   - **/list** : Affiche la liste des utilisateurs connectés.
   - **/mute <PseudoCible>** : Bloque les messages d'un utilisateur.
   - **/demute <PseudoCible>** : Débloque les messages d'un utilisateur précédemment bloqué.
   - **/clear** : Efface votre historique local de messages.
   - **/exit** : Quitte le tchat.
   - **/help** ou **/h** : Affiche la liste des commandes.

4. **Déconnexion**
   - Pour quitter le tchat, utilisez la commande **/exit**.
   - Votre déconnexion sera signalée aux autres utilisateurs.

## Notes importantes

- Le serveur doit être démarré avant tout client.
- Si le port est occupé ou invalide, un message d'erreur sera affiché.
- Chaque utilisateur doit choisir un nom unique lors de la connexion (Spiro est concidéré comme même pseudo que SPIRO).
- Les messages publics sont visibles par tous, sauf pour les utilisateurs bloqués (mute).

