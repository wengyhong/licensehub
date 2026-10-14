export type LicenseStatus = "ACTIVE" | "SUSPENDED" | "REVOKED";

export type License = {
    id:string;
    productId:string;
    keyId:string;
    customerEmail:string;
    status : LicenseStatus;
    maxActivations:number;
    expiresAt: string| null;
    createdAt: string;
    updatedAt: string;
};