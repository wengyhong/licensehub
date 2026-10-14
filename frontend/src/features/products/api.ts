import { api } from "@/lib/api/client";
import type { Product } from "@/features/products/types";

export type CreateProductRequest = {
  name: string;
  description: string | null;
};

export function createProduct(request: CreateProductRequest): Promise<Product> {
  return api.post<Product>("/api/products", request);
}

export type UpdateProductRequest = {
  name: string;
  description: string | null;
};

export function updateProduct(
  productId: string,
  request: UpdateProductRequest,
): Promise<Product> {
  return api.put<Product>(
    `/api/products/${encodeURIComponent(productId)}`,
    request,
  );
}
export function deleteProduct(productId:string):Promise<void>{
    return api.delete(`/api/products/${encodeURIComponent(productId)}`);
}
