package com.kola.backend.wallet;

import com.kola.backend.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                  WalletController.java                      ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * Toutes les routes sont protégées par défaut (SecurityConfig :
 * anyRequest().authenticated()), donc @AuthenticationPrincipal User
 * est toujours non-null ici — pas besoin de vérifier null.
 *
 * IMPORTANT SÉCURITÉ : aucune route ne prend un "ownerId" en paramètre.
 * L'utilisateur courant (extrait du JWT) est TOUJOURS la seule source de
 * vérité pour savoir "à qui appartient quoi" — ça élimine par construction
 * les failles IDOR de type "je modifie le wallet de quelqu'un d'autre en
 * changeant juste l'ID dans l'URL".
 */
@RestController
@RequestMapping("/api/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping
    public ResponseEntity<List<WalletResponse>> getMyWallets(
            @AuthenticationPrincipal User currentUser
    ) {
        return ResponseEntity.ok(walletService.getMyWallets(currentUser));
    }

    @GetMapping("/{walletId}")
    public ResponseEntity<WalletResponse> getWallet(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long walletId
    ) {
        return ResponseEntity.ok(walletService.getWalletById(currentUser, walletId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WalletResponse createWallet(
            @AuthenticationPrincipal User currentUser,
            @RequestBody @Valid CreateWalletRequest request
    ) {
        return walletService.createWallet(currentUser, request);
    }
}
