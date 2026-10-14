import "server-only"

import { cookies } from "next/headers";

export async function backendFetch(path:string, options:RequestInit={},):Promise<Response>{

    const cookieStore = await cookies();
    const sessionCookie = cookieStore.get("JSESSIONID");
    const headers = new Headers(options.headers);

    if(sessionCookie !==undefined)
    {
        headers.set("Cookie", `${sessionCookie.name}=${sessionCookie?.value}`);
    };

    const backendUrl = process.env.BACKEND_URL?? "http://localhost:8080";

    return fetch(`${backendUrl}${path}`,{
        ...options, headers, cache:options.cache??"no-store"
    });
}