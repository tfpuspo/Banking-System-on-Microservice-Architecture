"use client";

import { CreditCard, Snowflake, XCircle, History } from "lucide-react";

type Props = {
  type: string;
  currency: string;
  balanceMinorUnits: number;
  status: string;
  accountId: string;
  onViewHistory?: (accountId: string) => void;
};

const statusMeta: Record<string, { label: string; icon: React.ReactNode; className: string }> = {
  ACTIVE: { label: "Active", icon: null, className: "bg-white/15 text-white" },
  FROZEN: { label: "Frozen", icon: <Snowflake size={12} />, className: "bg-white/15 text-white" },
  CLOSED: { label: "Closed", icon: <XCircle size={12} />, className: "bg-white/15 text-white" },
};

export function AccountCard({ type, currency, balanceMinorUnits, status, accountId, onViewHistory }: Props) {
  const amount = (balanceMinorUnits / 100).toLocaleString(undefined, {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });
  const meta = statusMeta[status] ?? statusMeta.ACTIVE;
  const last4 = accountId.replace(/-/g, "").slice(-4).toUpperCase();

  return (
    <div className="relative overflow-hidden rounded-2xl bg-card-gradient p-6 text-white shadow-card">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs uppercase tracking-wide text-white/70">{type.toLowerCase()} account</p>
          <p className="mt-1 font-mono text-xs text-white/60">•••• {last4}</p>
        </div>
        <span className={`flex items-center gap-1 rounded-full px-2.5 py-1 text-[11px] font-medium ${meta.className}`}>
          {meta.icon}
          {meta.label}
        </span>
      </div>

      <p className="mt-8 text-3xl font-semibold tabular-nums">
        {currency} {amount}
      </p>

      {onViewHistory && (
        <button
          onClick={() => onViewHistory(accountId)}
          className="relative z-10 mt-4 flex items-center gap-1.5 text-xs text-white/70 hover:text-white"
        >
          <History size={13} />
          View history
        </button>
      )}

      <CreditCard className="absolute -bottom-4 -right-4 text-white/10" size={96} strokeWidth={1} />
    </div>
  );
}
