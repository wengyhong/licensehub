import "server-only"

import { backendFetch } from "@/lib/api/server";

 export type CurrentUser ={
    email:string;
 };

 export async function getCurrentUser():Promise<CurrentUser|null>{

    const response = await backendFetch("/api/auth/me");

    if(response.status === 401)
    {
        return null;
    }

    if(!response.ok)
    {
        throw new Error(`Current-user request failed: ${response.status}`);
    }

    return response.json() as Promise<CurrentUser>;
 }