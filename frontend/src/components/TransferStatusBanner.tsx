"use client";

import { Loader2, CheckCircle2, XCircle } from "lucide-react";

type Props = {
  status: "PENDING" | "COMPLETED" | "FAILED";
};

export function TransferStatusBanner({ status }: Props) {
  if (status === "PENDING") {
    return (
      <div className="mb-6 flex items-center gap-2 rounded-xl bg-brand-50 px-4 py-3 text-sm text-brand-700">
        <Loader2 size={16} className="animate-spin" />
        Transfer processing — this settles asynchronously, usually within a couple seconds.
      </div>
    );
  }
  if (status === "COMPLETED") {
    return (
      <div className="mb-6 flex items-center gap-2 rounded-xl bg-green-50 px-4 py-3 text-sm text-good">
        <CheckCircle2 size={16} />
        Transfer completed.
      </div>
    );
  }
  return (
    <div className="mb-6 flex items-center gap-2 rounded-xl bg-red-50 px-4 py-3 text-sm text-bad">
      <XCircle size={16} />
      Transfer failed — insufficient funds or the account could not be reached.
    </div>
  );
}
