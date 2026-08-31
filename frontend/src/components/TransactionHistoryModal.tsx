"use client";

import { X, ArrowDownLeft, ArrowUpRight } from "lucide-react";
import { Card } from "./Card";

type Transaction = {
  transactionId: string;
  fromAccountId: string | null;
  toAccountId: string | null;
  amountMinorUnits: number;
  currency: string;
  status: string;
  createdAt: string;
};

type Props = {
  accountId: string;
  transactions: Transaction[];
  loading: boolean;
  onClose: () => void;
};

const statusColor: Record<string, string> = {
  COMPLETED: "text-good",
  PENDING: "text-brand-600",
  FAILED: "text-bad",
};

export function TransactionHistoryModal({ accountId, transactions, loading, onClose }: Props) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink-900/40 px-6">
      <Card className="w-full max-w-md max-h-[80vh] overflow-hidden flex flex-col">
        <div className="mb-5 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-ink-900">Transaction history</h2>
          <button onClick={onClose} className="text-ink-500 hover:text-ink-900">
            <X size={18} />
          </button>
        </div>

        <div className="overflow-y-auto -mx-6 px-6">
          {loading ? (
            <p className="text-sm text-ink-500">Loading…</p>
          ) : transactions.length === 0 ? (
            <p className="text-sm text-ink-500">No transactions yet.</p>
          ) : (
            <ul className="flex flex-col divide-y divide-ink-100">
              {transactions.map((t) => {
                const isOutgoing = t.fromAccountId === accountId;
                const amount = (t.amountMinorUnits / 100).toLocaleString(undefined, {
                  minimumFractionDigits: 2,
                  maximumFractionDigits: 2,
                });
                return (
                  <li key={t.transactionId} className="flex items-center gap-3 py-3">
                    <div
                      className={`flex h-9 w-9 flex-shrink-0 items-center justify-center rounded-full ${
                        isOutgoing ? "bg-red-50 text-bad" : "bg-green-50 text-good"
                      }`}
                    >
                      {isOutgoing ? <ArrowUpRight size={16} /> : <ArrowDownLeft size={16} />}
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-medium text-ink-900">
                        {isOutgoing ? "Sent" : "Received"}
                      </p>
                      <p className="truncate font-mono text-xs text-ink-500">
                        {new Date(t.createdAt).toLocaleString()}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className="tabular-nums text-sm font-semibold text-ink-900">
                        {isOutgoing ? "-" : "+"}
                        {t.currency} {amount}
                      </p>
                      <p className={`text-xs ${statusColor[t.status] ?? "text-ink-500"}`}>{t.status}</p>
                    </div>
                  </li>
                );
              })}
            </ul>
          )}
        </div>
      </Card>
    </div>
  );
}
