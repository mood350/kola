// Page-level access matrix per admin role (see the seeded accounts in
// repositories/authRepository.js for what each role's scope covers).
// 'profile' is deliberately absent — every admin can always reach their own
// account settings regardless of role.
export const ROLE_ACCESS = {
  'Super-admin': ['dashboard', 'users', 'credit', 'finance', 'disputes', 'config', 'audit', 'roles', 'support'],
  'Agent conformité': ['dashboard', 'users', 'disputes', 'audit', 'support'],
  'Analyste crédit': ['dashboard', 'credit', 'finance'],
  'Support': ['dashboard', 'users', 'support'],
};

export function canAccessNav(role, navId) {
  if (navId === 'profile') return true;
  return (ROLE_ACCESS[role] || []).includes(navId);
}
