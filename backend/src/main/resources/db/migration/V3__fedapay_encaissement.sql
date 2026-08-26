-- ---------------------------------------------------------------------
-- Encaissement Mobile Money via un prestataire externe (FedaPay).
--
-- Un dépôt passant par un opérateur n'est plus instantané : l'écriture
-- naît en PENDING et n'est créditée qu'à la notification du prestataire.
-- Ces deux colonnes sont ce qui permet de retrouver l'écriture quand la
-- notification arrive — elle ne connaît que ses propres identifiants.
--
-- Les deux sont NULLABLES, et doivent le rester : la très grande majorité
-- des écritures (virements internes, coffres, frais, prêts) n'a aucune
-- contrepartie externe.
-- ---------------------------------------------------------------------
alter table transactions add column provider varchar(20);
alter table transactions add column provider_transaction_id varchar(64);

-- Unicité : une opération du prestataire ne peut créditer qu'une écriture.
-- C'est la garantie de dernier recours contre le rejeu d'un webhook — celle
-- qui tient même si le code se trompe. Sous PostgreSQL, un index unique
-- tolère autant de NULL qu'on veut : les écritures internes ne se gênent pas.
create unique index uk_tx_provider_transaction on transactions (provider_transaction_id);
