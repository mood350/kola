// Icônes au trait, dessinées ici : aucune bibliothèque, et elles héritent de la
// couleur du texte (`currentColor`), donc de l'état actif ou survolé.

function Icon({ children, size = 18 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor"
      strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {children}
    </svg>
  );
}

export const IconHome = (p) => (
  <Icon {...p}><rect x="3.5" y="3.5" width="7" height="8" rx="2" /><rect x="13.5" y="3.5" width="7" height="5" rx="2" /><rect x="13.5" y="11.5" width="7" height="9" rx="2" /><rect x="3.5" y="14.5" width="7" height="6" rx="2" /></Icon>
);

export const IconUsers = (p) => (
  <Icon {...p}><circle cx="9" cy="8" r="3.5" /><path d="M2.5 20c.6-3.4 3.2-5.5 6.5-5.5s5.9 2.1 6.5 5.5" /><path d="M16 4.8a3.5 3.5 0 0 1 0 6.4M18.5 14.9c1.6.8 2.7 2.5 3 5.1" /></Icon>
);

export const IconShield = (p) => (
  <Icon {...p}><path d="M12 3.2 19 6v5.4c0 4.4-2.9 8-7 9.4-4.1-1.4-7-5-7-9.4V6l7-2.8Z" /><path d="m9 12.2 2.1 2.1 4-4.2" /></Icon>
);

export const IconArrows = (p) => (
  <Icon {...p}><path d="M4 8h14l-3.5-3.5M20 16H6l3.5 3.5" /></Icon>
);

export const IconCoins = (p) => (
  <Icon {...p}><ellipse cx="9" cy="7.5" rx="5.5" ry="2.8" /><path d="M3.5 7.5v4.2c0 1.5 2.5 2.8 5.5 2.8s5.5-1.3 5.5-2.8V7.5" /><path d="M9.5 17.3c.8.1 1.6.2 2.5.2 3 0 5.5-1.3 5.5-2.8v-4.2" /><path d="M14.3 11.3c2 .3 3.2 1.1 3.2 1.9" /><path d="M11.5 18.5c.8 1.2 2.9 2 5.5 2 3 0 5.5-1.3 5.5-2.8V13" /></Icon>
);

export const IconLogout = (p) => (
  <Icon {...p}><path d="M14 4.5h3.5a2 2 0 0 1 2 2v11a2 2 0 0 1-2 2H14" /><path d="M10 8l-4 4 4 4M6 12h10" /></Icon>
);

export const IconSearch = (p) => (
  <Icon {...p}><circle cx="11" cy="11" r="6.5" /><path d="m20 20-4.2-4.2" /></Icon>
);

export const IconDownload = (p) => (
  <Icon {...p}><path d="M12 4v11M7.5 10.5 12 15l4.5-4.5M5 19.5h14" /></Icon>
);

export const IconFilter = (p) => (
  <Icon {...p}><path d="M4 6h16M7 12h10M10 18h4" /></Icon>
);
