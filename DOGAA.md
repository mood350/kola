## **DOGAA **

 

L'historique Mobile Money comme passeport pour le crédit 

en Afrique 



1. CONTEXTE ET PROBLÉMATIQUE 

1.1 Le contexte financier en afrique de l’ouest 

L'Afrique de l'Ouest, et particulièrement la zone UEMOA \(Union Économique et 

Monétaire Ouest Africaine\), connaît une révolution silencieuse : celle du Mobile 

Money. Au Togo, au Sénégal, en Côte d'Ivoire ou au Ghana, le taux de 

bancarisation reste historiquement faible \(souvent inférieur à 20%\), tandis que le 

taux de pénétration du téléphone mobile dépasse les 80%. 

Cette réalité a créé une économie "hors des radars" des banques traditionnelles. 

Des millions de transactions commerciales, de paiements de salaires et de 

transferts interpersonnels s'effectuent quotidiennement via des réseaux comme 

Orange Money, MTN, Moov Money ou Wave. 

1.2 La problématique de l'inclusion financière \(Le Crédit\) 

Si le Mobile Money a résolu le problème de l'accès aux services de base \(transferts, 

paiements\), il n'a pas résolu celui du financement. 

Les Très Petites et Moyennes Entreprises \(TPME\) et les travailleurs indépendants, qui 

constituent le moteur de l'économie locale, font face à un mur infranchissable 

lorsqu'ils ont besoin de liquidités ou de capitaux pour se développer : 

Absence d'historique bancaire : Les banques traditionnelles exigent des relevés 

bancaires sur 6 mois, que ces acteurs ne possèdent pas. 

Absence de garanties matérielles : Les systèmes classiques exigent des titres 

fonciers ou des cautions que l'informel ne peut fournir. 

Processus lourds et lents : Même en fournissant des dossiers, l'obtention d'un prêt 

prend des semaines, voire des mois, pour une réponse souvent négative. 

Résultat : une économie réelle, dynamique, mais asphyxiée par le manque d'outils 

de financement adaptés à sa réalité. 

 

1.3. L'opportunité Dogaa 

Dogaa part d'un constat simple : l'activité économique de ces utilisateurs existe déjà, 

elle est juste invisible aux yeux des systèmes de crédit classiques. 

Chaque jour, un commerçant reçoit des paiements Mobile Money, un transporteur 

épargne pour réparer son véhicule, un étudiant reçoit de l'argent de sa famille. 

Dogaa propose de numériser cette activité pour en faire un passeport vers le crédit. 

2. ÉTUDE DE L'EXISTANT 

 

2.1. Les acteurs actuels du marché 

Les Institutions Bancaires Classiques \(Ecobank, BIAO Togo, BICICI\) : Ils ciblent la 

clientèle aisée et les grandes entreprises. Leurs processus d'octroi de crédit sont 

lourds, basés sur des critères inaccessibles à la majorité de la population. 

Les Fintechs de transfert d'argent \(Wave, Orange Money\) : Ils excellent dans les 

paiements Peer-to-Peer \(P2P\). Cependant, leurs modèles économiques se limitent 

souvent à la collecte de frais sur les transactions et n'offrent pas de services de 

crédit ou d'épargne forcée. 

Les institutions de Microfinance classiques : Elles pallient partiellement le manque 

de banques, mais exigent souvent des garanties physiques, des réunions de 

groupe obligatoires \(tontines numérisées\) et des taux d'intérêt parfois prohibitifs 

\(20% à 30%\). 

2.2. Les limites identifiées 

Aucune couche d'épargne structurée : Les solutions actuelles permettent de 

dépenser, mais pas de mettre de l'argent de côté de manière intelligente et 

automatisée. 

Aucune évaluation du risque basée sur les données réelles : Les algorithmes de 

scoring traditionnels s'appuient sur des données que les utilisateurs n'ont pas. 

Friction utilisateur : L'ouverture d'un compte bancaire ou l'obtention d'un 

microcrédit nécessite des déplacements physiques, de la paperasse et un temps 

d'attente considérable. 
3. LA SOLUTION DOGAA : PRÉSENTATION GÉNÉRALE 

 

3.1. Vision 

Dogaa est une plateforme financière hybride \(Portefeuille Mobile Money \+ Épargne 

Programmée \+ Microcrédit Algorithmique\). Son objectif est de créer un 

écosystème fermé où l'utilisation même de l'application construit la crédibilité de 

l'utilisateur. 

3.2. Le principe du "Score de Crédit Alternatif" 

Plutôt que d'exiger un historique bancaire, Dogaa analyse les "signaux faibles" 

générés par l'utilisation quotidienne de l'application : 

La régularité des dépôts \(ex: le compte est approvisionné 3 fois par mois\). 

La discipline d'épargne \(activation de la fonctionnalité de virement programmé\). 

Le volume et la diversité des transactions \(paiement de marchands, transferts 

P2P\). 

La stabilité du solde \(absence de découverts fréquents\). 

Ces données permettent de générer un score sur 100, qui débloque 

automatiquement une ligne de crédit pré-approuvée, avec un taux et un montant 

adaptés au profil réel de l'utilisateur. 

4. SPÉCIFICATION FONCTIONNELLE DÉTAILLÉE 

 

4.1. Module de Portefeuille \(Wallet\) & Transferts 

Multi-devises : Un utilisateur peut créer plusieurs portefeuilles \(XOF, GHS, NGN, USD\). 

Transfert Instantané \(P2P\) : Envoi d'argent vers un autre utilisateur Dogaa ou un 

numéro externe via API Mobile Money. 

Paiement Marchand : Scan d'un QR code ou saisie du numéro pour régler un 

commerçant partenaire. 

Gestion des frais : Calcul dynamique des frais selon le niveau KYC de l'utilisateur 

\(ex: 1.5% pour un transfert, 0% pour un dépôt pour encourager l'alimentation\). 

4.2. Module d'Épargne Programmée \(Vaults\) 

Création d'objectifs : L'utilisateur crée un "Coffre-fort" \(ex: "Réparation camion", 

"Scolarité des enfants"\). 

Verrouillage des fonds : L'argent versé dans le Vault est retiré du "solde disponible" 

et comptabilisé dans le "solde bloqué". Il ne peut plus être dépensé pour des 

transactions courantes. 

Virements Programmés \(Scheduled Transfers\) : 

Fonctionnalité clé : L'utilisateur configure un virement automatique récurrent \(ex: 

"Verser 10 000 XOF dans mon Vault le 5 de chaque mois"\). 

Le système vérifie à minuit si les fonds sont disponibles. Si oui, il exécute le transfert, 

crée une trace transactionnelle, et reprogramme le mois suivant. 

Impact Crédit :\* Cette automatisation prouve aux algorithmes de Dogaa que 

l'utilisateur est discipliné, ce qui augmente mécaniquement son score de crédit. 

4.3. Module de Crédit et Scoring \(Credit Score\) 

Calcul du Score : Exécuté via un job planifié \(Scheduler\). Il prend en compte 

l'historique des 30 derniers jours. 

Tiers de prêt : Selon le score \(0-100\), l'utilisateur débloque des limites différentes. 

Déblocage instantané : Si le score est suffisant, l'utilisateur demande un prêt, et les 

fonds sont versés sur son portefeuille en quelques secondes. 

Remboursement automatisé : Le remboursement \(capital \+ intérêts\) est prélevé 

automatiquement sur le portefeuille à l'échéance. 

4.4. Module de Sécurité et Conformité \(KYC\) 

L'application intègre un système de vérification progressive \(Know Your Customer\) 

pour respecter les réglementations bancaires tout en restant accessible : 

TIER\_0 \(Inscription\) : Vérification par numéro de téléphone. Limite d'envoi très 

stricte \(ex: 50 000 XOF / jour\). 

TIER\_1 \(Email vérifié\) : Augmentation des limites. 

TIER\_2 \(Pièce d'identité\) : Accès aux montants élevés et au crédit. 

TIER\_3 \(Identité validée\) : Accès total et illimité. 

4.5. Module d'Administration \(Back-Office\) 

Un tableau de bord réservé aux administrateurs permettant de : 

Visualiser les métriques clés en temps réel \(Volume de transactions, solde global, 

croissance des utilisateurs\). 

Gérer les litiges et la fraude \(Ex: Annuler une transaction soupçonnée et effectuer 

un chargeback en reversant les fonds entre les comptes impliqués\). 

Débloquer manuellement un compte utilisateur ou forcer la fermeture d'un Vault 

. 

4.6. Module de Transactions Programmées (Scheduled Transactions) 

Au-delà des virements programmés vers les Vaults, nous étendons la 

programmation à l'ensemble des mouvements sortants du portefeuille. 

L'utilisateur peut ainsi planifier à l'avance un transfert P2P, un paiement 

marchand ou le règlement d'une facture, sans avoir à revenir sur l'application au 

moment voulu. 

4.6.1. Types de transactions programmables 

1. Transfert P2P programmé : envoi automatique vers un autre utilisateur Dogaa ou 

un numéro externe, à une date et une heure choisies. 

2. Paiement marchand programmé : règlement différé d'un commerçant 

partenaire (ex: loyer, abonnement, facture récurrente). 

3. Dépôt Vault programmé : cas déjà couvert en 4.2, désormais rattaché au 

même moteur de planification. 

4. Paiement de facture programmé : électricité, eau, forfait télécom, 

remboursement de prêt anticipé. 

4.6.2. Modes de récurrence 

1. Unique : exécution à une date et une heure précises, une seule fois. 

2. Récurrente : quotidienne, hebdomadaire, mensuelle ou sur un jour fixe du 

mois (ex: le 5 de chaque mois), avec une date de fin optionnelle ou un nombre 

d'occurrences défini. 

3. Conditionnelle : exécution uniquement si le solde disponible couvre le 

montant et les frais au moment du déclenchement. 

4.6.3. Fonctionnement technique 

1. Création : l'utilisateur choisit le type de transaction, le bénéficiaire ou le 

marchand, le montant, la devise, la date de première exécution et, le cas 

échéant, la règle de récurrence. 

2. Vérification : un job planifié (Scheduler) s'exécute quotidiennement à minuit 

et contrôle, pour chaque transaction programmée du jour, la disponibilité des 

fonds et le niveau KYC requis. 

3. Exécution : si les conditions sont réunies, la transaction est exécutée, une 

trace transactionnelle est créée, et l'occurrence suivante est reprogrammée si 

la règle est récurrente. 

4. Échec : en cas de solde insuffisant ou de restriction KYC, la transaction est 

marquée en échec, l'utilisateur est notifié, et une nouvelle tentative peut être 

configurée (ex: réessayer une fois 24h plus tard) ou la transaction annulée. 

4.6.4. Gestion par l'utilisateur 

1. Tableau de bord dédié listant les transactions programmées à venir, avec 

statut (active, en pause, échouée, terminée). 

2. Modification ou annulation possible à tout moment avant l'heure 

d'exécution. 

3. Mise en pause temporaire d'une récurrence sans la supprimer 

définitivement. 

4. Notification push et in-app avant chaque exécution (rappel J-1) et après 

chaque exécution (confirmation ou échec). 

4.6.5. Impact sur le Score de Crédit 

Comme pour les virements programmés vers les Vaults, la régularité des 

transactions programmées (paiements de facture honorés à temps, transferts 

récurrents exécutés sans échec) alimente le calcul du score de crédit alternatif 

décrit en section 3.2 : elle démontre une discipline financière et une prévisibilité 

des flux, deux signaux valorisés par l'algorithme de scoring. 

5. IMPACT ATTENDU ET CONCLUSION 

Dogaa ne se contente pas d'être une nouvelle application de paiement. En reliant 

l'épargne programmée à un algorithme de scoring alternatif, Dogaa crée un pont 

entre l'économie informelle et le financement formel. 

L'impact attendu est triple : 

5.1. Pour les PME et travailleurs indépendants \(L'accès au crédit\) : 

Actuellement, au Togo, on estime à plus de 200 000 micro-entreprises et 

travailleurs indépendants opérant de manière formelle ou informelle. Parmi eux, 

moins de 5% ont accès à un prêt bancaire classique. 

En éliminant la barrière de l'historique bancaire, Dogaa pourrait permettre à environ 

40 000 à 60 000 PME et indépendants \(soit 20 à 30% de cette population\) de 

franchir le cap du microcrédit lors des 3 premières années de lancement. Pour un 

commerçant qui a besoin de 500 000 XOF pour acheter un nouveau stock, cette 

différence est souvent celle entre la survie de l'entreprise et sa faillite. 

5.2. Pour l'écosystème économique \(La stimulation locale\) : 

Si l'on considère qu'une PME financée par Dogaa obtient en moyenne un microcrédit 

de 300 000 XOF, le volume de crédit injecté dans l'économie informelle pourrait 

atteindre 12 à 18 milliards de FCFA sur 3 ans. Cet argent, qui dormait sur des 

comptes Mobile Money ou sous des matelas, est désormais injecté dans le circuit 

économique réel \(achat de marchandises, création d'emplois, paiement de taxes\). 

C'est un levier de croissance locale puissant et inédit. 

5.3. Pour la plateforme Dogaa \(Le modèle économique\) 

Le génie de Dogaa réside dans sa capacité à générer des revenus à chaque étape 

du cycle de vie de l'utilisateur, tout en finançant son propre service de crédit de 

manière autonome. L'application monétise trois leviers principaux : 

A. Les commissions sur les transactions \(Le moteur principal\) 

Dogaa prélève des frais sur les mouvements d'argent sortants \(transferts P2P, 

paiements marchands, retraits vers d'autres réseaux Mobile Money\). 

Transferts P2P : Une commission de 1,5% est appliquée \(ex: 1 500 XOF de frais pour 

un transfert de 100 000 XOF\). 

Paiement Marchands : Une commission fixe ou variable est prélevée sur chaque 

transaction commerciale. 

Retrait Cash-out : Des frais de 1% sont appliqués lorsque l'utilisateur sort de 

l'écosystème Dogaa pour retirer de l'argent physique via un opérateur partenaire. 

Les dépôts \(Cash-in\) sont gratuits : C'est une stratégie d'acquisition délibérée pour 

inciter les utilisateurs à alimenter leur portefeuille Dogaa plutôt que celui de la 

concurrence. 

B. Les intérêts sur le Microcrédit \(La marge brute\) 

C'est ici que le modèle devient particulièrement lucratif. Le microcrédit en Afrique 

de l'Ouest est historiquement cher \(souvent entre 15% et 30% par mois dans le 

secteur informel\). Dogaa, grâce à son algorithme de score qui évalue le risque en 

temps réel, peut se permettre de prêter à des taux beaucoup plus compétitifs, tout 

en restant très rentable. 

Exemple concret : Dogaa accorde un prêt de 100 000 XOF sur 30 jours à un TIER\_2 à 

un taux de 8% par mois. L'utilisateur rembourse 108 000 XOF à l'échéance. Dogaa 

gagne 8 000 XOF de marge nette sur cette opération, le tout traité 

automatiquement sans aucun coût de distribution physique. 

C. La capitalisation de l'épargne \(Le levier financier invisible\) 

L'argent bloqué dans les Coffres-forts \(Vaults\) et les Virements Programmés n'est 

pas qu'un service gratuit pour l'utilisateur : c'est un actif financier pour Dogaa. 

Lorsqu'un utilisateur bloque 500 000 XOF dans un Vault, cet argent ne dort pas. Il 

est utilisé par Dogaa comme réserve de liquidité pour financer les prêts des autres 

utilisateurs de la plateforme. 

Dogaa capte l'écart de taux : l'utilisateur ne reçoit pas d'intérêts sur son épargne 

bloquée, et Dogaa prête cet argent à 8% de rendement. C'est un mécanisme de 

transformation de la liquidité très similaire à celui des banques de détail 

traditionnelles. 

D. Le boucle de fidélisation vertueuse 

Plus un utilisateur utilise Dogaa pour payer ou épargner, plus son score de crédit 

augmente. Plus il débloque des prêts avec des taux intéressants plus il génère de 

revenus d'intérêts pour Dogaa plus il est incité à continuer à tout centraliser sur Dogaa. 

En résumé, contrairement aux banques classiques qui dépensent des millions 

dans des agences physiques, Dogaa capture de la valeur à chaque clic. L'application 

démontre qu'en Afrique, l'historique des transactions Mobile Money n'est pas 

seulement un actif financier fiable : c'est aussi une source de revenus dynamique 

et hautement scalable.



