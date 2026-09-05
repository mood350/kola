import { roleRepository } from '../repositories';

export const roleService = {
  getAdmins: () => roleRepository.getAdmins(),
  updatePermissions: (id, changes) => roleRepository.updatePermissions(id, changes),
};
