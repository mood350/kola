/// Destinations de l'application.
///
/// La navigation est un simple changement d'état plutôt qu'une pile
/// `Navigator` : la barre du bas reste montée en permanence et les écrans se
/// remplacent sous elle, ce qui évite qu'elle soit reconstruite (et qu'elle
/// clignote) à chaque passage d'un onglet à l'autre.
enum KolaRoute {
  home,
  savings,
  vaults,
  vaultDetail,
  scan,
  bills,
  subscriptions,
  subscriptionDetail,
  credit,
  loan,
  loanDetail,
  profile,
  faq,
  kyc,
  transactionHistory,
  scheduled,
}
