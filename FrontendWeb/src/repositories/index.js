// Composition root — the ONLY file that knows whether the app is talking to
// mock data or a real backend. Services import their repository from here
// and never instantiate Mock*/Http* themselves.
//
// Today VITE_API_BASE_URL is unset, so every domain resolves to its Mock
// repository. The moment it's set (see .env.example), every domain switches
// to the real HTTP repository with no other code change required.

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

const USE_HTTP = isBackendConfigured();

export const authRepository = USE_HTTP ? new HttpAuthRepository() : new MockAuthRepository();
export const dashboardRepository = USE_HTTP ? new HttpDashboardRepository() : new MockDashboardRepository();
export const userRepository = USE_HTTP ? new HttpUserRepository() : new MockUserRepository();
export const creditRepository = USE_HTTP ? new HttpCreditRepository() : new MockCreditRepository();
export const financeRepository = USE_HTTP ? new HttpFinanceRepository() : new MockFinanceRepository();
export const disputeRepository = USE_HTTP ? new HttpDisputeRepository() : new MockDisputeRepository();
export const configRepository = USE_HTTP ? new HttpConfigRepository() : new MockConfigRepository();
export const auditRepository = USE_HTTP ? new HttpAuditRepository() : new MockAuditRepository();
export const roleRepository = USE_HTTP ? new HttpRoleRepository() : new MockRoleRepository();
export const supportRepository = USE_HTTP ? new HttpSupportRepository() : new MockSupportRepository();

export const usingMockData = !USE_HTTP;
