"use client";

import { useState } from "react";
import { X, Send } from "lucide-react";
import { Card } from "./Card";
import { Field } from "./Field";
import { Button } from "./Button";

type Account = { accountId: string; currency: string; type: string };

type Props = {
  accounts: Account[];
  onClose: () => void;
  onSubmit: (fromAccountId: string, toAccountId: string, amountMinorUnits: number, currency: string) => Promise<void>;
};

export function TransferModal({ accounts, onClose, onSubmit }: Props) {
  const [fromAccountId, setFromAccountId] = useState(accounts[0]?.accountId ?? "");
  const [toAccountId, setToAccountId] = useState("");
  const [amount, setAmount] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const fromAccount = accounts.find((a) => a.accountId === fromAccountId);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    const amountMinorUnits = Math.round(parseFloat(amount) * 100);
    if (!amountMinorUnits || amountMinorUnits <= 0) {
      setError("Enter a valid amount.");
      return;
    }
    if (!toAccountId.trim()) {
      setError("Enter a destination account ID.");
      return;
    }

    setSubmitting(true);
    try {
      await onSubmit(fromAccountId, toAccountId.trim(), amountMinorUnits, fromAccount?.currency ?? "USD");
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Transfer failed.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-ink-900/40 px-6">
      <Card className="w-full max-w-sm">
        <div className="mb-5 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-ink-900">Send money</h2>
          <button onClick={onClose} className="text-ink-500 hover:text-ink-900">
            <X size={18} />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <label className="block">
            <span className="mb-1.5 block text-sm font-medium text-ink-700">From account</span>
            <select
              value={fromAccountId}
              onChange={(e) => setFromAccountId(e.target.value)}
              className="w-full rounded-xl border border-ink-100 bg-white px-4 py-3 text-ink-900 outline-none focus:border-brand-500 focus:ring-4 focus:ring-brand-100"
            >
              {accounts.map((a) => (
                <option key={a.accountId} value={a.accountId}>
                  {a.type} · •••• {a.accountId.replace(/-/g, "").slice(-4).toUpperCase()}
                </option>
              ))}
            </select>
          </label>

          <Field
            label="To account ID"
            type="text"
            required
            placeholder="Recipient's account ID"
            value={toAccountId}
            onChange={(e) => setToAccountId(e.target.value)}
          />

          <Field
            label={`Amount (${fromAccount?.currency ?? "USD"})`}
            type="number"
            step="0.01"
            min="0.01"
            required
            placeholder="0.00"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
          />

          {error && <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-bad">{error}</p>}

          <Button type="submit" disabled={submitting} className="mt-2 w-full">
            <Send size={16} />
            {submitting ? "Sending…" : "Send"}
          </Button>
        </form>
      </Card>
    </div>
  );
}
