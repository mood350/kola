// Stub account directory — no backend yet. Login looks a phone number up here
// and redirects to the matching interface. Password is not checked.

export const DEMO_ACCOUNTS = [
  { phone: '+228 90 12 34 56', name: 'Aïcha Kodjo', role: 'client', dest: '/app/dashboard' },
  { phone: '+228 91 44 22 10', name: 'Kossi Sodji', role: 'client', dest: '/app/dashboard' },
  { phone: '+225 07 88 12 33', name: 'Mariam Bello', role: 'client', dest: '/app/dashboard' },
  { phone: '+228 92 10 55 09', name: 'Yao Tchalla', role: 'client', dest: '/app/dashboard' },
  { phone: '+225 05 60 41 27', name: 'Fatou Adé', role: 'client', dest: '/app/dashboard' },
  { phone: '+225 01 22 90 44', name: 'Jean-Marc N’Guessan', role: 'client', dest: '/app/dashboard' },

  { phone: '+228 70 11 22 33', name: 'Sena Amétépé', role: 'admin', dest: '/admin/dashboard' },
  { phone: '+228 70 22 33 44', name: 'Koffi Messan', role: 'admin', dest: '/admin/dashboard' },
  { phone: '+228 70 33 44 55', name: 'Aya Djobo', role: 'admin', dest: '/admin/dashboard' },
  { phone: '+228 70 44 55 66', name: 'Prisca Lawson', role: 'admin', dest: '/admin/dashboard' },
];

const normalize = (phone) => (phone || '').replace(/[\s.-]/g, '');

export function findAccountByPhone(phone) {
  const target = normalize(phone);
  if (!target) return null;
  return DEMO_ACCOUNTS.find((a) => normalize(a.phone) === target) || null;
}
