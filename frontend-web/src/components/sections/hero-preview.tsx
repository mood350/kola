import { CREDIT_TIERS, DEMO_SCORE } from "@/lib/content";

/**
 * Aperçu de l'espace client, dessiné en HTML.
 *
 * Tient la place de la capture ou de la vidéo produit d'un hero SaaS, sans en
 * avoir les défauts : aucun octet d'image, net sur tout écran, et fidèle aux
 * tokens réels — si la marque change, l'aperçu change avec elle.
 *
 * LES CHIFFRES SONT DES EXEMPLES ET LE DISENT. La légende l'écrit en toutes
 * lettres : un solde affiché sur une page d'accueil sans cette mention se lit
 * comme une promesse. Le score et le palier viennent en revanche de
 * `lib/content.ts`, comme la jauge de la section score — les deux ne peuvent
 * pas se contredire.
 *
 * Le contenu est décoratif pour les technologies d'assistance (`aria-hidden`) :
 * la légende résume ce qu'il montre, sans faire lire une maquette ligne à ligne.
 */
const OPERATIONS = [
  { label: "Dépôt Mobile Money", meta: "Aujourd'hui · 09:14", amount: "+ 25 000", positive: true },
  { label: "Coffre « Rentrée »", meta: "Hier · virement programmé", amount: "− 12 000", positive: false },
  { label: "Boutique Akwaba", meta: "Hier · paiement marchand", amount: "− 3 500", positive: false },
  { label: "Awa K.", meta: "Lun. · transfert reçu", amount: "+ 15 000", positive: true },
];

const RADIUS = 52;
const ARC = Math.PI * RADIUS;

export function HeroPreview() {
  const tier = [...CREDIT_TIERS].reverse().find((t) => DEMO_SCORE >= t.min);

  return (
    <figure className="relative mx-auto max-w-5xl">
      {/* Halo sous la maquette : elle flotte au-dessus de la trame. */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-10 -bottom-6 top-10 rounded-[3rem] bg-kola-500/25 blur-3xl"
      />

      <div className="relative rounded-[1.75rem] border border-white/80 bg-white/55 p-2 shadow-[0_40px_80px_-32px_rgb(16_17_56/0.35)] backdrop-blur-xl">
        <div
          aria-hidden="true"
          className="overflow-hidden rounded-[1.25rem] border border-hairline bg-mist"
        >
          {/* Barre de fenêtre */}
          <div className="flex items-center gap-3 border-b border-hairline bg-surface px-4 py-3">
            <span className="flex gap-1.5">
              <span className="h-2.5 w-2.5 rounded-full bg-ink-200" />
              <span className="h-2.5 w-2.5 rounded-full bg-ink-200" />
              <span className="h-2.5 w-2.5 rounded-full bg-ink-200" />
            </span>
            <span className="mx-auto rounded-full bg-mist px-4 py-1 text-[0.6875rem] text-ink-400">
              kola.africa/mon-compte
            </span>
            <span className="w-10" />
          </div>

          <div className="grid gap-3 p-3 text-left sm:p-4 md:grid-cols-2 lg:grid-cols-[1.15fr_0.85fr_1fr]">
            {/* Solde + coffre */}
            <div className="flex flex-col gap-3">
              <div className="relative overflow-hidden rounded-2xl bg-kola-600 p-5 text-white">
                <div className="pointer-events-none absolute -top-16 -right-10 h-40 w-40 rounded-full bg-kola-400/50 blur-2xl" />
                <p className="relative text-[0.75rem] text-kola-100">Compte courant</p>
                <p className="font-headline relative mt-1 text-[1.875rem] font-semibold tracking-tight tabular-nums">
                  248 500 <span className="text-base font-medium text-kola-200">XOF</span>
                </p>
                <div className="relative mt-5 grid grid-cols-3 gap-2">
                  {["Déposer", "Envoyer", "Payer"].map((action) => (
                    <span
                      key={action}
                      className="rounded-xl bg-white/12 py-2 text-center text-[0.75rem] font-medium"
                    >
                      {action}
                    </span>
                  ))}
                </div>
              </div>

              <div className="rounded-2xl border border-hairline bg-surface p-4">
                <div className="flex items-center justify-between">
                  <p className="text-[0.8125rem] font-medium text-ink-900">
                    Coffre « Rentrée »
                  </p>
                  <span className="rounded-full bg-ochre-200/70 px-2 py-0.5 text-[0.6875rem] font-medium text-ochre-500">
                    64 %
                  </span>
                </div>
                <div className="mt-3 h-2 overflow-hidden rounded-full bg-mist">
                  <div className="h-full w-[64%] rounded-full bg-linear-to-r from-kola-500 to-ochre-400" />
                </div>
                <p className="mt-2 text-[0.75rem] text-ink-400 tabular-nums">
                  96 000 / 150 000 XOF · débloqué le 1er sept.
                </p>
              </div>
            </div>

            {/* Score */}
            <div className="flex flex-col items-center justify-center rounded-2xl border border-hairline bg-surface p-5">
              <p className="self-start text-[0.8125rem] font-medium text-ink-900">
                Score de confiance
              </p>
              <div className="relative mt-3 w-full max-w-[180px]">
                <svg viewBox="0 0 128 72" className="w-full">
                  <defs>
                    <linearGradient id="preview-gauge" x1="0" y1="0" x2="1" y2="0">
                      <stop offset="0%" stopColor="var(--color-kola-500)" />
                      <stop offset="60%" stopColor="#b04fd2" />
                      <stop offset="100%" stopColor="var(--color-ochre-400)" />
                    </linearGradient>
                  </defs>
                  <path
                    d="M 12 66 A 52 52 0 0 1 116 66"
                    fill="none"
                    stroke="var(--color-mist)"
                    strokeWidth="9"
                    strokeLinecap="round"
                  />
                  <path
                    d="M 12 66 A 52 52 0 0 1 116 66"
                    fill="none"
                    stroke="url(#preview-gauge)"
                    strokeWidth="9"
                    strokeLinecap="round"
                    strokeDasharray={ARC}
                    strokeDashoffset={ARC * (1 - DEMO_SCORE / 100)}
                  />
                </svg>
                <p className="font-headline absolute inset-x-0 bottom-0 text-center text-[2.25rem] leading-none font-semibold text-ink-950 tabular-nums">
                  {DEMO_SCORE}
                </p>
              </div>
              <span className="mt-3 rounded-full bg-kola-50 px-3 py-1 text-[0.75rem] font-medium text-kola-700">
                Palier {tier?.name}
              </span>
              <dl className="mt-4 grid w-full grid-cols-2 gap-2 border-t border-hairline pt-3 text-[0.6875rem]">
                <div>
                  <dt className="text-ink-400">Plafond</dt>
                  <dd className="mt-0.5 font-medium text-ink-900 tabular-nums">{tier?.ceiling}</dd>
                </div>
                <div className="text-right">
                  <dt className="text-ink-400">Taux</dt>
                  <dd className="mt-0.5 font-medium text-ink-900 tabular-nums">{tier?.rate}</dd>
                </div>
              </dl>
            </div>

            {/* Opérations — masquées sur petit écran, où deux cartes suffisent. */}
            <div className="hidden rounded-2xl border border-hairline bg-surface p-4 md:col-span-2 md:block lg:col-span-1">
              <div className="flex items-center justify-between">
                <p className="text-[0.8125rem] font-medium text-ink-900">
                  Opérations récentes
                </p>
                <span className="text-[0.6875rem] text-kola-600">Tout voir</span>
              </div>
              <ul className="mt-2 divide-y divide-hairline">
                {OPERATIONS.map((op) => (
                  <li key={op.label} className="flex items-center gap-3 py-2.5">
                    <span
                      className={
                        op.positive
                          ? "flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-positive-50 text-positive-600"
                          : "flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-mist text-ink-500"
                      }
                    >
                      <svg viewBox="0 0 16 16" fill="none" className="h-3.5 w-3.5">
                        <path
                          d={op.positive ? "M8 3v10M4 9l4 4 4-4" : "M8 13V3M4 7l4-4 4 4"}
                          stroke="currentColor"
                          strokeWidth="1.6"
                          strokeLinecap="round"
                          strokeLinejoin="round"
                        />
                      </svg>
                    </span>
                    <span className="min-w-0 flex-1">
                      <span className="block truncate text-[0.8125rem] font-medium text-ink-900">
                        {op.label}
                      </span>
                      <span className="block truncate text-[0.6875rem] text-ink-400">
                        {op.meta}
                      </span>
                    </span>
                    <span
                      className={
                        op.positive
                          ? "text-[0.8125rem] font-semibold text-positive-600 tabular-nums"
                          : "text-[0.8125rem] font-semibold text-ink-900 tabular-nums"
                      }
                    >
                      {op.amount}
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      </div>

      <figcaption className="mt-5 text-center text-[0.8125rem] text-ink-400">
        Aperçu de l&apos;espace client — montants et opérations donnés à titre
        d&apos;exemple.
      </figcaption>
    </figure>
  );
}
