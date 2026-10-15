import "server-only"

import { cache } from "react";
import { backendFetch } from "@/lib/api/server";

 export type CurrentUser ={
    email:string;
 };

 export const getCurrentUser = cache(async ():Promise<CurrentUser|null> => {

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
 });
