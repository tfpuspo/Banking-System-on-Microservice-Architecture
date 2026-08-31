"use client";

import { useState } from "react";
import { Search, Snowflake, XCircle, PlayCircle, ShieldAlert } from "lucide-react";
import { Card } from "./Card";
import { Field } from "./Field";
import { Button } from "./Button";
import { authedGraphqlRequest } from "@/lib/graphqlAuth";

const ACCOUNT_QUERY = `
  query Account($accountId: ID!) {
    account(accountId: $accountId) {
      accountId
      userId
      type
      status
      currency
    }
  }
`;

const UPDATE_STATUS_MUTATION = `
  mutation UpdateAccountStatus($accountId: ID!, $status: String!) {
    updateAccountStatus(accountId: $accountId, status: $status) {
      accountId
      status
    }
  }
`;

type LookedUpAccount = {
  accountId: string;
  userId: string;
  type: string;
  status: string;
  currency: string;
};

/**
 * account-service already enforces that only teller/admin roles can call
 * updateAccountStatus (see AccountGrpcService.updateAccountStatus, Phase 4)
 * — this UI-level check just avoids showing controls a customer would get
 * a PERMISSION_DENIED error from anyway. The real enforcement is server-side.
 */
export function TellerPanel() {
  const [searchId, setSearchId] = useState("");
  const [account, setAccount] = useState<LookedUpAccount | null>(null);
  const [loading, setLoading] = useState(false);
  const [updating, setUpdating] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleSearch(e: React.FormEvent) {
    e.preventDefault();
    if (!searchId.trim()) return;
    setLoading(true);
    setError(null);
    setAccount(null);
    try {
      const data = await authedGraphqlRequest<{ account: LookedUpAccount }>(ACCOUNT_QUERY, {
        accountId: searchId.trim(),
      });
      setAccount(data.account);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Account not found.");
    } finally {
      setLoading(false);
    }
  }

  async function handleStatusChange(status: string) {
    if (!account) return;
    setUpdating(true);
    setError(null);
    try {
      const data = await authedGraphqlRequest<{ updateAccountStatus: LookedUpAccount }>(
        UPDATE_STATUS_MUTATION,
        { accountId: account.accountId, status }
      );
      setAccount({ ...account, status: data.updateAccountStatus.status });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not update status.");
    } finally {
      setUpdating(false);
    }
  }

  return (
    <Card className="mb-8">
      <div className="mb-4 flex items-center gap-2">
        <ShieldAlert size={16} className="text-brand-600" />
        <h2 className="text-sm font-semibold uppercase tracking-wide text-ink-700">Staff tools</h2>
      </div>

      <form onSubmit={handleSearch} className="flex items-end gap-3">
        <div className="flex-1">
          <Field
            label="Look up account by ID"
            type="text"
            placeholder="Account ID"
            value={searchId}
            onChange={(e) => setSearchId(e.target.value)}
          />
        </div>
        <Button type="submit" variant="outline" disabled={loading}>
          <Search size={16} />
          {loading ? "Searching…" : "Search"}
        </Button>
      </form>

      {error && <p className="mt-4 rounded-lg bg-red-50 px-3 py-2 text-sm text-bad">{error}</p>}

      {account && (
        <div className="mt-5 flex items-center justify-between rounded-xl border border-ink-100 p-4">
          <div>
            <p className="font-mono text-sm text-ink-900">{account.accountId}</p>
            <p className="mt-1 text-xs text-ink-500">
              {account.type} · {account.currency} · owner {account.userId.slice(0, 8)}… · status{" "}
              <span className="font-medium text-ink-700">{account.status}</span>
            </p>
          </div>
          <div className="flex gap-2">
            <Button
              variant="outline"
              disabled={updating || account.status === "FROZEN"}
              onClick={() => handleStatusChange("FROZEN")}
            >
              <Snowflake size={14} />
              Freeze
            </Button>
            <Button
              variant="outline"
              disabled={updating || account.status === "ACTIVE"}
              onClick={() => handleStatusChange("ACTIVE")}
            >
              <PlayCircle size={14} />
              Reactivate
            </Button>
            <Button
              variant="outline"
              disabled={updating || account.status === "CLOSED"}
              onClick={() => handleStatusChange("CLOSED")}
            >
              <XCircle size={14} />
              Close
            </Button>
          </div>
        </div>
      )}
    </Card>
  );
}
