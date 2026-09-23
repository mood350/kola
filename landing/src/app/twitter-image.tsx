/**
 * Image de la carte Twitter/X.
 *
 * Elle réutilise intégralement le rendu Open Graph : les deux formats visent le
 * même ratio 1,91:1, et maintenir deux visuels distincts garantirait qu'ils
 * divergent. Les métadonnées `alt`, `size` et `contentType` sont réexportées
 * telles quelles — Next les lit sur ce module, pas sur celui qu'il importe.
 */
export { alt, size, contentType, default } from "./opengraph-image";
