import "server-only";

import { backendFetch } from "@/lib/api/server";
import type { Product } from "./types";
import type { PagedResponse } from "@/lib/api/types";


export type ProductLookupResult =
  | { status: "success"; product: Product }
  | { status: "unauthorized" }
  | { status: "not-found" };

export async function getProducts(): Promise<PagedResponse<Product> | null> {
  const response = await backendFetch("/api/products");

  if (response.status === 401) {
    return null;
  }

  if (!response.ok) {
    throw new Error(`Products request failed: ${response.status}`);
  }

  return response.json() as Promise<PagedResponse<Product>>;
}

export async function getProduct(productId:string) : Promise<ProductLookupResult>{
    const response = await backendFetch(`/api/products/${encodeURIComponent(productId)}`);
 if (response.status === 401) {
    return { status: "unauthorized" };
  }

  if (response.status === 404) {
    return { status: "not-found" };
  }

   if (!response.ok) {
    throw new Error(`Product request failed: ${response.status}`);
  }

  const product = (await response.json()) as Product;

  return { status: "success", product };
}
