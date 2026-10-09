import { useState } from 'react';
import { download } from '../lib/api';
import { label } from '../lib/format';
import { useDialogFocus } from '../lib/useDialogFocus';
import { useDocumentImage } from '../lib/useDocumentImage';
import { usePresentation } from '../lib/usePresentation';
import { IconClose, IconDownload } from './icons';
import { ErrorNotice, Skeleton } from './ui';

const altOf = (doc, owner) => `${label('docType', doc.type)}${owner ? ` de ${owner}` : ''}`;

/**
 * La pièce, dans la page : plus besoin de la télécharger pour la voir. Une miniature qui s'agrandit
 * d'un clic. Seules les images sont montrées ; un PDF reste à télécharger (voir `useDocumentImage`).
 */
export function DocumentPreview({ doc, owner }) {
  const image = useDocumentImage(doc.id);
  const [open, setOpen] = useState(false);

  return (
    <div className="doc-preview">
      <ImageState image={image}>
        {(url) => (
          <>
            <button type="button" className="doc-frame doc-open" onClick={() => setOpen(true)} aria-label={`Agrandir : ${altOf(doc, owner)}`}>
              <img src={url} alt={altOf(doc, owner)} />
            </button>
            <p className="muted doc-caption">Cliquez sur l&apos;image pour l&apos;agrandir.</p>
          </>
        )}
      </ImageState>
      {open && <DocumentViewer doc={doc} owner={owner} image={image} onClose={() => setOpen(false)} />}
    </div>
  );
}

/** Ouvre une pièce en grand depuis une liste (la fiche d'un client), sans miniature préalable. */
export function DocumentModal({ doc, owner, onClose }) {
  const image = useDocumentImage(doc.id);
  return <DocumentViewer doc={doc} owner={owner} image={image} onClose={onClose} />;
}

/** Les quatre états d'une pièce qui charge : en cours, en erreur, format non montrable, prête. */
function ImageState({ image, children }) {
  if (image.error) {
    return <ErrorNotice message={image.error} onRetry={image.reload} title="L'aperçu n'a pas pu être chargé." />;
  }
  if (!image.data) {
    return <div className="doc-frame doc-loading"><Skeleton rows={4} /></div>;
  }
  if (!image.data.url) {
    return <p className="muted doc-unsupported">Aperçu indisponible pour ce format de fichier (un PDF, par exemple) : utilisez le téléchargement.</p>;
  }
  return children(image.data.url);
}

/** La pièce en grand : ajustée à l'écran, ou en taille réelle avec défilement. Échap ou « Fermer » la referme. */
export function DocumentViewer({ doc, owner, image, onClose }) {
  const { overlay, sheet, close } = usePresentation({ kind: 'scale', onClosed: onClose });
  useDialogFocus(sheet, close);
  const [actual, setActual] = useState(false);
  const [fileError, setFileError] = useState('');

  const fetchFile = async () => {
    setFileError('');
    try {
      await download(`/kyc/documents/${doc.id}/file`, doc.fileName);
    } catch (e) {
      setFileError(e.message);
    }
  };

  return (
    <div className="overlay viewer-overlay" ref={overlay} onClick={close}>
      <div className="viewer" ref={sheet} role="dialog" aria-modal="true" aria-label={altOf(doc, owner)} onClick={(e) => e.stopPropagation()}>
        <div className="viewer-head">
          <h2>{altOf(doc, owner)}</h2>
          <div className="btn-row end">
            {image.data?.url && (
              <button type="button" className="btn secondary sm" onClick={() => setActual((v) => !v)} aria-pressed={actual}>
                {actual ? 'Ajuster à l\'écran' : 'Taille réelle'}
              </button>
            )}
            <button type="button" className="btn secondary sm" onClick={fetchFile}><IconDownload size={14} /> Télécharger</button>
            <button type="button" className="icon-btn" aria-label="Fermer" onClick={close}><IconClose /></button>
          </div>
        </div>
        {fileError && <ErrorNotice message={fileError} title="Le téléchargement n'a pas abouti." />}
        <div className={`viewer-stage${actual ? ' actual' : ''}`}>
          <ImageState image={image}>
            {(url) => <img src={url} alt={altOf(doc, owner)} onClick={() => setActual((v) => !v)} />}
          </ImageState>
        </div>
      </div>
    </div>
  );
}
