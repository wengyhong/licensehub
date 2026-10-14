import { api  } from "@/lib/api/client";



export type LoginRequest={
    email:string,
    password:string
};

export type LoginResponse={
    email:string
};


export async function login(request: LoginRequest,):Promise<LoginResponse>{

    return api.post<LoginResponse>("/api/auth/login", request);

}

export async function logout():Promise<void>{

        return api.post<void>("/api/auth/logout");


}