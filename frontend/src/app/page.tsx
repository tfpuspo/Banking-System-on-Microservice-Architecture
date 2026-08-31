"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { Plus, ShieldCheck, ShieldAlert, Send } from "lucide-react";
import { Sidebar } from "@/components/Sidebar";
import { Card } from "@/components/Card";
import { Button } from "@/components/Button";
import { AccountCard } from "@/components/AccountCard";
import { TransferModal } from "@/components/TransferModal";
import { TransferStatusBanner } from "@/components/TransferStatusBanner";
import { TransactionHistoryModal } from "@/components/TransactionHistoryModal";
import { TellerPanel } from "@/components/TellerPanel";
import { graphqlRequest } from "@/lib/graphql";
import { authedGraphqlRequest } from "@/lib/graphqlAuth";
import { getAccessToken } from "@/lib/auth";

const VALIDATE_QUERY = `
  query ValidateToken($accessToken: String!) {
    validateToken(accessToken: $accessToken) { valid userId roles }
  }
`;

const MY_ACCOUNTS_QUERY = `
  query MyAccounts {
    myAccounts { accountId type status currency }
  }
`;

const BALANCE_QUERY = `
  query AccountBalance($accountId: ID!) {
    accountBalance(accountId: $accountId)
  }
`;

const CREATE_ACCOUNT_MUTATION = `
  mutation CreateAccount($type: String!, $currency: String!) {
    createAccount(type: $type, currency: $currency) { accountId }
  }
`;

const TRANSFER_MUTATION = `
  mutation Transfer($fromAccountId: ID!, $toAccountId: ID!, $amountMinorUnits: Int!, $currency: String!) {
    transfer(fromAccountId: $fromAccountId, toAccountId: $toAccountId, amountMinorUnits: $amountMinorUnits, currency: $currency) {
      transactionId
      status
    }
  }
`;

const TRANSACTION_QUERY = `
  query Transaction($transactionId: ID!) {
    transaction(transactionId: $transactionId) { status }
  }
`;

const ACCOUNT_TRANSACTIONS_QUERY = `
  query AccountTransactions($accountId: ID!) {
    accountTransactions(accountId: $accountId) {
      transactionId
      fromAccountId
      toAccountId
      amountMinorUnits
      currency
      status
      createdAt
    }
  }
`;

type Account = {
  accountId: string;
  type: string;
  status: string;
  currency: string;
  balanceMinorUnits: number;
};

type HistoryTransaction = {
  transactionId: string;
  fromAccountId: string | null;
  toAccountId: string | null;
  amountMinorUnits: number;
  currency: string;
  status: string;
  createdAt: string;
};

type SessionStatus = "checking" | "valid" | "invalid";
type TransferStatus = "PENDING" | "COMPLETED" | "FAILED" | null;

export default function DashboardPage() {
  const router = useRouter();
  const [sessionStatus, setSessionStatus] = useState<SessionStatus>("checking");
  const [roles, setRoles] = useState<string[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [accountsLoading, setAccountsLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [showTransfer, setShowTransfer] = useState(false);
  const [transferStatus, setTransferStatus] = useState<TransferStatus>(null);
  const [error, setError] = useState<string | null>(null);

  const [historyAccountId, setHistoryAccountId] = useState<string | null>(null);
  const [historyTransactions, setHistoryTransactions] = useState<HistoryTransaction[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);

  const isStaff = roles.includes("teller") || roles.includes("admin");

  const loadAccounts = useCallback(async () => {
    setAccountsLoading(true);
    try {
      const data = await authedGraphqlRequest<{ myAccounts: Omit<Account, "balanceMinorUnits">[] }>(
        MY_ACCOUNTS_QUERY,
        {}
      );

      const withBalances = await Promise.all(
        data.myAccounts.map(async (a) => {
          const balanceData = await authedGraphqlRequest<{ accountBalance: number }>(BALANCE_QUERY, {
            accountId: a.accountId,
          });
          return { ...a, balanceMinorUnits: balanceData.accountBalance };
        })
      );

      setAccounts(withBalances);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not load accounts.");
    } finally {
      setAccountsLoading(false);
    }
  }, []);

  useEffect(() => {
    const token = getAccessToken();
    if (!token) {
      router.push("/login");
      return;
    }

    graphqlRequest<{ validateToken: { valid: boolean; roles: string[] | null } }>(VALIDATE_QUERY, {
      accessToken: token,
    })
      .then((data) => {
        if (!data.validateToken.valid) {
          setSessionStatus("invalid");
          return;
        }
        setRoles(data.validateToken.roles ?? []);
        setSessionStatus("valid");
        return loadAccounts();
      })
      .catch(() => setSessionStatus("invalid"));
  }, [router, loadAccounts]);

  async function handleCreateAccount() {
    setCreating(true);
    setError(null);
    try {
      await authedGraphqlRequest(CREATE_ACCOUNT_MUTATION, { type: "CHECKING", currency: "USD" });
      await loadAccounts();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not open account.");
    } finally {
      setCreating(false);
    }
  }

  async function handleTransfer(fromAccountId: string, toAccountId: string, amountMinorUnits: number, currency: string) {
    const data = await authedGraphqlRequest<{ transfer: { transactionId: string; status: string } }>(
      TRANSFER_MUTATION,
      { fromAccountId, toAccountId, amountMinorUnits, currency }
    );

    setTransferStatus("PENDING");
    pollTransaction(data.transfer.transactionId);
  }

  function pollTransaction(transactionId: string) {
    let attempts = 0;
    const interval = setInterval(async () => {
      attempts += 1;
      try {
        const data = await authedGraphqlRequest<{ transaction: { status: string } }>(TRANSACTION_QUERY, {
          transactionId,
        });
        if (data.transaction.status !== "PENDING") {
          clearInterval(interval);
          setTransferStatus(data.transaction.status as TransferStatus);
          await loadAccounts();
          setTimeout(() => setTransferStatus(null), 4000);
        }
      } catch {
        // keep polling
      }
      if (attempts > 20) clearInterval(interval);
    }, 1500);
  }

  async function openHistory(accountId: string) {
    setHistoryAccountId(accountId);
    setHistoryLoading(true);
    try {
      const data = await authedGraphqlRequest<{ accountTransactions: HistoryTransaction[] }>(
        ACCOUNT_TRANSACTIONS_QUERY,
        { accountId }
      );
      setHistoryTransactions(data.accountTransactions);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Could not load history.");
    } finally {
      setHistoryLoading(false);
    }
  }

  if (sessionStatus === "checking") {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-sm text-ink-500">Verifying session…</p>
      </div>
    );
  }

  if (sessionStatus === "invalid") {
    return (
      <div className="flex min-h-screen items-center justify-center px-6">
        <Card className="max-w-sm text-center">
          <ShieldAlert className="mx-auto mb-3 text-bad" size={28} />
          <p className="font-medium text-ink-900">Session not valid</p>
          <p className="mt-2 text-sm text-ink-500">
            Your session has expired or was rejected. Sign in again to continue.
          </p>
          <Button className="mt-5 w-full" onClick={() => router.push("/login")}>
            Sign in
          </Button>
        </Card>
      </div>
    );
  }

  return (
    <div className="flex">
      <Sidebar />

      <main className="flex-1 px-10 py-8">
        <div className="mb-8 flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-semibold text-ink-900">Your accounts</h1>
            <div className="mt-1.5 flex items-center gap-1.5 text-sm text-good">
              <ShieldCheck size={15} />
              Session verified · {roles.join(", ") || "no roles"}
            </div>
          </div>
          <div className="flex gap-3">
            {accounts.length > 0 && (
              <Button variant="outline" onClick={() => setShowTransfer(true)}>
                <Send size={16} />
                Send money
              </Button>
            )}
            <Button onClick={handleCreateAccount} disabled={creating}>
              <Plus size={16} />
              {creating ? "Opening…" : "New account"}
            </Button>
          </div>
        </div>

        {isStaff && <TellerPanel />}

        {transferStatus && <TransferStatusBanner status={transferStatus} />}

        {error && <p className="mb-6 rounded-lg bg-red-50 px-3 py-2 text-sm text-bad">{error}</p>}

        {accountsLoading ? (
          <p className="text-sm text-ink-500">Loading accounts…</p>
        ) : accounts.length === 0 ? (
          <Card className="max-w-md">
            <p className="font-medium text-ink-900">No accounts yet</p>
            <p className="mt-1 text-sm text-ink-500">
              Open your first account to get started — it takes one click.
            </p>
            <Button className="mt-4" onClick={handleCreateAccount} disabled={creating}>
              <Plus size={16} />
              {creating ? "Opening…" : "Open account"}
            </Button>
          </Card>
        ) : (
          <div className="grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {accounts.map((a) => (
              <AccountCard
                key={a.accountId}
                type={a.type}
                currency={a.currency}
                balanceMinorUnits={a.balanceMinorUnits}
                status={a.status}
                accountId={a.accountId}
                onViewHistory={openHistory}
              />
            ))}
          </div>
        )}
      </main>

      {showTransfer && (
        <TransferModal accounts={accounts} onClose={() => setShowTransfer(false)} onSubmit={handleTransfer} />
      )}

      {historyAccountId && (
        <TransactionHistoryModal
          accountId={historyAccountId}
          transactions={historyTransactions}
          loading={historyLoading}
          onClose={() => setHistoryAccountId(null)}
        />
      )}
    </div>
  );
}
