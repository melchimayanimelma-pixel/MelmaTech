# MelmaTech RH — Plan technique de professionnalisation

## Décisions validées
- Niveau cible : RH professionnel complet.
- Confidentialité des dossiers personnels : responsable RH uniquement.
- Application Android Kotlin / Jetpack Compose existante conservée.
- Supabase Auth, PostgreSQL, RLS et Storage privé conservés.
- Les fonctions déjà présentes ne doivent pas être supprimées ni remplacées par une régression.

## Principes de livraison
1. Développer sur une branche dédiée, jamais directement sur main.
2. Faire des changements additifs et petits, relire les fichiers existants avant chaque modification.
3. Ne pas modifier les rôles, permissions ou politiques RLS sans tests explicites.
4. Ne jamais exposer de clé service_role dans l'APK.
5. Les documents personnels et photos sensibles restent dans le bucket privé rh-prive. Aucun lien public permanent.
6. Chaque phase doit compiler, conserver les flux existants et passer les tests fonctionnels et de sécurité avant fusion.
7. Toute suppression métier doit être précédée d'une confirmation et privilégier l'archivage avec journal d'audit.

## Architecture cible

### Application Android
- Ecrans Compose par domaine (Personnel, Organisation, Temps, Congés, Paie, Recrutement, Formation, Évaluation, Discipline, Rapports).
- Services/repositories dédiés pour isoler les appels HTTP Supabase de l'interface.
- Modèles métier et validations centralisés.
- États de chargement, erreurs, listes vides et rafraîchissement cohérents.
- Export PDF/CSV/XLSX et partage uniquement après contrôle des permissions.

### Backend Supabase
- PostgreSQL multi-entreprise : chaque enregistrement métier porte entreprise_id.
- RLS activée sur toutes les tables exposées et policies basées sur auth.uid(), entreprise_id, rôle actif et permission.
- Storage privé rh-prive : chemin entreprise_id/employe_id/UUID_nom; lecture, écriture et suppression réservées au responsable RH actif de la même entreprise.
- Audit des opérations sensibles (création, modification, archivage, export, consultation/téléchargement si requis).
- Les règles métier critiques (soldes, calculs, validations, anti-doublons) sont contrôlées côté serveur autant que possible.

## Phases de réalisation

### Phase 0 — Stabilisation et protection (prérequis)
- Cartographier les écrans, services, schéma, policies RLS et bucket existants.
- Créer des tests de non-régression pour accès RH, vendeur, comptable et administrateur.
- Vérifier que les données et documents d'une entreprise ne sont jamais visibles par une autre.
- Ajouter des états d'erreur et journalisation sans données personnelles dans les logs.
**Sortie attendue :** socle stable et scénarios de test reproductibles.

### Phase 1 — Dossier employé complet
- Fiche détaillée : identité, contacts, adresse, naissance, matricule, poste, département, embauche, contrat, salaire de base et statut.
- Création, consultation, modification et archivage.
- Validation des champs, unicité du matricule par entreprise, dates cohérentes.
- Photo de profil et pièces justificatives dans Storage privé.
- Historique des changements essentiels.
**Sortie attendue :** cycle de vie complet d'un employé sans perte des champs existants.

### Phase 2 — Organisation
- CRUD départements et postes.
- Affectation employé-département-poste.
- Organigramme et responsables.
- Historique des mutations avec date d'effet et motif.
**Sortie attendue :** structure organisationnelle exploitable et historique fiable.

### Phase 3 — Temps et présences
- Pointage arrivée/départ, unicité employé/jour ou sessions selon règles configurées.
- Retard, absence, permission, justification et correction tracée.
- Horaires, jours ouvrés, heures travaillées et heures supplémentaires.
- Feuilles de présence et exports périodiques.
**Sortie attendue :** registre quotidien fiable et rapports mensuels.

### Phase 4 — Congés et permissions
- Types et règles par entreprise.
- Soldes, acquisition, consommation et report.
- Demandes, approbation/refus, annulation, commentaires et calendrier.
- Prévention des chevauchements et impact sur les présences.
**Sortie attendue :** workflow complet et compteurs cohérents.

### Phase 5 — Paie et rémunération
- Paramètres de paie par entreprise et devise.
- Éléments détaillés : salaire, primes, indemnités, avances, retenues et cotisations paramétrables.
- Brouillon, validation, paiement, annulation contrôlée et historique.
- Bulletin PDF imprimable et registre de paie.
- Réconciliation des montants et droits distincts si un rôle comptable est autorisé.
**Sortie attendue :** paie traçable; les règles légales/fiscales restent configurables et doivent être validées par l'entreprise.

### Phase 6 — Recrutement
- Postes ouverts, candidatures, CV, étapes, entretiens, notes et décision.
- Conversion d'un candidat retenu en dossier employé.
**Sortie attendue :** suivi du besoin de recrutement à l'embauche.

### Phase 7 — Formation et compétences
- Catalogue, sessions, participants, coûts, présence, évaluations et certificats.
- Matrice des compétences par poste.
**Sortie attendue :** historique de formation et suivi des compétences.

### Phase 8 — Évaluations et discipline
- Objectifs, campagnes d'évaluation, entretiens, commentaires et plans d'action.
- Incidents, avertissements, sanctions, pièces justificatives et historique restreint.
**Sortie attendue :** processus documentés avec confidentialité et traçabilité.

### Phase 9 — Rapports et pilotage
- Effectifs, mouvements, absentéisme, congés, masse salariale, recrutement et formation.
- Filtres par période/département; exports PDF/CSV/XLSX.
- Indicateurs calculés depuis des données vérifiées, avec date de génération.
**Sortie attendue :** tableaux de bord fiables et exportables.

### Phase 10 — Notifications et portail employé
- Notifications des demandes et décisions, rappels de fin de contrat et expiration documentaire.
- Portail limité aux informations personnelles explicitement autorisées.
- Les documents confidentiels RH restent invisibles à l'employé, conformément à la décision validée.
**Sortie attendue :** libre-service contrôlé sans élargissement involontaire des accès.

## Matrice d'accès initiale
- Responsable RH : accès complet aux données RH de sa propre entreprise, y compris dossiers et documents privés.
- Employé / vendeur : aucun accès aux dossiers RH confidentiels; toute fonction libre-service future sera limitée aux données explicitement autorisées.
- Comptable : accès paie uniquement si une permission dédiée est explicitement attribuée; aucun accès aux CV, pièces d'identité ou dossiers RH.
- Administrateur / gérant : accès selon permissions métier explicites; pas d'accès automatique aux documents personnels.
- Toute personne : jamais d'accès aux données d'une autre entreprise.

## Tests obligatoires par phase
- Compilation Gradle et tests unitaires.
- Tests de création, lecture, modification, archivage et erreurs réseau.
- Tests RLS avec utilisateurs de rôles différents et deux entreprises distinctes.
- Tests Storage : lecture/téléchargement/ajout/suppression autorisés pour RH; refusés pour vendeur, comptable non autorisé et autre entreprise.
- Tests de non-régression des écrans et flux existants.
- Vérification des migrations sur schéma existant; aucune suppression destructive.
- Contrôle des advisors Supabase après changement de schéma ou policies.

## État de départ constaté
- EcranPersonnel.kt concentre encore une grande partie de l'interface et des appels REST.
- Les modules Employés, Dossiers, Organisation, Présences, Congés et Paie sont déjà amorcés.
- Le schéma contient déjà notamment employes, documents_employes, departements, postes, historique_affectations, presences_employes, demandes_conges et bulletins_paie.
- Le stockage rh-prive et des policies Storage RH existent; ils doivent être testés et renforcés sans les rendre publics.
- La première livraison de code doit prioriser la Phase 0 puis la Phase 1, sans refonte globale de l'écran en une seule opération.

## Critères de mise en production
Une phase n'est terminée que lorsque son interface, sa persistance, ses règles métier, ses permissions, ses erreurs et ses tests sont opérationnels. Une simple présence de bouton ou de table ne constitue pas une fonctionnalité terminée.

## Points de sécurité et de performance relevés pendant l'audit initial (24/11/2026)

Les advisors Supabase ont remonté les éléments suivants. Ils sont enregistrés comme travaux de la Phase 0; aucune policy ni fonction n'a été modifiée pendant cet audit.

- `public.employe_compteurs` a RLS activée sans policy. Vérifier si la table doit être accessible uniquement par une fonction contrôlée; sinon ajouter une policy minimale.
- Les fonctions `SECURITY DEFINER` `generer_matricule_employe()` et `mon_role()` sont exécutables par `anon`; vérifier leur nécessité puis révoquer l'exécution anonyme si non requise.
- Plusieurs fonctions `SECURITY DEFINER` sont exécutables par `authenticated`. Auditer le corps, vérifier `auth.uid()`, l'entreprise et les permissions avant de conserver ou restreindre leurs grants. Ne pas les convertir aveuglément en SECURITY INVOKER.
- Les advisors signalent des policies permissives multiples sur `employes`, `departements`, `postes` et `historique_affectations`. PostgreSQL combine les policies permissives avec OR : vérifier que les policies génériques à permission ne donnent pas plus d'accès que le modèle RH validé.
- Des clés étrangères RH ne sont pas toutes indexées; prioriser les index utiles après examen des requêtes et des volumes.
- Le contrôle anti-mot-de-passe compromis de Supabase Auth est désactivé; évaluer son activation dans les paramètres Auth.

### Tests de sécurité bloquants avant livraison Phase 1
- Un vendeur ne peut ni lire ni modifier les dossiers employés ou leurs documents.
- Un comptable sans permission RH ne peut pas lire les dossiers ni les documents RH.
- Un responsable RH actif ne peut accéder qu'aux données de son entreprise.
- Un utilisateur d'une entreprise A ne peut ni lister, ni télécharger, ni écraser, ni supprimer les fichiers de l'entreprise B.
- Les fonctions RPC sensibles refusent les appels anonymes et valident l'utilisateur appelant.
- Les opérations de mise à jour doivent vérifier la policy SELECT et USING + WITH CHECK afin d'éviter les mises à jour silencieuses ou le changement d'entreprise.


## Journal d'exécution — Phase 0 (24/11/2026)

### Changements appliqués
- Bucket `rh-prive` confirmé privé (`public = false`).
- Limite de stockage du bucket alignée à 12 MiB (12 582 912 octets), identique à la limite déjà vérifiée dans le sélecteur Android.
- Exécution anonyme révoquée sur `public.mon_role()`; exécution authentifiée conservée pour compatibilité avec les appels applicatifs/policies.
- Exécution publique et anonyme révoquée sur `public.generer_matricule_employe()`; exécution authentifiée ensuite révoquée aussi, car la fonction est un trigger `RETURNS trigger`, pas une RPC métier.
- Fichier de migration SQL ajouté au dépôt pour tracer le dernier changement.
- Workflow GitHub Actions configuré pour compiler aussi sur les Pull Requests vers `main`, afin de vérifier les branches de travail avant fusion.
- Policies Storage existantes inspectées : elles vérifient le rôle actif responsable RH, l'entreprise dans le premier dossier et l'identifiant employé dans le deuxième dossier.

### Contrôles effectués
- Après le premier durcissement, l'advisor ne signale plus d'exécution anonyme pour `mon_role()` et `generer_matricule_employe()`.
- Le bucket `rh-prive` est privé et accepte déjà PDF, JPEG, PNG, WebP et documents Word.
- Les politiques RLS de `documents_employes` limitent les opérations au responsable RH actif de la même entreprise.
- Aucune donnée métier n'a été supprimée et aucune policy existante n'a été remplacée.

### Risques restant à traiter — ne pas considérer la Phase 0 terminée
- `public.employe_compteurs` reste RLS-active sans policy, mais ses droits directs ont été révoqués pour `anon` et `authenticated`. La table appartient à `postgres`, n'a pas `FORCE ROW LEVEL SECURITY`, et la fonction trigger `SECURITY DEFINER` reste propriétaire/exécutante côté serveur. Le déclenchement doit encore être validé par le flux réel de création d'employé.
- Les 17 fonctions `SECURITY DEFINER` exposées aux utilisateurs authentifiés doivent être auditées individuellement; certaines sont probablement nécessaires à la gestion des permissions et ne doivent pas être révoquées en bloc.
- Les policies permissives en doublon sur les tables RH doivent être évaluées avec des tests de rôles et des données de deux entreprises. Leur combinaison OR peut élargir l'accès.
- La protection des mots de passe compromis reste désactivée dans Supabase Auth; son activation doit être faite dans la configuration Auth après vérification de l'impact sur les utilisateurs.
- La compilation de la branche doit être confirmée par le workflow déclenché par la Pull Request. Aucune réussite de build n'est déclarée avant résultat GitHub Actions.

### Prochain jalon
Tester le cycle de création d'employé (trigger matricule), les accès RH et non-RH aux documents, ainsi que l'isolation entre deux entreprises. Ensuite seulement, démarrer l'édition complète de la fiche employé.


## Début de la Phase 1 — Fiche employé (premier incrément)

- Ajout d'une action « Modifier » dans la liste des employés.
- Le formulaire de modification reprend les valeurs déjà présentes (nom, postnom, prénom, poste, département, téléphone, e-mail et type de contrat).
- La mise à jour est limitée à l'identifiant employé et à l'entreprise active dans la requête REST; les contrôles RLS existants restent la barrière serveur.
- Le formulaire conserve le matricule et les documents associés; il n'effectue aucune suppression.
- Cet incrément ne représente pas encore la fiche complète : adresse, sexe, date de naissance, date d'embauche, salaire, photo, archivage et historique détaillé restent à implémenter après validation du build et des tests d'accès.
