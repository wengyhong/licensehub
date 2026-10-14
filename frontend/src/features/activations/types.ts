export type MachineActivation = {
  id: string;
  licenseId: string;
  machineName: string | null;
  activatedAt: string;
  lastSeenAt: string;
  deactivatedAt: string | null;
  active: boolean;
};