"use client";

import { useEffect, useRef, useState, useSyncExternalStore } from "react";
import { Alert, Button, Modal } from "@/components/ui/primitives";

/**
 * Lecture d'un QR code marchand par la caméra.
 *
 * ═══ POURQUOI CE COMPOSANT PEUT NE PAS S'AFFICHER ═══
 *
 * Il s'appuie sur `BarcodeDetector`, un décodeur fourni par le navigateur.
 * Chromium le propose, Firefox et Safari non — et aucune bibliothèque n'est
 * embarquée pour compenser : décoder un QR en JavaScript coûterait plusieurs
 * centaines de kilooctets, chargés par tout le monde pour une fonction que la
 * plupart des navigateurs de bureau n'utiliseront jamais faute de caméra bien
 * placée. Le bouton n'apparaît donc que là où le décodage est natif, et la
 * saisie manuelle du code reste le chemin principal, disponible partout.
 *
 * ═══ LA CAMÉRA S'ARRÊTE, TOUJOURS ═══
 *
 * Un flux vidéo laissé ouvert garde le voyant de la caméra allumé après la
 * fermeture de la fenêtre. L'arrêt est donc fait à la fermeture ET au
 * démontage, sur le flux réellement obtenu — pas sur une référence copiée qui
 * pourrait déjà avoir été remplacée.
 */

/* `BarcodeDetector` n'est pas dans les types DOM de TypeScript : on décrit ici
   la portion utilisée, plutôt que de neutraliser le contrôle de types par un
   `any` qui masquerait aussi les vraies erreurs. */
type DetectedBarcode = { rawValue: string };
type BarcodeDetectorLike = {
  detect: (source: CanvasImageSource) => Promise<DetectedBarcode[]>;
};
type BarcodeDetectorConstructor = new (options?: {
  formats?: string[];
}) => BarcodeDetectorLike;

function getDetectorConstructor(): BarcodeDetectorConstructor | null {
  if (typeof window === "undefined") return null;
  const candidate = (window as unknown as Record<string, unknown>).BarcodeDetector;
  return typeof candidate === "function"
    ? (candidate as BarcodeDetectorConstructor)
    : null;
}

/* Rien à quoi s'abonner : la présence du décodeur ne change pas en cours de
   session. La fonction est définie au niveau du module pour garder une
   identité stable d'un rendu à l'autre. */
const noopSubscribe = () => () => {};

export function MerchantScanner({ onDetected }: { onDetected: (code: string) => void }) {
  /**
   * Lecture d'une capacité du navigateur, à la façon prévue par React.
   *
   * `useSyncExternalStore` prend deux instantanés : celui du client et celui du
   * serveur. Le serveur répond toujours « non supporté », donc le HTML initial
   * et la première hydratation coïncident, puis React réconcilie avec la vraie
   * valeur. Un `useState` initialisé depuis `window` produirait ici une
   * divergence d'hydratation, et un `setState` dans un effet un rendu en
   * cascade que le compilateur React refuse.
   */
  const supported = useSyncExternalStore(
    noopSubscribe,
    () => getDetectorConstructor() !== null,
    () => false
  );
  const [open, setOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const videoRef = useRef<HTMLVideoElement>(null);
  const streamRef = useRef<MediaStream | null>(null);

  useEffect(() => {
    if (!open) return;

    let cancelled = false;
    let frame = 0;
    const Detector = getDetectorConstructor();
    if (!Detector) return;
    const detector = new Detector({ formats: ["qr_code"] });

    const stop = () => {
      cancelAnimationFrame(frame);
      streamRef.current?.getTracks().forEach((track) => track.stop());
      streamRef.current = null;
    };

    const start = async () => {
      try {
        const stream = await navigator.mediaDevices.getUserMedia({
          /* `environment` demande la caméra arrière sur téléphone — celle qu'on
             pointe vers un QR code posé sur un comptoir. */
          video: { facingMode: "environment" },
        });
        if (cancelled) {
          stream.getTracks().forEach((track) => track.stop());
          return;
        }
        streamRef.current = stream;
        const video = videoRef.current;
        if (!video) return;
        video.srcObject = stream;
        await video.play();

        const scan = async () => {
          if (cancelled || !videoRef.current) return;
          try {
            const codes = await detector.detect(videoRef.current);
            const value = codes[0]?.rawValue?.trim();
            if (value) {
              onDetected(value);
              setOpen(false);
              return;
            }
          } catch {
            /* Une image illisible n'est pas une erreur : on retente à la frame
               suivante plutôt que d'interrompre le balayage. */
          }
          frame = requestAnimationFrame(() => void scan());
        };

        frame = requestAnimationFrame(() => void scan());
      } catch {
        setError(
          "Impossible d'accéder à la caméra. Vérifiez l'autorisation du navigateur, ou saisissez le code du marchand."
        );
      }
    };

    void start();

    return () => {
      cancelled = true;
      stop();
    };
  }, [open, onDetected]);

  if (!supported) return null;

  return (
    <>
      <Button
        type="button"
        variant="secondary"
        full
        onClick={() => {
          setError(null);
          setOpen(true);
        }}
      >
        Scanner un QR code
      </Button>

      <Modal open={open} onClose={() => setOpen(false)} title="Scanner le QR code">
        {error ? (
          <Alert tone="danger">{error}</Alert>
        ) : (
          <>
            <div className="overflow-hidden rounded-field bg-ink-950">
              {/* `muted` et `playsInline` sont indispensables : sans eux, iOS
                  refuse la lecture automatique et affiche un lecteur plein
                  écran à la place de l'aperçu. */}
              <video
                ref={videoRef}
                className="aspect-square w-full object-cover"
                muted
                playsInline
              />
            </div>
            <p className="mt-3 text-sm text-ink-500">
              Cadrez le QR code affiché par le marchand. Le code est lu
              automatiquement.
            </p>
          </>
        )}
      </Modal>
    </>
  );
}
