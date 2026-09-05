-- Aligne transactions_type_check sur l'énumération Java TransactionType.
--
-- Pourquoi cette migration existe : le schéma est géré par `spring.jpa.hibernate.ddl-auto=update`.
-- Hibernate 6+ génère une contrainte CHECK pour chaque colonne @Enumerated(EnumType.STRING), mais
-- en mode `update` il ne la RÉÉCRIT jamais. La contrainte créée le jour où la table est née fige
-- donc la liste des valeurs d'alors. SAVINGS_DEPOSIT et SAVINGS_WITHDRAWAL ont été ajoutés à
-- l'enum après coup : l'application les insère, PostgreSQL les refuse (SQLSTATE 23514), la
-- transaction JPA est annulée en bloc et le versement sur l'épargne échoue sans laisser de trace.
--
-- Idempotente et rejouable : elle reconstruit la contrainte à partir de la liste complète, quel
-- que soit son état de départ (absente, partielle, déjà correcte). Elle est rejouée à chaque
-- démarrage via `spring.sql.init.schema-locations`, après Hibernate — la table existe donc
-- toujours quand ces instructions s'exécutent, y compris sur une base neuve.
--
-- CONTRAINTE D'ÉCRITURE : le lecteur de scripts de Spring (ScriptUtils) découpe naïvement sur
-- « ; » et ne connaît pas le dollar-quoting. N'écrivez ici que des instructions simples —
-- pas de bloc DO $$ ... $$, pas de fonction PL/pgSQL : le script serait tronqué au premier
-- point-virgule interne et le démarrage échouerait.
--
-- À MAINTENIR : toute nouvelle valeur ajoutée à com.dogaa.backend.common.enums.TransactionType
-- doit être ajoutée à la liste ci-dessous dans le même commit, sinon le même silence se
-- reproduira sur le type suivant.

ALTER TABLE transactions DROP CONSTRAINT IF EXISTS transactions_type_check;

ALTER TABLE transactions ADD CONSTRAINT transactions_type_check CHECK (
    type IN (
        'CASH_IN',
        'CASH_OUT',
        'P2P_TRANSFER',
        'MERCHANT_PAYMENT',
        'BILL_PAYMENT',
        'VAULT_DEPOSIT',
        'VAULT_WITHDRAWAL',
        'SAVINGS_DEPOSIT',
        'SAVINGS_WITHDRAWAL',
        'LOAN_DISBURSEMENT',
        'LOAN_REPAYMENT',
        'CHARGEBACK'
    )
);

-- La planification possède sa propre colonne enum et donc sa propre contrainte générée par
-- Hibernate. Elle doit elle aussi connaître SAVINGS_DEPOSIT, sinon la création d'une cotisation
-- Bankivi est refusée avant même que le moteur puisse l'exécuter.
ALTER TABLE scheduled_tasks DROP CONSTRAINT IF EXISTS scheduled_tasks_type_check;

ALTER TABLE scheduled_tasks ADD CONSTRAINT scheduled_tasks_type_check CHECK (
    type IN (
        'P2P_TRANSFER',
        'MERCHANT_PAYMENT',
        'VAULT_DEPOSIT',
        'SAVINGS_DEPOSIT',
        'BILL_PAYMENT'
    )
);
