"use client"

import { useRouter } from "next/navigation";
import { useState } from "react";
import { deleteProduct } from "../api";
import { getApiErrorMessage } from "@/lib/api/client";

type DeleteProductProps = {
    productId: string,
    productName : string
};

export function DeleteProductButton( {productId, productName} : DeleteProductProps) {
    const router = useRouter();
    const [error, setError] = useState<string | null>(null);
    const [deleting, setDeleting] = useState(false);


    async function handleDelete() {
        const confirmed = window.confirm(
            `Delete "${productName}"? This cannot be undone.`,
        );

        if (!confirmed) {
            return;
        }

        setDeleting(true);
        setError(null);

        try {
            await deleteProduct(productId);
            router.replace("/dashboard/products");
            router.refresh();

        } catch(error) {
            setError( getApiErrorMessage(error,"The product could not be deleted."));
            setDeleting(false);
        }
    }

    return (
    <div className="flex items-center gap-3">
      {error !== null && (
        <span className="text-sm text-red-700" role="alert">
          {error}
        </span>
      )}

      <button
        className="rounded-md border border-slate-300 bg-white px-3 py-2 text-sm font-medium hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
        disabled={deleting}
        onClick={() => void handleDelete()}
        type="button"
      >
        {deleting ? "Deleting" : "Delete"}
      </button>
    </div>
  );
}