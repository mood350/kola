package com.kola.backend.wallet;

import com.kola.backend.exception.UnsupportedCurrencyException;
import com.kola.backend.exception.WalletInactiveException;
import com.kola.backend.user.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final EntityManager entityManager;

    /**
     * Kola n'opère qu'en XOF.
     *
     * GHS/NGN/USD/EUR étaient acceptés alors qu'AUCUNE conversion n'existe
     * dans le système : les plafonds KYC (TransactionPolicy), les plafonds de
     * prêt (CreditTier) et les seuils LAB-FT (AmlScreeningService) sont tous
     * exprimés en XOF et étaient appliqués tels quels à un montant en EUR ou
     * en USD. Concrètement, un prêt « 2 000 000 XOF » était crédité comme
     * 2 000 000 EUR sur un wallet EUR, et un plafond journalier de
     * 50 000 XOF laissait passer 50 000 USD.
     *
     * Tant qu'il n'y a pas de service de change (table de taux, devise pivot,
     * conversion aux frontières), une seule devise est la seule option
     * cohérente. Les colonnes receiverCurrency / exchangeRate restent en base
     * pour accueillir cette évolution sans migration.
     */
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("XOF");

    // ═══════════════════════════════════════════════════════════════
    //  LECTURE
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public List<WalletResponse> getMyWallets(User currentUser) {
        return walletRepository.findByOwnerId(currentUser.getId())
                .stream()
                .map(WalletResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public WalletResponse getWalletById(User currentUser, Long walletId) {
        Wallet wallet = findOwnedWalletOrThrow(currentUser, walletId);
        return WalletResponse.fromEntity(wallet);
    }

    // ═══════════════════════════════════════════════════════════════
    //  CRÉATION
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public WalletResponse createWallet(User currentUser, CreateWalletRequest request) {
        String currency = request.currency().toUpperCase();

        if (!SUPPORTED_CURRENCIES.contains(currency)) {
            throw new UnsupportedCurrencyException("La devise '" + currency + "' n'est pas supportée par Kola.");
        }

        if (walletRepository.existsByOwnerIdAndCurrency(currentUser.getId(), currency)) {
            throw new IllegalStateException("Vous possédez déjà un portefeuille en " + currency);
        }

        Wallet wallet = Wallet.builder()
                .currency(currency)
                .balance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO)
                .active(true)
                .owner(currentUser)
                .build();

        return WalletResponse.fromEntity(walletRepository.save(wallet));
    }

    // ═══════════════════════════════════════════════════════════════
    //  HELPERS INTERNES
    // ═══════════════════════════════════════════════════════════════

    @Transactional(readOnly = true)
    public Wallet findOwnedWalletOrThrow(User currentUser, Long walletId) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new EntityNotFoundException("Portefeuille introuvable"));

        if (!wallet.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Ce portefeuille ne vous appartient pas");
        }
        return wallet;
    }

    /**
     * Comme findOwnedWalletOrThrow, mais refuse aussi un portefeuille suspendu.
     * Utilisé pour les contrôles préalables à un verrouillage ordonné, où l'on
     * ne peut pas passer par findOwnedWalletForUpdateOrThrow.
     */
    @Transactional(readOnly = true)
    public Wallet findOwnedActiveWalletOrThrow(User currentUser, Long walletId) {
        Wallet wallet = findOwnedWalletOrThrow(currentUser, walletId);
        if (!wallet.isActive()) {
            throw new WalletInactiveException();
        }
        return wallet;
    }

    /**
     * Portefeuille de réception d'un transfert interne, créé à la volée s'il
     * n'existe pas encore dans la devise envoyée.
     *
     * Le modèle est « un portefeuille par devise » : un destinataire qui ne
     * possède qu'un wallet EUR doit pouvoir recevoir des XOF. Refuser le
     * transfert dans ce cas rendrait la réception dépendante d'une action
     * préalable du destinataire, ce qui n'a pas de sens pour du mobile money.
     * Aucune conversion n'est faite ici : on crédite dans la devise émise
     * (le multi-devises sans FX est une limite connue, cf. TransactionPolicy).
     */
    @Transactional
    public Wallet findOrCreateReceivingWallet(User recipient, String currency) {
        Wallet existing = walletRepository
                .findByOwnerIdAndCurrencyNoLock(recipient.getId(), currency)
                .orElse(null);

        if (existing != null) {
            if (!existing.isActive()) {
                throw new WalletInactiveException(
                        "Le portefeuille du destinataire est suspendu : le transfert est impossible.");
            }
            return existing;
        }

        return walletRepository.save(Wallet.builder()
                .currency(currency)
                .balance(BigDecimal.ZERO)
                .lockedBalance(BigDecimal.ZERO)
                .active(true)
                .owner(recipient)
                .build());
    }

    /**
     * Verrouille plusieurs portefeuilles en une fois, TOUJOURS par id
     * croissant.
     *
     * L'ordre n'est pas cosmétique : sur un transfert interne, deux virements
     * croisés simultanés (A→B et B→A) où chacun verrouillerait d'abord son
     * propre wallet s'attendraient mutuellement — interblocage garanti, et
     * sous PostgreSQL une des deux transactions est tuée au bout du
     * deadlock_timeout. Trier les identifiants impose un ordre global unique
     * d'acquisition, ce qui rend le cycle impossible par construction.
     *
     * Les contrôles d'appartenance/activité doivent être faits AVANT l'appel :
     * on ne pose jamais de verrou sur une ligne qui n'a pas passé les
     * contrôles (même invariant que findOwnedWalletForUpdateOrThrow).
     */
    @Transactional
    public Map<Long, Wallet> lockAllForUpdate(Collection<Long> walletIds) {
        Map<Long, Wallet> locked = new LinkedHashMap<>();
        walletIds.stream()
                .distinct()
                .sorted()
                .forEach(id -> {
                    // Les contrôles préalables (findOwnedActiveWalletOrThrow,
                    // findOrCreateReceivingWallet) ont déjà chargé ce portefeuille SANS
                    // verrou : une instance en est donc gérée par le contexte de
                    // persistance, avec le numéro de version lu AVANT l'attente du
                    // verrou. Or JPA compare cette version à celle de la ligne au
                    // moment d'acquérir un verrou pessimiste sur une entité déjà
                    // gérée : si une transaction concurrente a commité entre-temps,
                    // findByIdForUpdate lève OptimisticLockException *à l'acquisition*
                    // — avant même le moindre UPDATE. C'est ce qui transformait des
                    // virements parfaitement sérialisables en 409.
                    //
                    // Détacher la copie périmée avant de verrouiller fait recharger
                    // l'entité depuis la ligne verrouillée, avec sa version à jour.
                    // Les contrôles d'appartenance/activité restent valides : ils
                    // portent sur des champs qu'un virement concurrent ne modifie pas.
                    Wallet stale = entityManager.find(Wallet.class, id);
                    if (stale != null) {
                        entityManager.detach(stale);
                    }

                    locked.put(id, walletRepository.findByIdForUpdate(id)
                            .orElseThrow(() -> new EntityNotFoundException("Portefeuille introuvable")));
                });
        return locked;
    }

    /**
     * Récupère un wallet AVEC verrou pessimiste de manière 100% sécurisée.
     * Le verrou n'est posé en base de données QUE si le wallet appartient à l'utilisateur.
     */
    @Transactional
    public Wallet findOwnedWalletForUpdateOrThrow(User currentUser, Long walletId) {
        // CORRECTION IDOR : On filtre par owner.id directement dans la requête verrouillée.
        Wallet wallet = walletRepository.findOwnedWalletForUpdate(walletId, currentUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("Portefeuille introuvable")); // Message volontairement vague pour ne pas leak l'existence d'un wallet d'un autre user

        if (!wallet.isActive()) {
            throw new WalletInactiveException();
        }

        return wallet;
    }
}