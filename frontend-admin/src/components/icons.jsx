import {
  ArrowLeftRight, Calendar, Check, ChevronLeft, ChevronRight, Download, Ellipsis, Eye, HandCoins, House, LogOut, PiggyBank,
  Search, Settings, ShieldCheck, Users, X,
} from 'lucide-react';

// Les icônes de la console, en un seul endroit : taille et trait identiques partout, décoratives
// pour les lecteurs d'écran (le libellé voisin porte le sens). Les écrans importent ces noms,
// jamais la bibliothèque directement : changer de jeu d'icônes se fait ici.

const make = (Glyph) => function Icon({ size = 18, ...props }) {
  return <Glyph size={size} strokeWidth={1.75} aria-hidden="true" focusable="false" {...props} />;
};

export const IconHome = make(House);
export const IconUsers = make(Users);
export const IconShield = make(ShieldCheck);
export const IconArrows = make(ArrowLeftRight);
export const IconCoins = make(HandCoins);
export const IconSavings = make(PiggyBank);
export const IconSettings = make(Settings);
export const IconSearch = make(Search);
export const IconClose = make(X);
export const IconDownload = make(Download);
export const IconEye = make(Eye);
export const IconLogout = make(LogOut);
export const IconMore = make(Ellipsis);
export const IconCheck = make(Check);
export const IconChevron = make(ChevronRight);
export const IconCalendar = make(Calendar);
export const IconPrev = make(ChevronLeft);
