type CsrfTokenResponse = {
    headerName:string;
    parameterName:string;
    token:string;
}


export async function apiFetch<T>(path:string, options: RequestInit = {}):Promise<T> {

    const response = await fetch(path, { ...options, credentials:"include",});

    if(!response.ok)
    {
        throw new Error(`API request failed with status ${response.status}`);
    }

    if(response.status === 204){
        return undefined as T;
    }

    return response.json() as Promise<T>;
}

async function apiFetchWithCsrf<T>(path:string, options:RequestInit,):Promise<T>{
    const csrf = await apiFetch<CsrfTokenResponse>("/api/auth/csrf");
    const headers = new Headers(options.headers);

    headers.set(csrf.headerName, csrf.token);

    return apiFetch<T>(path, {...options, headers,});
}

function jsonMutationOptions(
  method: "POST" | "PUT" | "PATCH",
  body: unknown,
): RequestInit {
  return {
    method,
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
  };
}

export const api = {
  get<T>(path: string): Promise<T> {
    return apiFetch<T>(path);
  },

  post<T>(path: string, body?: unknown): Promise<T> {
    const options =
      body === undefined
        ? { method: "POST" }
        : jsonMutationOptions("POST", body);

    return apiFetchWithCsrf<T>(path, options);
  },

  put<T>(path: string, body: unknown): Promise<T> {
    return apiFetchWithCsrf<T>(
      path,
      jsonMutationOptions("PUT", body),
    );
  },

  patch<T>(path: string, body: unknown): Promise<T> {
    return apiFetchWithCsrf<T>(
      path,
      jsonMutationOptions("PATCH", body),
    );
  },

  delete(path: string): Promise<void> {
    return apiFetchWithCsrf<void>(path, {
      method: "DELETE",
    });
  }
};