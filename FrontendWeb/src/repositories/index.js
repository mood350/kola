// Composition root — the ONLY file that knows whether the app is talking to
// mock data or a real backend. Services import their repository from here
// and never instantiate Mock*/Http* themselves.
//
// Without VITE_API_BASE_URL (see .env.example) every domain resolves to its
// Mock repository. With it set, a domain switches to HTTP only if it is listed
// in BACKEND_READY below. Every domain is served by the Spring back-office
// today; the list stays because it is what lets a new domain be developed
// against mock data without the others falling back with it.

import { isBackendConfigured } from '../api/httpClient';

import { MockAuthRepository, HttpAuthRepository } from './authRepository';
import { MockDashboardRepository, HttpDashboardRepository } from './dashboardRepository';
import { MockUserRepository, HttpUserRepository } from './userRepository';
import { MockCreditRepository, HttpCreditRepository } from './creditRepository';
import { MockFinanceRepository, HttpFinanceRepository } from './financeRepository';
import { MockDisputeRepository, HttpDisputeRepository } from './disputeRepository';
import { MockConfigRepository, HttpConfigRepository } from './configRepository';
import { MockAuditRepository, HttpAuditRepository } from './auditRepository';
import { MockRoleRepository, HttpRoleRepository } from './roleRepository';
import { MockSupportRepository, HttpSupportRepository } from './supportRepository';

const BACKEND_READY = new Set([
  'auth', 'dashboard', 'users', 'roles', 'audit',
  'credit', 'finance', 'disputes', 'config', 'support',
]);

const backendConfigured = isBackendConfigured();

const pick = (domain, Http, Mock) =>
  backendConfigured && BACKEND_READY.has(domain) ? new Http() : new Mock();

export const authRepository = pick('auth', HttpAuthRepository, MockAuthRepository);
export const dashboardRepository = pick('dashboard', HttpDashboardRepository, MockDashboardRepository);
export const userRepository = pick('users', HttpUserRepository, MockUserRepository);
export const creditRepository = pick('credit', HttpCreditRepository, MockCreditRepository);
export const financeRepository = pick('finance', HttpFinanceRepository, MockFinanceRepository);
export const disputeRepository = pick('disputes', HttpDisputeRepository, MockDisputeRepository);
export const configRepository = pick('config', HttpConfigRepository, MockConfigRepository);
export const auditRepository = pick('audit', HttpAuditRepository, MockAuditRepository);
export const roleRepository = pick('roles', HttpRoleRepository, MockRoleRepository);
export const supportRepository = pick('support', HttpSupportRepository, MockSupportRepository);

/** True while a page still reads fabricated data — the console says so, so nobody acts on it. */
export const domainUsesMockData = (domain) =>
  !(backendConfigured && BACKEND_READY.has(domain));
