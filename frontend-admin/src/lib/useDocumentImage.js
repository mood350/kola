import { useEffect } from 'react';
import { fetchFile } from './api';
import { useResource } from './useResource';

/**
 * Le type réel d'une image, lu dans ses premiers octets. Le type déclaré par le client à l'envoi ne
 * prouve rien : un fichier piégé peut s'annoncer « image/jpeg ». Seuls les formats raster sont montrés,
 * jamais le SVG (qui peut porter du script) ni le PDF (son lecteur s'exécuterait dans l'origine de la console).
 */
export function sniffImage(bytes) {
  const startsWith = (...signature) => signature.every((byte, i) => bytes[i] === byte);
  if (startsWith(0xff, 0xd8, 0xff)) return 'image/jpeg';
  if (startsWith(0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a)) return 'image/png';
  if (startsWith(0x47, 0x49, 0x46, 0x38)) return 'image/gif';
  if (startsWith(0x52, 0x49, 0x46, 0x46) && bytes[8] === 0x57 && bytes[9] === 0x45 && bytes[10] === 0x42 && bytes[11] === 0x50) return 'image/webp';
  return null;
}

/**
 * Charge une pièce KYC pour l'afficher dans la page, dans une balise `<img>` : une image y est dessinée,
 * rien n'y est exécuté. `data` vaut `{ id, url }` (`url` nul quand le format n'est pas une image montrable),
 * et reste nul tant que ce n'est pas la pièce demandée : on ne montre jamais la pièce d'un autre dossier
 * pendant que celle-ci charge.
 */
export function useDocumentImage(documentId) {
  const resource = useResource(async (signal) => {
    const buffer = await fetchFile(`/kyc/documents/${documentId}/file`, signal);
    const type = sniffImage(new Uint8Array(buffer, 0, Math.min(12, buffer.byteLength)));
    return { id: documentId, url: type ? URL.createObjectURL(new Blob([buffer], { type })) : null };
  }, documentId);

  const url = resource.data?.url;
  useEffect(() => () => { if (url) URL.revokeObjectURL(url); }, [url]);

  return { ...resource, data: resource.data?.id === documentId ? resource.data : null };
}
