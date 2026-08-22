-- =====================================================================
-- V2 — Index manquants et invariants métier
--
-- PostgreSQL n'indexe PAS automatiquement les clés étrangères : toutes
-- les recherches par owner_id / sender_id / user_id faisaient jusqu'ici
-- un parcours séquentiel complet de la table.
--
-- NOTE PROD : sur une base déjà volumineuse, ces `create index` posent
-- un verrou bloquant les écritures le temps de la construction. Il
-- faudra alors les rejouer en `create index concurrently`, dans une
-- migration marquée `-- flyway:executeInTransaction=false` (Postgres
-- interdit CONCURRENTLY dans une transaction). Sur une base vide ou
-- petite, la forme ci-dessous est instantanée.
-- =====================================================================

-- ---------------------------------------------------------------------
-- user_roles : la table n'avait NI clé primaire NI index.
--
-- User.roles est une List (bag) : Hibernate ne pose pas de clé primaire
-- sur la table de jointure d'un bag, et une FK Postgres ne crée pas
-- d'index côté référençant. Comme les rôles sont chargés en EAGER à
-- chaque requête authentifiée (cf. commentaire dans User.java), chaque
-- appel API déclenchait un seq scan de user_roles.
--
-- La PK composite corrige les deux problèmes d'un coup : elle indexe
-- user_id (colonne de tête, donc utilisée par la lecture des rôles) et
-- elle interdit qu'un même rôle soit attribué deux fois au même
-- utilisateur — ce qu'un bag autorise côté Java.
-- ---------------------------------------------------------------------
alter table user_roles add constraint pk_user_roles primary key (user_id, role_id);

-- ---------------------------------------------------------------------
-- wallets : un seul portefeuille par (propriétaire, devise).
--
-- L'invariant existait déjà dans WalletService.createWallet, mais sous
-- forme d'un existsBy... : deux créations simultanées passaient toutes
-- les deux le test et créaient deux portefeuilles dans la même devise.
-- La contrainte rend la règle atomique, et son index sert en prime
-- findByOwnerId + findByOwnerIdAndCurrency (verrou pessimiste posé sur
-- chaque mouvement d'argent).
-- ---------------------------------------------------------------------
alter table wallets add constraint uk_wallet_owner_currency unique (owner_id, currency);

-- ---------------------------------------------------------------------
-- beneficiaries : même raisonnement que les portefeuilles.
-- Remplace le existsByOwnerIdAndPhoneNumberAndNetwork de
-- BeneficiaryService, lui aussi vulnérable aux doubles soumissions.
-- Sert également findByOwnerId (owner_id en tête).
-- ---------------------------------------------------------------------
alter table beneficiaries add constraint uk_benef_owner_phone_network
    unique (owner_id, phone_number, network);

-- ---------------------------------------------------------------------
-- transactions : les deux colonnes les plus sollicitées du ledger.
--
-- sender_id est lu à chaque transaction commitée (dépistage LAB-FT),
-- quatre fois par calcul de score de crédit, et à chaque virement pour
-- les plafonds KYC journaliers. created_at en seconde position sert à
-- la fois le tri décroissant de l'historique et le filtre
-- `created_at >= début du jour` des plafonds.
-- ---------------------------------------------------------------------
create index idx_tx_sender_created   on transactions (sender_id, created_at);
create index idx_tx_receiver_created on transactions (receiver_id, created_at);

-- Doublon : idempotency_key porte déjà une contrainte unique, donc un
-- index unique implicite. Cet index second était payé à chaque écriture
-- du grand livre sans jamais servir.
drop index if exists idx_tx_idempotency_key;

-- ---------------------------------------------------------------------
-- vaults : findByOwnerId / findByOwnerIdAndStatus, plus la FK wallet_id.
-- ---------------------------------------------------------------------
create index idx_vault_owner_status on vaults (owner_id, status);
create index idx_vault_wallet       on vaults (wallet_id);

-- ---------------------------------------------------------------------
-- loan_requests : hasActiveLoan (borrower + status) est évalué à chaque
-- demande de prêt ; findOverdueLoans (status + due_date) est le balayage
-- du batch quotidien de mise en défaut.
-- ---------------------------------------------------------------------
create index idx_loan_borrower_status on loan_requests (borrower_id, status);
create index idx_loan_status_due      on loan_requests (status, due_date);

-- ---------------------------------------------------------------------
-- scheduled_transfers : findDueTransfers parcourait toute la table à
-- chaque réveil du job.
-- ---------------------------------------------------------------------
create index idx_sched_status_next on scheduled_transfers (status, next_execution_date);
create index idx_sched_user        on scheduled_transfers (user_id);

-- ---------------------------------------------------------------------
-- token : findValidToken cherche par (token, token_type).
-- ---------------------------------------------------------------------
create index idx_token_value_type on token (token, token_type);
create index idx_token_user       on token (user_id);

-- ---------------------------------------------------------------------
-- aml_alerts : seule FK restée sans index.
-- ---------------------------------------------------------------------
create index idx_aml_transaction on aml_alerts (transaction_id);

-- ---------------------------------------------------------------------
-- _user : countByCreatedAtBetween, statistiques du dashboard admin.
-- ---------------------------------------------------------------------
create index idx_user_created on _user (created_at);
